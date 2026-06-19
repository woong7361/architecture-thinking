# Apple 인앱 구독 결제, 어디까지 백엔드에서 처리해야 할까

앱에서 구독 결제를 붙일 때 처음에는 흐름이 단순해 보인다.

사용자가 앱에서 결제한다. 결제가 성공한다. 그러면 프리미엄 기능을 열어준다.

하지만 실제 구독 결제는 여기서 끝나지 않는다. 자동 갱신이 있고, 결제 실패가 있고, 유예 기간이 있고, 만료와 환불도 있다. 사용자가 자동 갱신을 껐다고 해서 바로 권한을 뺏으면 안 되고, 반대로 결제가 성공한 것처럼 보여도 환불이나 revoke 상태라면 권한을 회수해야 한다.

그래서 Apple 인앱 구독 결제에서 중요한 원칙은 하나다.

**구독 권한의 최종 판단은 앱이 아니라 백엔드가 해야 한다.**

이 글은 StoreKit 2, App Store Server API, App Store Server Notifications V2를 기준으로 Apple 인앱 구독 결제를 구현할 때 백엔드가 어떤 책임을 가져야 하는지 정리한 글이다.

## 전체 구조

Apple 인앱 구독 결제의 큰 흐름은 다음과 같다.

```text
앱에서 결제
 -> 앱이 transaction 정보를 백엔드로 전달
 -> 백엔드가 Apple 기준으로 결제 검증
 -> 백엔드가 구독 권한 저장
 -> 갱신, 실패, 만료, 환불 이벤트는 Apple Notification으로 수신
 -> 백엔드가 App Store Server API로 최신 상태 재조회
 -> DB의 구독 권한 갱신
```

여기서 앱의 역할은 결제를 시작하고, 결제 결과로 받은 transaction 정보를 백엔드에 전달하는 것이다.

백엔드의 역할은 더 무겁다. Apple 서명을 검증하고, 서버 API로 상태를 다시 확인하고, 내부 사용자와 Apple transaction을 연결하고, 현재 사용자가 유료 기능을 사용할 수 있는지 판단해야 한다.

## 상품은 App Store Connect와 백엔드에 함께 정의한다

먼저 App Store Connect에 구독 상품을 등록한다.

예를 들어 프리미엄 플랜을 월간과 연간으로 제공한다면 다음처럼 구성할 수 있다.

| Apple productId | 내부 플랜 | 주기 | 권한 |
| --- | --- | --- | --- |
| `premium_monthly` | Premium | 월간 | Premium 기능 사용 |
| `premium_yearly` | Premium | 연간 | Premium 기능 사용 |

같은 권한을 제공하는 월간, 연간 상품은 같은 subscription group 안에 둔다. 서로 다른 권한을 제공하는 상품이라면 subscription group을 분리한다.

백엔드에는 Apple의 `productId`와 내부 플랜을 매핑해 둔다. 그래야 검증 시점에 이 상품이 우리 서비스에서 허용한 상품인지 확인할 수 있다.

## 앱은 결제하고, 백엔드는 검증한다

앱에서는 StoreKit으로 상품을 조회하고 결제를 시작한다.

이때 가능하면 백엔드에서 `appAccountToken`을 발급해 앱에 내려주는 편이 좋다. `appAccountToken`은 secret은 아니지만, Apple transaction과 내부 사용자를 연결하는 식별자로 사용할 수 있다.

앱 결제 시에는 보통 다음 정보가 중요하다.

- `productId`
- `appAccountToken`

결제가 성공하면 앱은 백엔드에 검증 요청을 보낸다.

```http
POST /api/subscriptions/apple/verify
```

요청에는 다음 값들이 포함될 수 있다.

```json
{
  "transactionId": "2000000000000000",
  "originalTransactionId": "2000000000000000",
  "productId": "premium_monthly",
  "signedTransactionInfo": "eyJ..."
}
```

여기서 `transactionId`와 `originalTransactionId`는 구분해서 봐야 한다.

`transactionId`는 개별 결제 이벤트의 ID다. 최초 결제, 갱신 결제마다 다른 값이 생길 수 있다.

반면 `originalTransactionId`는 구독 라이프사이클을 묶는 기준값이다. 한 사용자의 동일 구독 흐름을 추적할 때는 이 값을 중심으로 보는 편이 좋다.

