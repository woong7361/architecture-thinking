"""Render each contract as a single self-contained HTML page.

The spec is inlined into the page, so the result opens from the file system
without a server. The two pages share one shell and differ only in the renderer
they hand the spec to. Run this again after editing a contract:

    python phase2/taskE/api/build_viewer.py
"""

import json
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent

FONTS = (
    "https://fonts.googleapis.com/css2?family=Gowun+Batang:wght@400;700"
    "&family=IBM+Plex+Sans+KR:wght@300;400;500;600&family=IBM+Plex+Mono:wght@400;500&display=swap"
)

STYLE = """
  :root {
    --paper: #FBFAF7;
    --paper-sunk: #F4F2EB;
    --ink: #1C2321;
    --ink-soft: #5C635B;
    --line: #E2DFD5;
    --court: #2F6B4F;
    --clay: #C1663F;
    --chrome-h: 116px;
    color-scheme: light;
  }

  html, body { height: 100%; }

  body {
    margin: 0;
    background: var(--paper);
    color: var(--ink);
    font-family: 'IBM Plex Sans KR', system-ui, -apple-system, 'Segoe UI', sans-serif;
    -webkit-font-smoothing: antialiased;
  }

  .masthead {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px 32px;
    padding-block: 18px 16px;
    padding-left: 20px;
    padding-right: 20px;
    background: var(--paper);
    border-bottom: 1px solid var(--line);
  }

  .identity { display: flex; flex-direction: column; gap: 6px; min-width: 0; }

  .eyebrow {
    font-family: 'IBM Plex Mono', ui-monospace, monospace;
    font-size: 11px;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: var(--clay);
  }

  h1 {
    margin: 0;
    font-family: 'Gowun Batang', 'IBM Plex Sans KR', serif;
    font-weight: 700;
    font-size: clamp(21px, 2.4vw, 27px);
    line-height: 1.2;
    text-wrap: balance;
  }

  .blurb {
    margin: 0;
    max-width: 62ch;
    font-size: 13px;
    line-height: 1.55;
    color: var(--ink-soft);
  }

  .surfaces {
    display: flex;
    gap: 6px;
    margin-top: 4px;
    font-size: 12px;
  }

  .surfaces a, .surfaces span {
    padding: 4px 10px 5px;
    border: 1px solid var(--line);
    border-radius: 3px;
    text-decoration: none;
    color: var(--ink-soft);
    background: var(--paper);
  }

  .surfaces a:hover { color: var(--court); border-color: var(--court); }

  .surfaces [aria-current='page'] {
    color: var(--paper);
    background: var(--court);
    border-color: var(--court);
  }

  .facts { display: flex; flex-wrap: wrap; gap: 10px; }

  .fact {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 7px 12px 8px;
    background: var(--paper-sunk);
    border: 1px solid var(--line);
    border-radius: 3px;
    min-width: 74px;
  }

  .fact dt {
    font-size: 10px;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: var(--ink-soft);
  }

  .fact dd {
    margin: 0;
    font-family: 'IBM Plex Mono', ui-monospace, monospace;
    font-variant-numeric: tabular-nums;
    font-size: 15px;
    font-weight: 500;
    color: var(--court);
  }

  #mount { position: relative; }

  /* Both renderers stick their sidebar to the viewport top; hold it below the
     masthead so it does not ride over the heading. */
  #mount .menu-content {
    top: var(--chrome-h) !important;
    height: calc(100vh - var(--chrome-h)) !important;
  }

  .booting {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 56px 20px;
    text-align: center;
    color: var(--ink-soft);
    font-size: 14px;
  }

  .booting code {
    font-family: 'IBM Plex Mono', ui-monospace, monospace;
    font-size: 12px;
    color: var(--ink);
  }

  @media (max-width: 720px) {
    :root { --chrome-h: 0px; }
    .masthead { padding-block: 16px 14px; }
    .facts { gap: 8px; }
    .fact { flex: 1 1 auto; min-width: 66px; }
  }

  @media (prefers-reduced-motion: reduce) {
    * { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
  }
"""

