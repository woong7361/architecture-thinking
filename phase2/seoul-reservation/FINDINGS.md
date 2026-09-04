# 서울시 공공서비스예약 — 테니스장 빈자리 조회 조사 기록

조사일: 2026-09-03
대상: https://yeyak.seoul.go.kr
목표: 2026-09-04(금) 10:00~11:00 예약 가능한 테니스장 찾기

---

## 결론 요약

| | 상태 |
|---|---|
| 테니스장 서비스 목록 수집 | ✅ 됨 (270건 전수) |
| 접수중 / 온라인 / 이용기간 필터링 | ✅ 됨 (87건으로 축소) |
| 날짜별 잔여 조회 (9/4에 자리 있나) | ⚠️ 엔드포인트·응답구조는 파악, 실호출은 차단 |
| 시간대별 잔여 조회 (10~11시) | ❌ 예약 신청 플로우 안에 있음. 로그인 벽 추정 |
| 서버측 스크립트로 자동화 | ❌ 사이트가 차단 (dynapath) |
| 브라우저 콘솔에서 실행 | ⚠️ 가능할 것으로 보임. 미검증 |

**한 줄로**: 목록은 긁을 수 있고, 빈자리는 못 긁는다. 빈자리 조회 엔드포인트만 딱 골라서 자동화 차단이 걸려 있다.

---

## 되는 것

### 1. 목록 페이지 — 평범한 HTTP 클라이언트로 접근 가능

```
GET /web/search/selectPageListDetailSearchImg.do?code=T100&dCode=T108&currentPage=N
```

- `code=T100` = 체육시설, `dCode=T108` = 테니스장
- 페이지당 6건, 46페이지 = 270건
- 세션 쿠키 필요: `/web/main.do`를 먼저 한 번 GET → 쿠키 받고 → Referer 붙여서 요청
- 서버사이드 렌더링이라 HTML 파싱으로 전부 뽑힌다

카드 하나에서 뽑을 수 있는 것:

```
onclick="fnDetailPage('S260824180947671433', '', '')" title="망원 한강공원 테니스장 3번 코트 평일"
접수중 / 유료 / 선착순 / 장소명 / 이용대상 / 접수기간 / 이용기간 / 온라인
```

카테고리 코드 (체육시설 하위):

```
T101 농구장   T102 다목적경기장  T103 배구장    T104 배드민턴장
T105 야구장   T106 족구장        T107 축구장    T108 테니스장
T109 풋살장   T115 골프장        T116 교육시설  T117 수영장
T118 피클볼장 T125 운동장        T126 체육관    T127 탁구장
T129 서울어울림체육센터
```

### 2. 상세 페이지 — GET으로 접근 가능

```
GET /web/reservation/selectReservView.do?rsv_svc_id=<SVC_ID>
Referer: 목록 페이지 URL
```

비로그인으로 200 + 정상 HTML(133KB). 단 **자동화 브라우저에서 이 URL로 직접 이동하면 차단**된다 (아래 참조).

### 3. 서울 열린데이터광장 OpenAPI — 접근은 되지만 쓸모가 제한적

```
http://openapi.seoul.go.kr:8088/{KEY}/json/ListPublicReservationSport/{start}/{end}/
```

- 샘플키 `sample`은 한 번에 5건까지만. 전체는 발급 키 필요
- 체육시설 전체 575건

제공 필드:

```
GUBUN, SVCID, MAXCLASSNM, MINCLASSNM, SVCSTATNM, SVCNM, PAYATNM,
PLACENM, USETGTINFO, SVCURL, X, Y, SVCOPNBGNDT, SVCOPNENDDT,
RCPTBGNDT, RCPTENDDT, AREANM, IMGURL, DTLCONT
```

**날짜·시간대별 잔여석 필드가 없다.** `SVCSTATNM`은 서비스 단위 상태(접수중/접수마감)일 뿐. API 키를 발급받아도 "9/4 10시 빈자리" 질문에는 답이 안 나온다.

---

## 안 되는 것

### 1. 빈자리 조회 AJAX — dynapath로 보호됨

모든 페이지 `<head>`에 이 선언이 박혀 있다:

```js
var dpCnf = {
  d: function() { return "/management/ipRedirect.do?threatGb=DNP_D&..."; },
  x: [
    { u: "/web/reservation/selectListReservCalAjax.do" },      // 월 단위 날짜별 잔여
    { u: "/web/reservation/selectAllTimeCheckAjax.do" },
    { u: "/web/reservation/selectListReservCalUnitAjax.do" }   // 시간·회차별 잔여
  ]
};
```

동작 방식:

- `/common/js/dynapath/dp.helper.js` + `/dynaPath.jsp` (둘 다 난독화)가 이 URL들을 매 요청 다른 토큰 경로로 바꿔치기한다
- 토큰 없이 원래 경로로 요청하면 `d()`가 가리키는 차단 페이지로 리다이렉트
- 즉 **"페이지의 JS를 실제로 실행한 브라우저"만 호출 가능**하도록 설계

빈자리 데이터를 주는 엔드포인트가 정확히 이 세 개다. 다른 건 다 열려 있는데 이것만 잠겨 있다.

### 2. 자동화 브라우저 — 탐지되어 차단

Orca(Electron 기반) 브라우저로 시도한 것과 결과:

