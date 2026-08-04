# 프로젝트 문맥

- `task2/assignments/taskB-3.md`에서 Rich Domain으로 옮긴 뒤 Service에는 사용자 조회의 null 검사와 결제 결과 boolean 검사가 남아 있다.
- 직전 논의에서 `if (x == null) throw`와 Service가 호출하는 `Optional.orElseThrow`는 문법만 다르고 실패 판단 책임은 Service에 그대로 남는다고 정리했다.
- 사용자는 이제 실패 계약을 메서드 이름으로 명시해야 하는지, 이를 점진적으로 바꾸면 나중 리팩터링 비용이 너무 커지는지 판단하려 한다.
- 현재 대상은 학습용 Ticket 예제이며 실제 전체 호출자 수와 팀 네이밍 관례는 확인되지 않았다.

# 근거 앵커

- `task2/assignments/taskB-3.md`: "Service는 조립만"이라는 설명 뒤에 `findById + null 검사`, `paymentApi.charge + boolean 검사`가 남아 있다.
- `task2/assignments/taskB-2.md`: 원본 서비스는 Repository와 Payment API의 약한 반환 계약을 직접 해석한다.

# 판단 제약

- 이름만으로 Java의 unchecked exception 계약을 완전히 표현할 수 없다.
- 범용 Repository의 모든 조회가 사용자 부재를 실패로 취급한다고 가정할 수 없다.
- 결제 거절이 정상 결과인지 예외인지 현재 요구사항만으로 확정할 수 없다.
- 설계 방향만 제시하고 파일이나 코드는 수정하지 않는다.
