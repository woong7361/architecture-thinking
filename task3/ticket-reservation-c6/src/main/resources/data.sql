-- 컨테이너 기동 데모용 시드. 인수테스트는 시나리오가 데이터를 직접 만들므로 여기에 의존하지 않는다.
-- 시드를 어댑터 코드가 아니라 인프라 설정으로 두는 이유: 회원 조회 포트(LoadUserPort)에 등록 쓰기가 없고,
-- 적재는 유스케이스가 아니라 환경 준비의 일이기 때문이다.
-- SQL_INIT_MODE=always 인 환경에서만 실행된다(테스트 구성에서는 실행되지 않는다).
insert into users(id, name) values (1, 'user-1') on duplicate key update name = values(name);

insert into tickets(id, price, reserved, suspended, user_id) values
  (20, 30000, false, false, 0),   -- 예약 가능
  (21, 30000, false, true,  0),   -- 판매 중지
  (22, 30000, true,  false, 1),   -- 이미 예약됨
  (23, 50000, false, false, 0)    -- 할인 임계 이상
on duplicate key update price = values(price), reserved = values(reserved),
                        suspended = values(suspended), user_id = values(user_id);
