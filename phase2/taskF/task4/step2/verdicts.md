# 단계 3 — 채택 / 기각

판정은 여기 적는다. [REPORT.md](REPORT.md) 는 하네스가 다시 쓰므로 거기 적으면 덮인다.

## 무엇을 보고 정하는가

`READY` 는 주장이 참이라는 뜻이 아니라 **대조할 자료가 갖춰졌다**는 뜻이다. 참·거짓은 이 표에서 정한다.

주장마다 한 줄만 보면 된다. **주장과 원문의 차이**다. 게이트가 인용의 실존을 이미 확인했으므로 남은 위험은 진짜 인용으로 원문보다 넓은 주장을 뒷받침하는 것이고, 그 차이가 거기 적혀 있다.

그 줄이 걸리면 리포트의 그 묶음을 연다. 사본에서 떠낸 자리가 줄 번호와 함께 있다.

## 한눈에

| id | 종류 | 축 칸 | 제한 조항 | 채택·기각 | 이유 |
| --- | --- | --- | ---: | --- | --- |
| [claim-01](#claim-01) | fact | G3 D4 | 4 | **기각** | 한정이 원문보다 좁다. 비공개 메서드도 전파 속성이 읽힌다 |
| [claim-02](#claim-02) | fact | G3 D4 | 4 | **채택** | 앞 두 성분을 로그로 확인. 보류 갈래는 조건이 바뀌면 타는 길 |
| [claim-03](#claim-03) | fact | G3 D4 | 4 | **채택** | 기본값 REQUIRED, 메서드 선언이 클래스 선언을 대체. 원문 확인 |
| [claim-04](#claim-04) | fact | G3 D4 | 4 | **기각** | '무한 대기' 조건이 범위를 잘못 그린다. 숫자 타임아웃도 같은 조각 |
| [claim-06](#claim-06) | fact | G3 D4 | 3 | **채택** | 조건식을 원문에서 확인. 마지막 칸은 스냅샷 밖이고 실측이 메운다 |
| [claim-08](#claim-08) | choice | G3 D4 T5 T4 | 3 | **기각** | 뒷문장이 인용보다 넓고, 넓힌 범위의 반례가 이 코드다 |
| [claim-05](#claim-05) | choice | G3 D4 T5 T5 | 3 | **기각** | 좌표가 잠금 없는 측정을 가리킨다. 두 구현을 묶어 차이를 가렸다 |
| [claim-07](#claim-07) | choice | G3 D4 T5 T5 | 3 | **기각** | 문장은 실험으로 확인. 좌표 둘이 내가 쓴 글을 가리킨다 |
| [claim-09](#claim-09) | choice | G3 D4 T5 T5 | 3 | **기각** | failure 대조가 run3 에 안 맞는다. 뒤집히는 값은 측정으로 확인 |
| [claim-10](#claim-10) | choice | G3 D4 T5 T4 | 2 | **기각** | 전제는 측정 확인. 인용 하나가 문장과 반대, NULL 전제에 좌표 없음 |

축 칸은 `G` grounding, `D` depth, `T` tradeoff_coverage, `T` tradeoff_validity 순이다. 사실 주장은 앞의 둘만 있다. `제한 조항` 은 반대 조사자가 그 주장을 깎으려고 가져온 인용의 수다.

## 주장별

### claim-01

> 프록시를 거쳐 전파 속성이 읽힌 공개 메서드 호출에서 `Propagation.REQUIRES_NEW`는, 기존 트랜잭션이 있으면 그것을 보류한 뒤, 없으면 그대로, 새 트랜잭션을 시작한다. 보류는 트랜잭션 관리자가 보류를 구현한 경우에만 성립한다.

**차이** 문장에 넣은 '없으면 그대로'는 같은 메서드의 다른 갈래(REQUIRED·REQUIRES_NEW·NESTED를 한 조건으로 묶어 startTransaction을 부르는 자리)에서 정해지는데, 인용 상한 때문에 그 블록을 인용으로 들지 않았다. '보류는 트랜잭션 관리자가 보류를 구현한 경우에만'은 `Propagation.java`의 REQUIRES_NEW javadoc NOTE('Actual transaction suspension will not work out-of-the-box on all transaction managers')가 정하는데 그 인용도 상한 밖으로 밀렸다. 또 비공개 메서드 필터가 기본으로 켜져 있다는 사실은 `AnnotationTransactionAttributeSource`의 `publicMethodsOnly = true` 선언이 정하고 이 인용 묶음에 없다.

근거 `AbstractFallbackTransactionAttributeSource.java`, `TransactionAspectSupport.java`, `AbstractPlatformTransactionManager.java`

반대 조사자만 가져온 조항
- `spring-framework-6.2.19/org/springframework/transaction/annotation/AnnotationTransactionAttributeSource.java`

판정 — **기각**

이유 — "공개 메서드 호출에서"라는 한정이 원문보다 좁다. 전파 속성을 거르는 필터의 값은
`AnnotationTransactionAttributeSource`의 필드 선언이 아니라 `@EnableTransactionManagement`이 만드는
빈이 정하고, 거기서 `publicMethodsOnly`를 `false`로 넘긴다.

```
spring-framework-6.2.19/org/springframework/transaction/annotation/AbstractTransactionManagementConfiguration.java:86-87
    // Accept protected @Transactional methods on CGLIB proxies, as of 6.0
    AnnotationTransactionAttributeSource tas = new AnnotationTransactionAttributeSource(false);
```

대상 코드의 `OptimisticSubscriptionAttempt.subscribe`는 패키지 전용인데도 전파 속성이 읽힌다.
트랜잭션 인터셉터 로그를 켜고 낙관적 락 테스트를 돌려 확인했다. 요청 둘과 재시도 하나에 대해
`Creating new transaction with name [com.thinking.tennis.kata.OptimisticSubscriptionAttempt.subscribe]`가
세 번 나오고 비트랜잭션 진행 로그는 0건이다.

**이 오해를 세 역할이 함께 했다.** 조사자는 `차이`에 "비공개 메서드 필터가 기본으로 켜져 있다"고 적었고,
반대 조사자는 그 근거로 같은 파일을 제한 조항으로 가져왔고, 채점자도 같은 전제로 `grounding`을 3칸에
묶었다. 셋 다 필드 선언을 읽고 그 값을 정하는 자리를 읽지 않았다. 게이트는 인용이 실재하는지만 보므로
이 오해를 잡을 수 없었고, 테스트가 잡았다.

주장의 나머지는 참이다. `JpaTransactionManager`가 `doSuspend`와 `doResume`을 구현하므로 javadoc NOTE가
경고하는 보류 실패는 이 스택에 해당하지 않는다. **기존 코드는 틀리지 않았고 그것을 설명한 주장이 틀렸다.**

다음 run에 넘길 것 — 이 자리는 `depth` 축이 봐야 했다. 인용이 필터 코드에 닿았지만 참·거짓이 갈리는
자리는 필터가 아니라 그 필터에 무엇이 넘어오는지였다. 4칸 조건의 "주장이 거짓이라면 이 자리의 문장이
달라야 한다"를 적용하면 3칸 이하였다.

### claim-02

> `Propagation.NOT_SUPPORTED`로 읽힌 호출은 실제 트랜잭션 없이 실행된다. 기존 트랜잭션이 있으면 보류하고 없으면 보류할 것이 없으며, 기본 설정(`SYNCHRONIZATION_ALWAYS`)에서는 실제 트랜잭션이 없어도 동기화 범위는 남는다. 보류는 트랜잭션 관리자가 보류를 구현한 경우에만 성립한다.

**차이** 문장의 '동기화 범위는 남는다'는 인용 2의 newSynchronization 계산 한 줄과 `SYNCHRONIZATION_ALWAYS` 상수 이름까지로만 받친다. 그 상수가 기본값이라는 사실(`transactionSynchronization = SYNCHRONIZATION_ALWAYS`)과 그 javadoc이 NOT_SUPPORTED를 명시적으로 대상에 넣는다는 사실은 같은 파일 안이지만 인용 상한 때문에 들지 않았다. 또 run3의 `AlertService.subscribe`가 보류 갈래를 타는지 바깥 트랜잭션이 없는 갈래를 타는지는 호출자에 달려 있어 코드에 적혀 있지 않다.

근거 `TransactionAspectSupport.java`, `AbstractPlatformTransactionManager.java`, `Propagation.java`

판정 — **채택**

이유 — 주장의 앞 두 성분을 테스트로 확인했다. 단계 1의 구현을 트랜잭션 인터셉터 로그와 함께 돌리니
`Getting transaction for [com.thinking.tennis.kata.AlertService.subscribe]` 는 나오는데
그 메서드에 대한 `Creating new transaction` 은 0건이고 `Suspending current transaction` 도 0건이다.
트랜잭션은 `AlertRegistrar.register` 쪽에서만 9번 열린다. "실제 트랜잭션 없이 실행된다"와
"없으면 보류할 것이 없으며"가 그대로 성립한다.

남은 두 성분은 확인하지 못했다. 보류 갈래는 대상 코드가 타지 않아 로그에 나오지 않고,
`SYNCHRONIZATION_ALWAYS` 에서 동기화 범위가 남는다는 것은 인터셉터 로그가 보여 주지 않는다.
그래도 채택한다. **지금 안 타는 갈래가 아니라 조건이 바뀌면 타는 갈래이기 때문이다.** 나중에 누가
`subscribe` 를 `@Transactional` 메서드 안에서 부르면 그 순간 보류 갈래로 넘어가고 재시도가 바깥
트랜잭션과 엉킨다. 전제가 깨지는 조건을 적어 둔 것이 이 주장의 쓸모다.

이 주장이 받치는 주석은 `AlertService.java:20` 이다. "재시도는 트랜잭션 밖에서 돌린다. 시도들이 한
트랜잭션에 함께 들어가면 실패한 시도의 롤백이 다음 시도까지 끌고 간다." 주석이 맞다는 것까지 확인됐다.

### claim-03

> propagation을 적지 않은 메서드 수준 `@Transactional`의 전파 속성은 REQUIRED이고, 이 값은 클래스 수준 선언을 물려받는 대신 그것을 대체한다.

**차이** 원문은 선언된 기본값과 조회 순서까지만 정한다. 그 기본값이 실제 호출에 적용되려면 프록시를 거쳐야 하고, 같은 파일의 바로 앞 블록이 설정에 따라 비공개 메서드를 걸러 null을 돌려주므로 비공개 메서드에서는 전파 속성이 REQUIRED가 되는 것이 아니라 아예 비트랜잭션이 된다. 그 블록은 claim-01의 인용 묶음에 들어 있다. 선언 기본값이 트랜잭션 관리자가 보는 `getPropagationBehavior()`로 옮겨지는 자리(`SpringTransactionAnnotationParser.parseTransactionAnnotation`)도 인용 상한 밖이다.

근거 `Transactional.java`, `Propagation.java`, `AbstractFallbackTransactionAttributeSource.java`

판정 — **채택**

이유 — 두 성분을 원문에서 확인했다. 기본값은 `Transactional.java:175` 의
`Propagation propagation() default Propagation.REQUIRED;` 이고, 대체인지 물려받음인지는 조회 코드가
정한다.

```
spring-framework-6.2.19/org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java:179-185
    // First try is the method in the target class.
    TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
    if (txAttr != null) {
        return txAttr;
    }
    // Second try is the transaction attribute on the target class.
    txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
```

메서드 선언이 있으면 그 자리에서 돌려주고 클래스 선언은 보지도 않는다. 병합이 아니라 `return` 이다.
javadoc 도 클래스 수준을 "applies as a default" 로 적는다.

반대 조사자가 가져온 조항 넷 중 셋은 주장을 깎는 것이 아니라 **주장이 말하지 않은 범위**였다.
조상 클래스로 내려오지 않는다는 것과 인터페이스 선언 메서드에서 조회가 네 단계가 된다는 것과
`ClassUtils.isUserLevelMethod` 조건이 클래스 쪽에만 붙는다는 것은 전부 참이지만, 주장 문장은
메서드 수준과 클래스 수준 둘만 다루므로 그 범위 밖이다. 넷째인 가시성 게이트는 claim-01 에서
이미 틀린 것으로 확인됐다.

**쓸모 있는 자리** — 지금 `AlertService` 와 `PessimisticAlertService` 에는 클래스 수준 선언이 없어
차이가 없다. 그런데 누가 클래스에 `@Transactional(isolation = ...)` 을 달면 메서드의 맨몸
`@Transactional` 이 그것을 대체해서 격리 설정이 조용히 사라진다. 메서드 쪽이 이기는 것이 아니라
클래스 쪽을 안 보는 것이라 경고도 없다. 동시성 코드에서 조용히 깨지는 자리다.

### claim-04

> MySQL 방언은 `PESSIMISTIC_WRITE` 잠금에 대해, 대기 설정이 무한 대기이고 별칭을 지정하지 않은 경우 SQL 끝에 붙일 조각으로 " for update"를 만든다. 이 자리가 정하는 것은 조각을 만드는 데까지이고, 그 조각이 어느 SQL에 실려 나가는지는 정하지 않는다.

**차이** 문장에 조건으로 박은 '무한 대기'와 '별칭 없음'은 `MySQLDialect.withTimeout`(NO_WAIT·SKIP_LOCKED에 " nowait"·" skip locked"를 덧붙인다)과 `MySQLDialect.getForUpdateString(String aliases)`(별칭을 받으면 " for update of <별칭>")가 정하는데, PESSIMISTIC_WRITE에서 " for update"에 닿는 세 고리가 인용 상한을 다 써서 그 두 블록을 인용으로 들지 못했다. 조각이 실리는 SQL도 이 자리 밖이다. 잠금 갱신 경로에서는 `AbstractSelectLockingStrategy.generateLockString`이 엔티티 로드 SQL이 아니라 식별자 한 열만 뽑는 별도 SELECT를 만들고, 버전 매핑된 엔티티에는 버전 열 조건을 더 넣는다. 그래서 문장을 조각 생성까지로 좁혔다.

근거 `Dialect.java`, `MySQLDialect.java`, `Dialect.java`

판정 — **기각**

이유 — "대기 설정이 무한 대기이고"라는 조건이 조각을 설명하지 못한다. MySQL 에서는 숫자 타임아웃도
같은 조각을 만든다.

```
hibernate-orm-6.6.53.Final/org/hibernate/dialect/MySQLDialect.java:1409-1420
    private String withTimeout(String lockString, int timeout) {
        switch (timeout) {
            case LockOptions.NO_WAIT:      return supportsNoWait() ? lockString + " nowait" : lockString;
            case LockOptions.SKIP_LOCKED:  return supportsSkipLocked() ? lockString + " skip locked" : lockString;
            case LockOptions.WAIT_FOREVER: return lockString;
            default:                       return supportsWait() ? lockString + " wait " + ... : lockString;

hibernate-orm-6.6.53.Final/org/hibernate/dialect/MySQLDialect.java:1516-1519
    public boolean supportsWait() {
        //only supported on MariaDB
        return false;
    }
```

`supportsWait()` 가 거짓이므로 `default` 갈래가 꼬리를 못 붙인다. 네 갈래 중 둘이 `" for update"` 다.

| 대기 설정 | 조각 |
| --- | --- |
| `WAIT_FOREVER` | `" for update"` |
| 숫자 타임아웃 | `" for update"` — `supportsWait()` 가 거짓이라 무시된다 |
| `NO_WAIT` | `" for update nowait"` |
| `SKIP_LOCKED` | `" for update skip locked"` |

claim-01 과 기각 이유가 다르다. 거기서는 한정이 **틀렸고**, 여기서는 한정이 거짓은 아니지만 **범위를
잘못 그린다.** "무한 대기인 경우에 만든다"를 읽은 사람은 무한 대기가 아니면 다른 조각이 나온다고
믿는다. 그래서 이 조건은 조각을 설명하는 것이 아니라 **잠금 대기 시간을 코드로 줄 수 있다는 착각**을
만든다. 동시성 코드에서 `@Lock` 타임아웃을 숫자로 주고 그것이 걸렸다고 믿게 되는 자리다.

맞게 적자면 — MySQL 방언은 `PESSIMISTIC_WRITE` 잠금에 대해 별칭을 지정하지 않으면 `" for update"` 를
만든다. `NO_WAIT` 와 `SKIP_LOCKED` 에서만 꼬리가 붙고 숫자 타임아웃은 무시된다.

주장의 나머지는 참이다. 조각 생성까지만 정한다는 뒷문장도 참이고, Gen 이 인용한
`case PESSIMISTIC_WRITE: return getWriteLockString( timeout );` 가 `private` 오버로드 안에 있다는
반대 조사자의 지적도 사실이지만 결론은 다르다. 공개 진입점 둘이 전부 그 비공개 메서드로 들어가므로
거기가 `PESSIMISTIC_WRITE` 를 갈라 보내는 유일한 자리다. `private` 은 외부에서 부를 수 없다는 뜻일
뿐이고 참·거짓이 갈리는 자리라는 사실은 바뀌지 않는다.

**하네스 쪽 기록** — 이 기각의 근거가 `limiting_clause_missed` 관찰에서 나왔다. run3 에서 그 규칙을
위반에서 관찰로 내렸는데, 내린 판단은 유지한다. 위반으로 두면 Gen 이 Critique 이 찾을 조항을 미리 다
인용해야 하고 인용 상한 안에서 불가능하다. 다만 **관찰 하나가 주장을 기각시킬 수 있다**는 것이 이 run
의 기록이다. 리포트는 관찰을 "먼저 열어 볼 자리"로만 적었는데 실제로는 판정을 뒤집는 자리였다.

**내가 틀렸던 자리** — 처음 검토에서 "숫자 타임아웃이면 조각이 달라진다"고 적어 반대 조사자의 지적을
기각했다. `default` 갈래의 삼항을 보고 `supportsWait()` 가 참이라고 가정했고 확인하지 않았다.
`MySQLDialect` 가 그것을 거짓으로 재정의한다. 확인하지 않은 가정으로 반증을 물리친 것이
claim-01 에서 세 역할이 함께 한 실수와 같은 모양이다.

### claim-06

> 트랜잭션이 진행 중일 때 TRANSIENT 인스턴스를 DB가 키를 만드는 매핑으로 `persist`하면 삽입이 지연되지 않고 생성된 키를 그 자리에서 읽을 수 있다.

**차이** 주장은 '삽입이 지연되지 않는다'까지만 쓸 수 있고, 'INSERT 문이 persist 호출 안에서 DB로 나간다'는 더 강한 문장은 이 인용으로 갈리지 않는다. 실행 시점을 정하는 ActionQueue는 이 payload의 출처 루트에 들어 있지 않다. generatedOnExecution이 참인지(= IDENTITY 전략인지)를 정하는 Generator 쪽과, 엔티티가 Hibernate의 `Lifecycle`을 구현해 `onSave`가 삽입을 거부하면 identity 매핑에서 돌려주는 id가 `null`이 되는 갈래(`invokeSaveLifecycle`)도 인용 상한 밖이다. 대상 엔티티들은 `Lifecycle`을 구현하지 않는다.

근거 `DefaultPersistEventListener.java`, `AbstractSaveEventListener.java`, `AbstractSaveEventListener.java`

판정 — **채택**

이유 — 주장 문장이 자기가 받칠 수 있는 선에서 멈췄고, 그 선까지는 원문이 받친다.

```
hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultPersistEventListener.java:177
    saveWithGeneratedId( entity, event.getEntityName(), createCache, source, false );
                                                                            ^ requiresImmediateIdAccess

hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractSaveEventListener.java:132-135
    final boolean delayIdentityInserts =
            !source.isTransactionInProgress()
                    && !requiresImmediateIdAccess
                    && generatedOnExecution;
```

세 조건의 AND 다. `persist` 가 `false` 를 넘기니 둘째가 참이고, 트랜잭션 중이면 첫째가 거짓이라
전체가 거짓이 된다. 지연되지 않는다. 주장의 두 조건이 그 조건식을 그대로 옮긴 것이다.

**사슬의 마지막 한 칸은 스냅샷 밖이다.** `handleGeneratedId` 가 `insert.isEarlyInsert()` 일 때만
생성된 키를 돌려주는데, `isEarlyInsert()` 의 정의와 `EntityIdentityInsertAction` 이 둘 다
`org/hibernate/action/internal/` 에 있고 그 패키지를 스냅샷 include 목록에 넣지 않았다. 그래서
"`delayIdentityInserts` 가 거짓이면 `isEarlyInsert()` 가 참이고 INSERT 가 이미 나갔다"는 이름과
javadoc(`@param delayIdentityInserts Should the identity insert be delayed?`)으로 추론한 것이다.

**그 칸을 실측이 메운다.** 단계 1 로그에서 반환 ID 가 조건마다 106~205, 206~305, 306~405 으로 정확히
100칸이다. 코드는 `persist` 직후에 `.id()` 를 읽어 반환하는데 실제 값이 나왔고, 지연됐다면 그 자리의
id 가 비어 테스트가 깨진다. AUTO_INCREMENT 가 한 칸도 건너뛰지 않았으므로 INSERT 가 회차마다 한 번씩
그 자리에서 나갔다.

반대 조사자의 조항 셋은 주장을 깎지 않는다. 라이프사이클 콜백이 저장을 거부하는 경우와
`requiresImmediateIdAccess` 를 참으로 넘기는 다른 호출자와 identity 가 아닌 매핑은 모두 주장이 단
조건 밖이다. 대상 엔티티는 `Lifecycle` 을 구현하지 않는다.

**주석은 주장보다 더 나갔다.** run3 의 `AlertRepository.java:46` 이 "IDENTITY 키라 persist 시점에
INSERT 가 나가고, **유일 제약 위반도 이 자리에서 드러난다**"고 적는데, 뒷문장은 이 스냅샷으로 확인할
수 없고 단계 1 의 세 run 모두 그 경로를 한 번도 실행하지 않았다. 주장은 그 선을 넘지 않았으니 주장의
흠이 아니고, 주석 쪽이 미검증으로 남는다.

**하네스 쪽 기록 둘**

첫째, 스냅샷 include 목록에 `org/hibernate/action/` 과 `org/hibernate/engine/spi/ActionQueue.java` 가
없다. 주장이 멈춘 선이 원문의 한계가 아니라 **내가 수집한 범위의 한계**였다. claim-07 의 `flush` 도
같은 패키지를 봐야 하므로 다음 run 전에 추가한다.

둘째, `anchor_absent` 가 `symbols` 중 하나만 맞으면 통과한다. 이 주장의 `symbols` 에 들어 있는
`EntityIdentityInsertAction` 은 스냅샷에 없는데 나머지 셋이 맞아 통과했다. **닿을 수 없는 심볼을
목록에 섞어도 게이트가 구별하지 않는다.** 섞으면 더 깊은 자리까지 닿은 것처럼 보인다. 규칙을
"모든 심볼이 스냅샷 안에 실재해야 한다"로 고치는 것이 맞다.

### claim-08

> 낙관 경로 구현은 엔티티에 `@Version` 필드를 두고도 충돌 판정을 JPA의 버전 검사가 아니라 네이티브 SQL의 조건부 UPDATE로 한다. 매핑된 `@Version`은 이 경로에 개입하지 않지만, 같은 엔티티를 잠그거나 플러시로 갱신하는 다른 경로에서는 여전히 조건으로 들어간다.

**차이** 네이티브 UPDATE는 영속성 컨텍스트를 우회하므로 같은 트랜잭션 안의 KataUser 인스턴스가 들고 있는 값이 실제와 벌어질 수 있고, 그러면 버전 조건을 붙이는 잠금 경로가 실패한다. 그 실패가 조용한 무시가 아니라 `StaleObjectStateException`이라는 것까지는 인용 3과 같은 파일에 있지만 인용 상한 밖이다. 매핑된 `@Version`이 개입하는 또 한 경로인 플러시 시점 갱신(`DefaultFlushEntityEventListener.getNextVersion`)도 인용으로 들지 못해 문장 안의 '플러시로 갱신하는 다른 경로'로만 적었다. 네이티브 SQL로 읽는 쪽(`currentUserVersion`)은 조건부 UPDATE와 같은 말을 해서 인용에서 뺐다.

근거 `KataUser.java`, `AlertRepository.java`, `AbstractSelectLockingStrategy.java`

반대 조사자만 가져온 조항
- `hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java`

판정 — **기각**

이유 — 앞문장은 참이고 좌표 넷도 전부 맞는데, 뒷문장이 인용이 받치는 범위보다 넓고 넓힌 범위에서
거짓이며 **거짓이 되는 경로가 바로 이 코드가 타는 경로**다.

**좌표는 넷 다 맞다.** claim-05 와 claim-07 과 다른 자리다.

| 차원 | 좌표 | 거기 있는 것 |
| --- | --- | --- |
| `latency` | `lock-comparison-results.md:17-19` | 실행 3의 수치와 두 전략 중앙값 비교 |
| `resource` | `:21-23` | 두 구현의 자원 성격 서술 |
| `failure` | `:21` | "두 전략 모두 중복 0회, 요청 오류 0회" |
| `complexity` | `:23` | version SQL·REQUIRES_NEW·재시도를 복잡도로 꼽은 줄 |

수치도 맞다. 15.569ms 와 11.831ms 가 19줄에 그대로 있다.

**뒷문장이 문제다.** "같은 엔티티를 잠그거나 플러시로 갱신하는 다른 경로에서는 여전히 조건으로
들어간다"인데, 그 근거로 든 인용은 하나뿐이다.

```
hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/AbstractSelectLockingStrategy.java:65-66
    if ( lockable.isVersioned() ) {
        select.addRestriction( lockable.getVersionColumnName() );
    }
```

이 인용은 **select 기반 잠금 전략에서만** 그렇다는 것을 보인다. 문장은 "잠그는 다른 경로"라고 조건
없이 적었다. 그리고 실측한 결과 이 코드가 타는 잠금 경로에는 버전이 들어가지 않는다.

```
select ku1_0.id,ku1_0.version from kata_users ku1_0 where ku1_0.id=? for update
```

로드하면서 잠그는 길이라 `WHERE` 에 `id` 하나뿐이다. 즉 문장이 넓힌 범위의 반례가 이 코드 자신이다.

**claim-02 와 무엇이 다른가** — 세 조건이 겹쳐야 기각이고, 하나라도 빠지면 채택했을 자리다.

| | claim-02 | claim-08 |
| --- | --- | --- |
| 인용이 문장을 받치나 | 받친다 | 받치는 범위보다 넓다 |
| 안 타는 갈래를 말하나 | 말한다 | 말한다 |
| 그 갈래가 조건 바뀌면 타나 | 탄다 | 지금 타는 경로가 반례다 |

**플러시 쪽 절반은 일반 사실로 참이다.** `UpdateCoordinatorStandard` 가 버전 매핑이 있으면 따로 다루고
`applyVersionBasedOptLocking` 이 `OptimisticLockStyle.VERSION` 일 때 UPDATE 제약에 버전을 넣는다.
다만 이 코드는 그 경로를 타지 않는다. `KataUser` 에 매핑된 상태가 `id` 와 `version` 둘뿐이고
애플리케이션이 그 둘을 바꿀 수단이 없어 더티가 되지 않는다. 그러니 이 절반은 기각 사유가 아니다.

**부수 사유 — 자기가 덮은 지점 하나를 설명하지 않는다.** 주장이 좌표로
`AlertRepository.java:38` 을 달았는데 그 줄은 version 을 **읽는** 네이티브 SELECT 다. 주장과 인용은
갱신하는 UPDATE 만 다룬다. 이 설계의 핵심은 읽기와 갱신이 서로 다른 왕복이고 그 사이에 끼어들어도
안전한 이유가 `WHERE version = ?` 인데, 주장이 그 말을 하지 않는다. F-1 의 Read-Modify-Write 를 DB
조건으로 막는 것이 정확히 이 자리다.

**내가 먼저 과하게 말한 것** — 처음 검토에서 "두 경로 둘 다 이 코드에서 성립하지 않는다"고 하고
"setter 가 없으니 플러시 경로가 불가능하다"고 적었다. 플러시 쪽 일반 사실은 참이고, 불가능한 이유도
setter 유무가 아니라 애플리케이션이 그 필드를 바꿀 수단이 없다는 것이다. 다시 보라는 말에 열어 보고
고쳤다.

### claim-05

> 비관 경로의 두 구현은 신청 행이 아니라 두 요청이 공통으로 쥐는 `kata_users` 행을 기본 키로 찾아 쓰기 잠금을 거는 방식을 택했다. 그 행은 `@Version` 매핑이므로, 잠금이 로드가 아니라 잠금 갱신 경로로 가면 버전 열도 함께 조건으로 들어간다.

**차이** 주장이 붙인 '두 요청이 공통으로 쥐는'이라는 이유는 코드에 없다. run3 파일의 주석이 '이미 커밋되어 존재하는 행을 잠그므로 갭 잠금이 생기지 않는다'고 적지만 그것은 코드가 아니라 사람이 쓴 문장이고, 단계 1 기록도 그 문장을 테스트가 가르지 않는 항목으로 올려 두었다. 또 같은 `find` 한 줄이 세션 캐시 적중 여부에 따라 로드 SQL에 잠금을 싣는 경로와 별도 잠금문을 내는 경로로 갈리는데(`DefaultLoadEventListener`의 `lockAndLoad` 분기), 그 블록은 인용 상한 밖이라 전제 쪽에만 적었다. 잠금 갱신 경로에서는 조건에 맞는 행이 없을 때 반환이 `null`이 아니라 `StaleObjectStateException`이므로 run3의 `null` 검사가 모든 실패를 받는 방어가 아니다.

근거 `AlertRepository.java`, `AlertRepository.java`, `AbstractSelectLockingStrategy.java`

반대 조사자만 가져온 조항
- `hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/PessimisticWriteUpdateLockingStrategy.java`
- `hibernate-orm-6.6.53.Final/org/hibernate/persister/entity/AbstractEntityPersister.java`

판정 — **기각**

이유 — 메커니즘 문장은 참인데 트레이드오프의 좌표가 측정하지 않은 것을 가리키고, 두 구현을 하나로
묶어 서로 다른 동작을 가렸다.

**먼저 메커니즘은 참이다.** MySQL 기본 방언에서 `PESSIMISTIC_WRITE` 는 select 기반 전략으로 가고,
그 전략이 버전 매핑된 엔티티에 버전 열 조건을 더한다.

```
hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java:2194-2195
    case PESSIMISTIC_WRITE:
        return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );

hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/AbstractSelectLockingStrategy.java:62-66
        .addColumn( lockable.getRootTableIdentifierColumnNames()[0] )
        .addRestriction( lockable.getRootTableIdentifierColumnNames() );
    if ( lockable.isVersioned() ) {
        select.addRestriction( lockable.getVersionColumnName() );
    }
```

귀결이 주장보다 크다. 반대 조사자의 첫 조항대로, 버전이 어긋나면 행이 안 잡혀 **잠금을 기다리는 것이
아니라 `StaleObjectStateException` 으로 끝난다.** run3 의 `null` 검사는 그것을 받지 못한다.

**그런데 대상 코드는 그 길로 가지 않는다.** SQL 로그를 켜고 비관 구현을 돌려 실측했다.

```
select ku1_0.id,ku1_0.version from kata_users ku1_0 where ku1_0.id=? for update
```

WHERE 에 `id` 하나뿐이고 `version` 은 없다. SELECT 목록의 `version` 은 엔티티를 로드하면서 함께 읽는
컬럼이지 조건이 아니다. 트랜잭션 첫 줄에서 잠그니 그 엔티티가 세션에 아직 없고, Hibernate 가 로드
SQL 에 `for update` 를 실어 보낸다. 별도 잠금문을 낼 이유가 없다.

그래서 **version 으로 잠기는 것이 아니다.** 잠기는 것은 `kata_users` 행이고 version 은 두 번째 길에서
그 행을 찾는 조건으로만 붙는다. 조건이 맞아야 행을 찾고, 찾은 뒤에 그 행을 잠근다.

이 문장은 claim-02 와 같은 모양이라 그 기준으로는 채택이다. 지금 안 타는 길이지만 누가
`lockUserForUpdate` 앞에서 사용자를 한 번 읽어 두면 그 순간 두 번째 길로 넘어간다. 낙관 경로가
네이티브 SQL 로 버전을 올려 메모리 값이 뒤처진 상태라면 더 그렇다.

**기각 사유 1 — 좌표가 측정하지 않은 것을 가리킨다.** `failure` 차원이 "같은 테스트에서 SERIALIZABLE
로 잠금 범위를 넓혔을 때는 100회 모두 교착이 났다"고 적고 `uncoordinated-isolation-results.md:19-21`
을 좌표로 댔다. 그 파일의 머리가 "이번 실행은 `NEW_UNCOORDINATED` 조건만 대상으로 했다"이고, 그것은
**잠금이 없는 F-2 기본 서비스**의 측정이다. 비관적 락을 걸고 SERIALIZABLE 을 적용한 측정은 없다.

`tradeoff_validity` 가 5칸인데 2칸 조건에 걸린다. 2칸이 "좌표가 가리키는 것이 그 항목을 뒷받침하지
않는 것이 있다"다. 채점자가 좌표의 실존만 보고 그 좌표가 무엇을 측정한 것인지는 보지 않았다.
게이트도 같다. `metric_coordinate_absent` 는 파일과 줄 범위의 실존만 센다.

**기각 사유 2 — 두 구현을 하나로 묶었다.** `alert-kata` 의 `lockUserForUpdate` 는 반환값을 버리고,
run3 의 `lockSubscriber` 는 `null` 이면 `IllegalStateException` 을 던진다. 사용자 행이 없을 때 동작이
다르다. 그 차이가 바로 위에서 본 빗나간 방어라서, 묶은 탓에 가장 중요한 차이가 가려졌다.

**이름도 틀렸다.** 주장이 "잠금 갱신 경로"라고 적었는데 MySQL 기본 방언에서 `PESSIMISTIC_WRITE` 는
select 기반 전략을 쓴다. `PessimisticWriteUpdateLockingStrategy` 는 도달하지 않는다. 인용한 클래스는
맞는데 부른 이름이 틀려서, 읽는 사람이 UPDATE 가 나간다고 믿게 된다.

**다음 run에 넘길 것** — 좌표가 실존하는지와 그 좌표가 그 항목을 측정한 것인지는 다르다. 게이트는
앞만 보고 축은 뒤를 보기로 했는데 축이 그 일을 하지 않았다. `tradeoff_validity` 의 2칸 조건에
"좌표가 가리키는 측정이 주장한 조건에서 나온 것인가"를 명시해야 한다.

### claim-07

> run3 구현은 저장소의 `save` 안에서 `flush`를 직접 불러, 영속성 컨텍스트의 더티 검사와 액션 큐 실행을 트랜잭션 경계가 아니라 그 호출 자리에서 수행하게 했다. 이 매핑에서는 삽입이 `persist` 시점에 이미 지연되지 않으므로 그 `flush`가 당기는 쓰기는 없다.

**차이** 주석은 'persist 시점에 INSERT가 나간다'고 적는데, 그 말이 맞다면 뒤이은 flush는 INSERT를 앞당기지 않는다. 그래서 코드가 보여 주는 선택은 '명시적 flush를 둔다'이고, 전에 쓴 '쓰기 시점을 당긴다'는 문장은 성립하지 않아 지웠다. 플러시 주기를 돌릴지 말지가 '내보낼 문장이 남았는지'가 아니라 '관리 중인 엔티티가 있는지'로 갈린다는 조항(`DefaultFlushEventListener.onFlush`)은 인용 상한 밖이라 교환 서술의 좌표로만 달았다. 같은 과제의 다른 구현이 flush 없이 persist만 부른다는 대조 인용도 상한 때문에 뺐다.

근거 `AlertRepository.java`, `AbstractFlushingEventListener.java`, `AbstractSaveEventListener.java`

반대 조사자만 가져온 조항
- `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultFlushEventListener.java`

판정 — **기각**

이유 — 주장 문장은 실험으로 확인됐는데 트레이드오프의 좌표 둘과 전제 하나가 틀렸다.

**주장 문장은 참이다.** run3 구현을 SQL 로그와 함께 두 번 돌렸다. 한 번은 그대로, 한 번은
`entityManager.flush()` 를 주석 처리하고.

| | SQL 문장 | INSERT | 잠금 SELECT | 테스트 |
| --- | ---: | ---: | ---: | --- |
| `flush()` 있음 | 26 | 8 | 9 | 5/5 통과 |
| `flush()` 없음 | 26 | 8 | 9 | 5/5 통과 |

나가는 SQL 이 한 줄도 달라지지 않는다. claim-06 에서 확인한 대로 IDENTITY 매핑이고 트랜잭션 중이라
INSERT 가 `persist` 자리에서 이미 나갔고, `flush()` 가 당길 쓰기가 남아 있지 않다.

**그래서 run3 의 주석이 자기 모순이다.** `AlertRepository.java:46` 이 "IDENTITY 키라 persist 시점에
INSERT 가 나가고, 유일 제약 위반도 이 자리에서 드러난다"고 적는데, 앞문장이 맞으면 뒤의 `flush()` 가
할 일이 없다. 유일 제약 위반도 `flush()` 가 아니라 `persist` 자리에서 드러난다. 둘이 같은 메서드 안이라
결과는 같지만 설명은 틀렸다.

**기각 사유 1 — 좌표 둘이 측정이 아니라 내가 쓴 글을 가리킨다.**

| 차원 | 좌표 | 실제로 무엇인가 |
| --- | --- | --- |
| `failure` | `step1/run3/verdict.md:37-41` | 내가 쓴 판정문 |
| `complexity` | `step1/findings.md:57` | 내가 쓴 표의 한 줄 |

가리킬 것은 `step1/run3/test.log` 였다. 설계가 `measured` 등급을 "본인의 측정 로그"로 정의했는데
그 루트 안에 로그와 내 글이 섞여 있고 등급이 둘을 구별하지 못한다. 측정을 요약한 글을 좌표로 받으면
전제의 근거가 한 겹 멀어지고, 요약이 틀렸을 때 그것을 가릴 자리가 없어진다.

**기각 사유 2 — `resource` 전제의 이유가 틀렸다.** 주장은 플러시 주기가 도는 조건이 참인 이유로
"`lockSubscriber` 가 `KataUser` 를 올려 뒀다"를 적었다. 반대 조사자가 짚은 대로 `save` 가 불릴 때는
방금 `persist` 한 `AlertSubscription` 자체가 이미 관리 대상이라 `lockSubscriber` 가 필요하지 않다.
결론은 맞고 이유가 틀렸다.

**하네스 쪽 기록** — `measured` 루트를 로그와 글로 갈라야 한다. 지금 `phase2/taskF/task4/step1/` 전체가
한 등급인데 거기에는 `test.log` 와 `self-assessment.md` 와 내가 쓴 `verdict.md` 와 `findings.md` 가 함께
있다. 측정은 기계가 찍은 것이고 나머지는 사람과 AI 가 쓴 글이다. 등급을 `measured` 와 `prose` 로 갈라
좌표가 글을 가리키면 탈락시키는 것이 맞다.

### claim-09

> 두 재시도 구현은 시도 횟수의 상한을 설정이 아니라 코드에 박힌 상수로 고정했고, 그 상한은 각 구현이 재시도 대상으로 잡는 충돌 예외에만 적용된다.

**차이** '근거 없이 골랐다'는 평가는 코드가 아니라 구현자가 자기 평가에 적은 문장이고, 단계 1 기록이 그것을 선택 주장으로 분류해 두었다. 문장에 넣은 '잡는 충돌 예외에만 적용된다'는 인용 3이 alert-kata 쪽에서만 보이고, run3 쪽에서 같은 제한을 만드는 `catch (DataIntegrityViolationException | ConcurrencyFailureException conflict)` 블록은 인용 상한 밖이다. run3 쪽은 마지막 실패를 보관해 루프 밖에서 던지므로 예산 소진의 모양도 두 구현이 다르다.

근거 `OptimisticAlertService.java`, `AlertService.java`, `OptimisticAlertService.java`

반대 조사자만 가져온 조항
- `phase2/taskF/task4/step1/run3/impl/AlertRegistrar.java`

판정 — **기각**

이유 — 주장 문장은 코드에서 바로 확인되고 `뒤집히는 값` 한 줄이 이 묶음에서 가장 쓸모 있는 자리인데,
`failure` 차원의 대조가 두 구현 중 한쪽에만 맞는다.

**주장 문장은 참이다.** F-3 은 `MAX_ATTEMPTS = 5`, 단계 1 은 `3` 이고 둘 다 코드 상수다. 잡는 예외도
한쪽은 자기 도메인 예외 하나, 다른 쪽은 `DataIntegrityViolationException | ConcurrencyFailureException`
둘이라 그 계열 밖의 실패는 재시도 없이 올라간다. 주장이 "각 구현이 재시도 대상으로 잡는 충돌 예외에만
적용된다"로 그 차이까지 문장에 담았다.

**기각 사유 — `failure` 대조가 run3 에 성립하지 않는다.** 그 차원이 "예산을 다 쓰면 실패가 요청 오류로
호출자에게 나간다. 대안인 비관 잠금에는 이 실패 양상 자체가 없다"고 적었다. F-3 의 낙관 구현에는
맞는다. 비관과 낙관이 둘 중 하나를 고르는 관계이기 때문이다.

run3 에는 맞지 않는다. run3 는 사용자 행 잠금과 유일 제약과 재시도를 **다 가졌다.** 재시도가 비관
잠금의 대안이 아니라 그 위에 얹힌 2차 방어이고, 대조 관계 자체가 없다. claim-05 와 같은 모양으로
**주장이 두 구현을 함께 덮는데 서술이 한쪽에만 맞는다.**

**좌표** — 넷 중 셋은 맞다. `latency` 와 `resource` 가 가리킨 `lock-comparison-results.md:15-17` 은
실행 1~3 의 표이고 충돌·재시도 100회가 서술과 맞는다. `complexity` 의 `:25` 는 충돌률을 측정해
선택해야 한다는 결론 줄이다. `failure` 의 `run3/verdict.md:37-41` 은 또 내가 쓴 글이고, 그 항목의
본 주장("예산을 다 쓰면 호출자에게 나간다")은 측정이 아니라 코드 사실이라 좌표가 코드를 가리켜야 했다.

다만 반대 조사자가 "관찰되지 않은 양상을 관찰 기록으로 가리켰다"고 신고한 것은 과하다. 그 서술의
마지막 절이 "run3 쪽에서는 재시도 경로가 한 번도 실행되지 않아 이 양상이 관찰된 적이 없다"로 스스로
밝히고 있고, 좌표가 그 절을 받친다.

**`뒤집히는 값` 한 줄을 측정했다.** 주장이 `latency` 차원에 이렇게 적었다.

> 동시 요청이 셋 이상이면 밀린 쪽이 다시 밀리는 연쇄가 생겨 시도 수가 예산에 닿는다.

같은 사용자에게 동시 요청 N 개, 회차 30회, 상한 5로 측정했다. 코드는
[step3/RetryBudgetProbeTest.java](../step3/RetryBudgetProbeTest.java), 로그는
[step3/retry-budget-probe.log](../step3/retry-budget-probe.log) 다.

| 동시 요청 | 회차당 충돌 | 요청당 충돌 | 요청 오류 | 중복 |
| ---: | ---: | ---: | ---: | ---: |
| 2 | 1.00 | 0.50 | 0 | 0 |
| 3 | 3.00 | 1.00 | 0 | 0 |
| 5 | 9.00 | 1.80 | 0 | 0 |
| 8 | 17.00 | 2.13 | **58** | 0 |

요청 둘에서는 F-3 측정과 같다. 여덟에서 240개 요청 중 58개가 상한 5를 다 쓰고 실패했다. 요청당 충돌이
0.50 → 1.00 → 1.80 → 2.13 으로 올라가므로 한 요청이 겪는 충돌 횟수 자체가 늘어난다. 밀린 쪽이 다시
밀린다. 중복은 전 구간 0 이라 **정확성은 지켜지고 가용성이 깨진다.**

**그래서 이 주장의 값은 그 한 줄이다.** F-3 측정은 요청 둘뿐이었고, 둘에서는 상한이 3이든 5든 차이가
없다. 값을 고를 근거가 되는 구간을 측정한 적이 없었고, 그 구간을 지목한 것이 이 주장이다. 루브릭이
`tradeoff_validity` 5칸에 "전제가 뒤집히는 값을 지목했다"를 둔 이유가 이 자리에서 드러났다.

### claim-10

> run3 스키마는 상태에서 파생된 생성 컬럼을 유일 제약의 마지막 열로 끼워 넣어, 유일 인덱스가 NULL을 서로 다른 값으로 보는 DB에서 감시 중인 신청에만 걸리는 부분 유일 제약이 되게 했다. 이 제약은 엔티티 매핑이 아니라 손으로 쓴 스키마에만 있다.

**차이** '부분 유일 제약이 된다'는 결론은 그 NULL 성질이 참일 때만 성립해서, 그 조건을 주장 문장 안에 조건절로 넣었다. 그 성질을 정하는 자리는 DB 쪽이고, 이 payload의 1차 출처 루트(JDK·Spring·Hibernate)에도 copies에도 그것을 말하는 문서가 없다. 단계 1 기록도 이 항목을 테스트가 가르지 않는 사실 주장으로 올려 두었다. 인용 3은 생성 컬럼 DDL을 낼 수단이 매핑 쪽에 있다는 것까지만 보이고, `AlertSubscription` 엔티티에 `watching_marker` 대응 매핑이 없다는 것은 코드를 읽어야 보인다.

근거 `schema.sql`, `schema.sql`, `Dialect.java`

판정 — **기각**

이유 — 설계가 기대는 전제는 측정으로 확인됐는데, 인용 하나가 자기 문장과 반대되는 자리이고 가장
중요한 전제에 1차 출처 좌표가 없다.

**전제는 측정으로 확인됐다.** 일회용 MySQL 8.4.8 컨테이너에 run3 스키마를 올리고 직접 넣어 봤다.
코드는 [step3/NullDistinctProbeTest.java](../step3/NullDistinctProbeTest.java), 로그는
[step3/null-distinct-probe.log](../step3/null-distinct-probe.log) 다.

| 한 일 | 결과 |
| --- | --- |
| 같은 조건 `WATCHING` 첫째 | OK |
| 같은 조건 `WATCHING` 둘째 | 거부 — `Duplicate entry '1-10-2026-10-10-09:00-10:00-1'` |
| 같은 조건 `ENDED` 셋 | 셋 다 OK |
| 상태를 `ENDED` 로 바꾼 뒤 같은 조건 재신청 | OK |

행을 묶으면 `ENDED` 는 `marker=null` 로 3건, `WATCHING` 은 `marker=1` 로 1건이다. `SHOW INDEX` 의
다섯 번째 열이 `watching_marker` 이고 `nullable=YES` 다. 종료 후 재신청까지 되므로 업무 규칙이
요구한 그대로 돈다.

**기각 사유 1 — 인용 하나가 문장과 반대되는 자리다.** 주장이 "엔티티 매핑이 아니라 손으로 쓴 스키마에만
있다"고 적었는데 세 번째 인용으로 `Dialect.generatedAs` 를 들었다. 그 메서드는 Hibernate 가 매핑에서
DDL 을 만들 때 쓰는 것이고 이 스키마는 손으로 썼으니 그 경로를 타지 않는다. 반대 조사자가
`code_mismatch` 로 신고한 그대로다.

**기각 사유 2 — 가장 중요한 전제에 좌표가 없다.** "MySQL 유일 인덱스가 NULL 을 서로 다른 값으로 본다"가
이 설계 전체를 받치는 문장인데 1차 출처 인용이 없다. 스냅샷에 MySQL 문서를 받아 두지 않은 내 쪽 문제도
있지만, 공식 문서를 사본으로 떠 올 수 있었는데 하지 않았다.

**기각 사유 3 — 좌표 넷 중 셋이 내가 쓴 요약글이다.** `latency` 가 `findings.md:34-35`, `failure` 가
`run3/verdict.md:37-41`, `complexity` 가 `findings.md:55` 다. `resource` 만 코드를 가리킨다.
claim-07 과 claim-09 에서 본 것과 같고 더 많다.

## 이 방식이 좋은 선택인가

주장과 별개로 적어 둔다. **방식 자체는 MySQL 에서 방어할 수 있고, run3 의 구현에 결함이 둘 있다.**

PostgreSQL 이라면 `CREATE UNIQUE INDEX ... WHERE status = 'WATCHING'` 으로 의도를 그대로 적는다.
MySQL 에는 그 문법이 없어서 NULL 의 성질로 흉내 내는 것이 알려진 관용구다. 그리고 제약을 DB 에 두는
이유가 있다. 잠금은 이 서비스를 거쳐 오는 호출만 막고, 다른 코드 경로나 다른 인스턴스가 직접 INSERT
하면 아무것도 못 한다. run2 가 잠금만으로 테스트를 통과했지만 두 번째 경로에 대한 방어가 없고, 같은
테스트가 그 차이를 구별하지 못한다.

결함은 둘이다.

**상태 문자열이 두 곳에 있다.** Java 의 `AlertSubscription.WATCHING` 과 SQL 의
`CASE WHEN status = 'WATCHING'` 이다. 이름을 바꾸면 제약이 조용히 아무것도 막지 않게 되고, 오류도
없고 테스트도 통과한다. 보호가 사라진 것을 알 방법이 없다.

**코드에서 안 보인다.** `AlertSubscription` 엔티티에 `watching_marker` 가 없고, `ddl-auto=validate` 가
매핑 안 된 컬럼을 문제 삼지 않아 통과한다. 스키마와 엔티티가 조용히 갈라져 있다.

고치는 방향은 생성 컬럼을 엔티티에 읽기 전용으로 매핑하고 상태 문자열을 한 곳에서만 정하는 것이다.
방식을 버릴 이유는 아니다.

**DB 를 갈아타면 뒤집힐 수 있다.** MySQL 과 PostgreSQL 은 유일 인덱스에서 NULL 을 서로 다른 값으로
보지만, SQL Server 는 NULL 을 같은 값으로 보아 NULL 행을 하나만 허용하는 것으로 기억한다. 이번에
확인하지 않았으므로 옮길 때 확인이 필요하다. 표준 SQL 이 정하지 않은 자리라 **DB 가 바뀌면 조용히
뒤집히는 전제** 위에 서 있다.

**우회라서 나쁜 것이 아니라, 우회가 기대는 전제를 코드가 드러내지 않아서 나쁘다.** 전제 셋이 전부
숨어 있다. 상태 문자열, 매핑 안 된 컬럼, DB 의 NULL 취급이다. claim-10 이 한 일은 그 셋 중 둘을 문장으로
끌어낸 것이고, 가장 중요한 세 번째에 좌표를 못 달았다.

## 기각이 하나도 없으면

그 기록은 "AI가 맞다고 한다"를 한 겹 포장한 것이다. 게이트가 환각 인용을 전부 잡고 나면 남는 위험은 진짜 인용으로 원문보다 넓은 주장을 뒷받침하는 것이고, 그것은 사람이 원문을 열어야 보인다.

