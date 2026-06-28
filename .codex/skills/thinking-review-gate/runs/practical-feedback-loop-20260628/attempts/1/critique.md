## 문제 지점

- [근거 anchor 부족] 초안은 `SKILL.md`, `critique.py`, `validate.py`, `rubric.yaml`의 존재를 전제로 설계를 제안하지만, 어떤 현재 사실에서 어떤 제안이 나온 것인지 파일 경로 수준으로 충분히 연결하지 않는다. 예를 들어 "run들을 집계하는 collector는 없다"는 중요한 전제인데 초안 본문에는 근거 파일과 확인 위치가 드러나지 않는다.

- [사실/제안 구분 부족] 현재 시스템에 이미 있는 것과 새로 추가해야 하는 것이 한 문단 안에 섞여 있다. `validation.json`, `weak_axes`, `weighted_score`는 기존 산출물이고, `collect.py`, `reports/summary.json`, `propose_improvements.py`, `proposals/{date}/proposal.md`는 제안 사항인데 이 구분이 명시적이지 않아 사용자가 구현 범위를 검증하기 어렵다.

- [경로 범위 모호함] `scripts/collect.py`, `runs`, `reports` 같은 경로가 상대 경로로만 적혀 있다. 현재 문맥상 `.codex/skills/thinking-review-gate/scripts/collect.py`, `.codex/skills/thinking-review-gate/runs`, `.codex/skills/thinking-review-gate/reports`를 의미하는 것으로 보이지만 초안만 보면 프로젝트 루트의 `scripts/`인지 skill 내부의 `scripts/`인지 혼동될 수 있다.

- [읽기 전용 표현의 모순 가능성] collector를 "기본은 읽기 전용"이라고 설명하면서 동시에 `reports/summary.json`과 `reports/trend.md`를 출력한다고 말한다. 의도는 "run artifact는 읽기 전용으로 다루고 report만 생성한다"로 보이지만, 현재 표현은 실제 파일 쓰기 동작과 충돌해 보일 수 있다.

- [실전 운영 조건 부족] 사용자는 "실전에서 쓸 수 있게"를 요구했는데, 초안은 큰 방향은 제시하지만 운영 주기, 실행 시점, 실패 처리, CI/hook 연결 여부, report 보존 정책, 기준 변경 승인 절차의 최소 상태 모델이 부족하다. 특히 느린 개선 루프가 언제 실행되고 어떤 조건에서 proposal을 만들지 일부만 제시되어 있다.

- [대안과 trade-off가 압축됨] collector만, collector+proposal, 자동 patch까지의 선택지는 언급하지만 각 대안의 장단점이 명확한 비교 구조로 분리되어 있지 않다. 추천안은 보이지만, 왜 실전 초기에는 자동 apply를 미뤄야 하는지와 언제 자동 apply로 확장할 수 있는지가 더 분명해야 한다.

- [검증 계획 구체성 부족] 완성 기준은 있지만 테스트 방법이 부족하다. 예를 들어 샘플 run, 깨진 artifact, fail/pass/borderline run을 어떤 fixture로 만들고 어떤 명령으로 검증할지, 기존 `validate.py` 결과와 collector 결과를 어떻게 대조할지 제시되지 않는다.

## 확인 필요

- collector와 proposal 스크립트를 skill 내부 도구로 둘지, 프로젝트 루트의 일반 운영 도구로 둘지 확인이 필요하다.

- report 생성이 기존 report를 덮어쓰는 방식인지, 날짜별 디렉터리에 append-only로 남기는 방식인지 결정이 필요하다.

- 느린 개선 루프를 수동 실행으로 시작할지, git hook/CI/정기 작업 후보까지 설계 범위에 포함할지 확인이 필요하다.

- proposal이 patch 파일까지 생성해야 하는지, 아니면 `proposal.md`와 `proposal.json`만 생성해야 하는지 확정이 필요하다.

## 수정 제안

- 답변을 `현재 확인된 사실`, `추천 구조`, `대안 비교`, `구현 순서`, `검증 기준`으로 나누어라. 현재 사실에는 `input.md`에 있는 checked context를 파일 경로와 함께 명시하라.

- 모든 새 파일 경로를 skill 기준 절대 또는 명확한 상대 경로로 고쳐라. 예: `.codex/skills/thinking-review-gate/scripts/collect.py`, `.codex/skills/thinking-review-gate/reports/summary.json`.

- "읽기 전용" 표현을 "기존 run artifact는 수정하지 않고, report 디렉터리에만 새 산출물을 쓴다"처럼 정확히 바꿔라.

- collector 출력 스키마를 최소 필드 수준으로 더 구체화하라. 예: `runs_total`, `attempts_total`, `gate_counts`, `axis_averages`, `weak_axis_counts`, `skipped_artifacts`, `regression_candidates`.

- 느린 개선 루프에는 실행 조건을 넣어라. 예: 최근 N일 또는 최근 N개 run 기준, 같은 `weak_axis`가 2회 이상 반복될 때만 proposal 생성, 단일 실패 run은 관찰로만 기록.

- 대안 비교를 명시하라. 예: `A. collector만`, `B. collector + proposal`, `C. collector + proposal + apply`로 나누고 장점, 단점, 추천 여부를 적어라.

- 검증 방법을 명령과 fixture 기준으로 보강하라. 예: 정상 run 1개, 깨진 validation 1개, fail run 1개를 두고 collector가 report를 만들며 `skipped_artifacts`를 기록하는지 확인한다고 적어라.

## 요약

초안의 방향은 적절하지만, 실전 설계로 쓰기에는 근거 연결, 경로 명확성, 운영 조건, 검증 방법이 약하다. 기존 artifact는 건드리지 않고 report/proposal만 생성한다는 안전 경계를 분명히 하고, collector와 느린 개선 루프의 입력, 출력, 실행 조건, 대안 trade-off를 더 구체화해야 한다.