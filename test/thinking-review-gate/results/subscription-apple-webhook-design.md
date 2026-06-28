# Apple App Store Server Notification v2 Webhook 설계

## 결론

Spring 백엔드에서는 Apple webhook 요청을 컨트롤러에서 바로 구독 DB에 반영하지 않고, `수신/ACK`, `JWS 검증`, `원본 이벤트 저장`, `비동기 처리`, `구독 상태 반영`, `재처리/보정`을 분리하는 구조를 추천한다.

추천안은 "검증 가능한 이벤트를 먼저 DB에 원본 저장하고, outbox 또는 queue worker가 idempotent하게 처리하는 방식"이다. 이 방식은 Apple 재시도, 중복 이벤트, 순서 역전, 내부 장애를 모두 다루기 쉽다. 단점은 테이블과 worker가 추가되어 단순 동기 처리보다 운영 요소가 늘어난다는 점이다.

## 공식 문서 확인 지점

Apple App Store Server Notifications v2는 외부 정책/API이므로 구현 시점마다 공식 문서를 다시 확인해야 한다. 이 설계는 작성 시점에 다음 Apple 공식 문서 내용을 기준으로 한다.

- `responseBodyV2`: v2 webhook body는 `signedPayload`를 포함하며, `signedPayload`는 App Store가 서명한 JWS다. `signedTransactionInfo`, `signedRenewalInfo`도 각각 JWS이고 서버에서 서명을 검증할 수 있다.  
  https://developer.apple.com/documentation/appstoreservernotifications/responsebodyv2
- `responseBodyV2DecodedPayload`: decoded payload에는 `notificationType`, `subtype`, `data`, `version`, `signedDate`, `notificationUUID` 등이 있고, Apple은 `notificationUUID`를 중복 알림 식별에 사용하라고 설명한다.  
  https://developer.apple.com/documentation/appstoreservernotifications/responsebodyv2decodedpayload
- `Receiving App Store Server Notifications`: v2 body의 `signedPayload`는 JWS이고, `data` 안에 App Store가 JWS로 서명한 transaction/renewal 정보가 들어갈 수 있다.  
  https://developer.apple.com/documentation/appstoreservernotifications/receiving-app-store-server-notifications
- `Responding to App Store Server Notifications`: 성공 시 `200` 또는 `200`-`206` 범위 응답을 보내고, 실패 응답이면 Apple이 재시도한다. v2 production 재시도는 이전 시도 후 1, 12, 24, 48, 72시간에 총 5회이며 sandbox는 1회만 시도한다. 장애 복구에는 Notification History, Transaction History, Subscription Status API를 사용할 수 있다.  
  https://developer.apple.com/documentation/appstoreservernotifications/responding-to-app-store-server-notifications
- `Get Transaction Info`: 서버에서 transactionId로 App Store Server API를 호출해 거래 정보를 조회할 수 있다.  
  https://developer.apple.com/documentation/appstoreserverapi/get-transaction-info

## 목표와 범위

이 설계의 목표는 자동 갱신 구독 서비스에서 Apple v2 server notification을 안전하게 받아 내부 구독 상태를 최신화하는 것이다.

포함 범위:

- Spring MVC 또는 WebFlux 기반 webhook endpoint
- JWS 서명 검증과 payload decode
- webhook 원본 이벤트 저장
- 중복 이벤트, Apple 재시도, 순서 역전 대응
- 영수증/거래 검증과 Apple Server API 보정
- 구독 DB 반영 흐름
- 실패, dead letter, 수동/자동 재처리
- 테스트와 운영 관측 가능성

비포함 범위:

- App Store Connect 설정 절차의 상세 화면 가이드
- Apple shared secret 또는 App Store Server API private key 생성
- 실제 상품 정책, 환불 정책, 가격 정책

## 추천 아키텍처

