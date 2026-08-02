# C6 Prompt Sources

이 run의 레이어 지시는 JSON 문자열이 아니라 외부 프롬프트 파일로 관리한다.

## Common Context

- `../../CLAUDE.md`

## Target Context

- `../../pipeline/inputs/c6-ticket-skeleton.json`
- field: `target_context`

## Common Layer Prompts

- `../../pipeline/prompts/layers/`

## Shared System Prompt

- `../../pipeline/prompts/implement_layer.md`

## Layer Prompts

| Layer | Prompt |
| --- | --- |
| L0 | `../../pipeline/prompts/layers/L0-usecase.md` |
| L1 | `../../pipeline/prompts/layers/L1-outbound.md` |
| L2 | `../../pipeline/prompts/layers/L2-composition.md` |
| L3 | `../../pipeline/prompts/layers/L3-inbound.md` |

runner는 새 실행부터 각 `attempt_XX/context/` 아래에 다음 파일을 저장한다.

- `system.md`: 실제 system prompt
- `user.md`: 실제 user context
- `prompt-sources.json`: system prompt, layer prompt, 공통 컨벤션, input context field, boundary rule 위치

기존 `accepted.json`과 `implement.json`의 `files[].content` 및 `notes`는 당시 AI 출력 원문이다.
포트명 변경이나 프롬프트 구조 변경에 맞춰 손으로 고치지 않고 실행 이력으로 보존한다.
