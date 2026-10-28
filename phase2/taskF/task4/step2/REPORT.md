# 소스 근거 리포트

대상 코드가 기대고 있는 전제를 주장으로 뽑고, 그 주장의 근거를 라이브러리 소스와 공식 문서에서
찾아온 기록이다. 주장마다 **원문을 그대로 떠낸 자리**가 붙어 있어 소스를 따로 열지 않아도 된다.

상태 **READY**. 이 상태는 주장이 참이라는 뜻이 **아니다.** 사람이 원문과 대조해
판정할 자료가 갖춰졌다는 뜻이고, 참·거짓은 사람이 채택·기각으로 정한다.

읽는 순서는 셋이다.

1. 아래 목록에서 무엇에 대한 주장들인지 훑는다.
2. 주장마다 `의심할 자리` 한 줄을 읽는다. 거기에 판정의 재료가 있다.
3. 걸리는 주장만 그 아래 `근거`의 원문을 본다.

축 칸과 채점 근거 전체는 리포트에 싣지 않는다. 하한 판정에 이미 쓰였고 사람이 다시 읽을 것이
아니다. 필요하면 `eval.json` 에 있다. 다만 `grounding` 의 근거는 주장 문장의 어느 성분이
인용으로 덮이지 않는지를 적으므로 주장마다 따로 싣는다.

## 1. 주장 목록