```text
Apple
  -> POST /webhooks/apple/app-store/notifications
  -> AppleNotificationController
  -> SignedPayloadVerifier
  -> apple_notification_event insert
  -> 200 OK
  -> NotificationProcessor worker
  -> Apple Server API 보정 조회(필요 시)
  -> SubscriptionCommandService
  -> subscription / entitlement / transaction upsert
  -> processed 또는 dead_letter
```

핵심 원칙은 webhook HTTP 응답 경로를 짧게 유지하는 것이다. 컨트롤러는 요청을 받고 JWS를 검증한 뒤 원본 이벤트를 저장하고 빠르게 성공 응답한다. 구독 상태 변경은 별도 worker가 처리한다.

## 컴포넌트

| 컴포넌트 | 책임 |
| --- | --- |
| `AppleNotificationController` | POST body 수신, 기본 JSON 형식 확인, 처리 결과에 따른 HTTP status 반환 |
| `AppleSignedPayloadVerifier` | `signedPayload`, `signedTransactionInfo`, `signedRenewalInfo` JWS 검증과 decode |
| `AppleNotificationEventRepository` | 원본 payload, decoded metadata, 처리 상태 저장 |
| `AppleNotificationProcessor` | pending 이벤트를 가져와 구독 도메인 명령으로 변환 |
| `AppleTransactionClient` | App Store Server API 조회. transaction/status/history/notification history 보정 |
| `SubscriptionService` | 내부 구독, entitlement, transaction 테이블 반영 |
| `DeadLetterService` | 반복 실패 이벤트 격리, 재처리 요청, 운영 알림 |

## 데이터 모델

최소 테이블은 원본 이벤트 테이블과 도메인 구독 테이블을 분리한다.

### `apple_notification_event`

| 컬럼 | 설명 |
| --- | --- |
| `id` | 내부 PK |
| `notification_uuid` | Apple decoded payload의 `notificationUUID`. unique index |
| `environment` | Sandbox, Production |
| `notification_type` | `DID_RENEW`, `EXPIRED`, `REFUND` 등 Apple notification type |
| `subtype` | subtype이 있는 경우 저장 |
| `signed_date` | Apple이 JWS에 서명한 시각 |
| `original_transaction_id` | 구독 stream 식별자. 없는 notification은 null 허용 |
| `transaction_id` | 개별 transaction 식별자 |
| `raw_signed_payload` | 원본 `signedPayload` |
| `decoded_payload_json` | 검증 후 decoded payload |
| `signed_transaction_info` | 원본 signed transaction JWS |
| `signed_renewal_info` | 원본 signed renewal JWS |
| `status` | `RECEIVED`, `PROCESSING`, `PROCESSED`, `FAILED_RETRYABLE`, `DEAD_LETTER` |
| `attempt_count` | 내부 처리 시도 횟수 |
| `last_error_code` | 마지막 실패 분류 |
| `last_error_message` | 운영 확인용 축약 메시지 |
| `received_at`, `processed_at` | 수신/처리 시각 |

인덱스:

- `unique(notification_uuid)`
- `index(status, received_at)`
- `index(original_transaction_id, signed_date)`
- `index(transaction_id)`

### 도메인 테이블 예시

| 테이블 | 핵심 unique/index |
| --- | --- |
| `apple_transaction` | `unique(transaction_id)`, `index(original_transaction_id)` |
| `subscription` | `unique(platform, original_transaction_id)` |
| `subscription_entitlement` | `unique(user_id, product_group)` 또는 서비스 정책 기준 |
| `subscription_event_log` | 내부 상태 변경 audit log |

도메인 테이블에는 원본 webhook payload를 중복 저장하지 않고, 검증된 transaction/renewal 정보에서 필요한 필드만 정규화한다.

## Webhook 수신 흐름

