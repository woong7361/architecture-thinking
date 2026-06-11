from pathlib import Path
from some_llm_sdk import Client

import json
GEN_SYSTEM_PROMPT = Path("prompts/gen_system.md").read_text()
def generate(input_path: Path, output_path: Path) -> None:
    brief = json.loads(input_path.read_text())
    client = Client()
    response = client.messages.create(
    system=GEN_SYSTEM_PROMPT,
    messages=[{"role": "user", "content": json.dumps(brief)}],
    max_tokens=4000,
    )
    artifact = {
    "content": response.content,
    "brief_hash": brief["hash"],
    "generated_at": response.meta["timestamp"],
    # 중요: 자기 평가 점수를 여기 넣지 않는다.
    }
    output_path.write_text(json.dumps(artifact, ensure_ascii=False, indent=2))