SHELL = """<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>__TITLE__</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="__FONTS__">
__HEAD__
<style>
__STYLE__
__EXTRA_STYLE__
</style>
</head>
<body>

<header class="masthead">
  <div class="identity">
    <span class="eyebrow">__EYEBROW__</span>
    <h1>__HEADING__</h1>
    <p class="blurb">__BLURB__</p>
    <nav class="surfaces">__SURFACES__</nav>
  </div>
  <dl class="facts" id="facts"></dl>
</header>

<div id="mount"></div>
<div class="booting" id="booting">
  <span>계약을 불러오는 중이다.</span>
  <span><code>__SOURCE__</code></span>
</div>

<script type="application/json" id="spec-source">__SPEC__</script>
__SCRIPTS__
<script>
  (function () {
    var source = document.getElementById('spec-source').textContent;
    var spec = JSON.parse(source);
    var mount = document.getElementById('mount');

    function facts(rows) {
      document.getElementById('facts').innerHTML = rows.map(function (fact) {
        return '<div class="fact"><dt>' + fact[0] + '</dt><dd>' + fact[1] + '</dd></div>';
      }).join('');
    }

    function chromeHeight() {
      var header = document.querySelector('.masthead');
      var height = header ? Math.round(header.getBoundingClientRect().height) : 0;
      document.documentElement.style.setProperty('--chrome-h', (window.innerWidth > 720 ? height : 0) + 'px');
    }
    chromeHeight();
    window.addEventListener('resize', chromeHeight);

    function done() {
      var booting = document.getElementById('booting');
      if (booting) booting.remove();
      chromeHeight();
    }

    function unavailable() {
      var booting = document.getElementById('booting');
      if (!booting) return;
      booting.innerHTML =
        '<span>렌더러를 불러오지 못했다. 이 페이지는 렌더러와 웹폰트를 처음 열 때 한 번 내려받는다.</span>' +
        '<span><code>인터넷 연결을 확인하고 새로고침한다.</code></span>';
    }

__RENDER__
  })();
</script>
</body>
</html>
"""

SURFACES = {
    "openapi": (
        "<span aria-current=\"page\">HTTP 표면</span>"
        "<a href=\"asyncapi.html\">알림 경로</a>"
    ),
    "asyncapi": (
        "<a href=\"openapi.html\">HTTP 표면</a>"
        "<span aria-current=\"page\">알림 경로</span>"
    ),
}

REDOC = """
    var operations = 0;
    var methods = ['get', 'post', 'put', 'patch', 'delete'];
    Object.keys(spec.paths || {}).forEach(function (path) {
      methods.forEach(function (method) {
        if (spec.paths[path][method]) operations += 1;
      });
    });

    var errorCodes = ((spec.components || {}).schemas || {}).ErrorCode;
    facts([
      ['버전', spec.info.version],
      ['엔드포인트', Object.keys(spec.paths || {}).length],
      ['오퍼레이션', operations],
      ['에러 코드', errorCodes && errorCodes.enum ? errorCodes.enum.length : 0],
      ['스키마', Object.keys((spec.components || {}).schemas || {}).length]
    ]);

    if (typeof Redoc === 'undefined') {
      unavailable();
      return;
    }

    Redoc.init(spec, {
      hideDownloadButton: true,
      expandResponses: '200,201',
      jsonSampleExpandLevel: 3,
      requiredPropsFirst: true,
      pathInMiddlePanel: true,
      menuToggle: true,
      sortPropsAlphabetically: false,
      nativeScrollbars: false,
      theme: {
        spacing: { unit: 5, sectionHorizontal: 32, sectionVertical: 28 },
        colors: {
          primary: { main: '#2F6B4F' },
          success: { main: '#2F6B4F' },
          warning: { main: '#C1663F' },
          error: { main: '#A63A2A' },
          text: { primary: '#1C2321', secondary: '#5C635B' },
          border: { dark: '#D9D6CC', light: '#E7E4DB' },
          http: {
            get: '#2F6B4F',
            post: '#2B5E8A',
            put: '#C1663F',
            patch: '#C1663F',
            delete: '#A63A2A'
          }
        },
        typography: {
          fontSize: '15px',
          lineHeight: '1.65',
          fontFamily: "'IBM Plex Sans KR', system-ui, -apple-system, sans-serif",
          headings: {
            fontFamily: "'Gowun Batang', 'IBM Plex Sans KR', serif",
            fontWeight: '700'
          },
          code: {
            fontSize: '13px',
            fontFamily: "'IBM Plex Mono', ui-monospace, monospace",
            color: '#1C2321',
            backgroundColor: '#F2F0E9',
            wrap: true
          },
          links: { color: '#2F6B4F', visited: '#2F6B4F', hover: '#3E8A66' }
        },
        sidebar: {
          width: '278px',
          backgroundColor: '#F4F2EB',
          textColor: '#1C2321',
          activeTextColor: '#2F6B4F'
        },
        rightPanel: {
          backgroundColor: '#20282A',
          textColor: '#F2F0E9',
          width: '38%'
        },
        codeBlock: { backgroundColor: '#161D1F' }
      }
    }, mount, done);
"""