1. `POST /webhooks/apple/app-store/notifications`에서 body를 `Map` 또는 DTO로 받는다.
2. `signedPayload` 존재 여부와 문자열 형식만 가볍게 확인한다.
3. `signedPayload`의 JWS header, payload, signature를 파싱하고 서명을 검증한다.
4. decoded payload에서 `notificationUUID`, `notificationType`, `subtype`, `version`, `signedDate`, `data`를 추출한다.
5. `data.signedTransactionInfo`, `data.signedRenewalInfo`가 있으면 각각 별도 JWS로 검증하고 decode한다.
6. `notification_uuid` unique insert를 시도한다.
7. 이미 존재하면 내부 상태는 바꾸지 않고 성공 응답한다. Apple 입장에서는 중복 webhook도 성공적으로 수신된 것이다.
8. 신규 이벤트 저장에 성공하면 `200 OK`를 반환한다.
9. worker가 pending 이벤트를 처리한다.

중복 요청 자체의 흔적까지 남겨야 한다면 별도 `apple_notification_duplicate_audit` 같은 audit 테이블을 둔다. 원본 이벤트 테이블의 상태를 `IGNORED_DUPLICATE`로 바꾸면 최초 이벤트의 처리 상태와 중복 수신 사실이 섞이기 때문에 기본 설계에서는 사용하지 않는다.

서명 검증 실패, JSON 파싱 실패, 필수 필드 누락은 저장하지 않거나 별도 security audit 테이블에 축약 저장하고 `4xx`로 응답한다. Apple 공식 문서상 `40x`도 재시도를 유발하므로, 반복 재시도가 싫다면 잘못된 요청을 내부적으로 기록하고 `200`으로 삼킬지 결정해야 한다. 추천 기본값은 보안상 검증 실패를 성공 처리하지 않는 것이다. 운영에서 잘못된 설정 때문에 폭주하면 allowlist, rate limit, WAF, alert로 대응한다.

## JWS와 영수증/거래 검증

v2 webhook에서는 "클라이언트 영수증 문자열"을 신뢰하는 흐름보다 Apple이 서명한 JWS와 App Store Server API를 기준으로 삼는 편이 안전하다.

검증 단계:

- `signedPayload` JWS 서명을 검증한다.
- JWS header의 알고리즘과 인증서 체인을 Apple 문서와 라이브러리 요구사항에 맞게 검증한다.
- decoded payload의 `bundleId`, `appAppleId`, `environment`가 우리 서비스 설정과 일치하는지 확인한다.
- 단, `appAppleId` 등 환경별 필드 존재 여부와 sandbox/production 검증 조건은 구현 시점의 공식 문서와 실제 sandbox payload로 확인한 뒤 적용한다.
- `signedTransactionInfo`와 `signedRenewalInfo`가 있으면 각각 JWS 서명을 다시 검증한다.
- transaction payload의 `transactionId`, `originalTransactionId`, `productId`, `purchaseDate`, `expiresDate`, `revocationDate` 등을 도메인 이벤트로 변환한다.
- 중요한 상태 변경, 누락 의심, 순서 역전, worker 반복 실패 후 복구 시에는 App Store Server API의 transaction/status/history 조회로 현재 상태를 보정한다.

초기 구매 검증은 별도 API에서 처리한다. 앱이 구매 직후 `transactionId` 또는 receipt 정보를 백엔드에 보내면 서버가 Apple Server API로 거래를 확인하고 `user_id`와 `original_transaction_id`를 매핑한다. webhook은 이후 상태 변경을 보강하는 채널로 본다. webhook만으로는 어떤 내부 사용자의 구독인지 매핑하지 못하는 경우가 있으므로 초기 구매 검증 단계가 중요하다.

## Idempotency와 순서 문제

Apple 문서는 `notificationUUID`를 중복 알림 식별에 사용하라고 설명한다. 따라서 1차 idempotency key는 `notificationUUID`다.

하지만 도메인 반영은 별도 idempotency도 필요하다.

