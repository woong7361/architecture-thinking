#!/usr/bin/env bash
# B-7 실험 v2 — rule 을 사장님 자산 전량으로 강화하고, 설계도 arm 을 추가한다.
#
# 조건 4개:
#   A  무제약            ticket-kata-w1        (기존 실행 재사용 — 조건이 동일하므로 다시 안 돌린다)
#   Bw 약한 rule (v1)    ticket-kata-w2        (기존 실행 = "얼마나 세게 줘야 하나" 대조)
#   B  원칙 전량         ticket-kata-w3        (신규) rules-v2.md
#   C  원칙 + 설계도     ticket-kata-w4        (신규) rules-v2.md + my-design.md
set -u

TASK="C:/Users/PC-220627-03/Desktop/project/task"
LAB="$TASK/b7-lab"
JH="C:/Program Files/Amazon Corretto/jdk17.0.19_10"
GITC="-c user.name=movi -c user.email=unity1@cemware.com"
RUNS="$LAB/runs-v2"

W3="$TASK/ticket-kata-w3"
W4="$TASK/ticket-kata-w4"

say () { echo ""; echo "########## $* ##########"; }

mkprompt () { # $1=대상파일 $2=요구사항문서 $3=설계도여부(yes/no)
  {
    printf '`%s`의 요구사항을 구현해줘.\n\n' "$2"
    printf '`contract.md`에 바깥에서 부르는 방법이 정해져 있어. 거기 적힌 이름과 시그니처는 그대로 지켜야 하고,\n'
    printf '`provided` 패키지와 `src/test/`에 이미 있는 파일들은 고치면 안 돼.\n\n'
    printf '`rules.md`에 설계 규칙이 있어. 그 규칙을 지켜서 구현해줘.\n\n'
    [ "$3" = yes ] && printf '`my-design.md`에 이 유스케이스의 객체 설계도가 있어. 그 설계에 따라 구현해줘.\n\n'
    printf '다 하고 나서 `mvn test`로 인수테스트가 전부 통과하는지 확인해줘.\n'
  } > "$1"
}

run_arm () { # $1=워크스페이스 $2=프롬프트 $3=로그디렉터리
  mkdir -p "$3"; cd "$1" || exit 1
  JAVA_HOME="$JH" claude -p "$(cat "$2")" --model opus \
    --permission-mode bypassPermissions --verbose --output-format stream-json \
    > "$3/stream.jsonl" 2> "$3/stderr.log"
  cd "$1" || exit 1
  JAVA_HOME="$JH" mvn -q clean test > "$3/mvn.log" 2>&1
  echo "  mvn: $(grep -aoE '[0-9]+ Scenarios \([^)]*\)' "$3/mvn.log" | tail -1)"
}

say "0. 워크스페이스 세팅 (w3=원칙전량, w4=원칙+설계도)"
mkdir -p "$RUNS"
for w in "$W3" "$W4"; do
  rm -rf "$w"; mkdir -p "$w"
  cp -r "$LAB/inputs/net/." "$w/"
  cp "$LAB/inputs/spec.md" "$LAB/inputs/contract.md" "$w/"
  cp "$LAB/inputs/rules-v2.md" "$w/rules.md"      # arm 에겐 그냥 rules.md 다
  printf 'target/\n' > "$w/.gitignore"
done
cp "$LAB/inputs/my-design.md" "$W4/"

for w in "$W3" "$W4"; do
  cd "$w" || exit 1
  git init -q -b main; git add -A
  git $GITC commit -q -m "요구사항·계약·인수테스트·기존 인프라"
  echo "  $(basename "$w") scaffold: $(git rev-parse --short HEAD)"
done

say "1. baseline"
mkprompt "$RUNS/w3-baseline-prompt.txt" spec.md no
mkprompt "$RUNS/w4-baseline-prompt.txt" spec.md yes
echo "  프롬프트 차이(w3 vs w4 = 설계도 유무):"; diff "$RUNS/w3-baseline-prompt.txt" "$RUNS/w4-baseline-prompt.txt" | sed 's/^/    /'

echo "[w3 baseline · 원칙 전량]"; run_arm "$W3" "$RUNS/w3-baseline-prompt.txt" "$RUNS/w3/baseline"
cd "$W3" && git add -A && git $GITC commit -q -m "티켓 예매 구현"; echo "  w3 baseline: $(git rev-parse --short HEAD)"

echo "[w4 baseline · 원칙+설계도]"; run_arm "$W4" "$RUNS/w4-baseline-prompt.txt" "$RUNS/w4/baseline"
cd "$W4" && git add -A && git $GITC commit -q -m "티켓 예매 구현"; echo "  w4 baseline: $(git rev-parse --short HEAD)"

apply_change () { # $1=워크스페이스 $2=브랜치 $3=netdir $4=요구사항문서명 $5=scaffold $6=impl $7=설계도여부 $8=로그키
  cd "$1" || exit 1
  git checkout -q main; git checkout -q -b "$2"
  cp -r "$LAB/inputs/$3/." .
  [ "$2" = gateway-swap ] && rm -f src/main/java/com/thinking/ticket/provided/PaymentApi.java
  cp "$LAB/inputs/arm-facing/change-$2.md" "./$4"
  git add -A; git $GITC commit -q -m "$5"
  mkprompt "$RUNS/$8-prompt.txt" "$4" "$7"
  run_arm "$1" "$RUNS/$8-prompt.txt" "$RUNS/$8"
  cd "$1" || exit 1; git add -A; git $GITC commit -q -m "$6"
  echo "  $8: $(git rev-parse --short HEAD)"
}

say "2. 포인트 결제"
apply_change "$W3" point-payment net-c1 change-point-payment.md "포인트 결제 요구사항과 인수테스트 추가" "포인트 결제 구현" no  w3/point-payment
apply_change "$W4" point-payment net-c1 change-point-payment.md "포인트 결제 요구사항과 인수테스트 추가" "포인트 결제 구현" yes w4/point-payment

say "3. 1인 1매 제한"
apply_change "$W3" ticket-limit net-c2 change-ticket-limit.md "1인 1매 제한 요구사항과 인수테스트 추가" "1인 1매 제한 구현" no  w3/ticket-limit
apply_change "$W4" ticket-limit net-c2 change-ticket-limit.md "1인 1매 제한 요구사항과 인수테스트 추가" "1인 1매 제한 구현" yes w4/ticket-limit

say "4. 결제사 교체"
apply_change "$W3" gateway-swap net-c3 change-gateway-swap.md "결제사 교체 요구사항과 인수테스트 추가" "결제사 교체 구현" no  w3/gateway-swap
apply_change "$W4" gateway-swap net-c3 change-gateway-swap.md "결제사 교체 요구사항과 인수테스트 추가" "결제사 교체 구현" yes w4/gateway-swap

say "완료"
for w in "$W3" "$W4"; do cd "$w" && echo "$(basename "$w"):" && git branch --format='  %(refname:short) %(objectname:short)'; done
