"""Recompute the retry backoff comparison from the test's raw logs."""

import json
import re
from pathlib import Path

RUNS = 3
HERE = Path(__file__).parent
BACKOFFS = ("NONE", "FIXED", "FIXED_JITTER", "EXPONENTIAL", "EQUAL_JITTER", "FULL_JITTER")
LABELS = {
    "NONE": "대기 없음",
    "FIXED": "고정 간격",
    "FIXED_JITTER": "고정 + 지터",
    "EXPONENTIAL": "지수 백오프",
    "EQUAL_JITTER": "지수 + Equal",
    "FULL_JITTER": "지수 + Full",
}

rows = {}
for run in range(1, RUNS + 1):
    log = HERE / f"retry-scale-run{run}.log"
    text = log.read_text(encoding="utf-8", errors="replace")
    for line in text.splitlines():
        if not line.startswith(("RETRY_SUMMARY", "RETRY_SHAPE")):
            continue
        values = dict(re.findall(r"(\w+)=([^ ]+)", line))
        key = (values["scale"], values["backoff"], int(values["workers"]))
        record = rows.setdefault(key, {})
        for name, value in values.items():
            if name in ("scale", "backoff"):
                continue
            record.setdefault(name, []).append(float(value))

for key, record in rows.items():
    assert len(record["attempts"]) == RUNS, f"{key} has {len(record['attempts'])} runs"
    assert sum(record["duplicateRounds"]) == 0, f"{key} produced duplicate rows"


def median(values):
    values = sorted(values)
    middle = len(values) // 2
    return values[middle] if len(values) % 2 else (values[middle - 1] + values[middle]) / 2


def at(key, name):
    return median(rows[key][name])


# 대기가 없으면 한 회차에 한 건씩만 성공하므로 쓰기 시도는 N(N+1)/2가 된다. 그 계산을 측정으로 확인한다.
for (scale, backoff, workers), record in rows.items():
    if backoff != "NONE":
        continue
    rounds = median(record["rounds"])
    expected = workers * (workers + 1) / 2 * rounds
    measured = median(record["attempts"])
    gap = abs(measured - expected) / expected
    print(f"무대기 시도 수 검증  {scale:<10} 경합 {workers:>2}: 계산 {expected:>8.0f}  측정 {measured:>8.0f}  차이 {gap:.2%}")

summary = {}
for scale in ("RESPONSIVE", "TOLERANT"):
    keys = sorted((k for k in rows if k[0] == scale), key=lambda k: (k[2], BACKOFFS.index(k[1])))
    if not keys:
        continue
    print()
    print(f"## {scale}")
    for workers in sorted({k[2] for k in keys}):
        print()
        print(f"경합 {workers}")
        print("| 방식 | 시도 | 충돌율 | 재시도 몰림 | 빈 구간 | p50 | p95 | 최대 | 요청당 대기 | 최종 실패 |")
        print("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
        for backoff in BACKOFFS:
            key = (scale, backoff, workers)
            if key not in rows:
                continue
            requests = median(rows[key]["rounds"]) * workers
            per_request_wait = at(key, "totalWaitMs") / requests
            print(
                f"| {LABELS[backoff]} | {at(key, 'attempts'):.0f} | {at(key, 'conflictRate'):.3f} "
                f"| {at(key, 'retryPeakPerMeanMedian'):.2f} | {at(key, 'emptyBucketRatioMedian'):.1%} "
                f"| {at(key, 'requestP50Ms'):.0f}ms | {at(key, 'requestP95Ms'):.0f}ms "
                f"| {at(key, 'requestMaxMs'):.0f}ms | {per_request_wait:.0f}ms "
                f"| {sum(rows[key]['exhausted']):.0f} |"
            )
            summary[f"{scale}/{backoff}/{workers}"] = {
                "attempts": at(key, "attempts"),
                "conflictRate": at(key, "conflictRate"),
                "retryPeakPerMean": at(key, "retryPeakPerMeanMedian"),
                "emptyBucketRatio": at(key, "emptyBucketRatioMedian"),
                "requestP50Ms": at(key, "requestP50Ms"),
                "requestP95Ms": at(key, "requestP95Ms"),
                "requestMaxMs": at(key, "requestMaxMs"),
                "perRequestWaitMs": per_request_wait,
                "exhaustedTotal": sum(rows[key]["exhausted"]),
                "attemptsPerRun": rows[key]["attempts"],
            }

(HERE / "retry-jitter-summary.json").write_text(
    json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")
print()
print(f"조합 {len(summary)}개를 retry-jitter-summary.json에 적었다.")
