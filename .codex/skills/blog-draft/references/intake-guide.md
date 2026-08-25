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

## Section plan

새 intake는 `brief.spine` 대신 `brief.section_plan`을 만든다. section plan은 글의 완성된 목차가 아니라, 각 절이 어느 범위에서 무슨 일을 하고 어떤 재료를 쓸지 정하는 작성 계약이다.

```json
{
  "section_plan": [
    {
      "id": "s1",
      "heading_promise": "소제목이 독자에게 약속할 의미 범위",
      "purpose": "이 절이 글 전체에서 할 한 가지 일",
      "materials": [
        {
          "source": "raw_text",
          "anchor": "선택한 source에서 그대로 찾을 수 있는 짧은 문구",
          "role": "이 절에서 해당 재료가 하는 일"
        }
      ],
      "connection_to_next": "다음 절이 이 위치에 이어지는 이유"
    }
  ]
}
```

- 항목 하나는 최종 글의 H2 하나에 대응한다.
- `heading_promise`는 최종 소제목 문구가 아니라 소제목과 본문이 벗어나면 안 되는 의미 범위다.
- `purpose`는 이 절이 글 전체에서 하는 한 가지 일이다. 모든 절을 질문 형태로 만들지 않는다.
- `materials.source`는 `raw_text`, `reader`, `guide`, `judgment` 중 하나다. 저자 영역 블록은 사용자가 승인한 값이 있을 때만 쓴다.
- `anchor`는 source의 문자열 값에서 그대로 찾을 수 있어야 한다. 정규화하거나 요약하지 않는다.
- `role`은 재료 내용의 요약이 아니라 이 절에서 그 재료를 쓰는 이유다.
- `connection_to_next`는 선택 필드다. 원인과 결과, 대비, 시간의 흐름처럼 다음 절이 이 위치에 와야 하는 이유가 있을 때만 쓴다. 절이 독립적이면 `null`이나 빈 문자열을 쓰지 말고 필드를 생략한다.

section plan은 `raw_text`와 사용자가 승인한 저자 영역 블록을 기준으로 제안할 수 있다. 다만 승인 전에는 input에 기록하지 않는다.

```text
글의 기본값과 저자 영역 확인
-> section plan 제안
-> 사용자 승인 또는 수정
-> input에 section_plan 기록
```

기존 `spine`만 있는 input은 계속 통과한다. 새 intake는 `section_plan`만 생성하며, `spine`과 `section_plan`을 한 input에 같이 넣지 않는다.

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

## 블로그 프로필 적용

자기 PR 또는 포트폴리오 글이면 `../blog-profile.md`를 읽고 독자 전략의 기본값을 **제안**한다. 자동으로 넣지 않는다. 팀 내 문서나 일반 기술 노트는 독자와 목적이 다를 수 있으므로 profile을 적용할지 먼저 판단한다.

Profile과 run input의 책임은 다음처럼 나눈다.

- `brief.audience`: profile의 핵심 독자를 이번 글의 실제 독자에 맞게 제안한다.
- `brief.reader`: profile의 욕망, 악당, 외적·내적·철학적 문제와 잃는 것을 제안한다. 사용자가 승인하거나 고친 값만 기록한다.
- `brief.guide`: profile의 가이드 위치를 참고하되, 공감 장면과 권위 자료는 이번 글의 실제 경험에서만 받는다.
- `brief.judgment`: 버린 대안과 판단이 깨지는 조건을 이번 글마다 별도로 받는다. profile에서 만들지 않는다.
- `brief.constraints.emphasis`: 이번 글에서 가장 앞세울 판단 변화와 근거를 제안한다.
- `brief.constraints.avoid`: profile의 `쓰지 않을 것` 중 이번 글에 해당하는 항목만 제안한다.
- `constraints.target_length`, `tone`, `must_include`와 실제 금지 표현은 원재료와 발행 목적에 따라 run마다 확인한다.

확인 순서는 다음과 같다.

```text
profile 기본값
-> intake 제안
-> 사용자 승인 또는 수정
-> run input 기록
```

Profile 문장을 본문에 복사하지 않는다. 실제 경험, 감정, 실패, 갈등, 성과, 버린 대안과 판단 조건은 profile에서 추론하지 않는다.
