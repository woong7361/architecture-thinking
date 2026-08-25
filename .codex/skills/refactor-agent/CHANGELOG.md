# Changelog

## refactor:v2 — 2026-08-25

- `testability_improvement`를 설명의 풍부함과 변경 전후 Mock 개수로 채점하던 사다리에서 두 조건의 이진 채점으로 바꿨다.
- 조건 A는 대상 규칙의 최소 테스트 구성에 필요한 외부 의존성의 Mock·Stub·Fake가 0개인지 본다.
- 조건 B는 테스트 실패 원인을 이름 붙은 규칙 하나로 좁힐 수 있는지 본다. 같은 규칙의 결과를 확인하는 assert가 여러 개인 것은 허용한다.
- 두 조건 모두 불충족이면 1점, 하나만 충족하면 3점, 모두 충족하면 5점이다.
- runner가 각 축의 `scale`에 정의되지 않은 점수를 거부하도록 해 테스트 용이성 축의 2점·4점 출력을 막았다.
- 기존 `refactor:v1`은 `pipeline/rubrics/refactor.v1.rubric.json`에 보존했다. rubric 변경 전후 점수는 같은 잣대가 아니므로 직접 비교하지 않는다.
