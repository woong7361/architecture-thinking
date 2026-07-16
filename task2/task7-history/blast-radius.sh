#!/usr/bin/env bash
# 변경 비용을 '고친 파일 수'가 아니라 '영향이 미치는 범위'로 잰다.
#
# 주 지표: Divergent Change (refactoring-criteria.md 스멜 #7)
#   "한 클래스가 서로 다른 이유로 바뀜(책임 축 여럿)"
#   → 세 변경(결제수단·예약규칙·벤더교체) 중 몇 개가 같은 파일을 여는가.
#     3/3 이면 그 파일은 세 가지 서로 다른 이유로 바뀐다 = SRP 위반의 관찰 가능한 신호.
#
# 보조: 벤더 타입 도달 범위 (provided 를 import 하는 파일 = 벤더가 어디까지 보이나)
#       파일 수 (B-7 제출물이 요구 — 주 지표와 나란히 놓아 무엇을 못 보는지 드러낸다)
set -u
TASK="C:/Users/PC-220627-03/Desktop/project/task"
PS="src/main/java/com/thinking/ticket"
EX=":(exclude)src/main/java/com/thinking/ticket/provided"
CHANGES="point-payment ticket-limit gateway-swap"

WS="${*:-ticket-kata-w1 ticket-kata-w2 ticket-kata-w3 ticket-kata-w4}"

echo "═══════════════ 주 지표: Divergent Change ═══════════════"
echo "각 파일이 '서로 다른 이유' 몇 개로 열리는가 (3 = 세 변경 전부)"
echo ""
for w in $WS; do
  [ -d "$TASK/$w" ] || continue
  cd "$TASK/$w" || continue
  echo "── $w"
  tmp=$(mktemp)
  for ch in $CHANGES; do
    git rev-parse --verify -q "$ch" >/dev/null || continue
    git diff --name-only --diff-filter=M "$ch~1" "$ch" -- $PS "$EX" | while read -r f; do
      echo "${f#$PS/}|$ch"
    done >> "$tmp"
  done
  awk -F'|' '{files[$1]=files[$1]" "$2; n[$1]++}
    END{for(f in n) printf "  %-28s %d/3  %s\n", f, n[f], files[f]}' "$tmp" | sort -k2 -r
  worst=$(awk -F'|' '{n[$1]++} END{m=0; for(f in n) if(n[f]>m) m=n[f]; print m+0}' "$tmp")
  echo "  → 최대 Divergent Change: ${worst}/3"
  rm -f "$tmp"
  echo ""
done

echo "═══════════════ 주 지표 2: 벤더가 정책·도메인까지 새는가 ═══════════════"
echo "provided(내가 통제 못 하는 남의 것)를 import 하는 파일을 계층별로 가른다."
echo "인프라(infra/)와 조립(TicketService)이 벤더를 아는 건 정상 — 그게 그들의 일이다."
echo "정책·도메인이 벤더를 알면 그게 DIP 위반이고, 벤더가 바뀔 때 열리는 자리다."
echo ""
for w in $WS; do
  [ -d "$TASK/$w" ] || continue
  cd "$TASK/$w" || continue
  git checkout -q main 2>/dev/null
  hits=$(grep -rl "import com.thinking.ticket.provided" $PS --include=*.java 2>/dev/null | grep -v "/provided/")
  total=$(echo "$hits" | grep -c . )
  # infra/ 와 계약된 진입점(TicketService=조립)은 벤더를 알아도 되는 자리다.
  leak=$(echo "$hits" | grep -v "/infra/" | grep -v "TicketService.java" | grep -c . )
  echo "── $w (baseline): provided 를 아는 파일 ${total}개 / 그중 정책·도메인 ${leak}개"
  echo "$hits" | grep . | while read -r f; do
    case "$f" in
      */infra/*)        tag="인프라 (정상)" ;;
      *TicketService.java) tag="조립 (계약이 강제 — 정상)" ;;
      *)                tag="★ 정책·도메인 — 벤더가 샘" ;;
    esac
    printf '     %-34s %s\n' "${f#$PS/}" "$tag"
  done
done

echo ""
echo "═══════════════ 보조: 고친 파일 수 (B-7 요구 지표) ═══════════════"
printf '%-14s %-18s %6s %6s %10s\n' "변경" "워크스페이스" "신규" "수정" "라인"
for ch in $CHANGES; do
  for w in $WS; do
    [ -d "$TASK/$w" ] || continue
    cd "$TASK/$w" || continue
    git rev-parse --verify -q "$ch" >/dev/null || continue
    new=$(git diff --numstat --diff-filter=A "$ch~1" "$ch" -- $PS "$EX" | wc -l)
    mod=$(git diff --numstat --diff-filter=M "$ch~1" "$ch" -- $PS "$EX" | wc -l)
    ln=$(git diff --numstat "$ch~1" "$ch" -- $PS "$EX" | awk '{i+=$1;d+=$2} END{printf "+%d/-%d", i+0, d+0}')
    printf '%-14s %-18s %6s %6s %10s\n' "$ch" "$w" "$new" "$mod" "$ln"
  done
done
for w in $WS; do [ -d "$TASK/$w" ] && cd "$TASK/$w" && git checkout -q main 2>/dev/null; done
