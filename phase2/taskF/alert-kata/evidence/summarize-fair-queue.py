"""Recompute the fair queue comparison from the test's raw log."""

import json
import math
import re
from pathlib import Path


LOG = Path(__file__).with_name("fair-queue-resources.log")
text = LOG.read_text(encoding="utf-16")
rows = []
for line in text.splitlines():
    if not line.startswith("FAIR_SUMMARY"):
        continue
    values = dict(re.findall(r"(\w+)=([^ ]+)", line))
    rows.append({
        key: (value if key == "strategy" else float(value))
        for key, value in values.items()
    })

assert len(rows) == 9, f"expected 9 measured batches, got {len(rows)}"
for row in rows:
    assert row["rounds"] == 50
    assert row["requests"] == 5000
    assert row["winners"] == 50
    assert row["losers"] == 4950
    assert row["observations"] == 5000


def median(values):
    values = sorted(values)
    middle = len(values) // 2
    return values[middle] if len(values) % 2 else (values[middle - 1] + values[middle]) / 2


summary = {}
for strategy in ("SPIN", "CONDITION", "REDIS"):
    selected = [row for row in rows if row["strategy"] == strategy]
    assert len(selected) == 3
    for row in selected:
        expected = (row["javaOneCorePct"] + row["redisOneCorePct"]) / 20
        assert math.isclose(row["combinedCapacityPct"], expected, abs_tol=0.001)
    summary[strategy] = {
        "batches": len(selected),
        "rounds": sum(row["rounds"] for row in selected),
        "requests": sum(row["requests"] for row in selected),
        "median_round_ms": median(row["medianRoundMs"] for row in selected),
        "p95_round_ms": median(row["p95RoundMs"] for row in selected),
        "combined_capacity_pct": median(row["combinedCapacityPct"] for row in selected),
        "combined_capacity_pct_min": min(row["combinedCapacityPct"] for row in selected),
        "combined_capacity_pct_max": max(row["combinedCapacityPct"] for row in selected),
        "spin_checks": sum(row["spinChecks"] for row in selected),
        "condition_awaits": sum(row["conditionAwaits"] for row in selected),
    }

output = LOG.with_name("fair-queue-resources-summary.json")
output.write_text(json.dumps({"batches": rows, "summary": summary}, indent=2) + "\n", encoding="utf-8")
print(json.dumps(summary, indent=2))