ASYNCAPI = """
    facts([
      ['버전', spec.info.version],
      ['채널', Object.keys(spec.channels || {}).length],
      ['오퍼레이션', Object.keys(spec.operations || {}).length],
      ['메시지', Object.keys((spec.components || {}).messages || {}).length],
      ['전달 보증', (spec.info['x-delivery-guarantee'] || []).length],
      ['스키마', Object.keys((spec.components || {}).schemas || {}).length]
    ]);

    if (typeof AsyncApiStandalone === 'undefined') {
      unavailable();
      return;
    }

    // The renderer parses the document asynchronously and says nothing when it
    // finishes, so the notice goes away once something has actually landed.
    var landed = new MutationObserver(function () {
      if (mount.firstElementChild) {
        done();
        landed.disconnect();
      }
    });
    landed.observe(mount, { childList: true, subtree: true });
    window.setTimeout(function () {
      if (!mount.firstElementChild) unavailable();
    }, 20000);

    AsyncApiStandalone.render({
      schema: source,
      config: {
        show: {
          sidebar: true,
          info: true,
          servers: false,
          operations: true,
          messages: true,
          schemas: true,
          errors: true
        },
        sidebar: { showOperations: 'byDefault' }
      }
    }, mount);
"""

ASYNCAPI_STYLE = """
  /* The renderer ships its own type scale; only the family is pulled back. */
  #mount .aui-root { font-family: inherit; }

  /* It keeps the sidebar behind a burger button at every width. A wide screen
     has room for it, and it holds below the masthead like the other page's. */
  @media (min-width: 768px) {
    #mount .sidebar {
      display: block !important;
      position: sticky;
      align-self: flex-start;
      top: var(--chrome-h);
      height: calc(100vh - var(--chrome-h));
      max-height: none;
      overflow-y: auto;
      background: var(--paper-sunk);
    }
    #mount .burger-menu { display: none !important; }
  }
"""

PAGES = [
    {
        "output": "openapi.html",
        "source": "tennis-alert-api.yaml",
        "title": "테니스 코트 빈자리 알림 API",
        "eyebrow": "OpenAPI 3.1 · 계약",
        "heading": "테니스 코트 빈자리 알림 API",
        "blurb": (
            "코트·날짜·시간대로 알림을 신청하고, 서버가 예약처를 대신 확인해 빈자리를 알린다.\n"
            "      예약과 취소는 외부 예약 사이트에서 이뤄지므로 이 계약에 없다."
        ),
        "surfaces": SURFACES["openapi"],
        "head": "",
        "scripts": '<script src="https://cdn.jsdelivr.net/npm/redoc@2.1.5/bundles/redoc.standalone.js"></script>',
        "extra_style": "",
        "render": REDOC,
    },
    {
        "output": "asyncapi.html",
        "source": "notification-asyncapi.yaml",
        "title": "빈자리 알림 메시지 계약",
        "eyebrow": "AsyncAPI 3.1 · 계약",
        "heading": "빈자리 알림 메시지 계약",
        "blurb": (
            "빈자리를 확인한 뒤 알림이 사용자 단말까지 가는 경로다. 요청과 응답이 아니라 채널을 지나는 메시지라\n"
            "      보내는 쪽과 받는 쪽, 오가는 방향, 전달의 보증까지 이 계약이 정한다."
        ),
        "surfaces": SURFACES["asyncapi"],
        "head": (
            '<link rel="stylesheet" '
            'href="https://cdn.jsdelivr.net/npm/@asyncapi/react-component@3.2.1/styles/default.min.css">'
        ),
        "scripts": (
            '<script src="https://cdn.jsdelivr.net/npm/@asyncapi/react-component@3.2.1'
            '/browser/standalone/index.js"></script>'
        ),
        "extra_style": ASYNCAPI_STYLE,
        "render": ASYNCAPI,
    },
]


def build(page):
    spec = yaml.safe_load((HERE / page["source"]).read_text(encoding="utf-8"))
    payload = json.dumps(spec, ensure_ascii=False, separators=(",", ":"))
    # A literal </script> inside the data block would end the tag early.
    payload = payload.replace("</", "<\\/")

    html = SHELL
    for key, value in (
        ("__FONTS__", FONTS),
        ("__STYLE__", STYLE),
        ("__EXTRA_STYLE__", page["extra_style"]),
        ("__TITLE__", page["title"]),
        ("__EYEBROW__", page["eyebrow"]),
        ("__HEADING__", page["heading"]),
        ("__BLURB__", page["blurb"]),
        ("__SURFACES__", page["surfaces"]),
        ("__SOURCE__", page["source"]),
        ("__HEAD__", page["head"]),
        ("__SCRIPTS__", page["scripts"]),
        ("__RENDER__", page["render"]),
        ("__SPEC__", payload),
    ):
        html = html.replace(key, value)

    output = HERE / page["output"]
    output.write_text(html, encoding="utf-8")
    print(f"{output.name}: {len(output.read_bytes()):,} bytes from {page['source']}")


def main():
    for page in PAGES:
        build(page)


if __name__ == "__main__":
    main()
