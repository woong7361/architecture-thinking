# Blog Draft Intake Guide

Ask only questions that materially change the draft. Infer the rest from the material.

## Default Inference

- Technical project notes usually become a developer retrospective.
- Architecture or workflow notes usually use a calm, concrete developer tone.
- If no audience is obvious, use "AI native 개발 방식에 관심 있는 개발자".
- If no length is specified, propose "3000-4000 Korean characters".
- Avoid hype, unsupported metrics, and claims not grounded in `raw_text`.

## Fields

- `topic`: a concise title-like subject.
- `raw_text`: the original material, preserved without summary.
- `piece_type`: `retrospective`, `essay`, or `technical_note`.
- `intent`: why the user wants the draft.
- `audience`: expected readers.
- `constraints.target_length`: desired length.
- `constraints.tone`: voice and distance.
- `constraints.emphasis`: what to foreground.
- `constraints.must_include`: required points.
- `constraints.avoid`: claims, tones, or topics to avoid.

## 저자 영역 블록 (reader / guide / judgment)

`raw_text`가 재료라면 이 세 블록은 **저자만 아는 것**이다. 비어 있으면 모델은 해당 내용을 쓰지 않는다. 추론해서 채우지 않는다 — 지어낸 독자 문제와 지어낸 1인칭 경험은 글의 결함이 아니라 거짓이다.

`--context-file`에 넘길 JSON 한 덩어리로 받는다. 값이 여러 문장짜리 산문이라 CLI 플래그로는 따옴표가 깨진다.

```json
{
  "reader":   { "desire": "", "villain": "", "external": "", "internal": "", "philosophical": "", "stakes": "" },
  "guide":    { "empathy": "", "authority": [] },
  "judgment": { "discarded": [], "breaking_conditions": [] }
}
```

- `reader`를 넣으려면 `desire`, `external`, `internal`이 함께 있어야 한다. 욕망을 세우지 않으면 문제가 제자리를 못 찾고, 감정 층이 빠지면 글이 사실 나열로 끝난다.
- `internal`은 `external`을 감정어로 바꿔 쓴 문장이 아니다. 사실이 독자에게 만드는 감정이어야 한다.
- `philosophical`은 개인 사정이 아니라 부류 전체를 두고 "~해야 한다" 형태로 쓴다. 선택 항목이지만 여기까지 내려간 글은 드물다.
- `guide.authority`는 성공담보다 실패담을 앞에 둔다. 성공담은 독자와 주연 자리를 다투고 실패담은 다투지 않는다.
- `judgment`는 결과물에서 가장 먼저 지워지는 부분이라 입력에서 먼저 받는다.

## 기본 독자 프로필 (제안값)

자기 PR 목적의 글이면 아래를 **제안**하고 확인을 받는다. 자동으로 넣지 않는다 — 매 run의 `input.json`에 실제로 쓴 값이 남아야 나중에 그 글이 누구를 겨냥했는지 되짚을 수 있다. 팀 내 문서나 기술 노트는 독자가 다르므로 그대로 쓰지 않는다.

- `desire`: 우리 팀에서 잘할 사람을 뽑는 것. 실제로 쥐고 싶은 건 확신이 아니라 남에게 그대로 옮길 수 있는 근거.
- `villain`: 결과물에서 지워진 판단 과정.
- `external`: 지원자가 실제로 잘할 사람인지 확인할 수단이 없다. 결과 수치는 다 좋고 문장은 다 매끄럽다.
- `internal`: 이 사람이 팀에 짐이 될지 알 수 없다는 불안. 잘못 뽑은 비용은 보이고 놓친 비용은 안 보인다.
- `philosophical`: 확인은 쓴 사람이 할 일이지 읽는 사람이 캐낼 일이 아니다.
- `stakes`: 확신이 안 서면 떨어뜨린다.