- `apple_notification_event.notification_uuid` unique index로 동일 webhook 중복 저장을 막는다.
- `apple_transaction.transaction_id` unique index로 동일 거래 중복 반영을 막는다.
- 구독 상태 업데이트는 `original_transaction_id` 단위로 직렬화한다.
- 같은 구독 stream에서 이벤트 순서가 뒤집힐 수 있으므로 `signedDate`, `purchaseDate`, `expiresDate`, `revocationDate`를 비교한다.
- 더 오래된 이벤트가 도착해도 audit log에는 남기되, 현재 entitlement를 과거 상태로 되돌리지 않는다.
- 최종 entitlement는 가능하면 "이 이벤트가 말하는 상태"가 아니라 "현재까지 관측한 transaction/renewal 정보로 계산한 상태"로 만든다.

구현 방식:

- DB row lock: `subscription where original_transaction_id = ? for update`
- 또는 분산락: `apple-subscription:{originalTransactionId}`
- 또는 queue partition key: `originalTransactionId` 기준 같은 key는 순서 처리

단일 인스턴스/낮은 트래픽이면 DB row lock으로 충분하다. 다중 worker와 queue를 쓴다면 partition key를 함께 둔다.

## 구독 DB 반영 규칙

worker는 notification type을 곧바로 DB 상태로 덮어쓰지 않는다. transaction/renewal payload를 정규화한 뒤 도메인 정책에 따라 상태를 계산한다.

예시 규칙:

| Apple event 계열 | 내부 처리 방향 |
| --- | --- |
| 신규/갱신 계열 | `expiresDate`가 현재 entitlement보다 최신이면 활성 기간 연장 |
| 만료 계열 | 최신 transaction 기준 만료가 확정되면 entitlement 비활성화 |
| 환불/취소/철회 계열 | `revocationDate` 또는 환불 정보를 저장하고 권한 회수 |
| 결제 실패/유예/청구 재시도 계열 | grace period, billing retry 정책에 맞춰 상태 전이 |
| 가격 동의/갱신 설정 변경 계열 | 구독 상태 자체보다 renewal metadata 업데이트 |
| 테스트 알림 | 운영 DB 반영 없이 수신/검증 경로만 확인 |

정확한 `notificationType`/`subtype`별 매핑은 구현 시 Apple 공식 enum 문서를 다시 보고 별도 매핑 테이블로 관리한다. 이 문서에서는 enum 전체를 단정하지 않는다.

## 실패와 재시도 전략

Apple 재시도와 내부 재시도를 분리한다.

### Apple -> 우리 서버

- 검증과 원본 저장까지 성공하면 `200 OK`를 반환한다.
- DB 장애처럼 원본 저장 자체가 실패하면 `5xx`를 반환해 Apple 재시도를 유도한다.
- production v2는 Apple이 여러 시간 간격으로 재시도하지만, sandbox는 1회만 시도하므로 sandbox 테스트에서는 내부 재처리 도구가 필요하다.

### 우리 서버 내부 처리

- worker 실패는 `FAILED_RETRYABLE`로 저장하고 exponential backoff로 재시도한다.
- 재시도 대상: Apple API 일시 오류, DB deadlock, network timeout, rate limit.
- 즉시 dead letter 대상: 지원하지 않는 bundle/environment, 검증 실패, 필수 도메인 매핑 부재가 정책상 복구 불가능한 경우.
- 매핑 부재는 바로 버리지 말고 `WAITING_FOR_USER_MAPPING` 같은 상태를 둘 수 있다. 초기 구매 API가 늦게 도착할 수 있기 때문이다.
- 최대 시도 횟수 초과 시 `DEAD_LETTER`로 이동하고 운영 알림을 보낸다.
- dead letter는 원본 payload와 오류 분류를 유지해 수동 재처리할 수 있게 한다.

재처리 API 예시:

- `POST /admin/apple-notifications/{id}/retry`
- `POST /admin/apple-notifications/replay?originalTransactionId=...`
- `POST /admin/apple-notifications/reconcile?originalTransactionId=...`

관리 API는 운영자 인증, 감사 로그, dry-run 옵션을 필수로 둔다.

## 장애 복구와 보정

