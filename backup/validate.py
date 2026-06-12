# validate가 잡음 | validate가 못 잡음
# JSON 필드 누락 | 주장과 근거 배치의 설득력
# 길이 상·하한 위반 | 인용 선택의 자연스러움
# 금칙어 등장 | 문단 사이 흐름
# 스키마 타입 오류 | 톤의 미묘한 일관성
# brief_hash 불일치 | 독자가 느낄 신선함

# 왼쪽은 기계가 검사 가능한 영역, 오른쪽은 가치 판단이 필요한 영역입니다.


# validate.py — P3 Sprint Contract 실전 구현
# 사용: python validate.py <output.json> <verdict.json>
# 의존: pip install jsonschema pyyaml
import json
import sys
from pathlib import Path
from jsonschema import validate as jsonschema_validate, ValidationError
import yaml
# ----- 1. 스키마 정의 (파이프라인마다 교체) -----
ARTIFACT_SCHEMA = {
    "type": "object",
    "required": ["content", "brief_hash", "generated_at"],
    "properties": {
        "content": {"type": "string", "minLength": 1},
        "brief_hash": {"type": "string", "pattern": "^[a-f0-9]{8,}$"},
        "generated_at": {"type": "string", "format": "date-time"},
        "generator_model": {"type": "string"},
    },
    "additionalProperties": True,
}
LENGTH_MIN = 300
LENGTH_MAX = 4000
QUALITY_MIN = 2.5
BANNED_WORDS = ["무조건", "완벽보장", "절대안전", "반드시성공"]
# ----- 2. 개별 체크 -----
def check_schema(artifact: dict) -> list[str]:
    try:
        jsonschema_validate(artifact, ARTIFACT_SCHEMA)
        return []
    except ValidationError as e:
        return [f"schema: {e.message}"]
def check_length(content: str) -> list[str]:
    n = len(content)
    if n < LENGTH_MIN:
        return [f"length: {n} < {LENGTH_MIN}"]
    if n > LENGTH_MAX:
        return [f"length: {n} > {LENGTH_MAX}"]
    return []
def check_banned(content: str) -> list[str]:
    return [f"banned: '{w}'" for w in BANNED_WORDS if w in content]
def check_quality(rubric_scores: dict) -> list[str]:
    scores = rubric_scores.get("scores", {})

    weights = rubric_scores.get("weights", {})
    if not scores or not weights:
        return ["quality: missing scores or weights"]
    total = sum(scores[k] * weights[k] for k in scores if k in weights)
    if total < QUALITY_MIN:
        return [f"quality: {total:.2f} < {QUALITY_MIN}"]
    return []
# ----- 3. 합친 contract -----
def validate_contract(artifact_path: Path, verdict_path: Path) -> dict:
    artifact = json.loads(Path(artifact_path).read_text())
    verdict_existing = {}
    if Path(verdict_path).exists():
        verdict_existing = json.loads(Path(verdict_path).read_text())
    rubric_scores = verdict_existing.get("rubric_scores", {})
    errors = []
    errors += check_schema(artifact)
    errors += check_length(artifact.get("content", ""))
    errors += check_banned(artifact.get("content", ""))
    errors += check_quality(rubric_scores)
    verdict = {
        **verdict_existing,
        "contract_errors": errors,
        "verdict": "REJECT" if errors else "PASS",
    }
    Path(verdict_path).write_text(json.dumps(verdict, ensure_ascii=False, indent=2))
    return verdict
if __name__ == "__main__":
    out = validate_contract(Path(sys.argv[1]), Path(sys.argv[2]))
    print(json.dumps(out, ensure_ascii=False, indent=2))
