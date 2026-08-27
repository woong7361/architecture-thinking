## 문제 지점

- [단일 책임 위치] 초안 제목은 "단일 책임 위치"인데, 실제 목록에는 `SKILL.md`, 두 role Markdown, 두 rubric YAML, schema, scripts가 모두 포함되어 있다. 사용자의 핵심 요구는 "runner나 실행 코드에 prompt를 남기지 말고 prompt 책임을 한곳에 모으라"는 것이므로, 현재 표현은 "책임이 여러 군데로 나뉜 것처럼" 읽힐 수 있다. role prompt 본문, rubric, schema, orchestration, script 책임을 구분해서 써야 한다.

- [병렬 reviewer 실행 claim] "두 reviewer는 같은 immutable `review-context.json`을 보고 병렬 실행하며 서로의 결과를 먼저 읽지 않는다"는 문장이 이미 실제 실행까지 검증된 사실처럼 보인다. Checked Context에 따르면 이 세션에서는 실제 두 reviewer를 병렬 실행하지 않았고, `SKILL.md`가 그렇게 지시하도록 구성한 상태다. 이 문장은 "실행 지침상" 또는 "설계상"으로 낮춰야 한다.

- [구현 완료 범위] 초안 첫 문단의 "로컬 mechanics 구현과 계약 검증 완료"는 대체로 맞지만, 사용자가 기대한 진단 도구의 최종 가치인 실제 녹화 전사와 reviewer 품질 검증은 아직 빠져 있다. 뒤에서 "남은 검증"을 밝히고 있으나, 첫 문단에서도 완료 범위를 더 좁게 고정해야 과장으로 읽히지 않는다.

- [전사 API 표현] "`transcribe.py`: 파일 하나를 `gpt-transcribe` endpoint로 보낸다"는 표현은 부정확할 가능성이 있다. Checked Context의 근거는 `/v1/audio/transcriptions` endpoint와 `gpt-transcribe` 모델 지원이다. endpoint와 model을 구분해서 써야 한다.

- [근거 anchor] `rg`, unittest, JSON schema 검사 등 검증 근거는 좋지만, "새 skill과 agent 파일의 trailing whitespace, multipart body의 model과 원본 filename 포함 여부도 확인했다"는 claim은 실행 명령이나 확인 방식이 초안 안에 충분히 anchored 되어 있지 않다. 사용자가 재검증할 수 있게 명령 또는 테스트 이름과 연결하는 편이 낫다.

## 확인 필요

- 실제 변경 파일 목록을 최종 응답에 넣을지 확인이 필요하다. 현재 초안은 책임 위치 중심으로 파일을 나열하지만, 새로 생성·수정된 파일 전체 목록은 아니다.

- `developer_instructions`의 "최소 loader 지침"이 사용자 기준에서 prompt로 간주되지 않는지 확인이 필요하다. 초안은 "role·평가 기준 본문은 없다"는 관점으로 방어하지만, 사용자가 말한 "prompt같은 것"의 범위가 더 넓을 수 있다.

- `review-context.json`이 실제로 immutable하게 취급되는 방식이 파일 권한, hash 검증, 실행 관례 중 무엇으로 보장되는지 확인이 필요하다. 초안은 "immutable"이라고 표현하지만 보장 메커니즘 설명은 짧다.

## 수정 제안

- "단일 책임 위치" 섹션을 "책임 분리와 prompt 중복 방지"처럼 바꾸고, 핵심을 다음처럼 정리한다: reviewer prompt 본문은 role Markdown에만 있고, runner는 없으며, TOML에는 role 본문이 아닌 loader 지침만 있다. rubric과 schema는 prompt 본문이 아니라 평가 기준과 출력 계약의 권위 파일이라고 분리한다.

- 병렬 실행 관련 문장은 "실제 병렬 실행을 완료했다"가 아니라 "`SKILL.md`가 두 custom agent를 병렬로 실행하도록 지시한다. 이번 세션에서는 TOML parse와 공식 문서 근거까지 확인했고, 실제 병렬 reviewer 실행은 남았다"로 고친다.

- 첫 문단에 완료 범위를 명시한다: "파일 구조, local scripts, schema 검증, 단위 테스트까지 완료했다. 실제 음성 파일 기반 진단 품질은 아직 검증 전이다."

- `gpt-transcribe endpoint` 표현을 "`/v1/audio/transcriptions` endpoint에 `model=gpt-transcribe`로 요청한다"로 수정한다.

- 검증 claim마다 재현 가능한 anchor를 붙인다. 이미 명령이 있는 항목은 그대로 두고, trailing whitespace나 multipart 검증처럼 명령이 없는 항목은 테스트 이름 또는 사용한 검사 방식을 한 줄로 추가한다.

## 요약

초안은 사용자의 핵심 요구인 "runner에 prompt를 남기지 않고 책임을 한곳에 모으라"는 방향을 잘 반영하고, 검증 결과도 비교적 구체적이다. 다만 "단일 책임", "병렬 실행", "완료" 같은 표현이 실제 검증 범위보다 강하게 읽히는 부분이 있다. 최종 답변에서는 prompt 본문 책임과 다른 계약 파일의 책임을 분리하고, 실제로 검증한 것과 남은 것을 더 엄격히 구분해야 한다.