**[claim-01](#claim-01)** · 사실 · 제한 조항 4건

REQUIRES_NEW는 진행 중인 트랜잭션을 멈춰 두고 새 트랜잭션을 연다

**[claim-02](#claim-02)** · 사실 · 제한 조항 4건

NOT_SUPPORTED로 읽힌 호출은 실제 트랜잭션 없이 실행된다

**[claim-03](#claim-03)** · 사실 · 제한 조항 4건

propagation을 비우면 REQUIRED가 되고, 클래스 선언을 대체한다

**[claim-04](#claim-04)** · 사실 · 제한 조항 4건

MySQL 방언은 쓰기 잠금 때 SQL 끝에 붙일 for update 조각을 만든다

**[claim-06](#claim-06)** · 사실 · 제한 조항 3건

DB가 키를 만드는 매핑에서는 persist 직후에 그 키를 읽을 수 있다

**[claim-08](#claim-08)** · 선택 · 제한 조항 3건

낙관 경로는 @Version 대신 네이티브 조건부 UPDATE로 충돌을 가른다

**[claim-10](#claim-10)** · 선택 · 제한 조항 2건

run3 스키마는 생성 컬럼을 유일 제약 끝에 넣어 부분 유일 제약을 만든다

**[claim-05](#claim-05)** · 선택 · 제한 조항 3건

두 구현은 신청 행이 아니라 공통 사용자 행에 쓰기 잠금을 건다

**[claim-07](#claim-07)** · 선택 · 제한 조항 3건

run3는 저장소의 save 안에서 flush를 직접 부른다

**[claim-09](#claim-09)** · 선택 · 제한 조항 3건

두 재시도 구현은 시도 상한을 코드에 박힌 상수로 고정했다

`사실` 은 원문이 그렇다고 말하는 것이고, `선택` 은 대상 코드가 그 방식을 택했다는 것이다.
둘은 묻는 것이 다르다. 사실은 원문이 정말 그렇게 말하는지, 선택은 무엇을 치르고 얻었는지다.

`제한 조항` 은 그 주장을 깎으려고 반대 조사자가 따로 가져온 인용의 수다. 숫자가 큰 주장부터 읽는다.

## 2. 주장별

---

### claim-01

**REQUIRES_NEW는 진행 중인 트랜잭션을 멈춰 두고 새 트랜잭션을 연다**

사실 주장 · 축 칸 G3 D4 · 제한 조항 4건

무엇이 걸리는가 — 바깥 트랜잭션이 멈추지 않으면, 새 트랜잭션만 따로 커밋된다고 보고 짠 호출 쪽 가정이 깨진다.

**이 주장이 걸리는 코드 자리**

- `OptimisticSubscriptionAttempt.java:15` — @Transactional(propagation = Propagation.REQUIRES_NEW)
- `OptimisticSubscriptionAttempt.java:15` — @Transactional(propagation = Propagation.REQUIRES_NEW)
- `OptimisticSubscriptionAttempt.java:15` — @Transactional(propagation = Propagation.REQUIRES_NEW)
- `AlertRegistrar.java:19` — @Transactional(propagation = Propagation.REQUIRES_NEW)
- `AlertRegistrar.java:19` — @Transactional(propagation = Propagation.REQUIRES_NEW)
- `AlertRegistrar.java:19` — @Transactional(propagation = Propagation.REQUIRES_NEW)

**주장 그대로**

> 프록시를 거쳐 전파 속성이 읽힌 공개 메서드 호출에서 `Propagation.REQUIRES_NEW`는, 기존 트랜잭션이 있으면 그것을 보류한 뒤, 없으면 그대로, 새 트랜잭션을 시작한다. 보류는 트랜잭션 관리자가 보류를 구현한 경우에만 성립한다.

**의심할 자리**

문장에 넣은 '없으면 그대로'는 같은 메서드의 다른 갈래(REQUIRED·REQUIRES_NEW·NESTED를 한 조건으로 묶어 startTransaction을 부르는 자리)에서 정해지는데, 인용 상한 때문에 그 블록을 인용으로 들지 않았다. '보류는 트랜잭션 관리자가 보류를 구현한 경우에만'은 `Propagation.java`의 REQUIRES_NEW javadoc NOTE('Actual transaction suspension will not work out-of-the-box on all transaction managers')가 정하는데 그 인용도 상한 밖으로 밀렸다. 또 비공개 메서드 필터가 기본으로 켜져 있다는 사실은 `AnnotationTransactionAttributeSource`의 `publicMethodsOnly = true` 선언이 정하고 이 인용 묶음에 없다.

**채점자가 독립으로 읽은 것**

인용 3이 `SuspendedResourcesHolder suspendedResources = suspend(transaction);` 다음 `return startTransaction(definition, transaction, false, debugEnabled, suspendedResources);`까지 보여 '기존 트랜잭션이 있으면 보류한 뒤 새로 시작'을 함의하고, 인용 1의 `allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())`가 '공개 메서드 호출에서'를 받친다. 그러나 문장의 '없으면 그대로'를 정하는 블록(REQUIRED·REQUIRES_NEW·NESTED를 한 조건으로 묶어 startTransaction을 부르는 자리)과 '보류는 트랜잭션 관리자가 보류를 구현한 경우에만'을 정하는 `Propagation` javadoc NOTE가 인용 묶음에 없어, 문장 성분 둘이 인용으로 함의되지 않는다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

전파 속성 분기는 인터셉터가 그 메서드에 대해 null 아닌 TransactionAttribute를 돌려준 호출에서만 `getTransaction`에 닿는다. 그 속성을 계산하는 자리는 비공개 메서드를 설정에 따라 통째로 걸러 null을 돌려준다. 걸러지지 않은 호출에서 REQUIRES_NEW 갈래는 suspend(transaction)으로 기존 트랜잭션을 보류한 뒤 startTransaction을 부른다. 원문은 여기까지 말하고, 커밋 시점이나 연결을 몇 개 쓰는지는 이 자리에서 정하지 않는다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

spring-framework-6.2.19/org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java:166-169

```
    164 | 	@Nullable
    165 | 	protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
>   166 | 		// Don't allow non-public methods, as configured.
>   167 | 		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
>   168 | 			return null;
>   169 | 		}
    170 | 		// Skip setBeanFactory method on BeanFactoryAware.
    171 | 		if (method.getDeclaringClass() == BeanFactoryAware.class) {
```

spring-framework-6.2.19/org/springframework/transaction/interceptor/TransactionAspectSupport.java:636-640

```
    634 | 		}
    635 | 
>   636 | 		TransactionStatus status = null;
>   637 | 		if (txAttr != null) {
>   638 | 			if (tm != null) {
>   639 | 				status = tm.getTransaction(txAttr);
>   640 | 			}
    641 | 			else {
    642 | 				if (logger.isDebugEnabled()) {
```

spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java:445-452

```
    443 | 		}
    444 | 
>   445 | 		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
>   446 | 			if (debugEnabled) {
>   447 | 				logger.debug("Suspending current transaction, creating new transaction with name [" +
>   448 | 						definition.getName() + "]");
>   449 | 			}
>   450 | 			SuspendedResourcesHolder suspendedResources = suspend(transaction);
>   451 | 			try {
>   452 | 				return startTransaction(definition, transaction, false, debugEnabled, suspendedResources);
    453 | 			}
    454 | 			catch (RuntimeException | Error beginEx) {
```

**반대 조사자가 따로 가져온 제한 조항**

- 기존 트랜잭션이 없는 경우에도 REQUIRES_NEW 는 suspend 를 거친다. '없으면 그대로 새 트랜잭션을 시작한다'는 서술이 덮지 못하는 동작이다.
  - `spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java`
- 보류 분기로 갈지는 '기존 트랜잭션이 있다'는 사실이 아니라 관리자 구현의 isExistingTransaction 판정이 정한다. 기본 구현은 false 라 하위 클래스가 구현하지 않으면 보류 분기 자체에 도달하지 않는다.
  - `spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java`
- '보류는 관리자가 보류를 구현한 경우에만 성립한다'를 '아니면 보류 없이 진행된다'로 읽을 수 없다. 미구현이면 호출 자체가 예외로 끝난다.
  - `spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java`
- '공개 메서드 호출에서'라는 전제는 언어 규칙이 아니라 이 속성값에 달려 있다. setPublicMethodsOnly(false) 로 끄면 비공개 메서드도 전파 속성이 읽힌다.
  - `spring-framework-6.2.19/org/springframework/transaction/annotation/AnnotationTransactionAttributeSource.java`

**반대 조사자의 독립 판독**

getTransaction 은 먼저 isExistingTransaction(transaction) 으로 분기하고, 그 기본 구현은 false 를 돌려준다. 기존 트랜잭션이 있다고 판정될 때만 handleExistingTransaction 의 REQUIRES_NEW 분기(suspend 후 startTransaction)가 실행된다. 없다고 판정되면 REQUIRES_NEW 는 REQUIRED·NESTED 와 같은 분기로 떨어지는데, 거기서도 suspend(null) 이 한 번 호출되어 활성 동기화가 있으면 그것을 보류한 뒤 startTransaction 을 부르고, begin 이 실패하면 resume(null, ...) 로 되돌린다. 즉 '없으면 그대로'가 아니라 '보류할 트랜잭션이 없고 동기화만 보류한다'가 원문이다. 보류 미구현 관리자에서는 보류가 조용히 생략되지 않고 AbstractPlatformTransactionManager.doSuspend 가 TransactionSuspensionNotSupportedException 을 던진다. 공개 메서드 제한도 기본 동작이 아니다. AbstractFallbackTransactionAttributeSource.allowPublicMethodsOnly() 의 기본 반환은 false 이고, AnnotationTransactionAttributeSource 가 publicMethodsOnly 를 true 로 들고 있어 그 조합에서만 비공개 메서드가 걸러진다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** site-08·09·10 의 OptimisticSubscriptionAttempt 는 `long subscribe(SubscribeCommand command)` 로 선언된 패키지 전용 메서드다. 기본 구성인 AnnotationTransactionAttributeSource(publicMethodsOnly = true) 에서는 computeTransactionAttribute 의 `if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) { return null; }` 에 걸려 전파 속성이 읽히지 않는다. 주장이 '공개 메서드 호출에서'로 스스로를 한정했으므로 주장 자체는 깨지지 않지만, 그 전제가 이 지점의 코드에서는 성립하지 않는다. site-12·13·14 의 AlertRegistrar.register 는 public 이라 전제가 성립한다.

**주장을 이렇게 고치자는 발의**

> 프록시를 거쳐 전파 속성이 읽힌 호출에서 `Propagation.REQUIRES_NEW`는, 트랜잭션 관리자가 기존 트랜잭션이 있다고 판정하면 그것을 보류한 뒤, 없다고 판정하면 활성 동기화만 보류한 뒤, 새 트랜잭션을 시작한다. 보류를 구현하지 않은 관리자에서는 보류가 생략되는 것이 아니라 `TransactionSuspensionNotSupportedException`이 난다.

근거 — `SuspendedResourcesHolder suspendedResources = suspend(null);`(없을 때도 보류 호출이 있음), `protected boolean isExistingTransaction(Object transaction)`(판정이 관리자 구현에 달림), `throw new TransactionSuspensionNotSupportedException(`(미구현 시 예외) 세 인용.

---

### claim-02

**NOT_SUPPORTED로 읽힌 호출은 실제 트랜잭션 없이 실행된다**

사실 주장 · 축 칸 G3 D4 · 제한 조항 4건

무엇이 걸리는가 — 이 호출이 트랜잭션 없이 돌지 않는다면, 바깥 트랜잭션을 피하려고 이 설정을 둔 의도가 어긋난다.

**이 주장이 걸리는 코드 자리**

- `AlertService.java:23` — @Transactional(propagation = Propagation.NOT_SUPPORTED)
- `AlertService.java:23` — @Transactional(propagation = Propagation.NOT_SUPPORTED)
- `AlertService.java:23` — @Transactional(propagation = Propagation.NOT_SUPPORTED)

**주장 그대로**

> `Propagation.NOT_SUPPORTED`로 읽힌 호출은 실제 트랜잭션 없이 실행된다. 기존 트랜잭션이 있으면 보류하고 없으면 보류할 것이 없으며, 기본 설정(`SYNCHRONIZATION_ALWAYS`)에서는 실제 트랜잭션이 없어도 동기화 범위는 남는다. 보류는 트랜잭션 관리자가 보류를 구현한 경우에만 성립한다.

**의심할 자리**

문장의 '동기화 범위는 남는다'는 인용 2의 newSynchronization 계산 한 줄과 `SYNCHRONIZATION_ALWAYS` 상수 이름까지로만 받친다. 그 상수가 기본값이라는 사실(`transactionSynchronization = SYNCHRONIZATION_ALWAYS`)과 그 javadoc이 NOT_SUPPORTED를 명시적으로 대상에 넣는다는 사실은 같은 파일 안이지만 인용 상한 때문에 들지 않았다. 또 run3의 `AlertService.subscribe`가 보류 갈래를 타는지 바깥 트랜잭션이 없는 갈래를 타는지는 호출자에 달려 있어 코드에 적혀 있지 않다.

**채점자가 독립으로 읽은 것**

인용 2가 `prepareTransactionStatus(definition, null, false, newSynchronization, ...)`로 트랜잭션 자리에 null을 넣고, 인용 3의 'Execute non-transactionally, suspend the current transaction if one exists'와 'Actual transaction suspension will not work out-of-the-box on all transaction managers'가 '트랜잭션 없이 실행'과 '보류는 관리자가 구현한 경우에만'을 그대로 받친다. 다만 문장이 `SYNCHRONIZATION_ALWAYS`를 '기본 설정'이라고 단정하는데, 그 기본값을 정하는 `private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;` 선언이 인용에 없어 그 단정만 인용 밖이다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

NOT_SUPPORTED 갈래는 기존 트랜잭션을 suspend로 보류하고 transaction 인자 자리에 null을 넣은 TransactionStatus를 만들며, 그 자리에서 newSynchronization을 `SYNCHRONIZATION_ALWAYS` 설정과 비교해 계산한다. 애너테이션 쪽 원문은 이 전파를 '트랜잭션 없이 실행하고 있으면 보류한다'로 적고, 보류가 모든 트랜잭션 관리자에서 기본으로 동작하지 않는다는 것을 직접 적는다. 그리고 이 분기 자체가 인터셉터가 속성을 읽어 `getTransaction`을 부른 호출에서만 돈다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

spring-framework-6.2.19/org/springframework/transaction/interceptor/TransactionAspectSupport.java:636-640

```
    634 | 		}
    635 | 
>   636 | 		TransactionStatus status = null;
>   637 | 		if (txAttr != null) {
>   638 | 			if (tm != null) {
>   639 | 				status = tm.getTransaction(txAttr);
>   640 | 			}
    641 | 			else {
    642 | 				if (logger.isDebugEnabled()) {
```

spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java:435-442

```
    433 | 		}
    434 | 
>   435 | 		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
>   436 | 			if (debugEnabled) {
>   437 | 				logger.debug("Suspending current transaction");
>   438 | 			}
>   439 | 			Object suspendedResources = suspend(transaction);
>   440 | 			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
>   441 | 			return prepareTransactionStatus(
>   442 | 					definition, null, false, newSynchronization, debugEnabled, suspendedResources);
    443 | 		}
    444 | 
```

spring-framework-6.2.19/org/springframework/transaction/annotation/Propagation.java:71-75

```
     69 | 
     70 | 	/**
>    71 | 	 * Execute non-transactionally, suspend the current transaction if one exists.
>    72 | 	 * Analogous to EJB transaction attribute of the same name.
>    73 | 	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
>    74 | 	 * on all transaction managers. This in particular applies to
>    75 | 	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
     76 | 	 * which requires the {@code jakarta.transaction.TransactionManager} to be
     77 | 	 * made available to it (which is server-specific in standard Jakarta EE).
```

**반대 조사자가 따로 가져온 제한 조항**

- 기존 트랜잭션이 없으면 '보류할 것이 없다'가 아니라 보류 분기에 들어가지도 않고, 상태가 newTransaction=true 로 만들어진다. Gen 이 인용한 분기와 다른 코드 경로다.
  - `spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java`
- SYNCHRONIZATION_ALWAYS 라도 이미 동기화가 활성이면 새 동기화를 열지 않는다. '기본 설정에서는 동기화 범위가 남는다'는 이 조건이 붙어야 참이다.
  - `spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java`
- 동기화 범위가 열리는 것과 '트랜잭션이 활성'인 것은 분리된다. 이 자리에서 활성 플래그는 false 로 설정되므로 동기화 콜백이 보는 상태가 다르다.
  - `spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java`
- '보류는 관리자가 보류를 구현한 경우에만 성립한다'가 '아니면 트랜잭션 없이 그냥 실행된다'를 뜻하지 않는다. 기존 트랜잭션이 있는데 보류가 없으면 호출이 예외로 끝난다.
  - `spring-framework-6.2.19/org/springframework/transaction/support/AbstractPlatformTransactionManager.java`

**반대 조사자의 독립 판독**

NOT_SUPPORTED 의 보류 분기는 handleExistingTransaction 안에만 있다. 기존 트랜잭션이 없다고 판정되면 getTransaction 은 MANDATORY 도 REQUIRED/REQUIRES_NEW/NESTED 도 아닌 마지막 else 로 떨어지고, 거기서는 suspend 가 아예 호출되지 않으며 `prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null)` 로 newTransaction=true 인 상태가 만들어진다. 동기화도 무조건 남는 것이 아니다. newTransactionStatus 가 `newSynchronization && !isSynchronizationActive()` 로 한 번 더 거르고, prepareSynchronization 은 그때만 initSynchronization 을 부르면서 setActualTransactionActive 에는 status.hasTransaction()(여기서는 false)을 넣는다. 즉 동기화 범위는 열리되 '실제 트랜잭션 활성' 플래그는 꺼진다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** site-20·21·22 에서 AlertService.subscribe 를 부르는 더 바깥의 트랜잭션 경계는 대상 코드 안에 없다. 주장의 앞쪽 절('기존 트랜잭션이 있으면 보류하고')은 이 구현이 의존하는 전제가 아니며, 실제로 의존하는 것은 뒤쪽 절('없으면 실제 트랜잭션 없이 실행된다')뿐이다.

**주장을 이렇게 고치자는 발의**

> `Propagation.NOT_SUPPORTED`로 읽힌 호출은 실제 트랜잭션 없이 실행된다. 기존 트랜잭션이 있다고 판정되면 그것을 보류하고, 없다고 판정되면 보류 분기에 들어가지 않은 채 실제 트랜잭션 없는 상태가 만들어진다. 기본 설정(`SYNCHRONIZATION_ALWAYS`)에서도 동기화 범위는 이미 활성인 동기화가 없을 때에만 새로 열리며, 그때 '실제 트랜잭션 활성' 플래그는 꺼진 채로 설정된다.

근거 — `return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);`(없을 때의 별도 분기), `boolean actualNewSynchronization = newSynchronization &&`(동기화가 무조건 열리지 않음), `TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());`(활성 플래그는 false) 세 인용.

---

### claim-03

**propagation을 비우면 REQUIRED가 되고, 클래스 선언을 대체한다**

사실 주장 · 축 칸 G3 D4 · 제한 조항 4건

무엇이 걸리는가 — 메서드 선언이 클래스 선언을 물려받는다고 보면, 클래스에 건 설정이 그 메서드에도 걸린다고 잘못 읽는다.

**이 주장이 걸리는 코드 자리**

- `AlertService.java:14` — @Transactional
- `PessimisticAlertService.java:14` — @Transactional

**주장 그대로**

> propagation을 적지 않은 메서드 수준 `@Transactional`의 전파 속성은 REQUIRED이고, 이 값은 클래스 수준 선언을 물려받는 대신 그것을 대체한다.

**의심할 자리**

원문은 선언된 기본값과 조회 순서까지만 정한다. 그 기본값이 실제 호출에 적용되려면 프록시를 거쳐야 하고, 같은 파일의 바로 앞 블록이 설정에 따라 비공개 메서드를 걸러 null을 돌려주므로 비공개 메서드에서는 전파 속성이 REQUIRED가 되는 것이 아니라 아예 비트랜잭션이 된다. 그 블록은 claim-01의 인용 묶음에 들어 있다. 선언 기본값이 트랜잭션 관리자가 보는 `getPropagationBehavior()`로 옮겨지는 자리(`SpringTransactionAnnotationParser.parseTransactionAnnotation`)도 인용 상한 밖이다.

**채점자가 독립으로 읽은 것**

`Propagation propagation() default Propagation.REQUIRED;`와 `REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),`가 기본값과 그 대응을 함의하고, 인용 3의 '// First try is the method in the target class.' 뒤 `if (txAttr != null) { return txAttr; }`가 메서드 수준이 클래스 수준을 보지 않고 돌아간다는 '대체한다'를 함의한다. 다만 같은 파일 바로 앞의 비공개 메서드 가드 때문에 '메서드 수준 @Transactional의 전파 속성은 REQUIRED'가 비공개 메서드에서는 성립하지 않는데, 그 조건이 주장 문장에 들어 있지 않다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

애너테이션 선언이 propagation 요소의 기본값을 Propagation.REQUIRED로 고정하고, 그 열거 상수가 TransactionDefinition.PROPAGATION_REQUIRED 값을 들고 있다. 속성을 계산하는 자리는 메서드에서 찾은 속성이 있으면 그대로 돌려주고 클래스 수준은 그때 보지 않는다. 원문은 값의 대응과 조회 순서까지 말한다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

spring-framework-6.2.19/org/springframework/transaction/annotation/Transactional.java:175-175

```
    173 | 	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
    174 | 	 */
>   175 | 	Propagation propagation() default Propagation.REQUIRED;
    176 | 
    177 | 	/**
```

spring-framework-6.2.19/org/springframework/transaction/annotation/Propagation.java:37-37

```
     35 | 	 * <p>This is the default setting of a transaction annotation.
     36 | 	 */
>    37 | 	REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),
     38 | 
     39 | 	/**
```

spring-framework-6.2.19/org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java:179-186

```
    177 | 		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
    178 | 
>   179 | 		// First try is the method in the target class.
>   180 | 		TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
>   181 | 		if (txAttr != null) {
>   182 | 			return txAttr;
>   183 | 		}
>   184 | 
>   185 | 		// Second try is the transaction attribute on the target class.
>   186 | 		txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
    187 | 		if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
    188 | 			return txAttr;
```

**반대 조사자가 따로 가져온 제한 조항**

- '대체한다'의 정확한 성격을 한정한다. 클래스 수준 선언은 병합 대상이 아니라 기본값이고, 조상 클래스로는 내려오지 않아 상속 메서드는 하위 클래스에서 다시 선언해야 한다.
  - `spring-framework-6.2.19/org/springframework/transaction/annotation/Transactional.java`
- 조회가 '메서드 → 클래스' 두 단계라는 서술을 깎는다. 인터페이스 선언 메서드처럼 specificMethod 가 달라지면 원래 메서드와 원래 선언 클래스까지 두 단계가 더 있다.
  - `spring-framework-6.2.19/org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java`
- 클래스 수준 선언이 채택되는 데에는 조건이 하나 더 있다. 메서드가 사용자 수준 메서드가 아니면 클래스 선언을 찾아도 쓰지 않는다.
  - `spring-framework-6.2.19/org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java`
- 가시성 게이트가 네 단계 전부보다 앞에 있다. 비공개 메서드면 메서드 선언도 클래스 선언도 읽히지 않아 'REQUIRED 가 된다'가 아니라 '트랜잭션이 아니다'가 된다.
  - `spring-framework-6.2.19/org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.java`

**반대 조사자의 독립 판독**

`Transactional.propagation()` 의 기본값이 `Propagation.REQUIRED` 이고 `REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED)` 인 것은 원문 그대로다. 다만 메서드가 클래스를 '대체'하는 구조는 2단계가 아니라 4단계다. computeTransactionAttribute 는 비공개 메서드를 먼저 걸러낸 뒤, AopUtils.getMostSpecificMethod 로 구한 대상 클래스 메서드 → 그 메서드의 선언 클래스 → (specificMethod 가 원래 메서드와 다를 때) 원래 메서드 → 원래 메서드의 선언 클래스 순으로 내려가고, 클래스 쪽 두 단계는 `ClassUtils.isUserLevelMethod(method)` 가 참일 때만 채택된다. Transactional 의 javadoc 도 클래스 수준 선언을 '모든 메서드에 적용되는 기본값'으로 규정하고, 조상 클래스로는 상속되지 않는다고 적는다. 속성 해석은 AnnotatedElementUtils.findMergedAnnotationAttributes 를 쓰므로 메타 애너테이션으로 합성된 선언도 같은 자리에서 잡힌다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** site-05(alert-kata AlertService)와 site-11(PessimisticAlertService) 모두 클래스 수준 `@Transactional` 선언이 없다. 주장의 뒷절('클래스 수준 선언을 물려받는 대신 그것을 대체한다')은 두 지점 어느 쪽도 의존하지 않는 전제이고, 두 지점이 실제로 기대는 것은 앞절(생략 시 REQUIRED)뿐이다.

---

### claim-04

**MySQL 방언은 쓰기 잠금 때 SQL 끝에 붙일 for update 조각을 만든다**

사실 주장 · 축 칸 G3 D4 · 제한 조항 4건

무엇이 걸리는가 — 정해지는 것은 조각을 만드는 데까지라서, 그 for update가 어느 SQL에 실려 나가는지는 다른 자리를 봐야 한다.

**이 주장이 걸리는 코드 자리**

- `AlertRepository.java:34` — entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);
- `AlertRepository.java:21` — KataUser subscriber = entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);

**주장 그대로**

> MySQL 방언은 `PESSIMISTIC_WRITE` 잠금에 대해, 대기 설정이 무한 대기이고 별칭을 지정하지 않은 경우 SQL 끝에 붙일 조각으로 " for update"를 만든다. 이 자리가 정하는 것은 조각을 만드는 데까지이고, 그 조각이 어느 SQL에 실려 나가는지는 정하지 않는다.

**의심할 자리**

문장에 조건으로 박은 '무한 대기'와 '별칭 없음'은 `MySQLDialect.withTimeout`(NO_WAIT·SKIP_LOCKED에 " nowait"·" skip locked"를 덧붙인다)과 `MySQLDialect.getForUpdateString(String aliases)`(별칭을 받으면 " for update of <별칭>")가 정하는데, PESSIMISTIC_WRITE에서 " for update"에 닿는 세 고리가 인용 상한을 다 써서 그 두 블록을 인용으로 들지 못했다. 조각이 실리는 SQL도 이 자리 밖이다. 잠금 갱신 경로에서는 `AbstractSelectLockingStrategy.generateLockString`이 엔티티 로드 SQL이 아니라 식별자 한 열만 뽑는 별도 SELECT를 만들고, 버전 매핑된 엔티티에는 버전 열 조건을 더 넣는다. 그래서 문장을 조각 생성까지로 좁혔다.

**채점자가 독립으로 읽은 것**

인용 1의 `case PESSIMISTIC_WRITE: { return getWriteLockString( timeout ); }`, 인용 2의 `return withTimeout( getForUpdateString(), timeout );`, 인용 3의 `return " for update";`가 고리로 이어지지만, `withTimeout`이 `LockOptions.WAIT_FOREVER`에서 조각을 그대로 돌려준다는 몸체가 인용에 없다. 그래서 문장이 조건으로 박은 '무한 대기'와 '별칭을 지정하지 않은 경우'는 적혀만 있고 인용으로 받쳐지지 않아, " for update"라는 결론이 인용만으로 닫히지 않는다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

Dialect는 잠금 모드별 for-update 조각을 고르는 자리에서 PESSIMISTIC_WRITE를 getWriteLockString(timeout)으로 보낸다. MySQLDialect는 그것을 getForUpdateString()에 타임아웃 수식을 입히는 것으로 구현하고, Dialect의 getForUpdateString()은 문자열 " for update"를 돌려준다. 원문이 정하는 것은 생성되는 조각까지이고, 그 조각이 실리는 문장이 무엇인지도 InnoDB가 그 구문으로 어떤 잠금을 어느 범위에 거는지도 말하지 않는다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java:2236-2237

```
   2234 | 				return getReadLockString( timeout );
   2235 | 			}
>  2236 | 			case PESSIMISTIC_WRITE: {
>  2237 | 				return getWriteLockString( timeout );
   2238 | 			}
   2239 | 			case UPGRADE_NOWAIT:
```

hibernate-orm-6.6.53.Final/org/hibernate/dialect/MySQLDialect.java:1423-1424

```
   1421 | 
   1422 | 	@Override
>  1423 | 	public String getWriteLockString(int timeout) {
>  1424 | 		return withTimeout( getForUpdateString(), timeout );
   1425 | 	}
   1426 | 
```

hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java:2269-2270

```
   2267 | 	 * @return The appropriate {@code FOR UPDATE} clause string.
   2268 | 	 */
>  2269 | 	public String getForUpdateString() {
>  2270 | 		return " for update";
   2271 | 	}
   2272 | 
```

**반대 조사자가 따로 가져온 제한 조항**

- '무한 대기인 경우'라는 한정의 반대쪽을 보여 준다. NO_WAIT 에서는 같은 메서드가 " for update nowait" 를 만든다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/MySQLDialect.java`
- 숫자 타임아웃을 준 경우에도 결과 조각은 " for update" 로 같아진다. 조각만 보고 대기 설정을 역추론할 수 없다는 뜻이라, '무한 대기이고'라는 조건이 조각을 유일하게 결정하지 않는다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/MySQLDialect.java`
- '별칭을 지정하지 않은 경우'의 반대쪽 오버로드가 같은 클래스에 있고, supportsAliasLocks() 에 따라 " for update of <aliases>" 로 갈린다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/MySQLDialect.java`
- 같은 LockMode 를 두고 조각 생성이 아닌 별도 잠금 전략 경로가 존재한다. '이 자리가 정하는 것은 조각을 만드는 데까지'라는 단서를 넘어, 조각이 아예 쓰이지 않는 경로가 있다는 점까지 깎는다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java`

**반대 조사자의 독립 판독**

Gen 이 인용한 `case PESSIMISTIC_WRITE: return getWriteLockString( timeout );` 는 Dialect 의 **private** 오버로드 `getForUpdateString(LockMode lockMode, int timeout)` 안에 있고, 외부에서는 `getForUpdateString(LockOptions)`(LockOptions 의 timeout 사용) 또는 `getForUpdateString(LockMode)`(WAIT_FOREVER 를 하드코딩)로만 들어온다. MySQLDialect 는 `getForUpdateString()` 무인자 오버로드를 재정의하지 않으므로 " for update" 는 Dialect 의 값이 맞다. withTimeout 은 WAIT_FOREVER 에서만 조각을 그대로 돌려주고, NO_WAIT 에서는 supportsNoWait()가 true 라 " nowait" 를, SKIP_LOCKED 에서는 " skip locked" 를 덧붙이며, 숫자 타임아웃에서는 MySQL 의 supportsWait()가 false 라 타임아웃이 조용히 버려진 채 " for update" 만 남는다. 조각을 만드는 자리와 별개로, 같은 LockMode 에 대해 Dialect.getLockingStrategy 는 PessimisticWriteSelectLockingStrategy 를 돌려준다. 즉 잠금이 기존 SQL 끝에 붙는 대신 별도 문장으로 나가는 경로가 같은 방언 안에 있다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

---

### claim-06

**DB가 키를 만드는 매핑에서는 persist 직후에 그 키를 읽을 수 있다**

사실 주장 · 축 칸 G3 D4 · 제한 조항 3건

무엇이 걸리는가 — persist 뒤에 바로 id를 쓰는 코드가 이 성질에 기대고 있어, 삽입이 지연되면 그 자리의 id가 비어 버린다.

**이 주장이 걸리는 코드 자리**

- `AlertRepository.java:57` — entityManager.persist(subscription);
- `AlertRepository.java:45` — entityManager.persist(subscription);

**주장 그대로**

> 트랜잭션이 진행 중일 때 TRANSIENT 인스턴스를 DB가 키를 만드는 매핑으로 `persist`하면 삽입이 지연되지 않고 생성된 키를 그 자리에서 읽을 수 있다.

**의심할 자리**

주장은 '삽입이 지연되지 않는다'까지만 쓸 수 있고, 'INSERT 문이 persist 호출 안에서 DB로 나간다'는 더 강한 문장은 이 인용으로 갈리지 않는다. 실행 시점을 정하는 ActionQueue는 이 payload의 출처 루트에 들어 있지 않다. generatedOnExecution이 참인지(= IDENTITY 전략인지)를 정하는 Generator 쪽과, 엔티티가 Hibernate의 `Lifecycle`을 구현해 `onSave`가 삽입을 거부하면 identity 매핑에서 돌려주는 id가 `null`이 되는 갈래(`invokeSaveLifecycle`)도 인용 상한 밖이다. 대상 엔티티들은 `Lifecycle`을 구현하지 않는다.

**채점자가 독립으로 읽은 것**

인용 1의 `case TRANSIENT: entityIsTransient( event, createCache );`와 인용 2의 `!source.isTransactionInProgress() && !requiresImmediateIdAccess && generatedOnExecution`이 '트랜잭션 진행 중이면 지연되지 않는다'를 함의한다. 뒷문장 '생성된 키를 그 자리에서 읽을 수 있다'는 인용 3이 `useIdentityColumn && insert.isEarlyInsert()`라는 다른 이름의 조건으로 갈리는데, `isEarlyInsert()`가 `delayIdentityInserts`의 반대라는 고리가 인용에 없다. `invokeSaveLifecycle` 갈래의 예외도 문장이 아니라 claim_delta에만 적혀 있다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

`persist` 이벤트는 인스턴스 상태로 먼저 갈라 DETACHED면 예외를 던지고 PERSISTENT면 이미 관리 중인 것으로 처리하며, TRANSIENT일 때만 저장 경로로 보낸다. 그 저장 경로는 트랜잭션이 진행 중이면 delayIdentityInserts를 거짓으로 만들고, 키가 실행 시점에 생성되는 엔티티에 걸린 삽입 액션이 이른 삽입이면 생성된 키를 바로 꺼내 돌려준다. 원문은 지연 여부와 키를 읽는 시점까지 말하고, 액션 큐가 그 액션을 언제 실행하는지는 이 파일들 밖에 있다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultPersistEventListener.java:87-96

```
     85 | 		final EntityEntry entityEntry = source.getPersistenceContextInternal().getEntry( entity );
     86 | 		final String entityName = entityName( event, entity, entityEntry );
>    87 | 		switch ( entityState( event, entity, entityName, entityEntry ) ) {
>    88 | 			case DETACHED:
>    89 | 				throw new PersistentObjectException( "detached entity passed to persist: "
>    90 | 						+ EventUtil.getLoggableName( event.getEntityName(), entity) );
>    91 | 			case PERSISTENT:
>    92 | 				entityIsPersistent( event, createCache );
>    93 | 				break;
>    94 | 			case TRANSIENT:
>    95 | 				entityIsTransient( event, createCache );
>    96 | 				break;
     97 | 			case DELETED:
     98 | 				entityEntry.setStatus( Status.MANAGED );
```

hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractSaveEventListener.java:132-136

```
    130 | 			persister.setIdentifier( entity, generatedId, source );
    131 | 		}
>   132 | 		final boolean delayIdentityInserts =
>   133 | 				!source.isTransactionInProgress()
>   134 | 						&& !requiresImmediateIdAccess
>   135 | 						&& generatedOnExecution;
>   136 | 		return performSave( entity, generatedId, persister, generatedOnExecution, context, source, delayIdentityInserts );
    137 | 	}
    138 | 
```

hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractSaveEventListener.java:335-338

```
    333 | 	}
    334 | 
>   335 | 	private static Object handleGeneratedId(boolean useIdentityColumn, Object id, AbstractEntityInsertAction insert) {
>   336 | 		if ( useIdentityColumn && insert.isEarlyInsert() ) {
>   337 | 			if ( insert instanceof EntityIdentityInsertAction ) {
>   338 | 				final Object generatedId = ((EntityIdentityInsertAction) insert).getGeneratedId();
    339 | 				insert.handleNaturalIdPostSaveNotifications( generatedId );
    340 | 				return generatedId;
```

**반대 조사자가 따로 가져온 제한 조항**

- 저장 라이프사이클 콜백이 거부하면 performSave 가 그 자리에서 반환하고 insert 액션 자체가 만들어지지 않는다. '트랜잭션 중 TRANSIENT 를 persist 하면'이라는 조건만으로는 삽입이 보장되지 않는다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractSaveEventListener.java`
- requiresImmediateIdAccess 를 false 로 고정하는 것은 persist 경로다. 다른 호출자가 true 로 넘기면 트랜잭션 밖에서도 지연되지 않으므로, 주장은 persist 로 한정될 때에만 '트랜잭션 진행 중'이 결정적이다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultPersistEventListener.java`
- delayIdentityInserts 는 identity 분기에만 전달된다. 'DB가 키를 만드는 매핑'이 아니면 플래그가 쓰이지 않으므로 주장의 범위는 generatedOnExecution 인 생성기로 좁다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractSaveEventListener.java`

**반대 조사자의 독립 판독**

saveWithGeneratedId 는 generatedOnExecution 인 생성기에서 id 를 null 로 두고, `delayIdentityInserts = !source.isTransactionInProgress() && !requiresImmediateIdAccess && generatedOnExecution` 를 계산해 performSave 로 넘긴다. DefaultPersistEventListener.entityIsTransient 가 requiresImmediateIdAccess 를 false 로 고정해 넘기므로, persist 경로에서는 이 플래그가 사실상 트랜잭션 진행 여부 하나로 결정된다. 생성된 키는 handleGeneratedId 가 `useIdentityColumn && insert.isEarlyInsert()` 일 때만 EntityIdentityInsertAction.getGeneratedId() 로 돌려준다. 다만 이 소스 루트가 보여 주는 것은 거기까지다. 액션을 만들어 큐에 넣는 addInsertAction 까지는 있지만, 그 액션이 그 자리에서 실제로 실행되어 INSERT 가 나가는지를 정하는 ActionQueue 와 EntityIdentityInsertAction 은 이 루트에 없다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** 주장이 결정 자리로 든 symbols 에 EntityIdentityInsertAction 이 들어 있으나, 이 소스 루트에는 org/hibernate/action 과 org/hibernate/engine/spi/ActionQueue.java 가 없다. '삽입이 지연되지 않는다'까지는 AbstractSaveEventListener 가 받치지만, 'INSERT 가 그 자리에서 나간다'를 정하는 실행 자리는 이 payload 안에서 열어 볼 수 없다.

**주장을 이렇게 고치자는 발의**

> 트랜잭션이 진행 중일 때 TRANSIENT 인스턴스를 DB가 키를 만드는 매핑으로 `persist`하면, 저장 라이프사이클 콜백이 거부하지 않는 한 삽입 액션이 지연 표시 없이 큐에 들어가고 생성된 키를 그 자리에서 읽을 수 있다.

근거 — `if ( invokeSaveLifecycle( entity, persister, source ) ) {` 인용. 이 분기가 참이면 액션이 만들어지지 않아 주장이 그대로는 성립하지 않는다.

---

### claim-08

**낙관 경로는 @Version 대신 네이티브 조건부 UPDATE로 충돌을 가른다**

선택 주장 · 축 칸 G3 D4 T5 T4 · 제한 조항 3건

무엇이 걸리는가 — 네이티브 UPDATE가 영속성 컨텍스트를 비켜 가서, 같은 엔티티에 버전 조건을 붙이는 잠금 경로가 실패할 수 있다.

**이 주장이 걸리는 코드 자리**

- `AlertRepository.java:38` — Number version = (Number) entityManager.createNativeQuery("SELECT version FROM kata_users WHERE id = ?")
- `KataUser.java:15` — @Version

**주장 그대로**

> 낙관 경로 구현은 엔티티에 `@Version` 필드를 두고도 충돌 판정을 JPA의 버전 검사가 아니라 네이티브 SQL의 조건부 UPDATE로 한다. 매핑된 `@Version`은 이 경로에 개입하지 않지만, 같은 엔티티를 잠그거나 플러시로 갱신하는 다른 경로에서는 여전히 조건으로 들어간다.

**의심할 자리**

네이티브 UPDATE는 영속성 컨텍스트를 우회하므로 같은 트랜잭션 안의 KataUser 인스턴스가 들고 있는 값이 실제와 벌어질 수 있고, 그러면 버전 조건을 붙이는 잠금 경로가 실패한다. 그 실패가 조용한 무시가 아니라 `StaleObjectStateException`이라는 것까지는 인용 3과 같은 파일에 있지만 인용 상한 밖이다. 매핑된 `@Version`이 개입하는 또 한 경로인 플러시 시점 갱신(`DefaultFlushEntityEventListener.getNextVersion`)도 인용으로 들지 못해 문장 안의 '플러시로 갱신하는 다른 경로'로만 적었다. 네이티브 SQL로 읽는 쪽(`currentUserVersion`)은 조건부 UPDATE와 같은 말을 해서 인용에서 뺐다.

**채점자가 독립으로 읽은 것**

인용 1의 `@Version private long version;`과 인용 2의 `UPDATE kata_users SET version = version + 1 WHERE id = ? AND version = ?` + `return updated == 1;`이 '매핑을 두고도 판정은 네이티브 조건부 UPDATE로 한다'를 함의하고, 인용 3이 잠금 경로의 버전 조건을 받친다. 그러나 그 조건부 UPDATE가 승패를 가르는 지점이라는 것은 이를 부르는 서비스 코드가 인용에 없어 닫히지 않고, 문장의 '플러시로 갱신하는 다른 경로'는 claim_delta가 스스로 인용 밖이라고 적는다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

KataUser에 `@Version` 필드가 있고 주석은 충돌 감지를 위한 준비라고 적는다. 그런데 승패를 실제로 가르는 메서드는 엔티티를 거치지 않고 네이티브 SQL로 version을 조건부로 올리며 갱신된 행 수가 1인지로 판정한다. 1차 출처 쪽은 같은 엔티티를 잠그는 경로에서 버전 매핑이면 잠금 SELECT에 버전 열 조건을 더한다고 말한다. 코드는 두 수단이 함께 있다는 것까지 보여 준다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

phase2/taskF/alert-kata/src/main/java/com/thinking/tennis/kata/KataUser.java:14-16

```
     12 |     private Long id;
     13 | 
>    14 |     // F-3에서 이미 존재하는 사용자 행을 대상으로 충돌을 감지할 수 있게 준비한다.
>    15 |     @Version
>    16 |     private long version;
     17 | 
     18 |     protected KataUser() {
```

phase2/taskF/alert-kata/src/main/java/com/thinking/tennis/kata/AlertRepository.java:44-54

```
     42 |     }
     43 | 
>    44 |     public boolean advanceUserVersion(long userId, long expectedVersion) {
>    45 |         int updated = entityManager.createNativeQuery("""
>    46 |                 UPDATE kata_users
>    47 |                 SET version = version + 1
>    48 |                 WHERE id = ? AND version = ?
>    49 |                 """)
>    50 |                 .setParameter(1, userId)
>    51 |                 .setParameter(2, expectedVersion)
>    52 |                 .executeUpdate();
>    53 |         return updated == 1;
>    54 |     }
     55 | 
     56 |     public AlertSubscription save(AlertSubscription subscription) {
```

hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/AbstractSelectLockingStrategy.java:64-66

```
     62 | 						.addColumn( lockable.getRootTableIdentifierColumnNames()[0] )
     63 | 						.addRestriction( lockable.getRootTableIdentifierColumnNames() );
>    64 | 		if ( lockable.isVersioned() ) {
>    65 | 			select.addRestriction( lockable.getVersionColumnName() );
>    66 | 		}
     67 | 		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
     68 | 			select.setComment( lockMode + " lock " + lockable.getEntityName() );
```

**무엇을 치르고 얻었는가**

*latency* — 대안인 비관 잠금 대비 대기는 없애지만 충돌할 때마다 트랜잭션을 다시 연다. 충돌이 매회 일어난 측정에서 요청 중앙값은 15.569ms로 비관 경로의 11.831ms보다 느렸고, 그 차이는 왕복 수가 아니라 재실행에서 왔다.

- 이 교환이 성립하는 전제: 충돌률 100%, 회차당 재시도 1회.
- 뒤집히는 값: 충돌률이 0에 가까워지면 재실행이 사라져 대기가 없는 쪽이 빨라진다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:17-19`

*resource* — 읽기만 하면 끝날 신청 요청이 사용자 행을 매 시도마다 갱신하므로 쓰기 잠금과 리두를 쓴다. 대안인 비관 잠금은 같은 행을 갱신하지 않고 잠그기만 한다.

- 이 교환이 성립하는 전제: 한 사용자당 신청 요청 빈도가 낮아 사용자 행의 쓰기 경합이 문제되지 않는다.
- 뒤집히는 값: 같은 사용자에게 요청이 몰리면 사용자 행이 쓰기 병목이 되어 낙관 경로가 스스로 충돌을 만든다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:21-23`

*failure* — 충돌이 대기가 아니라 예외로 나타난다. 조건부 UPDATE가 0행을 돌려주면 전용 예외가 올라가고 트랜잭션이 롤백된다. 측정에서 요청 오류가 0회였던 것은 재시도가 모두 예산 안에서 성공했기 때문이다.

- 이 교환이 성립하는 전제: 회차당 충돌이 1회라 재시도 예산 안에서 끝난다.
- 뒤집히는 값: 한 요청이 예산만큼 연속으로 밀리면 충돌 예외가 호출자에게 그대로 나간다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:21`

*complexity* — 엔티티 매핑과 별개로 version 읽기·증가 SQL, 전용 충돌 예외, 재시도 루프, 재시도 단위를 담을 별도 빈을 두어야 한다. 측정 기록도 같은 목록을 복잡도로 꼽는다.

- 이 교환이 성립하는 전제: 충돌률과 재시도 횟수를 관측할 수단을 함께 갖춘다.
- 뒤집히는 값: 관측 없이 운영하면 충돌률이 예상보다 높아졌을 때 비용 계산이 뒤집힌 것을 알 수 없다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:23`

**반대 조사자가 따로 가져온 제한 조항**

- '잠그는 다른 경로'가 어느 전략인지를 고정한다. 버전 열이 조건으로 들어가는 것은 select 기반 전략을 탈 때이고, 로드와 함께 잠그는 경로에서는 들어가지 않는다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java`
- '여전히 조건으로 들어간다'의 귀결을 바꾼다. 네이티브 UPDATE 가 version 을 올려 둔 뒤 같은 세션의 낡은 인스턴스를 잠그면 조건이 어긋나 잠금이 실패한다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/AbstractSelectLockingStrategy.java`
- '플러시로 갱신하는 다른 경로'를 깎는다. 이 엔티티에는 version 과 id 외에 매핑된 상태가 없고 둘 다 설정 수단이 없어, 더티 검사가 UPDATE 를 만들 자리가 이 구현 안에는 존재하지 않는다.
  - `phase2/taskF/alert-kata/src/main/java/com/thinking/tennis/kata/KataUser.java`

**반대 조사자의 독립 판독**

충돌 판정을 네이티브 조건부 UPDATE 로 하는 것은 코드 그대로다. 다만 낙관 경로에서 `@Version` 은 '개입하지 않는' 정도가 아니다. `currentUserVersion` 도 네이티브 SQL 이라 그 경로에서는 `KataUser` 가 엔티티로 로드되지 않으며, 네이티브 UPDATE 는 Hibernate 가 추적하는 버전을 올리지 않으므로 같은 영속성 컨텍스트에 `KataUser` 가 올라와 있었다면 그 인스턴스의 version 은 낡은 값으로 남는다. '다른 경로에서는 여전히 조건으로 들어간다'의 근거로 든 addRestriction 은 select 기반 잠금 전략의 조립부이고(Dialect.getLockingStrategy 가 PESSIMISTIC_WRITE 에 그 전략을 돌려준다), 그 조건이 어긋나면 StaleObjectStateException 이 난다. '플러시로 갱신하는 다른 경로'에 해당하는 코드는 이 구현들 안에 없다. `KataUser` 는 id 와 version 만 매핑하고 둘 다 변경 수단이 없다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** site-03 의 실제 줄은 `currentUserVersion` 의 네이티브 SELECT 다. 주장과 인용은 `advanceUserVersion` 의 조건부 UPDATE 만 다루고, 버전을 읽는 문장과 갱신하는 문장이 서로 다른 트랜잭션 왕복이라는 점은 다루지 않는다. 또 claim-08 이 말하는 '같은 엔티티를 잠그는 다른 경로'는 PessimisticAlertService 쪽인데, 그 경로와 낙관 경로가 같은 배포에서 동시에 돌 수 있는지는 이 코드에 나타나 있지 않다.

**주장을 이렇게 고치자는 발의**

> 낙관 경로 구현은 엔티티에 `@Version` 필드를 두고도 충돌 판정을 JPA의 버전 검사가 아니라 네이티브 SQL의 조건부 UPDATE로 한다. 매핑된 `@Version`은 이 경로에 개입하지 않고, 같은 엔티티를 별도 잠금 전략으로 잠그는 경로에서만 버전 열이 조건으로 들어가 불일치 시 잠금이 `StaleObjectStateException`으로 실패한다.

근거 — `return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );`(조건이 들어가는 경로의 한정)와 `throw new StaleObjectStateException( lockable.getEntityName(), id );`(귀결), 그리고 `private long version;`(플러시 갱신 경로가 없음) 인용.

---

### claim-10

**run3 스키마는 생성 컬럼을 유일 제약 끝에 넣어 부분 유일 제약을 만든다**

선택 주장 · 축 칸 G3 D4 T5 T4 · 제한 조항 2건

무엇이 걸리는가 — DB가 유일 인덱스에서 NULL을 서로 다르게 보지 않으면, 감시 중인 신청만 막으려던 제약이 전부를 막는다.

**이 주장이 걸리는 코드 자리**

- `schema.sql:19` — GENERATED ALWAYS AS (CASE WHEN status = 'WATCHING' THEN 1 ELSE NULL END) STORED,
- `schema.sql:22` — UNIQUE (user_id, court_id, play_date, time_slot, watching_marker),

**주장 그대로**

> run3 스키마는 상태에서 파생된 생성 컬럼을 유일 제약의 마지막 열로 끼워 넣어, 유일 인덱스가 NULL을 서로 다른 값으로 보는 DB에서 감시 중인 신청에만 걸리는 부분 유일 제약이 되게 했다. 이 제약은 엔티티 매핑이 아니라 손으로 쓴 스키마에만 있다.

**의심할 자리**

'부분 유일 제약이 된다'는 결론은 그 NULL 성질이 참일 때만 성립해서, 그 조건을 주장 문장 안에 조건절로 넣었다. 그 성질을 정하는 자리는 DB 쪽이고, 이 payload의 1차 출처 루트(JDK·Spring·Hibernate)에도 copies에도 그것을 말하는 문서가 없다. 단계 1 기록도 이 항목을 테스트가 가르지 않는 사실 주장으로 올려 두었다. 인용 3은 생성 컬럼 DDL을 낼 수단이 매핑 쪽에 있다는 것까지만 보이고, `AlertSubscription` 엔티티에 `watching_marker` 대응 매핑이 없다는 것은 코드를 읽어야 보인다.

**채점자가 독립으로 읽은 것**

인용 1에 `watching_marker TINYINT GENERATED ALWAYS AS (CASE WHEN status = 'WATCHING' THEN 1 ELSE NULL END) STORED`와 `UNIQUE (user_id, court_id, play_date, time_slot, watching_marker)`가 있어 '생성 컬럼을 유일 제약의 마지막 열로 끼워 넣었다'가 함의되고, 인용 2의 'MySQL 의 유일 인덱스는 NULL 을 서로 다른 값으로 보므로'가 문장 안 조건절과 대응한다. 그러나 뒷문장 '엔티티 매핑이 아니라 손으로 쓴 스키마에만 있다'는 부재를 주장하는데 `AlertSubscription` 매핑이 인용에 없어 함의되지 않고, 인용 3의 `generatedAs`는 DDL을 낼 수단만 보일 뿐 이 제약이 부분 유일이 되는지와 무관하다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

스키마는 status에서 파생된 저장 생성 컬럼을 만들어 유일 제약의 다섯 번째 열로 넣고, 같은 네 열에 status를 더한 별도 조회 인덱스를 따로 둔다. 주석은 이 구성이 유일 인덱스가 NULL을 서로 다른 값으로 본다는 DB 성질에 기대고 있다고 적는다. 1차 출처 쪽은 생성 컬럼이 DDL 수준의 구문이고 매핑에서 그 DDL을 낼 수단이 있다는 것까지 말한다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

phase2/taskF/task4/step1/run3/impl/schema.sql:17-22

```
     15 |     play_date DATE NOT NULL,
     16 |     time_slot VARCHAR(32) NOT NULL,
>    17 |     status VARCHAR(16) NOT NULL,
>    18 |     watching_marker TINYINT
>    19 |         GENERATED ALWAYS AS (CASE WHEN status = 'WATCHING' THEN 1 ELSE NULL END) STORED,
>    20 |     CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES kata_users(id),
>    21 |     CONSTRAINT uq_watching_subscription
>    22 |         UNIQUE (user_id, court_id, play_date, time_slot, watching_marker),
     23 |     INDEX idx_active_lookup (user_id, court_id, play_date, time_slot, status)
     24 | ) ENGINE=InnoDB;
```

phase2/taskF/task4/step1/run3/impl/schema.sql:6-10

```
      4 | ) ENGINE=InnoDB;
      5 | 
>     6 | -- 중복 판단은 서비스의 조회와 아래 유일 제약이 함께 보장한다.
>     7 | -- watching_marker 는 감시 중인 행에서만 1 이고 그 밖에는 NULL 이다.
>     8 | -- MySQL 의 유일 인덱스는 NULL 을 서로 다른 값으로 보므로
>     9 | -- 감시 중인 신청만 (사용자, 코트, 날짜, 시간대) 하나로 제한되고
>    10 | -- 종료된 신청은 같은 조건으로 몇 건이든 남을 수 있다.
     11 | CREATE TABLE alert_subscriptions (
     12 |     id BIGINT AUTO_INCREMENT PRIMARY KEY,
```

hibernate-orm-6.6.53.Final/org/hibernate/dialect/Dialect.java:5226-5228

```
   5224 | 	 * @return The {@code generated as} clause containing the given expression
   5225 | 	 */
>  5226 | 	public String generatedAs(String generatedAs) {
>  5227 | 		return " generated always as (" + generatedAs + ") stored";
>  5228 | 	}
   5229 | 
   5230 | 	/**
```

**무엇을 치르고 얻었는가**

*latency* — 대안인 애플리케이션 조회 단독 판단 대비 정상 경로에 추가 왕복이 없다. 측정에서 중복 키 위반이 0건이고 반환 ID가 조건마다 빈칸 없이 100칸이어서, 회차마다 INSERT가 한 번씩만 일어났다.

- 이 교환이 성립하는 전제: 사용자 행 잠금이 먼저 직렬화에 성공해 제약이 2차 방어로만 남는다.
- 뒤집히는 값: 직렬화가 깨져 같은 조건의 INSERT가 겹치기 시작하면 진 쪽은 거부를 받고 재시도 왕복이 더해진다.
- 좌표: `phase2/taskF/task4/step1/findings.md:34-35`

*resource* — 중복을 막는 비용이 요청마다의 추가 조회에서 인덱스 유지로 옮겨가는 것이 아니라, 인덱스 한 벌이 더해진다. 유일 인덱스의 마지막 열은 `watching_marker`이고 조회 인덱스의 마지막 열은 `status`여서 유일 제약이 조회를 받칠 수 없고, 두 인덱스를 함께 유지하는 것이 이 설계의 요구다. 생성 컬럼도 행마다 저장된다.

- 이 교환이 성립하는 전제: 신청 테이블의 쓰기 빈도가 낮아 인덱스 두 벌을 유지하는 비용이 문제되지 않는다.
- 뒤집히는 값: 쓰기가 많아지면 인덱스 두 벌의 갱신 비용이 조회 한 번을 아끼는 이득을 넘는다.
- 좌표: `phase2/taskF/task4/step1/run3/impl/schema.sql`

*failure* — 조회 경합을 빠져나간 중복이 중복 행 대신 INSERT 거부로 나타나고, 바깥 재시도 루프가 그것을 받는다. 다만 측정에서 중복 키 위반과 교착이 모두 0건이라, 같은 테스트가 이 안전망이 있는 구현과 없는 구현을 구별하지 못했다.

- 이 교환이 성립하는 전제: 잠금이 먼저 직렬화에 성공한다.
- 뒤집히는 값: 잠금이 중복을 못 막는 순간부터 실패 양상이 '중복 행이 남는다'에서 '요청이 거부된다'로 바뀐다.
- 좌표: `phase2/taskF/task4/step1/run3/verdict.md:37-41`

*complexity* — status를 바꾸는 모든 경로가 생성 컬럼의 규칙을 자동으로 통과하므로 애플리케이션이 직접 챙길 것은 줄어든다. 대신 제약이 DB의 NULL 취급 성질 위에 서 있어 그 성질을 따로 확인해야 하고, 실제로 구현자는 그것만 별도 컨테이너에서 확인했다.

- 이 교환이 성립하는 전제: 운영 DB가 테스트에 쓴 것과 같은 엔진이고 유일 인덱스의 NULL 취급이 같다.
- 뒤집히는 값: NULL을 중복으로 보는 DB로 옮기면 이 제약은 종료된 신청까지 하나로 묶어 정상 저장을 막는다.
- 좌표: `phase2/taskF/task4/step1/findings.md:55`

**반대 조사자가 따로 가져온 제한 조항**

- '감시 중인 신청에만 걸린다'를 문자열 정확 일치로 좁힌다. status 가 'WATCHING' 과 글자 하나라도 다르면 제약 밖으로 조용히 빠지고, 그 판정 기준이 스키마와 애플리케이션 두 곳에 나뉘어 있다.
  - `phase2/taskF/task4/step1/run3/impl/schema.sql`
- '이 제약은 엔티티 매핑이 아니라 손으로 쓴 스키마에만 있다'가 덮지 않는 부분이다. 신청 INSERT 는 부모 행에 대한 참조 무결성 검사를 함께 요구하므로, 같은 `kata_users` 행을 쓰기 잠금으로 쥔 다른 트랜잭션과 만나는 자리가 생긴다.
  - `phase2/taskF/task4/step1/run3/impl/schema.sql`

**반대 조사자의 독립 판독**

스키마는 인용된 그대로다. 제약의 범위를 정하는 것은 생성 컬럼 문법이 아니라 `status = 'WATCHING'` 이라는 문자열 동등 비교이고, 그 비교의 한쪽은 SQL 리터럴, 다른 쪽은 Java 상수 `AlertSubscription.WATCHING` 에 따로 적혀 있다. 핵심 전제인 'MySQL 의 유일 인덱스가 NULL 을 서로 다른 값으로 본다'를 이 payload 안에서 1차 출처로 확인할 수 없다. SOURCES_JSON 이 가리키는 copies 디렉터리는 존재하지 않고, Hibernate 소스에는 MySQL 의 유일 인덱스 NULL 취급을 규정하는 자리가 없다. 지금 그 전제를 받치는 글은 구현 자신의 주석뿐이며, 측정 기록도 그 문장을 '테스트가 가르지 않는' 목록에 올려 두었다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** 인용 세 번째인 `Dialect.generatedAs` 는 대상 코드가 의존하는 전제가 아니다. 그 메서드는 Hibernate 가 매핑으로부터 DDL 을 만들 때 쓰는 공통 Dialect 구현이고 MySQLDialect 가 재정의하지도 않는데, run3 의 생성 컬럼은 손으로 쓴 schema.sql 에 있다. 또 resource 차원이 '유일 인덱스의 마지막 열은 watching_marker 이고 조회 인덱스의 마지막 열은 status 여서 유일 제약이 조회를 받칠 수 없다'고 적지만, 두 인덱스의 선행 네 열이 같으므로 조회가 어느 인덱스를 쓰는지는 선행 열 일치로 결정된다. 마지막 열 차이만으로는 그 결론이 서지 않는다.

---

### claim-05

**두 구현은 신청 행이 아니라 공통 사용자 행에 쓰기 잠금을 건다**

선택 주장 · 축 칸 G3 D4 T5 T5 · 제한 조항 3건

무엇이 걸리는가 — 잠금이 로드가 아니라 별도 잠금문으로 가면 버전 조건이 붙어, null 검사로 실패를 다 받는다는 전제가 깨진다.

**이 주장이 걸리는 코드 자리**

- `AlertRepository.java:34` — entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);
- `AlertRepository.java:21` — KataUser subscriber = entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);

**주장 그대로**

> 비관 경로의 두 구현은 신청 행이 아니라 두 요청이 공통으로 쥐는 `kata_users` 행을 기본 키로 찾아 쓰기 잠금을 거는 방식을 택했다. 그 행은 `@Version` 매핑이므로, 잠금이 로드가 아니라 잠금 갱신 경로로 가면 버전 열도 함께 조건으로 들어간다.

**의심할 자리**

주장이 붙인 '두 요청이 공통으로 쥐는'이라는 이유는 코드에 없다. run3 파일의 주석이 '이미 커밋되어 존재하는 행을 잠그므로 갭 잠금이 생기지 않는다'고 적지만 그것은 코드가 아니라 사람이 쓴 문장이고, 단계 1 기록도 그 문장을 테스트가 가르지 않는 항목으로 올려 두었다. 또 같은 `find` 한 줄이 세션 캐시 적중 여부에 따라 로드 SQL에 잠금을 싣는 경로와 별도 잠금문을 내는 경로로 갈리는데(`DefaultLoadEventListener`의 `lockAndLoad` 분기), 그 블록은 인용 상한 밖이라 전제 쪽에만 적었다. 잠금 갱신 경로에서는 조건에 맞는 행이 없을 때 반환이 `null`이 아니라 `StaleObjectStateException`이므로 run3의 `null` 검사가 모든 실패를 받는 방어가 아니다.

**채점자가 독립으로 읽은 것**

`entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE)`가 두 인용에 모두 있어 '신청 행이 아니라 kata_users 행을 기본 키로 찾아 쓰기 잠금'은 함의된다. 그러나 '두 요청이 공통으로 쥐는'은 코드에 없는 이유 설명이고(claim_delta도 '코드에 없다'고 적는다), '그 행은 @Version 매핑이므로'의 전제인 KataUser의 `@Version` 선언은 이 인용 묶음에 없다. 인용 3은 `if ( lockable.isVersioned() ) select.addRestriction( lockable.getVersionColumnName() );`로 조건부만 보여 준다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

두 코드 모두 신청 테이블이 아니라 사용자 테이블의 행 하나를 기본 키로 찾아 PESSIMISTIC_WRITE를 건다. run3 쪽은 null이면 예외를 던지는 한 줄이 더 있다. 1차 출처 쪽은 잠금 SELECT를 조립하는 자리에서 엔티티가 버전 매핑이면 식별자 조건에 버전 열 조건을 더한다고 말한다. 코드는 선택이 그렇다는 것까지만 보여 주고, 조회 자체를 FOR UPDATE로 하는 안이나 유일 제약에만 맡기는 안과 비교하지 않는다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

phase2/taskF/alert-kata/src/main/java/com/thinking/tennis/kata/AlertRepository.java:33-35

```
     31 |     }
     32 | 
>    33 |     public void lockUserForUpdate(long userId) {
>    34 |         entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);
>    35 |     }
     36 | 
     37 |     public long currentUserVersion(long userId) {
```

phase2/taskF/task4/step1/run3/impl/AlertRepository.java:20-25

```
     18 |      * 이미 커밋되어 존재하는 행을 잠그므로 갭 잠금이 생기지 않고, 다른 사용자끼리는 서로 기다리지 않는다.
     19 |      */
>    20 |     public void lockSubscriber(long userId) {
>    21 |         KataUser subscriber = entityManager.find(KataUser.class, userId, LockModeType.PESSIMISTIC_WRITE);
>    22 |         if (subscriber == null) {
>    23 |             throw new IllegalStateException("Unknown subscriber: " + userId);
>    24 |         }
>    25 |     }
     26 | 
     27 |     public Optional<AlertSubscription> findActive(SubscribeCommand command) {
```

hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/AbstractSelectLockingStrategy.java:64-66

```
     62 | 						.addColumn( lockable.getRootTableIdentifierColumnNames()[0] )
     63 | 						.addRestriction( lockable.getRootTableIdentifierColumnNames() );
>    64 | 		if ( lockable.isVersioned() ) {
>    65 | 			select.addRestriction( lockable.getVersionColumnName() );
>    66 | 		}
     67 | 		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
     68 | 			select.setComment( lockMode + " lock " + lockable.getEntityName() );
```

**무엇을 치르고 얻었는가**

*latency* — 대안인 낙관 재시도 대비, 같은 사용자의 뒤 요청이 앞 요청의 커밋까지 기다린다. 충돌이 매회 일어나는 조건의 측정에서는 기다리는 쪽이 재실행보다 빨라 요청 중앙값 11.831ms 대 15.569ms였다.

- 이 교환이 성립하는 전제: 같은 사용자에게 겹치는 요청 두 개만 보내 충돌률이 100%이고, 잠금을 쥔 구간이 조회 한 번과 INSERT 한 번으로 짧다. 이 '조회 한 번'은 잠금 모드를 준 `find`가 로드와 함께 잠그는 경로를 탔을 때만 참이고, 세션에 이미 올라온 인스턴스를 잠그면 별도 잠금문이 한 번 더 간다.
- 뒤집히는 값: 충돌률이 떨어져 낙관 경로의 재시도가 회차당 1회 미만이 되면 순서가 뒤집힌다. 측정 조건에서는 충돌 100%에 재시도 100회였다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:16-19`

*resource* — 잠금을 쥔 구간 내내 트랜잭션에 묶인 EntityManager와 그 연결을 점유한다. 대안인 낙관 재시도는 대기 대신 트랜잭션을 한 번 더 열어 같은 연결을 두 번 쓴다.

- 이 교환이 성립하는 전제: 트랜잭션당 연결 하나를 쓰는 풀 구성이고, 동시 요청 수가 풀 크기보다 작다. 잠금 한 번이 왕복 한 번이라는 것도 로드와 함께 잠그는 경로를 전제한다.
- 뒤집히는 값: 같은 사용자에 몰리는 동시 요청이 연결 풀 크기에 닿으면 대기가 풀 고갈로 번져 다른 사용자 요청까지 막힌다.
- 좌표: `spring-framework-6.2.19/org/springframework/orm/jpa/JpaTransactionManager.java`

*failure* — 실패 양상에 잠금 대기 초과와 교착이 들어온다. 다만 이 구현은 기본 키로 찾은 단일 행만, 모든 쓰기 경로에서 같은 순서로 잠그므로 구현 안에서 잠금 순서가 엇갈릴 자리가 없다. 같은 테스트에서 SERIALIZABLE로 잠금 범위를 넓혔을 때는 100회 모두 교착이 났다.

- 이 교환이 성립하는 전제: 잠그는 행이 하나이고 그 행이 이미 커밋되어 존재한다. 그리고 잠금 조건이 기본 키 하나라는 것은 잠금 갱신 경로를 타지 않을 때만 참이다.
- 뒤집히는 값: 잠그는 행이 둘 이상이 되거나 경로마다 잠금 순서가 달라지면 교착이 생긴다.
- 좌표: `phase2/taskF/alert-kata/evidence/uncoordinated-isolation-results.md:19-21`

*complexity* — 재시도 코드와 충돌 예외가 필요 없어 애플리케이션 쪽 코드는 단순하다. 대신 잠금 대상이 기능의 자연스러운 대상(신청)이 아니라 사용자 행이므로, 새 쓰기 경로를 더하는 사람이 같은 행을 조회보다 앞에서 잠가야 한다는 규칙을 알아야 한다.

- 이 교환이 성립하는 전제: 중복이 같은 사용자의 요청끼리만 생긴다.
- 뒤집히는 값: 서로 다른 사용자가 같은 신청 행을 만들 수 있는 규칙이 생기면 사용자 행 잠금은 직렬화 대상 자체가 어긋난다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:21`

**반대 조사자가 따로 가져온 제한 조항**

- '버전 열도 함께 조건으로 들어간다'의 귀결을 바꾼다. 버전이 어긋나면 잠금이 걸리지 않고 예외로 끝나므로, 직렬화 수단으로서의 성질 자체가 달라진다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/AbstractSelectLockingStrategy.java`
- 이 경로는 타임아웃을 무한 대기로 고정한다. 호출 쪽에서 대기 한도를 주었다고 가정할 수 없다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/persister/entity/AbstractEntityPersister.java`
- update 기반 전략은 버전 매핑을 전제로만 성립한다. 주장이 쓴 '잠금 갱신 경로'를 이 클래스로 읽으면 MySQL 기본 방언에서는 도달하지 않는 경로를 가리키게 된다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/dialect/lock/PessimisticWriteUpdateLockingStrategy.java`

**반대 조사자의 독립 판독**

두 구현 모두 기본 키로 `KataUser` 를 찾아 PESSIMISTIC_WRITE 를 거는 것은 코드 그대로다. 버전 열에 관한 서술은 가리키는 자리가 다르다. Gen 이 인용한 `select.addRestriction( lockable.getVersionColumnName() )` 는 AbstractSelectLockingStrategy.generateLockString 안, 즉 **select 기반** 잠금 전략의 SQL 조립부다. Dialect.getLockingStrategy 는 PESSIMISTIC_WRITE 에 대해 PessimisticWriteSelectLockingStrategy 를 돌려주고, update 기반인 PessimisticWriteUpdateLockingStrategy 는 이 switch 가 돌려주지 않는다. 그 경로의 결과도 '조건이 하나 더 붙는다'에서 끝나지 않는다. 결과 집합이 비면 AbstractSelectLockingStrategy.lock 이 StaleObjectStateException 을 던지므로, 비관 잠금 요청이 낙관 실패로 끝난다. 또 AbstractEntityPersister.lock 의 LockMode 오버로드는 타임아웃을 LockOptions.WAIT_FOREVER 로 고정한다. 영속성 컨텍스트에 이미 올라온 인스턴스가 이 경로로 가는지를 결정하는 CacheEntityLoaderHelper/LoaderHelper 는 이 소스 루트에 없다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** 두 가지. (1) failure 차원이 근거로 든 uncoordinated-isolation-results.md:19-21 은 `-Pkata-red` 로 돌린 `NEW_UNCOORDINATED` 기준선 측정이다. 잠금을 건 비관 구현에 SERIALIZABLE 을 적용한 결과가 아니라 잠금이 없는 구현의 결과이므로, '같은 테스트에서 SERIALIZABLE로 잠금 범위를 넓혔을 때'라는 서술이 측정 대상과 어긋난다. (2) 두 구현을 같은 선택으로 묶었지만 alert-kata 의 `lockUserForUpdate` 는 반환값을 버리고 존재 확인을 하지 않는 반면 run3 의 `lockSubscriber` 는 null 이면 IllegalStateException 을 던진다. 사용자 행이 없을 때의 동작이 서로 다르다.

**주장을 이렇게 고치자는 발의**

> 비관 경로의 두 구현은 신청 행이 아니라 두 요청이 공통으로 쥐는 `kata_users` 행을 기본 키로 찾아 쓰기 잠금을 거는 방식을 택했다. 그 행은 `@Version` 매핑이므로, 로드와 함께 잠그지 않고 별도 잠금 전략을 타면 버전 열이 조건에 들어간 `select ... for update`가 한 번 더 나가고 버전이 어긋나면 잠금이 `StaleObjectStateException`으로 실패한다.

근거 — `throw new StaleObjectStateException( lockable.getEntityName(), id );`(버전 불일치의 귀결이 조건 추가가 아니라 실패)와 `return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );`(그 경로가 update 가 아니라 select 라는 점) 두 인용.

---

### claim-07

**run3는 저장소의 save 안에서 flush를 직접 부른다**

선택 주장 · 축 칸 G3 D4 T5 T5 · 제한 조항 3건

무엇이 걸리는가 — 이 매핑에서 삽입이 이미 지연되지 않으므로, 그 flush가 쓰기 시점을 당겨 준다고 본 설명은 들어맞지 않는다.

**이 주장이 걸리는 코드 자리**

- `AlertRepository.java:47` — entityManager.flush();

**주장 그대로**

> run3 구현은 저장소의 `save` 안에서 `flush`를 직접 불러, 영속성 컨텍스트의 더티 검사와 액션 큐 실행을 트랜잭션 경계가 아니라 그 호출 자리에서 수행하게 했다. 이 매핑에서는 삽입이 `persist` 시점에 이미 지연되지 않으므로 그 `flush`가 당기는 쓰기는 없다.

**의심할 자리**

주석은 'persist 시점에 INSERT가 나간다'고 적는데, 그 말이 맞다면 뒤이은 flush는 INSERT를 앞당기지 않는다. 그래서 코드가 보여 주는 선택은 '명시적 flush를 둔다'이고, 전에 쓴 '쓰기 시점을 당긴다'는 문장은 성립하지 않아 지웠다. 플러시 주기를 돌릴지 말지가 '내보낼 문장이 남았는지'가 아니라 '관리 중인 엔티티가 있는지'로 갈린다는 조항(`DefaultFlushEventListener.onFlush`)은 인용 상한 밖이라 교환 서술의 좌표로만 달았다. 같은 과제의 다른 구현이 flush 없이 persist만 부른다는 대조 인용도 상한 때문에 뺐다.

**채점자가 독립으로 읽은 것**

인용 1에 `entityManager.persist(subscription);` 다음 `entityManager.flush();`가 있어 '저장소의 save 안에서 flush를 직접 부른다'가 함의되고, 인용 2의 `flushEntities( event, persistenceContext )`·`flushCollections(...)`가 더티 검사 수행 자리를, 인용 3의 `delayIdentityInserts` 논리식이 '삽입이 이미 지연되지 않는다'를 받친다. 다만 그 논리식을 거짓으로 만드는 결정적 조건은 `!source.isTransactionInProgress()`인데, '호출이 트랜잭션 안'이라는 조건이 주장 문장에는 없고 교환 항목의 premise에만 적혀 있다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

run3 저장소는 persist 바로 뒤에 flush를 부르고 주석으로 그 의도를 적는다. 1차 출처 쪽에서 flush가 하는 일은 영속성 컨텍스트를 플러시 상태로 두고 엔티티와 컬렉션을 돌며 더티 검사를 해 실행할 것을 모으는 것이고, 트랜잭션 안의 identity 저장은 delayIdentityInserts가 거짓이어서 삽입이 이미 지연되지 않는다. 두 사실을 합치면 이 자리의 flush가 모을 삽입이 남아 있지 않다는 것까지 원문이 말한다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

phase2/taskF/task4/step1/run3/impl/AlertRepository.java:44-49

```
     42 |     }
     43 | 
>    44 |     public AlertSubscription save(AlertSubscription subscription) {
>    45 |         entityManager.persist(subscription);
>    46 |         // IDENTITY 키라 persist 시점에 INSERT 가 나가고, 유일 제약 위반도 이 자리에서 드러난다.
>    47 |         entityManager.flush();
>    48 |         return subscription;
>    49 |     }
     50 | }
     51 | 
```

hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractFlushingEventListener.java:87-91

```
     85 | 	}
     86 | 
>    87 | 	protected void flushEverythingToExecutions(FlushEvent event, PersistenceContext persistenceContext, EventSource session) {
>    88 | 		persistenceContext.setFlushing( true );
>    89 | 		try {
>    90 | 			int entityCount = flushEntities( event, persistenceContext );
>    91 | 			int collectionCount = flushCollections( session, persistenceContext );
     92 | 
     93 | 			event.setNumberOfEntitiesProcessed( entityCount );
```

hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractSaveEventListener.java:132-136

```
    130 | 			persister.setIdentifier( entity, generatedId, source );
    131 | 		}
>   132 | 		final boolean delayIdentityInserts =
>   133 | 				!source.isTransactionInProgress()
>   134 | 						&& !requiresImmediateIdAccess
>   135 | 						&& generatedOnExecution;
>   136 | 		return performSave( entity, generatedId, persister, generatedOnExecution, context, source, delayIdentityInserts );
    137 | 	}
    138 | 
```

**무엇을 치르고 얻었는가**

*latency* — 대안인 커밋 시점 일괄 쓰기 대비 왕복 수가 같다. 트랜잭션 안의 IDENTITY 저장은 삽입이 이미 지연되지 않아 이 flush가 새로 내보낼 INSERT가 남아 있지 않기 때문이다.

- 이 교환이 성립하는 전제: 키가 실행 시점에 생성되는 매핑이고 호출이 트랜잭션 안이다.
- 뒤집히는 값: 키를 미리 생성하는 전략(시퀀스, 할당)으로 바꾸면 삽입이 flush까지 모이므로 flush가 왕복을 앞당기는 실제 효과를 갖는다.
- 좌표: `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractSaveEventListener.java`

*resource* — 왕복은 늘지 않지만 플러시 주기가 한 번 더 돈다. 주기를 돌릴지는 내보낼 문장이 남았는지가 아니라 관리 중인 엔티티가 하나라도 있는지로 갈리고, run3의 흐름은 `lockSubscriber`가 `KataUser`를 올려 두어 그 조건이 참이다. 그래서 치르는 것은 연결이나 왕복이 아니라 영속성 컨텍스트를 돌며 하는 더티 검사다.

- 이 교환이 성립하는 전제: 한 트랜잭션에서 저장이 한 건이고, flush 호출 시점에 관리 중인 엔티티가 하나 이상 올라와 있다.
- 뒤집히는 값: 한 트랜잭션에서 여러 건을 저장하는 경로가 생기면 건건이 flush가 배치 쓰기를 깨뜨려 왕복까지 늘어난다.
- 좌표: `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultFlushEventListener.java`

*failure* — 유일 제약 위반이 커밋 경계가 아니라 save 호출 자리에서 예외로 올라오고, 그 예외를 바깥의 재시도 루프가 잡는다. 다만 측정에서 중복 키 위반과 교착이 0건이라 이 경로는 한 번도 실행되지 않았다.

- 이 교환이 성립하는 전제: 사용자 행 잠금이 먼저 직렬화에 성공해 제약 위반이 발생하지 않는다.
- 뒤집히는 값: 직렬화가 깨져 위반이 실제로 나기 시작하면 예외 발생 위치의 차이가 비로소 동작 차이가 된다.
- 좌표: `phase2/taskF/task4/step1/run3/verdict.md:37-41`

*complexity* — 저장소가 영속성 컨텍스트의 쓰기 시점을 직접 다루게 되어, 읽는 쪽이 flush의 필요 여부를 따로 판단해야 한다. 그 판단의 근거인 'IDENTITY 전략은 persist 시점에 INSERT를 보낸다'는 전제는 이 테스트가 가르지 않는 항목으로 기록되어 있다.

- 이 교환이 성립하는 전제: 매핑 전략을 바꾸지 않는다.
- 뒤집히는 값: 키 생성 전략이 바뀌면 이 flush의 의미가 달라지므로 저장소 코드를 다시 읽어야 한다.
- 좌표: `phase2/taskF/task4/step1/findings.md:57`

**반대 조사자가 따로 가져온 제한 조항**

- '관리 중인 엔티티가 하나라도 있는지로 갈린다'를 깎는다. 조건은 컬렉션 엔트리 수와의 OR 이라 관리 엔티티가 0 이어도 주기가 돌 수 있다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultFlushEventListener.java`
- 두 조건이 모두 거짓이어도 큐에 액션이 남았으면 performExecutions 가 실행된다. '조건이 거짓이면 아무 일도 없다'가 아니다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/DefaultFlushEventListener.java`
- '치르는 것은 더티 검사다'를 깎는다. 플러시 한 번은 preFlush(캐스케이드 포함)를 먼저 돌고 나서 엔티티·컬렉션 플러시로 간다.
  - `hibernate-orm-6.6.53.Final/org/hibernate/event/internal/AbstractFlushingEventListener.java`

**반대 조사자의 독립 판독**

`entityManager.flush()` 가 도는지는 DefaultFlushEventListener.onFlush 가 가른다. 조건은 관리 중인 엔티티 수 하나가 아니라 `getNumberOfManagedEntities() > 0 || getCollectionEntriesSize() > 0` 의 OR 이고, 둘 다 0 이어도 `else if ( source.getActionQueue().hasAnyQueuedActions() )` 에서 큐에 남은 미로드 엔티티 삭제는 실행된다. 주기가 도는 비용도 더티 검사만이 아니다. 한 인자 오버로드 flushEverythingToExecutions(FlushEvent) 가 preFlush 를 먼저 부르고 나서 Gen 이 인용한 세 인자 오버로드로 넘어가며, 그 안에서 flushEntities 와 flushCollections 가 모두 돈다. delayIdentityInserts 인용은 '이 매핑에서 삽입이 지연되지 않는다'만 받치고, 그 결과 '이 flush 가 당길 쓰기가 없다'는 추론은 액션 큐 쪽을 읽어야 닫히는데 그 코드는 이 루트에 없다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** resource 차원의 전제가 'run3의 흐름은 `lockSubscriber`가 `KataUser`를 올려 두어 그 조건이 참이다'라고 적지만, `save` 가 호출되는 시점에는 방금 persist 한 `AlertSubscription` 자체가 영속성 컨텍스트에 올라와 있다. 게이트가 참인 이유로 lockSubscriber 가 필요하지 않으므로, 그 전제는 이 코드가 실제로 기대는 자리가 아니다. 또 `save` 는 `findActive(...).orElseGet(...)` 안에서만 불리는데 그 JPQL 조회가 이미 자동 플러시를 거치므로, 이 `flush` 가 그 트랜잭션의 첫 플러시 주기도 아니다.

---

### claim-09

**두 재시도 구현은 시도 상한을 코드에 박힌 상수로 고정했다**

선택 주장 · 축 칸 G3 D4 T5 T5 · 제한 조항 3건

무엇이 걸리는가 — 상한이 설정이 아니고 특정 충돌 예외에만 걸리므로, 그 밖의 실패는 재시도 없이 그대로 호출자에게 나간다.

**이 주장이 걸리는 코드 자리**

- `OptimisticAlertService.java:8` — private static final int MAX_ATTEMPTS = 5;
- `AlertService.java:11` — private static final int MAX_ATTEMPTS = 3;

**주장 그대로**

> 두 재시도 구현은 시도 횟수의 상한을 설정이 아니라 코드에 박힌 상수로 고정했고, 그 상한은 각 구현이 재시도 대상으로 잡는 충돌 예외에만 적용된다.

**의심할 자리**

'근거 없이 골랐다'는 평가는 코드가 아니라 구현자가 자기 평가에 적은 문장이고, 단계 1 기록이 그것을 선택 주장으로 분류해 두었다. 문장에 넣은 '잡는 충돌 예외에만 적용된다'는 인용 3이 alert-kata 쪽에서만 보이고, run3 쪽에서 같은 제한을 만드는 `catch (DataIntegrityViolationException | ConcurrencyFailureException conflict)` 블록은 인용 상한 밖이다. run3 쪽은 마지막 실패를 보관해 루프 밖에서 던지므로 예산 소진의 모양도 두 구현이 다르다.

**채점자가 독립으로 읽은 것**

`private static final int MAX_ATTEMPTS = 5;`와 `= 3;` 두 인용이 '설정이 아니라 코드에 박힌 상수'를 함의하고, 인용 3의 `catch (OptimisticConflictException conflict)`와 `if (attemptNumber == MAX_ATTEMPTS) throw conflict;`가 상한이 그 예외에만 걸린다는 것을 보인다. 다만 '두 재시도 구현'이라고 묶은 문장의 뒷절은 run3 쪽 `catch (DataIntegrityViolationException | ConcurrencyFailureException conflict)`가 인용에 없어 한 구현에서만 함의된다.

위 `의심할 자리` 는 주장을 만든 쪽이 스스로 적은 차이이고, 이 문단은 주장을 만들지 않은 채점자가 인용만 보고 읽은 것이다. 둘이 어긋나면 그 자리를 먼저 열어 본다.

**원문이 말하는 것**

두 파일 모두 상수 선언 한 줄로 시도 횟수를 정하고, 값은 5와 3으로 서로 다르다. 루프는 그 횟수를 다 쓰면 마지막 예외를 그대로 던지는데, 그 루프가 잡는 것은 전용 충돌 예외 하나다. 코드 어디에도 그 값을 고른 근거가 없다.

**근거**

`>` 가 붙은 줄이 인용한 줄이고 앞뒤 두 줄은 문맥이다. 인용이 잘려 뜻이 바뀌었는지 보인다.

phase2/taskF/alert-kata/src/main/java/com/thinking/tennis/kata/OptimisticAlertService.java:8-8

```
      6 | @Service
      7 | public class OptimisticAlertService {
>     8 |     private static final int MAX_ATTEMPTS = 5;
      9 | 
     10 |     private final OptimisticSubscriptionAttempt attempt;
```

phase2/taskF/task4/step1/run3/impl/AlertService.java:11-11

```
      9 | @Service
     10 | public class AlertService {
>    11 |     private static final int MAX_ATTEMPTS = 3;
     12 | 
     13 |     private final AlertRegistrar registrar;
```

phase2/taskF/alert-kata/src/main/java/com/thinking/tennis/kata/OptimisticAlertService.java:18-27

```
     16 | 
     17 |     public long subscribe(SubscribeCommand command) {
>    18 |         for (int attemptNumber = 1; attemptNumber <= MAX_ATTEMPTS; attemptNumber++) {
>    19 |             try {
>    20 |                 return attempt.subscribe(command);
>    21 |             } catch (OptimisticConflictException conflict) {
>    22 |                 conflictCount.incrementAndGet();
>    23 |                 if (attemptNumber == MAX_ATTEMPTS) {
>    24 |                     throw conflict;
>    25 |                 }
>    26 |             }
>    27 |         }
     28 |         throw new IllegalStateException("Unreachable retry state");
     29 |     }
```

**무엇을 치르고 얻었는가**

*latency* — 예산이 클수록 충돌이 잦을 때 한 요청이 붙잡는 시간의 상한이 커진다. 측정 조건에서는 회차당 충돌이 1회라 5든 3이든 두 번째 시도에서 끝나 상한에 닿지 않았다.

- 이 교환이 성립하는 전제: 동시 요청이 둘이라 충돌이 연쇄하지 않는다.
- 뒤집히는 값: 동시 요청이 셋 이상이면 밀린 쪽이 다시 밀리는 연쇄가 생겨 시도 수가 예산에 닿는다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:15-17`

*resource* — 실패한 시도마다 트랜잭션과 연결 점유를 한 번씩 더 쓴다. 측정에서 낙관 경로는 100회 모두 충돌해 회차마다 트랜잭션을 두 번 실행했다.

- 이 교환이 성립하는 전제: 재시도 사이에 대기를 두지 않아 실패한 시도가 즉시 다음 트랜잭션을 연다.
- 뒤집히는 값: 충돌률이 높은 상태에서 예산을 키우면 재시도가 부하를 다시 키우는 쪽으로 돌아선다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:15-17`

*failure* — 예산을 다 쓰면 실패가 요청 오류로 호출자에게 나간다. 대안인 비관 잠금에는 이 실패 양상 자체가 없다. 다만 run3 쪽에서는 재시도 경로가 한 번도 실행되지 않아 이 양상이 관찰된 적이 없다.

- 이 교환이 성립하는 전제: 예산 안에서 충돌이 해소되고, 올라오는 예외가 각 구현이 잡는 계열에 들어간다.
- 뒤집히는 값: 잡는 계열 밖의 RuntimeException이 올라오면 상한과 무관하게 시도가 1회로 끝나 그대로 호출자에게 나간다.
- 좌표: `phase2/taskF/task4/step1/run3/verdict.md:37-41`

*complexity* — 상수라서 운영 중에 바꾸려면 배포가 필요하고, 값을 고를 근거가 되는 충돌률을 따로 관측해야 한다. 한쪽 구현이 충돌 횟수를 세는 카운터를 들고 있는 이유가 그것이고, 측정 기록도 충돌률을 재서 고르라고 적는다.

- 이 교환이 성립하는 전제: 충돌률이 배포 주기보다 느리게 변한다.
- 뒤집히는 값: 부하에 따라 충돌률이 분 단위로 흔들리면 고정 상수로는 맞출 수 없어 설정이나 적응형 백오프가 필요해진다.
- 좌표: `phase2/taskF/alert-kata/evidence/lock-comparison-results.md:25`

**반대 조사자가 따로 가져온 제한 조항**

- '각 구현이 재시도 대상으로 잡는 충돌 예외'가 두 구현에서 같은 종류가 아님을 보인다. 한쪽은 자기 도메인 예외 하나, 다른 쪽은 Spring 데이터 접근 예외 계층의 두 가지다.
  - `phase2/taskF/task4/step1/run3/impl/AlertService.java`
- run3 의 재시도 예산은 비관 잠금의 대안이 아니라 비관 잠금 위에 얹힌 2차 방어다. '대안인 비관 잠금에는 이 실패 양상 자체가 없다'는 failure 차원의 서술이 run3 에는 성립하지 않는다.
  - `phase2/taskF/task4/step1/run3/impl/AlertRegistrar.java`
- 상한 소진의 종착이 구현마다 다르다. alert-kata 는 마지막 시도에서 충돌 예외를 되던지고 이 줄에 도달하지 않는 구조이며, run3 는 루프를 모두 돈 뒤 마지막 실패를 던진다.
  - `phase2/taskF/alert-kata/src/main/java/com/thinking/tennis/kata/OptimisticAlertService.java`

**반대 조사자의 독립 판독**

두 상수와 두 루프는 코드 그대로다. 상한이 적용되는 예외 계열은 구현마다 다르고, 그 차이가 상한의 뜻을 바꾼다. alert-kata 쪽은 자기 도메인 예외인 `OptimisticConflictException` 만 잡고 마지막 시도에서 그대로 되던진다. run3 쪽은 `DataIntegrityViolationException | ConcurrencyFailureException` 을 잡는데, 그 루프가 감싸는 `AlertRegistrar.register` 는 `repository.lockSubscriber(...)` 로 사용자 행에 쓰기 잠금을 먼저 거는 비관 구현이다. 즉 run3 의 예산은 낙관 충돌이 아니라 제약 위반·교착·잠금 대기 초과를 대상으로 하며, 비관 잠금의 대안이 아니라 그 위에 얹혀 있다. 이 주장을 받칠 1차 출처 쪽 조항은 Spring·Hibernate 어디에도 없다. 재시도 예산은 프레임워크가 정하는 값이 아니다.

조사자와 반대 조사자는 서로의 판독을 보지 못한 채 같은 원문을 읽었다. 위의 `원문이 말하는 것` 과 이 문단이 어긋나면 그 자리를 먼저 열어 본다.

**코드 불일치 신고** failure 차원이 근거로 든 run3/verdict.md:37-41 은 '유니크 제약과 REQUIRES_NEW 재시도가 한 번도 돌지 않았다'를 말한다. 그 기록이 받치는 것은 '예산을 다 쓰면 실패가 호출자에게 나간다'가 아니라 '이 실행에서는 재시도 진입 자체가 없었다'이다. 관찰되지 않은 양상을 관찰 기록의 좌표로 가리키고 있다.

**주장을 이렇게 고치자는 발의**

> 두 재시도 구현은 시도 횟수의 상한을 설정이 아니라 코드에 박힌 상수로 고정했고, 그 상한이 적용되는 예외 계열은 구현마다 다르다. 한쪽은 자체 낙관 충돌 예외에만, 다른 쪽은 제약 위반·교착·잠금 대기 초과에 적용되며 뒤쪽은 비관 잠금의 대안이 아니라 그 위에 얹힌 2차 방어다.

근거 — `} catch (DataIntegrityViolationException | ConcurrencyFailureException conflict) {`(잡는 계열이 다름)와 `repository.lockSubscriber(command.userId());`(run3 의 루프가 감싸는 것이 비관 구현임) 두 인용.

