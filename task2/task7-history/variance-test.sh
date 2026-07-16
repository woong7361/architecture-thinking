#!/usr/bin/env bash
# 분산 측정: 1인 1매 제한(두 arm 이 갈린 유일한 변경)을 arm 당 3회 추가 실행한다.
# 전부 깨끗한 baseline 에서 출발한다 — 1차의 라벨 오염과 무관하게 분산을 잰다.
set -u
TASK="C:/Users/PC-220627-03/Desktop/project/task"
LAB="$TASK/b7-lab"
JH="C:/Program Files/Amazon Corretto/jdk17.0.19_10"
GITC="-c user.name=movi -c user.email=unity1@cemware.com"
RUNS="$LAB/runs-variance"
mkdir -p "$RUNS"

mkprompt () { # $1=대상파일 $2=rules여부
  {
    printf '`change-ticket-limit.md`의 요구사항을 구현해줘.\n\n'
    printf '`contract.md`에 바깥에서 부르는 방법이 정해져 있어. 거기 적힌 이름과 시그니처는 그대로 지켜야 하고,\n'
    printf '`provided` 패키지와 `src/test/`에 이미 있는 파일들은 고치면 안 돼.\n\n'
    [ "$2" = yes ] && printf '`rules.md`에 설계 규칙이 있어. 그 규칙을 지켜서 구현해줘.\n\n'
    printf '다 하고 나서 `mvn test`로 인수테스트가 전부 통과하는지 확인해줘.\n'
  } > "$1"
}
mkprompt "$RUNS/prompt-no-rule.txt" no
mkprompt "$RUNS/prompt-with-rule.txt" yes

for pair in "ticket-kata-w1:no:no-rule" "ticket-kata-w2:yes:with-rule"; do
  src="${pair%%:*}"; rest="${pair#*:}"; rules="${rest%%:*}"; label="${rest##*:}"
  for i in 2 3 4; do
    ws="$TASK/${src}-r${i}"
    rm -rf "$ws"; cp -r "$TASK/$src" "$ws"
    cd "$ws" || exit 1
    git checkout -q main
    git branch -q -D ticket-limit point-payment gateway-swap 2>/dev/null
    git checkout -q -b ticket-limit
    cp -r "$LAB/inputs/net-c2/." .
    cp "$LAB/inputs/arm-facing/change-ticket-limit.md" ./change-ticket-limit.md
    git add -A
    git $GITC commit -q -m "1인 1매 제한 요구사항과 인수테스트 추가"
    out="$RUNS/$label/r$i"; mkdir -p "$out"
    JAVA_HOME="$JH" claude -p "$(cat "$RUNS/prompt-${label}.txt")" --model opus \
      --permission-mode bypassPermissions --verbose --output-format stream-json \
      > "$out/stream.jsonl" 2> "$out/stderr.log"
    cd "$ws" || exit 1
    JAVA_HOME="$JH" mvn -q clean test > "$out/mvn.log" 2>&1
    git add -A && git $GITC commit -q -m "1인 1매 제한 구현"
    newf=$(git diff --name-status --diff-filter=A ticket-limit~1 ticket-limit -- src/main/java/com/thinking/ticket ':(exclude)src/main/java/com/thinking/ticket/provided' | wc -l)
    echo "[$label r$i] $(grep -aoE '[0-9]+ Scenarios \([^)]*\)' "$out/mvn.log" | tail -1) | 신규 ${newf}개"
    git diff --name-status ticket-limit~1 ticket-limit -- src/main/java/com/thinking/ticket ':(exclude)src/main/java/com/thinking/ticket/provided' | sed 's|src/main/java/com/thinking/ticket/|    |'
  done
done
echo "=== 분산 측정 완료 ==="
