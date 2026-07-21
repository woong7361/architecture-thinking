# Original User Input

KG이니시스에서 kakaopay 정기결제를 지원해줘?


# Checked Context

# 확인한 문맥과 근거

- 프로젝트 내부에서 KG이니시스, 카카오페이, 정기결제 관련 기존 문서는 검색되지 않았다.
- KG이니시스 공식 빌링 FAQ는 빌링 대상을 신용카드와 휴대폰으로 설명한다.
  - https://www.inicis.com/blog/archives/126775
  - https://www.inicis.com/blog/archives/472
- KG이니시스의 카카오페이 안내는 일반 간편결제 서비스로 확인되지만, 공개 빌링 문서에는 카카오페이가 결제수단으로 기재되어 있지 않다.
- 포트원 KG이니시스 연동 문서의 정기결제 빌링키 발급 예시는 `pay_method: "card"`만 지원한다고 명시한다.
  - https://developers.portone.io/opi/ko/integration/pg/v1/inicis
- 포트원의 빌링키 지원표는 KG이니시스와 카카오페이를 서로 다른 PG provider로 구분한다.
  - https://developers.portone.io/opi/ko/support/code-info/pg?v=v1
- 카카오페이 공식 개발자센터는 카카오페이 자체 정기결제 API와 정기결제용 CID, SID 흐름을 제공한다.
  - https://developers.kakaopay.com/docs/payment/online/subscription
- 공개 문서만으로 특정 가맹점의 별도 제휴나 예외 계약까지 배제할 수는 없다.
