# Apple 인앱 구독 결제 Flow

작성일: 2026-06-15

## 전제

- 결제 상품은 구독 상품만 제공한다.
- 앱에서 Apple In-App Purchase로 결제를 수행한다.
- 백엔드는 앱이 전달한 결제 정보를 Apple 서버 기준으로 검증한다.
- 구독 권한의 최종 판단은 앱이 아니라 백엔드가 담당한다.
- 신규 구현 기준으로는 StoreKit 2, App Store Server API, App Store Server Notifications V2 사용을 전제로 한다.

## 전체 흐름

```text
앱 결제
 -> 앱이 transaction 정보를 백엔드로 전달
 -> 백엔드가 Apple 서명 및 서버 API로 결제 검증
 -> 백엔드가 구독 권한 저장
 -> 이후 갱신/실패/만료/환불 이벤트는 App Store Server Notifications로 수신
 -> 알림 수신 시 App Store Server API로 최신 상태 재조회
 -> 백엔드 DB의 구독 권한 갱신
```

## 1. 상품 구성

App Store Connect에 구독 상품을 등록한다.

- 같은 서비스 권한을 제공하는 월간, 연간 등 대체 구독 상품은 같은 subscription group 안에 구성한다.
- 서로 독립적인 권한을 제공하는 구독 상품이라면 subscription group을 분리한다.
- 백엔드는 Apple 상품 ID와 내부 플랜을 매핑한다.

예시:

| Apple productId | 내부 플랜 | 주기 | 권한 |
| --- | --- | --- | --- |
| `premium_monthly` | Premium | 월간 | Premium 기능 사용 |
| `premium_yearly` | Premium | 연간 | Premium 기능 사용 |

## 2. 앱에서 결제 시작

앱은 StoreKit으로 상품을 조회하고 결제를 시작한다.

가능하면 백엔드에서 내부 사용자와 매핑된 UUID 형태의 `appAccountToken`을 발급해 앱에 전달한다.

앱 결제 시 포함할 정보:

- `productId`
- `appAccountToken`

`appAccountToken`을 사용하면 백엔드 검증 시 결제 트랜잭션과 내부 사용자를 더 안전하게 연결할 수 있다.

`appAccountToken`은 secret이 아니라 사용자와 App Store transaction을 연결하기 위한 식별자이다.

## 3. 앱에서 백엔드로 결제 검증 요청

결제가 성공하면 앱은 백엔드에 검증 요청을 보낸다.

요청에 포함할 값:

- 현재 로그인 사용자 ID
- `transactionId`
- `originalTransactionId`
- StoreKit 2 signed transaction JWS
- `productId`

`originalTransactionId`는 구독 라이프사이클을 묶는 기준값이고, `transactionId`는 최초 결제나 갱신마다 생성되는 개별 트랜잭션 ID이다.

예시:

```http
POST /api/subscriptions/apple/verify
```

```json
{
  "transactionId": "2000000000000000",
  "originalTransactionId": "2000000000000000",
  "productId": "premium_monthly",
  "signedTransactionInfo": "eyJ..."
}
```

## 4. 백엔드 결제 검증

백엔드는 클라이언트의 결제 성공 응답을 그대로 신뢰하지 않고 Apple 기준으로 검증한다.

검증 항목:

- signed transaction JWS 서명 검증
- `bundleId` 확인
- `environment` 확인
- `productId`가 백엔드에 등록된 상품인지 확인
- `appAccountToken`과 내부 사용자 매칭 확인
- App Store Server API로 트랜잭션 또는 구독 상태 재조회
- `revocationDate`, `revocationReason`, `revocationType` 기준으로 환불 또는 권한 회수 여부 확인

검증이 성공하면 구독 권한을 저장한다.

저장 필드 예시:

