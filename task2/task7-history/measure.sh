#!/usr/bin/env bash
# 2차 실험 측정. 모든 수치는 git diff 기반이라 재현 가능하다.
set -u
TASK="C:/Users/PC-220627-03/Desktop/project/task"
PS="src/main/java/com/thinking/ticket"
EX=":(exclude)src/main/java/com/thinking/ticket/provided"

echo "═══════════ baseline 규모 ═══════════"
printf '%-16s %8s %8s %12s %8s\n' "워크스페이스" "파일" "줄" "인터페이스" "테스트"
for w in ticket-kata-w1 ticket-kata-w2; do
  cd "$TASK/$w" || exit 1
  git checkout -q main
  n=$(find $PS -name "*.java" ! -path "*provided*" | wc -l)
  l=$(find $PS -name "*.java" ! -path "*provided*" -exec cat {} + | wc -l)
  i=$(grep -rlE "^(public )?interface " $PS --include=*.java 2>/dev/null | grep -v provided | wc -l)
  t=$(JAVA_HOME="C:/Program Files/Amazon Corretto/jdk17.0.19_10" mvn -q clean test 2>&1 | grep -aoE '[0-9]+ Scenarios \([0-9]+ passed\)' | tail -1)
  printf '%-16s %8s %8s %12s   %s\n' "$w" "$n" "$l" "$i" "$t"
done

echo ""
echo "═══════════ 변경 비용 ═══════════"
printf '%-14s %-16s %6s %6s %10s %10s %s\n' "변경" "워크스페이스" "신규" "수정" "전체" "수정분만" "테스트"
for ch in point-payment ticket-limit gateway-swap; do
  for w in ticket-kata-w1 ticket-kata-w2; do
    cd "$TASK/$w" || exit 1
    git checkout -q "$ch" 2>/dev/null || continue
    new=$(git diff --numstat --diff-filter=A "$ch~1" "$ch" -- $PS "$EX" | wc -l)
    mod=$(git diff --numstat --diff-filter=M "$ch~1" "$ch" -- $PS "$EX" | wc -l)
    all=$(git diff --numstat "$ch~1" "$ch" -- $PS "$EX" | awk '{i+=$1;d+=$2} END{printf "+%d/-%d", i, d}')
    modonly=$(git diff --numstat --diff-filter=M "$ch~1" "$ch" -- $PS "$EX" | awk '{i+=$1;d+=$2} END{printf "+%d/-%d", i+0, d+0}')
    t=$(JAVA_HOME="C:/Program Files/Amazon Corretto/jdk17.0.19_10" mvn -q clean test 2>&1 | grep -aoE '[0-9]+ Scenarios \([0-9]+ passed\)' | tail -1)
    printf '%-14s %-16s %6s %6s %10s %10s %s\n' "$ch" "$w" "$new" "$mod" "$all" "$modonly" "$t"
  done
done

echo ""
echo "═══════════ net 무결성 (수정 금지 파일 변경 수 — 전부 0이어야 정상) ═══════════"
for w in ticket-kata-w1 ticket-kata-w2; do
  for ch in point-payment ticket-limit gateway-swap; do
    cd "$TASK/$w" || exit 1
    git checkout -q "$ch" 2>/dev/null || continue
    n=$(git diff --numstat "$ch~1" "$ch" -- src/test/ pom.xml "$PS/provided" | wc -l)
    echo "  $w / $ch: $n"
  done
done

echo ""
echo "═══════════ 변경이 건드린 파일 목록 ═══════════"
for ch in point-payment ticket-limit gateway-swap; do
  for w in ticket-kata-w1 ticket-kata-w2; do
    cd "$TASK/$w" || exit 1
    git checkout -q "$ch" 2>/dev/null || continue
    echo "── $ch / $w"
    git diff --name-status "$ch~1" "$ch" -- $PS "$EX" | sed "s|$PS/|    |"
  done
done
for w in ticket-kata-w1 ticket-kata-w2; do cd "$TASK/$w" && git checkout -q main; done
