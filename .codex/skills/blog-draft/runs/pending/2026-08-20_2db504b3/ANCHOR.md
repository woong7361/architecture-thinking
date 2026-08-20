# 앵커 보관: 거짓 통과 (2026-08-20, run 2db504b3)

이 run은 지우지 않고 반례로 보관한다.

## 무슨 일이 있었나

iter_002 초안의 `content`가 산문이 아니라 `{"content":"..."}` JSON 문자열이었다. refine 모델이 출력을 이중으로 감쌌다.

- `gen_output.schema.json`: 문자열이므로 통과
- `draft.schema.json`: 통과
- LLM 평가자: weighted_total 4.165 / min_total 4.1 → PASS
- 사람 판정: 불합격 (초안이 아니라 JSON 덩어리다)

## 왜 보관하나

루브릭이 사람 판정과 어긋난 첫 실물이다. "점수가 낮다"는 루브릭을 고칠 근거가 못 되지만 "루브릭이 사람 판정과 어긋난다"는 근거가 되고, 이것이 그 사례다. 동결 캘리브레이션 셋의 첫 항목으로 쓴다.

## 카운트에서 뺀 이유

`2db504b3_final.json` → `2db504b3_final.false-pass.json`으로 이름을 바꿨다. `run_draft.py`의 `count_passing_pending()`이 `*_final.json`을 세어 slow loop 발동을 판단하는데, 거짓 통과가 통과 run으로 잡히면 안 된다.

## 이후 조치

`validate.py`에 결정적 검사를 넣어 같은 실패는 재발하지 않는다. 오염된 iter_002는 이제 `draft content is a JSON envelope, not prose`로 REJECT된다.