| 필드 | 설명 |
| --- | --- |
| `userId` | 내부 사용자 ID |
| `originalTransactionId` | 구독 라이프사이클의 기준 ID |
| `transactionId` | 개별 결제 트랜잭션 ID |
| `webOrderLineItemId` | 갱신을 포함한 구독 구매 이벤트 식별자 |
| `productId` | Apple 상품 ID |
| `subscriptionGroupIdentifier` | Apple 구독 그룹 식별자 |
| `status` | `ACTIVE`, `EXPIRED`, `BILLING_RETRY`, `BILLING_GRACE_PERIOD`, `REVOKED` 등 |
| `expiresAt` | 내부 권한 만료 시각. 보통 Apple `expiresDate`에서 산출 |
| `gracePeriodExpiresAt` | grace period 권한 만료 시각. Apple `gracePeriodExpiresDate`에서 산출 |
| `autoRenewStatus` | 자동 갱신 여부 |
| `revocationDate` | 환불 또는 권한 회수 시각 |
| `environment` | Sandbox 또는 Production |
| `lastVerifiedAt` | 마지막 검증 시각 |

## 5. 앱에 구독 권한 반영

백엔드는 검증 결과를 앱에 반환한다.

앱은 Apple 결제 결과가 아니라 백엔드의 구독 권한 상태를 기준으로 기능을 열어준다.

예시 응답:

```json
{
  "active": true,
  "plan": "Premium",
  "productId": "premium_monthly",
  "expiresAt": "2026-07-15T10:00:00Z"
}
```

## 결제 주기별 처리

구독은 최초 결제 이후에도 자동 갱신, 결제 실패, 유예 기간, 만료, 재구독, 환불이 발생한다.

백엔드는 App Store Server Notifications V2를 수신하고, 알림을 받은 뒤 App Store Server API로 최신 상태를 다시 조회해 DB를 갱신한다.

| 상황 | notificationType | subtype 또는 status | 백엔드 처리 |
| --- | --- | --- | --- |
| 최초 결제 | `SUBSCRIBED` | `INITIAL_BUY` | 구독 생성, `expiresDate` 기준으로 권한 부여 |
| 자동 갱신 성공 | `DID_RENEW` | 없음 또는 Apple payload 기준 subtype | 새 `expiresDate`로 구독 권한 연장 |
| 자동 갱신 해제 | `DID_CHANGE_RENEWAL_STATUS` | `AUTO_RENEW_DISABLED` | 즉시 권한 제거하지 않고 기존 `expiresDate`까지 유지 |
| 자동 갱신 재활성화 | `DID_CHANGE_RENEWAL_STATUS` | `AUTO_RENEW_ENABLED` | 자동 갱신 상태 갱신 |
| 결제 실패 | `DID_FAIL_TO_RENEW` | `BILLING_RETRY` 또는 `GRACE_PERIOD` | 결제 재시도 또는 유예 상태로 전환 |
| 유예 기간 진입 | `DID_FAIL_TO_RENEW` | `GRACE_PERIOD` | `BILLING_GRACE_PERIOD` 상태로 보고 `gracePeriodExpiresDate`까지 권한 유지 가능 |
| 유예 기간 종료 | `GRACE_PERIOD_EXPIRED` | 없음 | 구독 권한 제거 |
| 결제 복구 | `DID_RENEW` | `BILLING_RECOVERY` | 구독 재활성화, 새 `expiresDate` 반영 |
| 만료 | `EXPIRED` | `VOLUNTARY`, `BILLING_RETRY`, `PRICE_INCREASE`, `PRODUCT_NOT_FOR_SALE` 등 | 구독 권한 제거 |
| 재구독 | `SUBSCRIBED` | `RESUBSCRIBE` | 기존 구독 기준으로 다시 활성화 |
| 업그레이드 | `DID_CHANGE_RENEWAL_PREF` 또는 신규 transaction | `UPGRADE` | 상위 플랜 권한 반영. 즉시 적용 여부는 Apple transaction 상태 기준으로 판단 |
| 다운그레이드 | `DID_CHANGE_RENEWAL_PREF` | `DOWNGRADE` | 보통 다음 갱신 시점부터 하위 플랜 반영 |
| 환불 | `REFUND` | 없음 | `revocationDate` 기준으로 권한 즉시 회수 또는 내부 정책에 따라 처리 |
| 환불 철회 | `REFUND_REVERSED` | 없음 | 이전 환불 회수 처리 복구 여부 확인 |
| 권한 회수 | `REVOKE` | 없음 | 권한 제거 |

