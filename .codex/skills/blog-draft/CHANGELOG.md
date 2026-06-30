# Changelog

파이프라인 component 변경 이력. slow loop proposal을 사람이 수락·적용할 때 여기에 한 줄을 추가한다.

형식:
```
## {component}:{vN} ({날짜})
- 변경: {무엇을}
- 근거: {왜 — 분석 신호 요약}
- 분석 run: {pending run hash들}
- 위험: 낮음 | 중 | 높음
- commit: {적용 커밋 해시}
```

과거 버전은 git이 관리한다. 별도 스냅샷을 만들지 않는다.

---

## gen_system:v2 (2026-06-30)
- 변경: emphasis 주제를 비중 중심으로 삼고 다른 소재는 보조 역할로만 배치하는 지시 추가 (P1)
- 겨냥 axis: originality
- 근거: critique 반복 지적 — 1·2절이 emphasis 주제(multi-provider 확장)보다 비중이 큼; 분석 run 3930539b
- 분석 run: 2026-06-29_3930539b
- 위험: 중
- commit: (적용 후 기입)

## gen_system:v3 (2026-06-30)
- 변경: raw_text의 핵심 기술 결정·선택 이유·설계 판단을 본문에서 생략 금지하는 지시 추가 (P2)
- 겨냥 axis: evidence
- 근거: critique 반복 지적 — build_prompt의 system/user 분리 결정이 draft에서 완전히 사라짐; 분석 run 3930539b
- 분석 run: 2026-06-29_3930539b
- 위험: 중
- commit: (적용 후 기입)

## gen_system:v4 (2026-06-30)
- 변경: 결론 지시를 "한계 나열 금지, 독자가 가져갈 판단·관점·실험 단서로 마무리" 로 교체 (P3)
- 겨냥 axis: originality
- 근거: critique 반복 지적 — 마지막 단락이 한계를 나열하지만 독자가 가져갈 단서가 없음; 분석 run 3930539b
- 분석 run: 2026-06-29_3930539b
- 위험: 중
- commit: (적용 후 기입)
