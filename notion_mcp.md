# Notion MCP로 페이지 댓글 가져오기

Notion 페이지의 **본문과 댓글(디스커션)** 을 AI가 읽을 수 있게 가져오는 방법을 정리한다.
접근 권한 상태에 따라 쓸 수 있는 경로가 달라지므로, 먼저 자기 상황을 판별하고 해당 경로로 간다.

---

## 0. 경로 판별

| 상황 | 본문 | 댓글 | 경로 |
| --- | --- | --- | --- |
| 커넥터가 설치된 워크스페이스의 페이지 | ✅ | ✅ 전량 | **경로 A** |
| 게스트로만 접근하는 타 워크스페이스 페이지 | ❌ | ❌ | 접근 불가 → 게시 후 B/C |
| 웹에 게시(Publish)된 페이지 | ✅ | ⚠️ 일부 | **경로 B** |
| 웹에 게시된 페이지 + 전량 필요 | ✅ | ✅ 사실상 전량 | **경로 C** |

핵심 제약 두 가지:

- **MCP 도구는 워크스페이스 단위로 인증된다.** 커넥터가 설치되지 않은 워크스페이스의 페이지는
  URL을 알아도 `object_not_found`(404)가 난다. 게스트 자격으로는 커넥터를 설치할 수 없다.
- **웹 게시(Publish)는 읽기 경로를 열지만 댓글 API 권한을 열지 않는다.** 게시하면 공개 사이트
  도메인으로는 읽히지만, 워크스페이스 API를 타는 댓글 조회 도구는 여전히 막힌다.

---

## 1. 경로 A — 커넥터가 연결된 페이지 (가장 쉬움)

준비: 해당 워크스페이스에 커넥터를 설치하고, 대상 페이지의 `···` → **Connections** 에 추가한다.
상위 페이지에 추가하면 하위 페이지로 상속되므로 보통 루트 한 번만 하면 된다.

```
1) fetch 로 본문 + 댓글이 어디 달렸는지 파악
   notion-fetch { id: <페이지 URL 또는 ID>, include_discussions: true }
   → 본문에 discussion:// 앵커가 삽입되고, <page-discussions> 에 총 개수가 나온다.
     단 댓글 본문은 샘플 몇 건만 인라인으로 붙는다.

2) get-comments 로 댓글 전량 수집
   notion-get-comments { page_id: <UUID>, include_all_blocks: true, include_resolved: true }
```

- `include_all_blocks: true` 가 없으면 **페이지 레벨 댓글만** 나온다. 문단에 달린 인라인 댓글을
  받으려면 반드시 켠다.
- `page_id` 는 **UUID만** 받는다. URL을 넣으면 검증 오류가 난다.
- 특정 스레드만 필요하면 `discussion_id` 에 `discussion://…` 를 넘긴다.

---

## 2. 경로 B — 게시된 페이지를 MCP로 읽기

대상 페이지를 Notion에서 **Share → Publish** 로 웹에 게시하면, 공개 사이트 URL이 생긴다.
이 URL은 인증 없이 열리므로 커넥터가 없는 워크스페이스의 페이지도 `fetch` 가 통과한다.

```
notion-fetch { id: <공개 사이트 URL>, include_discussions: true }
```

얻는 것과 못 얻는 것:

- ✅ 본문 전체
- ✅ 디스커션 **총 개수**, 그리고 본문 안에 **댓글이 달린 위치**(앵커)
- ⚠️ 댓글 **본문은 샘플 몇 건만**
- ❌ `get-comments` 는 여전히 불가 (워크스페이스 API를 타기 때문)

댓글 전량이 필요하면 경로 C로 간다.

> **주의**: 게시는 페이지를 인터넷에 공개하는 행위다. 작업이 끝나면 게시를 해제한다.
> 남의 워크스페이스 문서를 게시할 때는 사전 동의를 받는다.

---

## 3. 경로 C — 게시된 페이지에서 댓글 전량 수집