Apple 문서는 서버 장애로 알림을 놓친 경우 App Store Server API의 Transaction History, Subscription Status, Notification History 등을 통해 복구할 수 있다고 설명한다.

운영 보정 job:

- 최근 N일 동안 `FAILED_RETRYABLE`, `DEAD_LETTER`, `WAITING_FOR_USER_MAPPING` 이벤트를 재검사한다.
- active subscription에 대해 주기적으로 App Store Server API status를 샘플링 또는 전체 reconcile한다.
- 장애 기간이 있으면 Notification History로 누락 알림을 가져와 `apple_notification_event`에 같은 pipeline으로 넣는다.
- Apple API rate limit과 Notification History 사용 조건은 구현 시점 공식 문서로 재확인하고, 그 결과에 맞춰 backoff와 batch size를 둔다.

## 설계 대안 비교

| 대안 | 설명 | 장점 | 단점 | 적합한 경우 |
| --- | --- | --- | --- | --- |
| A. 동기 즉시 처리 | Controller에서 검증 후 곧바로 구독 DB 업데이트 | 구조가 단순하고 구현이 빠름 | Apple timeout/재시도와 내부 장애가 결합됨. 순서/중복/재처리 대응이 약함 | MVP, 트래픽이 매우 낮고 장애 비용이 작은 경우 |
| B. DB 저장 후 비동기 worker | 검증된 원본 이벤트를 DB에 저장하고 worker가 처리 | 원본 보존, idempotency, 재처리, 감사가 쉬움 | worker와 상태 관리 테이블 필요 | 일반적인 Spring 백엔드의 추천 기본안 |
| C. Queue 기반 처리 | 수신 후 DB/outbox와 Kafka/SQS/RabbitMQ 등으로 처리 | 확장성, partition ordering, backpressure가 좋음 | 운영 복잡도와 메시지 중복 처리 비용 증가 | 트래픽이 크거나 결제 이벤트 처리가 여러 서비스로 퍼지는 경우 |

추천은 B다. 단일 Spring 백엔드에서도 충분히 견고하고, 나중에 C로 확장할 때 `apple_notification_event`와 outbox를 그대로 활용할 수 있다.

## Spring 구현 방향

패키지 예시:

```text
subscription
  apple
    web
      AppleNotificationController
    application
      AppleNotificationReceiveService
      AppleNotificationProcessor
      AppleNotificationRetryService
    infra
      AppleSignedPayloadVerifier
      AppleServerApiClient
      AppleNotificationEventRepository
    domain
      AppleNotificationEvent
      AppleNotificationStatus
      AppleSubscriptionMapper
```

컨트롤러 의사코드:

```java
@PostMapping("/webhooks/apple/app-store/notifications")
public ResponseEntity<Void> receive(@RequestBody AppleNotificationRequest request) {
    ReceiveResult result = receiveService.receive(request.signedPayload());

    if (result.isAccepted()) {
        return ResponseEntity.ok().build();
    }

    if (result.isDuplicate()) {
        return ResponseEntity.ok().build();
    }

    if (result.isRetryableInfrastructureFailure()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    return ResponseEntity.badRequest().build();
}
```

receive service 의사코드:

```java
@Transactional
public ReceiveResult receive(String signedPayload) {
    VerifiedNotification notification = verifier.verifyNotification(signedPayload);
    VerifiedTransaction transaction = null;
    if (notification.hasSignedTransactionInfo()) {
        transaction = verifier.verifyTransaction(notification.signedTransactionInfo());
    }

    VerifiedRenewal renewal = null;
    if (notification.hasSignedRenewalInfo()) {
        renewal = verifier.verifyRenewal(notification.signedRenewalInfo());
    }

    try {
        eventRepository.insertReceived(notification, transaction, renewal, signedPayload);
        return ReceiveResult.accepted(notification.notificationUUID());
    } catch (DuplicateKeyException e) {
        return ReceiveResult.duplicate(notification.notificationUUID());
    }
}
```

processor 의사코드:

