# Original User Input

먼저 해당 답변들을 taskB-0들의 각각의 피드백 아래 답변: 으로 적어줘

task2에 질문.md만들어주고 

질문으로 EtoE가 그러면 .feature가 세부에 아예 종속되지 않도록 하는게 좋을까?
input과 output 그리고 outbound port로만 판별할 수 있도록 하고, 나머지는 unit test로 밀어넣는것은 어떤가? 

그러면 feature로 잡지 못하는 사안들이 생겨 문제가 생길 것인가? 하지만 feature가 세부에 의존하면 나중에 리팩터링과 같은걸 시행하면 test가 깨지는 사항이 발생할 수 도 있는데 

이는 feature를 생성하는 skill과 연관이 있다. 

해당 내용을 분석하여 질문으로 적어줘


# Checked Context

# 요청 수행 문맥

- `task2/assignments/taskB-0.md`의 실제 열린 질문은 FB-B0-03과 FB-B0-08이다.
- 앞서 작성한 답변 중 테스트 경계 답변을 FB-B0-03 아래, 협업 방식 답변을 FB-B0-08 아래에 `답변:`으로 추가했다.
- 페이지 전체 총평의 질문 3개는 `taskB-0.md`의 개별 피드백이 아니므로 이 파일에 임의로 삽입하지 않았다.
- `task2/질문.md`를 새로 만들었다.

# 확인한 skill 문맥

- `generate-test` v0는 도메인 언어의 feature와 unit 테스트를 split으로 생성하지만 contract feature를 실행 코드와 연결하는 step/fake는 생성하지 않는다.
- `generate-test-v1`은 이미 다음 설계를 fixed로 정했다.
  - boundary feature의 When은 Inbound Port 하나와 1:1이다.
  - Then은 반환, 외부 관찰 도메인 상태, outbound 누적/net 결과만 허용한다.
  - 규칙 계산·상태 전이·불변식은 별도 규칙 예시표로 분리한다.
  - 메커니즘·quirk 특성화는 별도 test-after job으로 미룬다.
- `.codex/skills/generate-test-v1/docs/open-questions/feature-altitude-coupling.md`에는 이 설계의 실증이 아직 열려 있다고 기록되어 있다.
- 기존 `task2/task5-history` 사례에서 feature 텍스트는 유지됐지만 step glue가 setter와 타입 이름에 결합되어 리팩터링 때 깨졌다.

# 질문.md에 반영한 분석

- `.feature` 파일 자체와 E2E 실행을 구분했다. feature는 명세이고 실제 API·DB·외부 시스템까지 연결할 때 E2E가 된다. 포트와 fake만 사용하면 애플리케이션 경계 인수테스트다.
- 종속 0이 아니라 내부 구조 대신 비즈니스 경계 행동에만 종속되는 것을 목표로 재정의했다.
- 명세의 고도와 실행 범위를 별도 축으로 분리했다.
- 세 접근을 비교했다.
  1. 경계 feature + 나머지 unit
  2. feature에 세부 상태·상호작용 포함
  3. boundary feature + 규칙/unit + adapter contract·소수 E2E·사후 특성화
- 접근 3을 유력 가설로 표시하되 결함 주입과 리팩터링 실험 전에는 결론으로 고정하지 않았다.
- feature에서 누락될 수 있는 입력 변환, 배선, 트랜잭션, 보상, 재시도, 중복 효과 결함을 명시했다.
- generate-test 계열 스킬이 생성 규칙과 rubric을 어떤 실험으로 검증해야 하는지 핵심 질문과 하위 질문으로 만들었다.
- 현재 fixed 설계를 새로 채택한다고 쓰지 않고, 실증된 운영 규칙으로 사용할 조건을 묻도록 정정했다.

# 검증

- 두 파일 모두 UTF-8로 읽어 확인했다.
- `git diff --check -- task2/assignments/taskB-0.md task2/질문.md`는 오류 없이 통과했다.
- 기존 사용자 변경은 삭제하거나 덮어쓰지 않았다.