게시된 페이지는 인증 없이 내부 렌더링 API를 호출할 수 있고, 그 응답에 댓글 레코드가 함께 실려 온다.
이걸 이용한다.

### 3-1. 무엇이 열려 있고 무엇이 막혀 있나

| 엔드포인트 | 게시 페이지에서 | 용도 |
| --- | --- | --- |
| `POST /api/v3/loadPageChunk` | ✅ 200 | 블록 + 디스커션 + 댓글 레코드 |
| `POST /api/v3/syncRecordValues` | ❌ 403 (Cloudflare) | 인증 필요 |

즉 **`loadPageChunk` 하나로만 수집해야 한다.**

### 3-2. 호출 형태

```
POST https://<공개-사이트-도메인>/api/v3/loadPageChunk
Content-Type: application/json

{ "pageId": "<블록 UUID>", "limit": 300,
  "cursor": { "stack": [] }, "chunkNumber": 0, "verticalColumns": false }
```

응답 `recordMap` 에 `block` / `discussion` / `comment` / `notion_user` 가 들어온다.

### 3-3. 핵심 함정 — 토글 안쪽은 따라오지 않는다

루트 페이지 ID로 한 번 호출하면 **최상위 블록만** 온다. 토글로 접힌 본문은 자식 블록이 로드되지
않아, 그 안의 댓글도 누락된다. 각 블록의 `content` 배열에 있는 자식 ID 중 아직 안 받은 것을
**같은 엔드포인트에 `pageId` 로 다시 넣어** 재귀적으로 긁어야 한다.

```js
// 블록 트리 BFS 크롤링 (핵심만)
const queue = [ROOT_ID], fetched = new Set();
const blocks = {}, discussions = {}, comments = {}, users = {};

while (queue.length) {
  const id = queue.shift();
  if (fetched.has(id)) continue;
  fetched.add(id);

  const res = await fetch(`https://${SITE}/api/v3/loadPageChunk`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ pageId: id, limit: 300,
      cursor: { stack: [] }, chunkNumber: 0, verticalColumns: false }),
  });
  if (!res.ok) continue;                       // 실패는 건너뛴다
  const rm = (await res.json()).recordMap || {};

  for (const [table, store] of [["block", blocks], ["discussion", discussions],
                                ["comment", comments], ["notion_user", users]]) {
    for (const [rid, w] of Object.entries(rm[table] || {})) {
      const v = w.value && w.value.value;      // 이중 래핑에 주의
      if (v) store[rid] = v;
    }
  }
  for (const b of Object.values(blocks))
    for (const c of (b.content || []))
      if (!blocks[c] && !fetched.has(c)) queue.push(c);
}
```

- 레코드는 `recordMap[table][id].value.value` 로 **두 겹 래핑**돼 있다.
- 호출 수는 블록 수에 비례한다(수백 회). 상한과 실패 스킵을 넣어 무한 루프를 막는다.

### 3-4. 레코드 구조

```
discussion : { id, parent_id(블록), parent_table, context, resolved,
               comments?[], reactions?[] }