```java
@Transactional
public void process(long eventId) {
    AppleNotificationEvent event = eventRepository.lockForProcessing(eventId);
    Subscription subscription = subscriptionRepository.lockByOriginalTransactionId(event.originalTransactionId());

    AppleTransactionSnapshot snapshot = mapper.toSnapshot(event);

    if (event.requiresReconcile() || snapshot.isOlderThan(subscription)) {
        snapshot = appleServerApiClient.fetchCurrentSnapshot(event.originalTransactionId());
    }

    subscription.applyAppleSnapshot(snapshot);
    transactionRepository.upsert(snapshot.transactions());
    event.markProcessed();
}
```

## 테스트 전략

단위 테스트:

- `signedPayload` 누락, malformed JWS, 서명 검증 실패
- `notificationUUID` 중복 insert 시 `200 OK`와 duplicate 처리
- 같은 `originalTransactionId`에 오래된 이벤트가 늦게 도착해도 entitlement가 과거로 되돌아가지 않는지
- `DID_RENEW`, `EXPIRED`, refund/revocation 계열의 도메인 상태 전이
- Apple API 일시 오류와 rate limit에서 retryable 상태로 남는지
- 복구 불가능한 오류가 dead letter로 이동하는지

통합 테스트:

- Testcontainers DB로 unique index, row lock, transaction 동작 검증
- worker 동시 실행 시 같은 구독 stream이 중복 반영되지 않는지
- Apple test notification endpoint의 최신 동작을 공식 문서로 재확인한 뒤 수신/응답 확인
- Notification History 기반 replay가 기존 처리 pipeline을 재사용하는지

보안 테스트:

- 다른 bundleId/appAppleId/environment의 payload 거부
- JWS header 알고리즘/인증서 검증 실패
- 너무 큰 payload, 반복 실패 요청, rate limit

## 운영 관측 가능성

메트릭:

- `apple_webhook_received_total`
- `apple_webhook_duplicate_total`
- `apple_webhook_verify_failed_total`
- `apple_notification_process_success_total`
- `apple_notification_process_failed_total`
- `apple_notification_dead_letter_total`
- `apple_notification_processing_lag_seconds`
- `apple_server_api_latency_seconds`
- `apple_server_api_error_total`

로그:

- `notificationUUID`, `notificationType`, `subtype`, `environment`, `transactionId`, `originalTransactionId`, 내부 `eventId`
- 원본 payload 전체는 일반 로그에 남기지 않는다. DB 원본 테이블 또는 보안 저장소에서 접근 통제한다.

알림:

- 검증 실패율 급증
- dead letter 증가
- 처리 lag 증가
- Apple API 401/429/5xx 증가
- 특정 `originalTransactionId` 반복 실패

대시보드:

- 수신량, 중복률, 처리 성공률, 처리 지연
- notification type별 분포
- sandbox/production 환경별 분리

## 확인 필요와 남은 결정

- 실제 서비스가 StoreKit 1 receipt 기반인지 StoreKit 2 transactionId 기반인지 확인해야 한다.
- notification type/subtype별로 `signedTransactionInfo`, `signedRenewalInfo`가 항상 존재하는지, 일부 케이스에서 누락 가능한지 공식 enum/스키마 기준으로 확인해야 한다.
- Apple JWS 검증은 직접 구현보다 Apple이 제공하는 서버 라이브러리 또는 검증된 JOSE 라이브러리 사용을 우선 검토한다. 구현 시 최신 공식 문서와 라이브러리 지원 범위를 다시 확인한다.
- 내부 구독 정책, grace period, billing retry, refund 후 권한 회수 시점은 제품 정책 결정이 필요하다.
- queue를 도입할지 여부는 현재 트래픽, 운영 인력, 장애 허용 범위에 따라 결정한다. 초기 추천은 DB 저장 후 worker 방식이다.
- App Store Server API private key, issuer id, key id 같은 secret은 임의 생성하지 않고 사용자가 제공한 값을 secret manager에 저장한다.
