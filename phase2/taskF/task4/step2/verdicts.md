# 단계 3 — 채택 / 기각

판정은 여기 적는다. [REPORT.md](REPORT.md) 는 하네스가 다시 쓰므로 거기 적으면 덮인다.

## 무엇을 보고 정하는가

`READY` 는 주장이 참이라는 뜻이 아니라 **대조할 자료가 갖춰졌다**는 뜻이다. 참·거짓은 이 표에서 정한다.

주장마다 한 줄만 보면 된다. **주장과 원문의 차이**다. 게이트가 인용의 실존을 이미 확인했으므로 남은 위험은 진짜 인용으로 원문보다 넓은 주장을 뒷받침하는 것이고, 그 차이가 거기 적혀 있다.

그 줄이 걸리면 리포트의 그 묶음을 연다. 사본에서 떠낸 자리가 줄 번호와 함께 있다.

## 한눈에

| id | 종류 | 축 칸 | 제한 조항 | 채택·기각 | 이유 |
| --- | --- | --- | ---: | --- | --- |
| [claim-01](#claim-01) | fact | G3 D4 | 4 |  |  |
| [claim-02](#claim-02) | fact | G3 D4 | 4 |  |  |
| [claim-03](#claim-03) | fact | G3 D4 | 4 |  |  |
| [claim-04](#claim-04) | fact | G3 D4 | 4 |  |  |
| [claim-06](#claim-06) | fact | G3 D4 | 3 |  |  |
| [claim-08](#claim-08) | choice | G3 D4 T5 T4 | 3 |  |  |
| [claim-05](#claim-05) | choice | G3 D4 T5 T5 | 3 |  |  |
| [claim-07](#claim-07) | choice | G3 D4 T5 T5 | 3 |  |  |
| [claim-09](#claim-09) | choice | G3 D4 T5 T5 | 3 |  |  |
| [claim-10](#claim-10) | choice | G3 D4 T5 T4 | 2 |  |  |

축 칸은 `G` grounding, `D` depth, `T` tradeoff_coverage, `T` tradeoff_validity 순이다. 사실 주장은 앞의 둘만 있다. `제한 조항` 은 반대 조사자가 그 주장을 깎으려고 가져온 인용의 수다.

## 주장별

### claim-01

> 프록시를 거쳐 전파 속성이 읽힌 공개 메서드 호출에서 `Propagation.REQUIRES_NEW`는, 기존 트랜잭션이 있으면 그것을 보류한 뒤, 없으면 그대로, 새 트랜잭션을 시작한다. 보류는 트랜잭션 관리자가 보류를 구현한 경우에만 성립한다.

**차이** 문장에 넣은 '없으면 그대로'는 같은 메서드의 다른 갈래(REQUIRED·REQUIRES_NEW·NESTED를 한 조건으로 묶어 startTransaction을 부르는 자리)에서 정해지는데, 인용 상한 때문에 그 블록을 인용으로 들지 않았다. '보류는 트랜잭션 관리자가 보류를 구현한 경우에만'은 `Propagation.java`의 REQUIRES_NEW javadoc NOTE('Actual transaction suspension will not work out-of-the-box on all transaction managers')가 정하는데 그 인용도 상한 밖으로 밀렸다. 또 비공개 메서드 필터가 기본으로 켜져 있다는 사실은 `AnnotationTransactionAttributeSource`의 `publicMethodsOnly = true` 선언이 정하고 이 인용 묶음에 없다.

근거 `AbstractFallbackTransactionAttributeSource.java`, `TransactionAspectSupport.java`, `AbstractPlatformTransactionManager.java`

반대 조사자만 가져온 조항
- `spring-framework-6.2.19/org/springframework/transaction/annotation/AnnotationTransactionAttributeSource.java`

판정 — 

이유 — 

### claim-02

> `Propagation.NOT_SUPPORTED`로 읽힌 호출은 실제 트랜잭션 없이 실행된다. 기존 트랜잭션이 있으면 보류하고 없으면 보류할 것이 없으며, 기본 설정(`SYNCHRONIZATION_ALWAYS`)에서는 실제 트랜잭션이 없어도 동기화 범위는 남는다. 보류는 트랜잭션 관리자가 보류를 구현한 경우에만 성립한다.

**차이** 문장의 '동기화 범위는 남는다'는 인용 2의 newSynchronization 계산 한 줄과 `SYNCHRONIZATION_ALWAYS` 상수 이름까지로만 받친다. 그 상수가 기본값이라는 사실(`transactionSynchronization = SYNCHRONIZATION_ALWAYS`)과 그 javadoc이 NOT_SUPPORTED를 명시적으로 대상에 넣는다는 사실은 같은 파일 안이지만 인용 상한 때문에 들지 않았다. 또 run3의 `AlertService.subscribe`가 보류 갈래를 타는지 바깥 트랜잭션이 없는 갈래를 타는지는 호출자에 달려 있어 코드에 적혀 있지 않다.

근거 `TransactionAspectSupport.java`, `AbstractPlatformTransactionManager.java`, `Propagation.java`

판정 — 

이유 — 

### claim-03

> propagation을 적지 않은 메서드 수준 `@Transactional`의 전파 속성은 REQUIRED이고, 이 값은 클래스 수준 선언을 물려받는 대신 그것을 대체한다.

**차이** 원문은 선언된 기본값과 조회 순서까지만 정한다. 그 기본값이 실제 호출에 적용되려면 프록시를 거쳐야 하고, 같은 파일의 바로 앞 블록이 설정에 따라 비공개 메서드를 걸러 null을 돌려주므로 비공개 메서드에서는 전파 속성이 REQUIRED가 되는 것이 아니라 아예 비트랜잭션이 된다. 그 블록은 claim-01의 인용 묶음에 들어 있다. 선언 기본값이 트랜잭션 관리자가 보는 `getPropagationBehavior()`로 옮겨지는 자리(`SpringTransactionAnnotationParser.parseTransactionAnnotation`)도 인용 상한 밖이다.

근거 `Transactional.java`, `Propagation.java`, `AbstractFallbackTransactionAttributeSource.java`

판정 — 

이유 — 

### claim-04

> MySQL 방언은 `PESSIMISTIC_WRITE` 잠금에 대해, 대기 설정이 무한 대기이고 별칭을 지정하지 않은 경우 SQL 끝에 붙일 조각으로 " for update"를 만든다. 이 자리가 정하는 것은 조각을 만드는 데까지이고, 그 조각이 어느 SQL에 실려 나가는지는 정하지 않는다.

**차이** 문장에 조건으로 박은 '무한 대기'와 '별칭 없음'은 `MySQLDialect.withTimeout`(NO_WAIT·SKIP_LOCKED에 " nowait"·" skip locked"를 덧붙인다)과 `MySQLDialect.getForUpdateString(String aliases)`(별칭을 받으면 " for update of <별칭>")가 정하는데, PESSIMISTIC_WRITE에서 " for update"에 닿는 세 고리가 인용 상한을 다 써서 그 두 블록을 인용으로 들지 못했다. 조각이 실리는 SQL도 이 자리 밖이다. 잠금 갱신 경로에서는 `AbstractSelectLockingStrategy.generateLockString`이 엔티티 로드 SQL이 아니라 식별자 한 열만 뽑는 별도 SELECT를 만들고, 버전 매핑된 엔티티에는 버전 열 조건을 더 넣는다. 그래서 문장을 조각 생성까지로 좁혔다.

근거 `Dialect.java`, `MySQLDialect.java`, `Dialect.java`

판정 — 

이유 — 

### claim-06

> 트랜잭션이 진행 중일 때 TRANSIENT 인스턴스를 DB가 키를 만드는 매핑으로 `persist`하면 삽입이 지연되지 않고 생성된 키를 그 자리에서 읽을 수 있다.

**차이** 주장은 '삽입이 지연되지 않는다'까지만 쓸 수 있고, 'INSERT 문이 persist 호출 안에서 DB로 나간다'는 더 강한 문장은 이 인용으로 갈리지 않는다. 실행 시점을 정하는 ActionQueue는 이 payload의 출처 루트에 들어 있지 않다. generatedOnExecution이 참인지(= IDENTITY 전략인지)를 정하는 Generator 쪽과, 엔티티가 Hibernate의 `Lifecycle`을 구현해 `onSave`가 삽입을 거부하면 identity 매핑에서 돌려주는 id가 `null`이 되는 갈래(`invokeSaveLifecycle`)도 인용 상한 밖이다. 대상 엔티티들은 `Lifecycle`을 구현하지 않는다.

근거 `DefaultPersistEventListener.java`, `AbstractSaveEventListener.java`, `AbstractSaveEventListener.java`

판정 — 

이유 — 

### claim-08

> 낙관 경로 구현은 엔티티에 `@Version` 필드를 두고도 충돌 판정을 JPA의 버전 검사가 아니라 네이티브 SQL의 조건부 UPDATE로 한다. 매핑된 `@Version`은 이 경로에 개입하지 않지만, 같은 엔티티를 잠그거나 플러시로 갱신하는 다른 경로에서는 여전히 조건으로 들어간다.

**차이** 네이티브 UPDATE는 영속성 컨텍스트를 우회하므로 같은 트랜잭션 안의 KataUser 인스턴스가 들고 있는 값이 실제와 벌어질 수 있고, 그러면 버전 조건을 붙이는 잠금 경로가 실패한다. 그 실패가 조용한 무시가 아니라 `StaleObjectStateException`이라는 것까지는 인용 3과 같은 파일에 있지만 인용 상한 밖이다. 매핑된 `@Version`이 개입하는 또 한 경로인 플러시 시점 갱신(`DefaultFlushEntityEventListener.getNextVersion`)도 인용으로 들지 못해 문장 안의 '플러시로 갱신하는 다른 경로'로만 적었다. 네이티브 SQL로 읽는 쪽(`currentUserVersion`)은 조건부 UPDATE와 같은 말을 해서 인용에서 뺐다.

근거 `KataUser.java`, `AlertRepository.java`, `AbstractSelectLockingStrategy.java`

반대 조사자만 가져온 조항
- `hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java`

판정 — 

이유 — 

### claim-05

> 비관 경로의 두 구현은 신청 행이 아니라 두 요청이 공통으로 쥐는 `kata_users` 행을 기본 키로 찾아 쓰기 잠금을 거는 방식을 택했다. 그 행은 `@Version` 매핑이므로, 잠금이 로드가 아니라 잠금 갱신 경로로 가면 버전 열도 함께 조건으로 들어간다.

**차이** 주장이 붙인 '두 요청이 공통으로 쥐는'이라는 이유는 코드에 없다. run3 파일의 주석이 '이미 커밋되어 존재하는 행을 잠그므로 갭 잠금이 생기지 않는다'고 적지만 그것은 코드가 아니라 사람이 쓴 문장이고, 단계 1 기록도 그 문장을 테스트가 가르지 않는 항목으로 올려 두었다. 또 같은 `find` 한 줄이 세션 캐시 적중 여부에 따라 로드 SQL에 잠금을 싣는 경로와 별도 잠금문을 내는 경로로 갈리는데(`DefaultLoadEventListener`의 `lockAndLoad` 분기), 그 블록은 인용 상한 밖이라 전제 쪽에만 적었다. 잠금 갱신 경로에서는 조건에 맞는 행이 없을 때 반환이 `null`이 아니라 `StaleObjectStateException`이므로 run3의 `null` 검사가 모든 실패를 받는 방어가 아니다.

근거 `AlertRepository.java`, `AlertRepository.java`, `AbstractSelectLockingStrategy.java`

반대 조사자만 가져온 조항
- `hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/PessimisticWriteUpdateLockingStrategy.java`
- `hibernate-orm-6.6.53.Final/org/hibernate/persister/entity/AbstractEntityPersister.java`

판정 — 

이유 — 

### claim-07

> run3 구현은 저장소의 `save` 안에서 `flush`를 직접 불러, 영속성 컨텍스트의 더티 검사와 액션 큐 실행을 트랜잭션 경계가 아니라 그 호출 자리에서 수행하게 했다. 이 매핑에서는 삽입이 `persist` 시점에 이미 지연되지 않으므로 그 `flush`가 당기는 쓰기는 없다.

**차이** 주석은 'persist 시점에 INSERT가 나간다'고 적는데, 그 말이 맞다면 뒤이은 flush는 INSERT를 앞당기지 않는다. 그래서 코드가 보여 주는 선택은 '명시적 flush를 둔다'이고, 전에 쓴 '쓰기 시점을 당긴다'는 문장은 성립하지 않아 지웠다. 플러시 주기를 돌릴지 말지가 '내보낼 문장이 남았는지'가 아니라 '관리 중인 엔티티가 있는지'로 갈린다는 조항(`DefaultFlushEventListener.onFlush`)은 인용 상한 밖이라 교환 서술의 좌표로만 달았다. 같은 과제의 다른 구현이 flush 없이 persist만 부른다는 대조 인용도 상한 때문에 뺐다.

근거 `AlertRepository.java`, `AbstractFlushingEventListener.java`, `AbstractSaveEventListener.java`

반대 조사자만 가져온 조항
- `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultFlushEventListener.java`

판정 — 

이유 — 

### claim-09

> 두 재시도 구현은 시도 횟수의 상한을 설정이 아니라 코드에 박힌 상수로 고정했고, 그 상한은 각 구현이 재시도 대상으로 잡는 충돌 예외에만 적용된다.

**차이** '근거 없이 골랐다'는 평가는 코드가 아니라 구현자가 자기 평가에 적은 문장이고, 단계 1 기록이 그것을 선택 주장으로 분류해 두었다. 문장에 넣은 '잡는 충돌 예외에만 적용된다'는 인용 3이 alert-kata 쪽에서만 보이고, run3 쪽에서 같은 제한을 만드는 `catch (DataIntegrityViolationException | ConcurrencyFailureException conflict)` 블록은 인용 상한 밖이다. run3 쪽은 마지막 실패를 보관해 루프 밖에서 던지므로 예산 소진의 모양도 두 구현이 다르다.

근거 `OptimisticAlertService.java`, `AlertService.java`, `OptimisticAlertService.java`

반대 조사자만 가져온 조항
- `phase2/taskF/task4/step1/run3/impl/AlertRegistrar.java`

판정 — 

이유 — 

### claim-10

> run3 스키마는 상태에서 파생된 생성 컬럼을 유일 제약의 마지막 열로 끼워 넣어, 유일 인덱스가 NULL을 서로 다른 값으로 보는 DB에서 감시 중인 신청에만 걸리는 부분 유일 제약이 되게 했다. 이 제약은 엔티티 매핑이 아니라 손으로 쓴 스키마에만 있다.

**차이** '부분 유일 제약이 된다'는 결론은 그 NULL 성질이 참일 때만 성립해서, 그 조건을 주장 문장 안에 조건절로 넣었다. 그 성질을 정하는 자리는 DB 쪽이고, 이 payload의 1차 출처 루트(JDK·Spring·Hibernate)에도 copies에도 그것을 말하는 문서가 없다. 단계 1 기록도 이 항목을 테스트가 가르지 않는 사실 주장으로 올려 두었다. 인용 3은 생성 컬럼 DDL을 낼 수단이 매핑 쪽에 있다는 것까지만 보이고, `AlertSubscription` 엔티티에 `watching_marker` 대응 매핑이 없다는 것은 코드를 읽어야 보인다.

근거 `schema.sql`, `schema.sql`, `Dialect.java`

판정 — 

이유 — 

## 기각이 하나도 없으면

그 기록은 "AI가 맞다고 한다"를 한 겹 포장한 것이다. 게이트가 환각 인용을 전부 잡고 나면 남는 위험은 진짜 인용으로 원문보다 넓은 주장을 뒷받침하는 것이고, 그것은 사람이 원문을 열어야 보인다.