## 클라이언트의 결제 성공 응답을 그대로 믿지 않는다

백엔드는 앱에서 "결제 성공"이라고 보내온 값을 그대로 신뢰하면 안 된다.

최소한 다음 항목을 확인해야 한다.

- signed transaction JWS 서명 검증
- `bundleId` 확인
- `environment` 확인
- `productId`가 백엔드에 등록된 상품인지 확인
- `appAccountToken`과 내부 사용자 매칭 확인
- App Store Server API로 transaction 또는 구독 상태 재조회
- `revocationDate`, `revocationReason`, `revocationType` 기준 환불 또는 권한 회수 여부 확인

검증이 끝나면 백엔드는 구독 권한을 DB에 저장한다.

저장 필드는 서비스마다 다를 수 있지만, 최소한 다음 정보는 관리하는 편이 좋다.

| 필드 | 설명 |
| --- | --- |
| `userId` | 내부 사용자 ID |
| `originalTransactionId` | 구독 라이프사이클 기준 ID |
| `transactionId` | 개별 결제 transaction ID |
| `productId` | Apple 상품 ID |
| `subscriptionGroupIdentifier` | Apple 구독 그룹 식별자 |
| `status` | `ACTIVE`, `EXPIRED`, `BILLING_RETRY`, `BILLING_GRACE_PERIOD`, `REVOKED` 등 |
| `expiresAt` | 내부 권한 만료 시각 |
| `gracePeriodExpiresAt` | 유예 기간 권한 만료 시각 |
| `autoRenewStatus` | 자동 갱신 여부 |
| `revocationDate` | 환불 또는 권한 회수 시각 |
| `environment` | Sandbox 또는 Production |
| `lastVerifiedAt` | 마지막 검증 시각 |

앱에는 Apple 결제 결과가 아니라 백엔드가 판단한 구독 상태를 내려준다.

```json
{
  "active": true,
  "plan": "Premium",
  "productId": "premium_monthly",
  "expiresAt": "2026-07-15T10:00:00Z"
}
```

앱은 이 응답을 기준으로 프리미엄 기능을 열어준다.

## 구독은 최초 결제보다 이후 처리가 더 중요하다

구독 결제에서 까다로운 부분은 최초 결제가 아니다. 최초 결제 이후 발생하는 상태 변화다.

대표적으로 다음 이벤트가 있다.

| 상황 | 백엔드 처리 |
| --- | --- |
| 최초 결제 | 구독 생성, `expiresDate` 기준 권한 부여 |
| 자동 갱신 성공 | 새 `expiresDate`로 권한 연장 |
| 자동 갱신 해제 | 즉시 권한 제거하지 않고 기존 만료일까지 유지 |
| 결제 실패 | 결제 재시도 또는 유예 상태로 전환 |
| 유예 기간 진입 | `gracePeriodExpiresDate`까지 권한 유지 가능 |
| 유예 기간 종료 | 권한 제거 |
| 결제 복구 | 구독 재활성화 |
| 만료 | 권한 제거 |
| 재구독 | 기존 구독 기준으로 다시 활성화 |
| 환불 | 정책에 따라 권한 즉시 회수 |
| revoke | 권한 제거 |

이 이벤트들은 App Store Server Notifications V2로 받을 수 있다.

다만 notification payload만 보고 DB를 바로 바꾸는 것보다는, 알림을 받은 뒤 App Store Server API로 최신 상태를 다시 조회하는 방식이 안전하다.

```text
Apple Notification 수신
 -> signedPayload 서명 검증
 -> notificationUUID 중복 처리 여부 확인
 -> originalTransactionId 기준 구독 조회
 -> App Store Server API로 최신 구독 상태 조회
 -> DB 갱신
 -> 200 OK 응답
```

Apple notification은 중복으로 올 수 있으므로 반드시 멱등하게 처리해야 한다. 예를 들어 `notificationUUID`를 저장해 이미 처리한 알림인지 확인할 수 있다.

또 처리에 실패했다면 성공 응답을 먼저 보내면 안 된다. Apple이 재전송할 수 있도록, 실제 처리가 완료된 뒤에만 `200 OK`를 반환하는 편이 좋다.