comment    : { id, parent_id(=discussion id), created_by_id, created_time, text }
notion_user: { id, name, email }
```

- **`comment.parent_id` 로 스레드를 묶는다.** `created_time` 오름차순 정렬로 대화 순서를 만든다.
- `text` / `properties.title` 은 리치텍스트 배열이다. `[["문자열", [주석들]], …]` 형태라
  `arr.map(s => s[0]).join("")` 로 평문을 얻는다.
- **`comments` 배열이 없고 `reactions` 만 있는 디스커션이 있다.** 이건 이모지 리액션만 달린
  스레드이고 댓글 본문이 애초에 존재하지 않는다. 데이터 누락으로 오해하지 않는다.
- `discussion.context` 에는 댓글이 달린 대상 텍스트가 들어 있고, `[["m", <discussion id>]]`
  주석이 붙은 조각이 **실제 하이라이트 구간**이다. 보통 문단 전체지만 일부만 잡힌 경우도 있다.

### 3-5. 댓글 위치를 로컬 문서에 매핑하기

Notion 본문과 로컬 원고가 같은 내용이라면, 앵커 텍스트로 라인 번호를 역산할 수 있다.

1. 양쪽 텍스트를 **정규화**한다 — 문자/숫자만 남기고 공백·마크업·기호를 제거한다.
   줄바꿈 위치와 강조 기호(`**`, 백틱)가 서로 달라도 매칭되게 만드는 것이 목적이다.
   로컬 파일에 HTML 엔티티(`&gt;` 등)가 있으면 먼저 디코드한다. 안 하면 `gt` 같은 잔여
   문자가 매칭을 깨뜨린다.
2. 정규화 문자열의 각 문자 위치 → 원본 라인 번호 배열을 만들어 둔다.
3. 앵커를 정규화해 `indexOf` 로 찾고, 그 구간을 라인 번호로 되돌린다.
4. 못 찾으면 앞에서부터 길이를 줄여가며(prefix) 재시도하고, 신뢰도를 기록한다.

**코드 블록 앵커는 2단계로 좁힌다.** 코드 블록의 `context` 는 하이라이트된 조각만 주므로
짧은 문자열이 엉뚱한 첫 등장에 붙는다. 먼저 블록 전체 텍스트(`properties.title`)로 파일 안의
코드 영역을 확정하고, **그 영역 안에서만** 앵커를 찾는다.

**같은 문구가 여러 번 나오면 라인이 확정되지 않는다.** 전체 등장 위치를 세어 2개 이상이면
후보를 모두 남기고 모호함을 명시한다. 임의로 하나 고르면 조용히 틀린 좌표가 박힌다.

---

## 4. 수집 결과를 문서화할 때

- **댓글 본문은 절대 편집하지 않는다.** 요약·교정·재구성 없이 원문 그대로 옮긴다.
  마크다운 인용부호(`>`)를 붙이는 것도 원문 변경이므로, **펜스 코드 블록**에 넣는다.
  본문에 백틱이 있을 수 있으니 펜스 길이는 내용의 최대 백틱 연속 길이보다 길게 잡는다.
- 옮긴 뒤 **원본과 바이트 단위로 비교해 검증한다.** 눈으로 보지 말고 스크립트로 확인한다.
- 위치는 라인 번호로 적고, **어느 파일의 어느 시점 기준인지** 함께 남긴다. 본문이 편집되면
  라인 번호는 무효가 되므로 재수집이 필요하다.
- 수집 개수와 원본이 보고한 개수가 다르면 **차이를 숨기지 말고 적는다.**
  (해결됨 처리된 스레드, 리액션 전용 스레드, 크롤링 범위 밖 블록 등이 원인이 된다.)

---

## 부록 — 이 저장소에서의 실제 적용 (예시)

> 아래는 위 규칙을 적용한 한 번의 실행 기록이며, 규칙 자체는 이 예시에 종속되지 않는다.

- 대상: Phase 1 Station 1-2 제출 페이지 (타 워크스페이스, 게스트 접근)
- 경로 A 불가 → 게시 후 **경로 C** 로 수집
- `loadPageChunk` 329회 호출, 블록 419개, 디스커션 47개, 댓글 40개
- 원본이 보고한 디스커션 50개 중 47개 확보 — 나머지 3개는 미확인
- 47개 중 40개가 댓글 스레드, 7개는 리액션 전용(댓글 본문 없음)
- 위치 매핑: 46/46 확인(페이지 레벨 1건 제외), 그중 1건은 동일 문구 중복으로 후보 2개 명시
- 산출물:
  - `task2/assignments/taskB-*.md` 하단 `## 리뷰 피드백 (Notion 원본)` 섹션
  - `task2/assignments/FEEDBACK-overall.md` — 페이지 레벨 총평
  - `task2/assignments/feedback.json` — 기계 판독용 전체 데이터
