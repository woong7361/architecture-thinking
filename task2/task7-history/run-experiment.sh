#!/usr/bin/env bash
# B-7 비교 실험 2차 — 오염 채널을 막고 전부 다시 실행한다.
#
# 1차와 달라진 것:
#   - 워크스페이스 이름에서 실험 라벨 제거 (arm-a → ticket-kata-w1)
#   - 커밋 메시지에서 실험 라벨 제거 (Claude Code 가 최근 커밋을 시스템 프롬프트에 주입한다)
#   - 브랜치 이름 중립화 (c1 → point-payment)
#   - baseline 도 재실행 (덤으로 2번째 표본이 된다)
#   - C3(결제사 교체) 추가 — arm B 의 포트가 실제로 끊은 축
set -u

LAB="C:/Users/PC-220627-03/Desktop/project/task/b7-lab"
TASK="C:/Users/PC-220627-03/Desktop/project/task"
JH="C:/Program Files/Amazon Corretto/jdk17.0.19_10"
GITC="-c user.name=movi -c user.email=unity1@cemware.com"
RUNS="$LAB/runs2"

W1="$TASK/ticket-kata-w1"   # 무제약
W2="$TASK/ticket-kata-w2"   # rule 제시

say () { echo ""; echo "########## $* ##########"; }

# --- 프롬프트 생성: 독립변수는 rules 한 문단뿐 ---
mkprompt () { # $1=대상파일  $2=요구사항문서  $3=rules여부(yes/no)
  {
    printf '`%s`의 요구사항을 구현해줘.\n\n' "$2"
    printf '`contract.md`에 바깥에서 부르는 방법이 정해져 있어. 거기 적힌 이름과 시그니처는 그대로 지켜야 하고,\n'
    printf '`provided` 패키지와 `src/test/`에 이미 있는 파일들은 고치면 안 돼.\n\n'
    [ "$3" = yes ] && printf '`rules.md`에 설계 규칙이 있어. 그 규칙을 지켜서 구현해줘.\n\n'
    printf '다 하고 나서 `mvn test`로 인수테스트가 전부 통과하는지 확인해줘.\n'
  } > "$1"
}

# --- arm 1회 실행 ---
run_arm () { # $1=워크스페이스  $2=프롬프트파일  $3=로그디렉터리
  mkdir -p "$3"
  cd "$1" || exit 1
  JAVA_HOME="$JH" claude -p "$(cat "$2")" --model opus \
    --permission-mode bypassPermissions --verbose --output-format stream-json \
    > "$3/stream.jsonl" 2> "$3/stderr.log"
  cd "$1" || exit 1
  JAVA_HOME="$JH" mvn -q clean test > "$3/mvn.log" 2>&1
  echo "  mvn: $(grep -aoE '[0-9]+ Scenarios \([^)]*\)' "$3/mvn.log" | tail -1)"
}

# ============ 0. 워크스페이스 세팅 ============
say "0. 중립 워크스페이스 세팅"
rm -rf "$W1" "$W2"
mkdir -p "$W1" "$W2" "$RUNS"
for w in "$W1" "$W2"; do
  cp -r "$LAB/inputs/net/." "$w/"
  cp "$LAB/inputs/spec.md" "$LAB/inputs/contract.md" "$w/"
  printf 'target/\n' > "$w/.gitignore"
done
cp "$LAB/inputs/rules.md" "$W2/"

for w in "$W1" "$W2"; do
  cd "$w" || exit 1
  git init -q -b main
  git add -A
  git $GITC commit -q -m "요구사항·계약·인수테스트·기존 인프라"
  echo "  $(basename "$w") scaffold: $(git rev-parse --short HEAD)"
done

# ============ 1. baseline ============
say "1. baseline 실행"
mkprompt "$RUNS/w1-baseline-prompt.txt" spec.md no
mkprompt "$RUNS/w2-baseline-prompt.txt" spec.md yes
diff "$RUNS/w1-baseline-prompt.txt" "$RUNS/w2-baseline-prompt.txt" > "$RUNS/prompt-diff.txt"
echo "  프롬프트 차이(독립변수):"; sed 's/^/    /' "$RUNS/prompt-diff.txt"

echo "[w1 baseline]"; run_arm "$W1" "$RUNS/w1-baseline-prompt.txt" "$RUNS/w1/baseline"
cd "$W1" && git add -A && git $GITC commit -q -m "티켓 예매 구현"
echo "  w1 baseline: $(git rev-parse --short HEAD)"

echo "[w2 baseline]"; run_arm "$W2" "$RUNS/w2-baseline-prompt.txt" "$RUNS/w2/baseline"
cd "$W2" && git add -A && git $GITC commit -q -m "티켓 예매 구현"
echo "  w2 baseline: $(git rev-parse --short HEAD)"

# ============ 2. 변경 3종 (각각 baseline 에서 독립 분기) ============
apply_change () { # $1=워크스페이스 $2=브랜치 $3=netdir $4=요구사항문서명 $5=scaffold메시지 $6=impl메시지 $7=rules여부 $8=로그키
  cd "$1" || exit 1
  git checkout -q main
  git checkout -q -b "$2"
  cp -r "$LAB/inputs/$3/." .
  [ "$2" = gateway-swap ] && rm -f src/main/java/com/thinking/ticket/provided/PaymentApi.java
  cp "$LAB/inputs/arm-facing/change-$2.md" "./$4"
  git add -A
  git $GITC commit -q -m "$5"
  mkprompt "$RUNS/$8-prompt.txt" "$4" "$7"
  run_arm "$1" "$RUNS/$8-prompt.txt" "$RUNS/$8"
  cd "$1" || exit 1
  git add -A
  git $GITC commit -q -m "$6"
  echo "  $8: $(git rev-parse --short HEAD)"
}

say "2. 포인트 결제"
apply_change "$W1" point-payment net-c1 change-point-payment.md "포인트 결제 요구사항과 인수테스트 추가" "포인트 결제 구현" no w1/point-payment
apply_change "$W2" point-payment net-c1 change-point-payment.md "포인트 결제 요구사항과 인수테스트 추가" "포인트 결제 구현" yes w2/point-payment

say "3. 1인 1매 제한"
apply_change "$W1" ticket-limit net-c2 change-ticket-limit.md "1인 1매 제한 요구사항과 인수테스트 추가" "1인 1매 제한 구현" no w1/ticket-limit
apply_change "$W2" ticket-limit net-c2 change-ticket-limit.md "1인 1매 제한 요구사항과 인수테스트 추가" "1인 1매 제한 구현" yes w2/ticket-limit

say "4. 결제사 교체"
apply_change "$W1" gateway-swap net-c3 change-gateway-swap.md "결제사 교체 요구사항과 인수테스트 추가" "결제사 교체 구현" no w1/gateway-swap
apply_change "$W2" gateway-swap net-c3 change-gateway-swap.md "결제사 교체 요구사항과 인수테스트 추가" "결제사 교체 구현" yes w2/gateway-swap

say "완료"
for w in "$W1" "$W2"; do
  cd "$w" || exit 1
  echo "$(basename "$w") 브랜치:"; git branch --format='  %(refname:short) %(objectname:short)'
done
