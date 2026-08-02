# Original User Input

task3\assignments\taskC-6.md 에서 

제출물3이 너무 길고 보기가 힘든것 같아 과제로 제출함에 
따라 보는 사람이 보기 쉬워야하는데 
어떻게 하는게 좋
을까?
 그리고 내용도 괜찮은지 확인해줘


# Checked Context

# 확인한 문맥

- 대상: `task3/assignments/taskC-6.md`의 `## 제출물 3` 이하.
- 과제 요구: AI 제안 중 헥사고날 경계 위반을 잡아낸 사례와 본인 판단. 위반이 없다면 적용한 경계 점검과 AI 출력이 경계를 지킨 이유. 최소 300자.
- 현재 분량: 177줄, 6,083자, 코드 펜스 표식 14개.
- 현재 구성: 결론, 직접 본 코드, 경계 점검, 위반이 없던 이유, 결제 계약 오류, AI의 지름길 거절, 손코딩 대조.
- `TicketPersistenceAdapter.save`는 조건부 UPDATE가 0행이면 이미 `TicketAlreadyReservedException`을 던진다.
- `ReservationExceptionHandler`는 추가로 `OptimisticLockingFailureException`을 직접 처리하지만 L3 review도 죽은 분기일 가능성이 높다고 기록한다.
- ArchUnit은 Core -> Adapter, Core -> Spring, Core -> JPA/Hibernate, Inbound -> Outbound를 검사한다. 별도 Core 비공허성 테스트까지 합쳐 5개 테스트다.
- L1 rejected 기록에는 `/payments`와 2xx 승인 가정이 있고, notes가 계약 미제공과 미검증 가정을 명시한다. 수정 입력과 수용본은 `/charge` 및 응답 본문의 `approved`를 사용한다.
- L2 기록은 `@ConditionalOnMissingBean`과 `@Primary`를 함께 언급하지만 두 장치의 효과는 다르다. 전자는 구성 비활성화 경로를 만들 수 있고, 후자는 중복 후보 중 우선순위를 정할 뿐 빈을 비활성화하지 않는다.
- 손코딩과 파이프라인 구현의 동일성은 21개 시나리오 실행 범위에서 확인됐다. 모든 관찰 가능한 동작의 동일성을 증명한 것은 아니다.
- 현재 문서 183행에는 `` `**notes` ``라는 Markdown 오타가 있다.
- 로컬에는 `mvn`과 Maven wrapper가 없어 전체 테스트를 새로 실행하지 못했다. Docker Compose에도 테스트 전용 서비스는 없다. 코드와 저장된 run 로그를 근거로 검토했다.