## 권한 판단에서 자주 실수하는 부분

구독 권한을 판단할 때 단순히 `expiresAt` 하나만 보면 위험하다.

예를 들어 사용자가 자동 갱신을 껐다고 해도 이미 결제한 기간이 남아 있다면 만료일까지는 권한을 유지해야 한다. 자동 갱신 해제는 "다음 갱신을 하지 않겠다"는 뜻이지, "지금 바로 구독을 취소한다"는 뜻이 아니다.

반대로 grace period에서는 `expiresDate`가 이미 지났더라도 `gracePeriodExpiresDate`까지 권한을 유지할 수 있다.

활성 구독으로 판단할 수 있는 조건은 대략 다음과 같다.

- `status`가 `ACTIVE`이고 `expiresAt`이 현재 시각보다 이후다.
- `status`가 `BILLING_GRACE_PERIOD`이고 `gracePeriodExpiresAt`이 현재 시각보다 이후다.
- 환불 또는 revoke 상태가 아니다.
- 해당 사용자의 `appAccountToken` 또는 계정 연결이 유효하다.

결국 권한 판단은 `autoRenewStatus`, `expiresAt`, `gracePeriodExpiresAt`, `revocationDate`, 현재 상태값을 함께 보고 결정해야 한다.

## 필요한 API 엔드포인트

백엔드에는 보통 다음 엔드포인트가 필요하다.

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/subscriptions/apple/verify` | 앱 결제 후 서버 검증 |
| `POST` | `/api/subscriptions/apple/notifications` | App Store Server Notifications V2 수신 |
| `GET` | `/api/me/subscription` | 현재 사용자 구독 권한 조회 |
| `POST` | `/api/subscriptions/apple/sync` | 수동 구독 상태 동기화 또는 복구 |

특히 `/sync` 같은 수동 동기화 엔드포인트는 운영 중에 유용하다. notification 처리 실패, 지연, 사용자 문의 대응 상황에서 Apple 기준 최신 상태를 다시 가져와 내부 DB를 복구할 수 있기 때문이다.

## 정리

Apple 인앱 구독 결제는 "결제 버튼을 누르고 성공하면 끝"인 기능이 아니다.

앱은 결제를 수행하지만, 구독 권한의 최종 판단은 백엔드가 담당해야 한다. 백엔드는 Apple 서명을 검증하고, App Store Server API로 최신 상태를 확인하고, notification을 멱등하게 처리해야 한다.

구현할 때 기억해야 할 핵심은 다음과 같다.

- 결제 성공 여부는 클라이언트가 아니라 백엔드에서 최종 판단한다.
- 구독 라이프사이클의 기준 키는 `originalTransactionId`로 둔다.
- 개별 결제 이력은 `transactionId` 기준으로 저장한다.
- notification은 중복 수신될 수 있으므로 멱등하게 처리한다.
- 자동 갱신 해제는 즉시 권한 제거가 아니다.
- 환불, revoke, grace period 종료는 권한 제거 대상이다.
- grace period에서는 `expiresDate`뿐 아니라 `gracePeriodExpiresDate`를 함께 봐야 한다.
- Sandbox와 Production 환경은 분리해서 저장하고 검증한다.

처음부터 이 구조를 잡아두면 결제 성공, 갱신, 만료, 환불, 복구 같은 구독의 전체 라이프사이클을 일관되게 다룰 수 있다. 인앱 구독 결제에서 정말 중요한 것은 결제 순간보다, 그 이후의 권한 상태를 안정적으로 유지하는 일이다.

## 참고 링크

- App Store Server API: https://developer.apple.com/documentation/appstoreserverapi
- App Store Server Notifications: https://developer.apple.com/documentation/appstoreservernotifications
- Apple App Store Server Java Library: https://github.com/apple/app-store-server-library-java
- NotificationTypeV2 enum: https://apple.github.io/app-store-server-library-java/com/apple/itunes/storekit/model/NotificationTypeV2.html
- Subtype enum: https://apple.github.io/app-store-server-library-java/com/apple/itunes/storekit/model/Subtype.html
- Subscription Status enum: https://apple.github.io/app-store-server-library-java/com/apple/itunes/storekit/model/Status.html
