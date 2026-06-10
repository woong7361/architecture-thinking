# evaluator.py
# 역할: output.json을 입력으로 받아 점수와 판정을 verdict.json으로 낸다.
# Generator의 대화 히스토리나 내부 상태를 받지 않는다.
from pathlib import Path
from some_llm_sdk import Client
import json, yaml

EVAL_SYSTEM_PROMPT = Path("prompts/eval_system.md").read_text()
RUBRIC = yaml.safe_load(Path("rubric.yaml").read_text())
def evaluate(artifact_path: Path, verdict_path: Path) -> None:
 artifact = json.loads(artifact_path.read_text())
 client = Client()
 user_message = {
"rubric": RUBRIC,
"artifact": artifact["content"],
# brief_hash만 참조용. 내용은 Evaluator가 독립 판단한다.
"brief_hash": artifact["brief_hash"],
 }
 response = client.messages.create(
 system=EVAL_SYSTEM_PROMPT,
 messages=[{"role": "user", "content": json.dumps(user_message)}],
 max_tokens=800,
 )
 verdict = json.loads(response.content)
 verdict_path.write_text(json.dumps(verdict, ensure_ascii=False, indent=2))