## 문제 지점

- [테스트 개수 일관성] `Checked Context`에는 단위 테스트 4개가 통과했다고 되어 있는데, 초안은 단위 테스트 5개 통과라고 쓴다. attempt 2에서 실제로 테스트가 추가되어 5개가 된 것인지, 아니면 초안의 숫자가 잘못된 것인지 확인 가능한 근거가 없다.

- [단일 책임 claim] 초안은 책임 위치를 잘 나열하지만, 사용자의 핵심 요구인 “runner나 이런 곳에 prompt를 남기지 말라”에 대해 무엇을 prompt로 보았고 무엇을 허용한 loader 지침으로 보았는지 경계가 덜 선명하다. 특히 custom agent TOML의 `developer_instructions` 자체도 prompt-like instruction으로 보일 수 있으므로, “role/rubric 본문은 복제하지 않았고 TOML에는 권위 파일을 읽으라는 최소 loader만 있다”는 구분을 더 명시해야 한다.

- [근거 anchor 부족] `rg`로 role 본문과 rubric criterion 이름 복제가 없음을 확인했다는 claim은 좋지만, 어떤 검색 대상과 어떤 패턴을 확인했는지 사용자가 재현하기 어렵다. 현재는 결과 요약만 있고, 실제 확인 명령이나 검색 범위가 충분히 고정되어 있지 않다.

- [공식 문서 claim] OpenAI 공식 문서가 project custom agent와 병렬 subagent 실행, GPT Transcribe endpoint를 설명한다고 쓰지만 링크나 확인 일자가 없다. 최신 문서에 의존하는 claim이므로, 초안 안에서 출처를 따라갈 수 없으면 근거 품질이 약해진다.

- [구현 완료 범위] 제목이 “로컬 구현 완료”라서 구현 전체가 완료된 인상을 준다. 본문에 live transcription과 reviewer 품질 미검증을 밝히고 있지만, 사용자가 실제 진단 도구로 바로 쓸 수 있는 상태와 local mechanics만 검증된 상태가 제목 단계에서 구분되지 않는다.

- [검증 결과 표현] “세 CLI entrypoint 정상 로드”, “multipart body에 model과 원본 filename이 포함” 같은 claim은 구체 파일, 테스트명, 명령 출력 요약과 연결되어 있지 않다. 초안에 없는 근거를 새로 만들 필요는 없지만, 현재 표현은 검증 가능성이 약하다.

## 확인 필요

- 실제 attempt 2 기준 단위 테스트 개수가 4개인지 5개인지 확인해야 한다.
- `rg` 검증에서 사용한 정확한 검색 범위와 검색 대상 문자열을 초안에 넣을 수 있는지 확인해야 한다.
- OpenAI 공식 문서 claim을 유지할 경우, 링크와 확인일을 붙일 수 있는지 확인해야 한다.
- custom agent TOML의 loader instruction이 사용자 요구의 “prompt를 남기지 말라”와 충돌하지 않는다고 볼 근거를 더 명확히 해야 한다.

## 수정 제안

- 테스트 개수는 `Checked Context`와 맞추거나, 5개가 맞다면 “attempt 2에서 추가 검증 후 5개 통과”처럼 변경 사유를 명시한다.
- “단일 책임 위치” 섹션에 다음 구분을 추가한다: role prompt 본문, rubric 기준, output schema, orchestration 지시, mechanics script가 각각 어디에 있고 무엇을 소유하는지.
- `rg` 검증 claim은 최소한 검색 범위와 목적을 같이 적는다. 예: scripts 안에 role 본문과 rubric criterion 이름이 복제되지 않았다는 수준으로 고정한다.
- 공식 문서 claim은 링크와 확인일을 붙이거나, 링크를 넣지 않을 경우 “확인한 문맥 기준”으로 낮춰 쓴다.
- 제목 또는 첫 문장을 “로컬 mechanics 구현 및 정적/단위 검증 완료”처럼 조정해 live 전사와 실제 reviewer 실행이 남았다는 경계를 앞에서 드러낸다.
- 남은 검증 섹션에서 “D-5 결과물까지 완성”이라는 표현은 실제 end-to-end 완료 조건을 더 직접적으로 풀어 쓴다. 예: 녹화 파일 제공, API key 제공, 전사 실행, 두 reviewer JSON 생성, 최종 `feedback.md` 확인.

## 요약

초안은 사용자의 단일 책임 요구와 로컬 검증 범위를 대체로 반영하지만, 테스트 개수 불일치와 공식 문서·검색 검증의 근거 anchor 부족이 있다. 특히 “prompt를 runner에 남기지 않는다”는 핵심 요구는 TOML loader instruction과 role/rubric 본문 복제 금지의 경계를 더 선명하게 써야 한다.