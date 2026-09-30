"""결정적 판정 장치. 기계가 셀 수 있는 것만 여기서 판정한다.

한 사실은 한 자리에서 판정한다. 기계가 셀 수 있는 것은 게이트가, 코드를 읽어야 아는 것은 루브릭이,
뜻을 붙여야 아는 것은 비평이 맡는다. 계약 적합성을 점수로 재면 근거 없는 문장이 네 건 있는 초안이 4.8을
받고 그런 문장이 없는 초안이 4.5를 받는 역전이 생기므로, 계약과 구현의 대조는 결정적으로 센다.

| 무엇을 | 누가 |
| --- | --- |
| 컴파일과 부팅, 층 사이의 의존 방향 | `run_g0` |
| 구현이 자기 계약 판본을 지켰는가 | `run_g1` |
| 계약을 고쳤는데 신고하지 않았거나 근거가 비었는가 | `check_contract_changes` |

위반과 기록은 같은 모양이다.

    {"rule": "response.field_missing", "point": "createAlert:201",
     "where": "Alert.lastCheckedAt", "detail": "...", "verdict": "violation"|"observation"}

규칙 id의 뜻은 `rules/conformance_rules.yaml`에 한 줄로 고정한다. 모델이 위반 문장을 매번 새로 쓰면
같은 위반이 run마다 다른 말로 남아 집계가 안 된다.
"""

from .g0 import run_g0
from .g1 import run_g1
from .changes import check_contract_changes

__all__ = ["run_g0", "run_g1", "check_contract_changes", "load_rules"]


def load_rules(path=None):
    """규칙 카드를 읽는다. 게이트를 부르는 쪽이 한 번 읽어 넘긴다."""
    from pathlib import Path
    import yaml
    target = Path(path) if path else Path(__file__).resolve().parents[2] / "rules" / "conformance_rules.yaml"
    return yaml.safe_load(target.read_text(encoding="utf-8")) or {}