## 백엔드 Notification 처리 흐름

```text
Apple Notification 수신
 -> signedPayload 서명 검증
 -> notificationUUID 중복 처리 여부 확인
 -> originalTransactionId 기준 구독 조회
 -> App Store Server API로 최신 구독 상태 조회
 -> DB 갱신
 -> 200 OK 응답
```

주의할 점:

- Apple 알림은 중복으로 올 수 있으므로 idempotency 처리가 필요하다.
- 알림 payload만 믿지 말고 App Store Server API로 최신 상태를 재조회하는 편이 안전하다.
- 알림 처리 실패 시 Apple이 재전송할 수 있으므로, 처리 완료 후에만 성공 응답을 반환한다.

## 권한 판단 기준

앱에서 유료 기능 접근을 요청하면 백엔드는 현재 사용자 구독 상태를 확인한다.

활성 구독으로 판단할 수 있는 조건 예시:

- `status`가 `ACTIVE`이고 `expiresAt`이 현재 시각보다 이후이다.
- `status`가 `BILLING_GRACE_PERIOD`이고 `gracePeriodExpiresAt`이 현재 시각보다 이후이다.
- 환불 또는 revoke 상태가 아니다.
- 해당 사용자의 `appAccountToken` 또는 계정 연결이 유효하다.

권한 판단은 단순히 `autoRenewStatus` 또는 `expiresAt`만으로 하면 안 된다.

자동 갱신이 꺼져 있어도 `expiresAt` 전까지는 사용자가 이미 결제한 기간이므로 권한을 유지해야 한다.

반대로 grace period에서는 `expiresDate`가 이미 지났더라도 `gracePeriodExpiresDate`까지 권한을 유지할 수 있다.

## 구현 시 필요한 엔드포인트 예시

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/subscriptions/apple/verify` | 앱 결제 후 서버 검증 |
| `POST` | `/api/subscriptions/apple/notifications` | App Store Server Notifications V2 수신 |
| `GET` | `/api/me/subscription` | 현재 사용자 구독 권한 조회 |
| `POST` | `/api/subscriptions/apple/sync` | 수동 구독 상태 동기화 또는 복구 |

## 핵심 원칙

- 결제 성공 여부는 클라이언트가 아니라 백엔드에서 최종 판단한다.
- 구독 라이프사이클의 기준 키는 `originalTransactionId`로 둔다.
- 개별 결제 이력은 `transactionId` 기준으로 저장한다.
- notification은 중복 수신될 수 있으므로 반드시 멱등하게 처리한다.
- notificationType과 subtype은 구분해서 저장한다.
- 자동 갱신 해제는 즉시 권한 제거가 아니다.
- 환불, revoke, grace period 종료는 권한 제거 대상이다.
- grace period 권한 판단은 `expiresDate`가 아니라 `gracePeriodExpiresDate`를 함께 봐야 한다.
- Sandbox와 Production 환경을 분리해서 저장하고 검증한다.

## 참고 링크

- App Store Server API: https://developer.apple.com/documentation/appstoreserverapi
- App Store Server Notifications: https://developer.apple.com/documentation/appstoreservernotifications
- Apple App Store Server Java Library: https://github.com/apple/app-store-server-library-java
- NotificationTypeV2 enum: https://apple.github.io/app-store-server-library-java/com/apple/itunes/storekit/model/NotificationTypeV2.html
- Subtype enum: https://apple.github.io/app-store-server-library-java/com/apple/itunes/storekit/model/Subtype.html
- Subscription Status enum: https://apple.github.io/app-store-server-library-java/com/apple/itunes/storekit/model/Status.html