| 시도 | 결과 |
|---|---|
| 새 프로파일 → 목록 페이지 로드 | ✅ 정상 |
| 목록 페이지에서 `fetch()`로 46페이지 순회 | ✅ 정상 (데이터 다 받음) |
| `goto` 로 상세 URL 직접 이동 | ❌ 차단 |
| 사이트 자체 함수 `fnDetailPage()` 호출 | ❌ 차단 |
| 신규 프로파일에서 메인→체육시설→테니스장 **정상 클릭 경로** | ❌ 차단 |

차단 문구:

> 비정상 접근이 감지되어 서비스가 제한되었습니다.
> 반복적인 시도 시에는 관계 법령에 따라 수사 의뢰 등 조치될 수 있음을 알려드립니다.
> 정상 접근을 위해서는 인터넷 쿠키를 삭제하고 브라우저 전체를 완전 종료 후 다시 접속하시기 바랍니다.

프로파일을 4번 새로 만들어 재시도했으나 전부 동일. **IP 차단은 아니다** — 같은 시점에 curl/urllib은 계속 200을 받았다. 브라우저 세션 단위 탐지로 보인다.

경고 문구 수위 때문에 여기서 시도를 중단했다. 우회는 하지 않음.

### 3. `/web/reservation/selectPageListReserveStatus.do`

응답 0바이트. 로그인 필요한 "내 예약현황"으로 추정. 미확인.

---

## 빈자리 조회 흐름 (코드에서 읽어낸 것, 미검증)

### 1단계 — 날짜별 잔여

상세페이지의 `fnDraw()`가 하는 일:

```js
POST /web/reservation/selectListReservCalAjax.do
body: $('#aform').serialize()
```

`#aform`은 상세페이지에 이미 렌더링되어 있는 히든 폼. 핵심 필드:

```
rsv_svc_id=S260824180947671433   # 코트 서비스 ID
yyyy=2026  mm=09  yyyymm=202609  # 조회할 달
use_time_unit_code=B402          # B401=회차 B402=시간 B403=일 B409=박
tme_ty_code=TM02
sysToday=20260903
```

응답 JSON:

```js
resultListDays[i] = { YMD: "20260904", SVC_RESVE_CODE: "Y" }
resultListTm["20260904"] = {
  RCEPT_POSBL_YN:  "1",   // 접수 가능 여부
  RESVE_POSBL_CNT: 3,     // 예약 가능 수
  REG_TOTAL_CNT:   5,     // 현재 신청
  RCRIT_NMPR_CNT:  8      // 정원
}
```

판정 (페이지 코드 그대로):

```js
SVC_RESVE_CODE == 'Y' && RCEPT_POSBL_YN == '1' && RESVE_POSBL_CNT > 0
  → 예약가능, 달력에 "5/8" 표시
(SVC_RESVE_CODE == 'C' || 'N') && RCEPT_POSBL_YN == '1'
  → 예약불가, 숫자만 표시
else
  → 예약불가, 빈칸
```

**한계: 날짜 단위까지만.** "9/4에 3자리 남음"은 알아도 그게 10시인지 15시인지는 안 나온다.

### 2단계 — 시간대별 잔여

날짜 선택 후 `fnRevervInsertForm()`:

```js
$('[name=useDe]').val('20260904');
$('#aform').attr({action:'/web/reservation/insertFormReserve.do', method:'post'}).submit();
```

넘어간 화면에서 `selectListReservCalUnitAjax.do`로 시간·회차별 잔여를 가져오는 것으로 보인다.
이 화면은 **예약 신청서 화면**이라 로그인 벽이 여기 있을 확률이 높다. 미확인.

---

## 남은 선택지

**A. 브라우저 콘솔 스크립트 (1단계만)** — 가장 현실적

사용자가 직접 연 상세페이지의 콘솔에서 그 페이지의 함수를 호출하면 dynapath 토큰이 자연히 적용된다. 세션·로그인 상태도 그대로 물려받으므로 로그인을 스크립트가 처리할 필요가 없다.

관심 코트 10~20개에 대해 1단계만 돌려서 "9/4에 자리가 있는 코트"로 후보를 줄이고, 10~11시 확인은 살아남은 3~5개만 손으로. 요청 간격은 사람 수준(2~3초)으로.

미검증이고, 빠르게 순회하면 WAF가 여전히 반응할 수 있다.

**B. 사이트 자체 알림 기능** — 취소표 노림수라면 이쪽이 정답

로그인 후 '관심 서비스' 등록 + 알림. 폴링할 이유 자체가 없어진다.
관련 엔드포인트: `/web/mypage/addBookmark.do`, `/web/mypage/addSubscribe.do`

**C. 데이터 제공 요청** — 정공법, 느림

열린데이터광장에 잔여석 필드 추가 요청, 또는 관리기관 직접 문의.
(한남·장충은 각각 용산구·중구 시설관리공단)

---

## 산출물

- `tennis_0904.md` — 2026-09-04 기준 접수중·온라인 테니스장 87건. 자치구/시설별 그룹 + 예약 상세 링크
- `tennis_0904.csv` — 같은 데이터. 자치구·시설·서비스명·접수·이용기간·URL. UTF-8 BOM

87건 필터 조건: `체육시설>테니스장` AND `상태=접수중` AND `신청방법=온라인` AND `이용기간이 2026-09-04를 포함`

9/4는 금요일이므로 목록 중 **주말·공휴일 전용**(광나루 5번 주말, 보라매 4·5번 주말, 남부도로사업소 A/B 주말, 홍은 주말야간, 난지물재생센터 주말)과 **저녁·야간 전용**(광나루 3·5·7번 저녁, 동부·남부 평일야간)은 10~11시 대상이 아니다. 제외하면 실질 후보는 약 60건.
