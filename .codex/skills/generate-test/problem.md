# 사용자 피드백 누적 (generate-test)

생성된 테스트에 대한 **세션 내 사용자 반응(raw)**을 모은다. 실행 신호가 없는 이 하네스에서
**유일한 사람발 non-circular 앵커**다(설계 [docs/v1-slow-loop-design.md](docs/v1-slow-loop-design.md) §5).

- **캡처 주체**: 인터랙티브 스킬 세션(파이프라인 아님 — 파이프라인은 JSON in/out이라 피드백을 못 받음).
  SKILL.md Workflow 8단계가 캡처를 담당한다.
- **역할**: 같은 지적이 반복되면 v1 slow-loop의 proposer가 이 섹션을 context로 읽어 proposal에 반영한다.
  긍정 피드백은 회귀 방지 신호다(제안이 긍정 지점을 건드리면 경고 근거).
- **범위**: 사용자 피드백만. 열린 설계 쟁점은 여기가 아니라 저장소 루트 `PROBLEM.md`에 쓴다.

항목 형식:

```
- (YYYY-MM-DD, run_id, verdict=pos|neg) 사용자 반응 요약 → 도출한 교훈
```

## 항목

<!-- 아직 없음. 첫 피드백부터 채운다. -->
