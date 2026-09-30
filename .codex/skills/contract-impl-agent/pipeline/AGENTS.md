# AGENTS.md

`contract-impl-agent/pipeline` 안에서 작업할 때 따르는 규칙이다. 설계 정본은
[../docs/v0-design.md](../docs/v0-design.md)이고 조각 경계와 게이트 시그니처는
[../BUILD.md](../BUILD.md)에 있다. 이 문서와 설계가 어긋나면 설계가 맞다.

이 파이프라인의 1차 산출물은 사람이 받아 검토할 **구현 초안과 그 초안이 서 있는 계약 판본**이고,
2차 산출물은 그 사이에 내린 **판단의 기록**이다. 통과 여부만 남기면 판단을 기록한 뜻이 없다.

## 기본 원칙

- 구현 전 `../docs/v0-design.md`, `../BUILD.md`, `schemas/*.schema.json`을 먼저 확인한다.
- 단계별 역할을 섞지 않는다. 발의와 반영과 판정은 서로 다른 단계의 일이다.
- LLM stage 사이의 핸드오프는 파일과 JSON payload로만 한다. 대화 히스토리를 공유하지 않는다.
  과제가 말한 "새 세션"은 프롬프트 문구가 아니라 이 구조가 보장한다.
- runner가 경로, 파일명, payload 구성을 제어한다. stage 코드가 run 폴더 전체를 훑지 않는다.
- 코드 변경 후 `python -B validate.py ...` 또는 `python -B run_stage.py --stage ...`로 확인한다.

## 역할 경계

| 단계 | 하는 일 | 못 하는 일 |
| --- | --- | --- |
| Gen · Refine | 코드를 쓰고, 계약이 비운 자리에서 고른 것을 신고하고, 비평이 발의한 계약 변경을 판본에 반영한다 | 계약 개정을 스스로 발의하는 것, `self_score`·`verdict`·`rubric_scores` |
| Critique | 다섯 축에 고칠 방향을 붙이고 계약 변경을 발의한다 | 점수, 판정, 재작성, 계약 변경의 반영 |
| Eval | 초안의 계약 판본을 분모로 루브릭을 매긴다 | 판정, 개선 지시, 발의, 반영 |
| Validate | 스키마·역할경계·매니페스트 경로·점수 하한처럼 기계로 판정되는 것만 본다 | 품질의 주관 판단, 창작, 비평 |
| runner | 흐름 제어, payload 구성, 게이트 호출, 합산, 계보 계산 | 채점, 비평 |

구현자가 개정을 발의하면 구현하기 어려운 조항을 스스로 지우는 길이 열린다. 그래서 발의는
Critique에서만 나오고, 모든 계약 변경에는 리뷰어의 지적이 근거로 붙는다. 근거 없는 변경은 G1이 잡는다.

## 정보 차단

`validate.py`의 금지필드 집합과 각 stage의 payload 구성이 이 표를 이중으로 막는다.

| 단계 | 봐도 되는 것 | 보면 안 되는 것 |
| --- | --- | --- |
| Gen | input, 기준선 `prose_slots` | 사람 feature와 step, 게이트 결과, critique, eval |
| Validate | draft의 파일들(계약 판본 포함), 기준선, 규칙 카드, 계약 변경 기록, 결정이 가리키는 위치 | 결정과 변경의 근거 문장, `claims`, `repairs`, critique, eval |
| Critique | input, `prose_slots`, 현재 draft, 서 있는 결정, 계약 변경 기록, 게이트가 남긴 **기록**, 축 이름과 뜻 | 루브릭의 스케일·가중·하한, 게이트의 **위반** 목록, eval, 사람 feature와 step |
| Eval | input, `prose_slots`, 현재 draft, 루브릭, 서 있는 결정, 계약 변경 기록 | critique, 게이트 결과, 사람 feature와 step |
| Refine | input, 이전 draft, critique, refine_request | eval 총점 원문, 루브릭, 사람 feature와 step |

네 단계 모두 작업 디렉터리를 `output/`으로 받고 그 밖은 읽지 못한다. 사람이 쓴 feature와 step, 게이트 결과,
다른 run의 파일이 모두 그 밖에 있으므로 이 표는 프롬프트의 약속이 아니라 샌드박스 경계가 지킨다.

Critique에 위반 목록을 주지 않는 이유는 Eval에 critique를 주지 않는 이유와 같다. 주면 이미 잡힌 것을
다시 말하는 데 지면을 쓰고 못 보는 것을 찾지 않는다. Critique가 받는 축은 이름과 뜻까지다 —
`runner.rubric_axes()`가 루브릭에서 그 둘만 뽑는다. 스케일을 주면 지적이 "3점을 4점으로 올리는 방법"이 된다.

사람이 쓴 계약 테스트(feature와 step)는 어느 단계에도 주지 않는다. 주면 구현이 계약 대신 테스트에
맞춰지고, 구현이 계약에서만 파생됐다는 근거가 사라진다.

## run 폴더와 작업 폴더

```
runs/<run_id>/
  origin/<contract>            사람이 확정한 원본. 사슬 전체의 감사 기준. 읽기전용
  baseline/<contract>          이 run의 기준선. 읽기전용으로 잠근다
  <brief_hash>_input.json
  output/                      작업 폴더. iteration마다 제자리에서 고친다
    .git/                      iteration마다 커밋 하나. 스냅샷과 diff를 맡는다
    pom.xml, src/...           스켈레톤 사본과 생성 코드
    contract/<name>-v1.yaml    기준선 사본. 계약을 고치면 판 번호가 오른 사본이 하나 더 생긴다
    target/                    Maven 캐시가 남아 다음 iteration이 증분 빌드된다(git에서는 무시)
  iter_001/draft.json          색인과 판단 기록, 그 iteration의 커밋 해시
  iter_001/gate.json, critique.json, eval.json, refine-request.json
  iter_001/api-docs.json       G0가 낸 추출 스펙(work_dir가 iter 폴더다)
  <hash>_final.json | <hash>_failed.json, run_ledger.json, REPORT.md
```

**작업 폴더는 하나다.** iteration마다 새로 만들지 않고 제자리에서 고친다. 복사하면 같은 파일이 판본 수만큼
쌓이고, 고칠 것만 고치는 refine의 성질도 사라진다. 승격 폴더(`artifact/`)는 없다 — PASS한 iteration의
커밋에 `pass-iter-00N` 태그를 달고 그것이 run의 산출이다.

**스냅샷과 diff는 git이 맡는다.** `runner`가 run 시작에 스켈레톤을 복사하고 기준선 계약 사본을 놓고 첫 커밋을
남긴다. 그 커밋이 iteration 001의 대조 기준이다. 단계가 끝나면 runner가 커밋 하나를 남기고 해시를
`draft.json`의 `commit`에 적는다.

**커밋은 runner가 남긴다.** 생성기는 git을 만지지 않는다. 만지면 원장의 iteration 경계가 흐려진다.
그래도 생성기가 커밋을 남긴 경우는 막지 않고 `detect_generator_commits`가 찾아 로그와 원장 payload에
`generator_commits`로 남긴다. 커밋 자체는 판정 대상이 아니라서 그 iteration의 산출을 버리지 않는다.

작업 저장소는 프로젝트 저장소와 무관한 별도의 것이고 run 폴더는 프로젝트 이력에 넣지 않는다.
`init_output`이 그 저장소에만 `core.autocrlf=false`·`core.excludesFile=`·`core.longpaths=true`를 건다.
`autocrlf`가 켜져 있으면 커밋이 줄끝을 바꿔 해시로 못 박은 기준선의 바이트가 달라지고, 전역 `excludesFile`이
파일을 숨기면 `git status`에 안 나타나 신고하지 않은 파일을 놓친다. 판정이 기계의 git 설정에 흔들리지 않게 한다.

기준선은 run 안에서 움직이지 않는다. 기준선이 흔들리면 iteration 001의 위반과 003의 위반이 서로 다른
잣대로 센 것이 되어 재발도 회귀도 셀 수 없다. 계약 판본은 iteration마다 움직이고 기준선은 run 사이에서 움직인다.
`--origin`을 주지 않으면 첫 run으로 보고 원본과 기준선을 같게 둔다.

계약 원문은 줄끝을 `\n`으로 정규화해 싣고 해시한다(`runner.sha256_text`, `intake_to_input.read_text`).

## 산출은 색인이고 실물은 작업 폴더에 있다

생성기는 파일 내용을 응답에 담지 않는다. `output/`에 실제 자바 프로젝트를 쓰고 응답으로는 색인만 낸다.
오퍼레이션 다섯 개의 구현을 JSON 문자열로 한 응답에 담으면 응답 한 개의 한도에 걸려 아무것도 나오지 않는다.
실제로 40분을 돌려 산출을 받지 못했다. **텍스트 뷰(`content`)를 되살리지 마라.** 단계가 그 자리에서 읽는다.

색인에는 **이번 iteration에 쓰거나 고친 파일만** 적는다. 건드리지 않은 파일은 적지 않아도 작업 폴더에 그대로
남아 이어진다. 예외는 계약 판본이다 — 고치지 않은 iteration에도 적는다. `contract_version.path`가 판정에 쓰이는
포인터이므로 그것이 색인 밖을 가리키면 어느 파일이 이 초안의 계약인지 말하는 자리가 빈다.

**판정과 집계는 지점 단위로, refine에 주는 신호는 원인 단위로 본다.** 계약이 같은 컴포넌트를 여러 응답에서
참조하면 그 하나의 결함이 지점마다 한 번씩 나온다. 게이트가 지점 단위로 세는 것은 맞지만 그것을 그대로
refine에 넘기면 신호가 잡음이 된다. `group_by_cause`가 `(rule, where, detail)`로 묶어 `refine_request`의
`open_causes`에 싣고 묶음마다 닿는 지점의 목록과 개수를 함께 준다. 원장에는 지점 단위 그대로 간다 —
어느 지점이 아직 열려 있는지는 지점 단위로만 말할 수 있다.

**원인에는 안정된 `id`가 붙는다**(`cause_id`, `(rule, where, detail)`의 sha256 앞 8자). 같은 원인이 다음
iteration에도 같은 id를 받아야 원장이 "이 원인이 두 iteration 연속 열려 있다"를 말할 수 있으므로 iteration마다
다시 세는 순번을 쓰지 않는다. 지점 단위 항목에도 `stamp_cause_ids`가 같은 값을 `cause_id`로 붙여, 원장과
refine이 한 원인을 같은 이름으로 부른다.

**한 원인을 두 이름으로 부르지 않는다.** `priority`도 `gate:<rule>@<where>` 대신 그 `id`를 쓴다. 모델은
`repairs[].violation_id`에 그 값을 **옮기기만** 한다 — 조립하는 자리는 틀리는 자리다.

판정 항목에는 `label`(닫힌 다섯 값, 뜻은 규칙 카드의 `labels`)과, 접혔을 때만 붙는 `evidence`가 있다.
둘 다 판정을 가르지 않으므로 배관은 그대로 실어 보내고 원인 묶음에 `label`과 접힌 잎의 수를 옮긴다.

**선언과 실물의 대조는 `git status`로 한다.** 매니페스트를 들여다보는 것보다 세다 — 생성기가 신고하지 않고
고친 것까지 잡는다. 세 규칙의 문장은 `rules/conformance_rules.yaml`의 카드에 있고 게이트는 이 셋을 내지 않는다.

| 규칙 | 언제 |
| --- | --- |
| `manifest.declared_missing` | 색인에 적힌 경로가 작업 폴더에 없다 |
| `manifest.undeclared_file` | 바뀐 파일이 색인에 없다. 색인에 있지만 바뀌지 않은 것은 위반이 아니다 |
| `manifest.declared_missing` | 색인이나 `contract_version`이 가리키는 파일이 작업 폴더에 없다 |
| `manifest.forbidden_path` | 스켈레톤이 소유한 경로(`immutable_paths`)가 diff에 나타났다 |
| `contract.previous_version_modified` | 앞 계약 판본이 고쳐졌다 |

빌드 수리 산출은 `repair_output`으로 검증한다. 색인 규약은 다른 산출과 같다 — 고친 파일만 적으면 되고 계약
항목을 요구하지 않는다. 계약 항목은 그 iteration의 초안에 합칠 때 기존 색인에서 이어받는다.

**계약 판본은 기준선 사본까지 전부 읽기전용으로 잠근다**(`lock_contract_versions`, `init_output`에서 시작).
`init_output`이 기준선 사본을 놓으므로 생성기가 그것을 다시 쓸 이유가 없다. 4만 7천 자를 옮겨 적으라고 하면
모델은 요약한다 — 여덟 run 중 일곱에서 gen이 계약 사본을 쓰면서 `description`·`summary`·`x-requirement`를
지웠고 스키마만 남았다. 지시로 막을 성질이 아니라 권한으로 막는다. 판 번호를 올리는 것은 아직 없는 파일을
만드는 일이라 걸리지 않고, 같은 이름으로 내용을 바꾸는 길만 닫힌다. 잠금이 뚫린 흔적은
`check_previous_versions`가 `contract.previous_version_modified`로, `git status`에 나타난 변경은
`manifest.undeclared_file`이나 `manifest.forbidden_path`가 낸다.

**정확한 판본 경로를 payload가 준다.** `WORKSPACE_JSON`의 `contract_versions`에 작업 폴더에 실재하는 판본의
상대경로를 싣는다(`payload.contract_version_paths`). 폴더 경로만 주면 모델이 이름을 짜맞춘다 —
`<이름>-v<n>.yaml`의 `<이름>`을 확장자까지 포함한 파일명으로 읽어 `tennis-alert-api.yaml-v1.yaml`을 선언한
run이 19분을 쓰고 그 한 줄로 떨어졌다. runner는 그 이름을 확정적으로 알고 있으므로 추측할 것을 남기지 않는다.

**그래도 틀린 이름이 오면 고치고 기록한다.** 선언한 경로의 파일이 없을 때 그 이름에서 `-v(\d+)`를 뽑아 같은
판 번호를 가진 실재 파일이 **정확히 하나**면 그 경로로 바꾼다(`repair_version_paths`). 포인터의 철자는 이 하네스가
재려는 것이 아니다 — 게이트가 재는 것은 구현이 계약을 지켰는지이고, 비용을 파일명 한 줄에 태우는 것은 그 대상이
아니다. 위반이 아니라 정규화이므로 판정에 들어가지 않고 `normalizations`로 원장에 간다. 판 번호를 못 뽑거나
후보가 둘 이상이면 어느 것을 뜻했는지 기계가 말할 수 없으므로 `manifest.declared_missing`으로 REJECT다.

**색인에 계약 항목을 요구하지 않는다.** 기준선 사본은 생성기가 쓴 파일이 아니다. `contract_version` 포인터는
그대로 받고 그 포인터가 가리키는 파일이 작업 폴더에 있는지만 본다(`manifest.declared_missing`). 판본을 올린
iteration은 그 파일이 diff에 나타나므로 적지 않으면 `manifest.undeclared_file`이 잡는다. 계약 역할로 적은 것이
있으면 그것은 이번 판본이어야 한다(`manifest.contract_version_role`).

**나쁜 초안은 루프가 다룬다.** gen·refine 산출이나 초안이 색인 규약을 어기면 그것을 그 iteration의
위반으로 합류시켜(`violations_from_validation`) 다음 iteration으로 보낸다. run을 끝내지 않는다 —
규약 위반으로 죽이면 그 iteration에 남은 예산까지 버린다. `max_iterations`까지 남으면 `FAILED`다.
`ERROR`는 초안의 잘못이 아니라 단계 실행 자체가 진행하지 못한 상태에만 쓴다. 스키마가 깨져 초안을 읽을 수
없는 경우만 예외로 `ERROR`다.

refine 산출이 규약을 어기면 작업 폴더 상태는 그대로 커밋하고 색인은 `git diff`에서 다시 세운다
(`build_recovery_draft`, `metadata.index_source = git-diff-recovery`). 다음 refine이 무엇을 되돌려야 하는지
알아야 하고, 실제로 무엇이 움직였는지는 diff가 말한다. 어긋났다는 사실은 위반으로 이미 남는다.

## 단계마다 권한이 다르다

네 단계 모두 작업 디렉터리를 `output/`으로 받는다. 코드를 payload에 싣지 않고 그 자리에서 읽게 한다.
다시 payload에 넣으면 gen에서 막혔던 것과 같은 문제가 입력 쪽에서 되살아난다.

| 단계 | 샌드박스 | 무엇을 할 수 있나 |
| --- | --- | --- |
| Gen · Refine | `workspace-write` | `output/` 아래 읽기와 쓰기. 그 밖에는 쓰지 못한다 |
| Critique · Eval | `read-only` | `output/` 아래 읽기만. 쓰기 없음 |

**읽기 권한을 작업 폴더로 가두는 것이 정보 차단의 실행 장치다.** 사람이 쓴 feature와 step, 게이트 결과,
다른 run의 파일이 모두 그 밖에 있으므로 프롬프트의 약속이 아니라 경계로 막힌다.

**그래도 코드는 runner가 payload에 담아 준다**(`collect_code_files` → `DRAFT_FILES_JSON`). 읽기를 모델의
도구 성공에 맡기지 않는다. 채점자가 코드를 열지 못하면 "대지 못하는 슬롯은 미충족" 규칙 때문에 도구 실패가
최저점이 된다 — 실제로 네 적합성 축이 모두 1점이 된 run이 있었다. 대상은 색인이 선언한 파일과 첫 커밋 이후
쌓인 생성 코드 전부다. 상한을 넘겨 빠진 파일은 `omitted`와 로그에 적는다. 빠진 것이 조용히 빠지면 채점자가
그것을 미충족으로 센다. gen의 산출을 응답에 담지 않는 결정은 그대로다 — 그건 출력의 한도 문제이고 이건 입력이다.
읽기 전용 샌드박스는 그대로 두어 채점자가 더 보고 싶은 것을 볼 수 있게 한다.

승인 우회 플래그(`--dangerously-bypass-approvals-and-sandbox`)는 쓰지 않는다. 그 플래그는 모델이 실행하는
명령을 샌드박스 밖에서 돌리므로 저장소 전체에 쓸 수 있고, 판정 대상이 아닌 것이 움직여도 게이트가 보지 못한다.
실제로 이 조합이 열어 둔 문으로 판정 장치 파일 다섯 개가 지워진 일이 있었다.

`claude` provider는 OS 샌드박스가 아니라 권한 체계로 막으므로 codex보다 약하다. 막는 것은 세 겹이다.
작업 디렉터리를 작업 폴더로 두고, `--permission-prompts none`으로 물어봐야 하는 일을 자동 거절하며,
**무엇을 못 하게 하는가는 권한 모드가 아니라 `--disallowed-tools`가 정한다.** 읽기 단계는 쓰기 도구와 셸을
빼고, 쓰기 단계는 셸만 뺀다. 권한 모드는 두 단계 모두 `acceptEdits`다 — `manual`로 두면 읽기까지 거절될 수
있고 채점자가 더 보고 싶은 것을 볼 수 없다. 우회 플래그(`--dangerously-skip-permissions`)는 쓰지 않는다.

셸을 두 단계에서 다 빼는 근거는 모델이 셸을 쓸 일이 없다는 것이다. 빌드는 하네스가 돌리고, 파일은 Read로
읽고, 계약 판본의 경로는 `WORKSPACE_JSON`이 준다. 열어 두면 편집 승인과 무관하게 작업 폴더 밖을 건드릴 길이 생긴다.

**모델은 `--model`로 네 단계를 한꺼번에 고르고 단계별 인자가 그 위를 덮는다.** 기본값은 이름으로 못 박는다
(`MODEL_CLAUDE_DEFAULT`). `None`으로 두면 CLI의 그날 기본값으로 돌고 `config.agent_models`에도 `None`이 남아
나중에 이 run이 어떤 모델로 돌았는지 기록만 보고 말할 수 없다. 더 센 모델은 인자로 준다.

**claude 경로에는 `--output-schema`가 없다.** 그래서 `_parse_json`이 응답에서 균형 잡힌 JSON 객체를 뒤에서부터
찾아 건진다(문자열 안의 중괄호와 이스케이프를 세지 않는다). 순수 JSON이 오는 것이 보통이어도 앞뒤에 한 줄이
붙는 것만으로 한 iteration을 버리게 된다.

## 응답 원문과 전송 실패

**모델 응답 원문을 파싱 전에 디스크에 남긴다**(`textio.save_raw` → `iter_00N/<단계>.attempt-N.raw.txt`).
성공해도 남긴다 — 실패했을 때만 남기면 실패 경로에 버그가 있을 때 아무것도 안 남는다. 원문이 없으면 응답이
잘려서 온 것인지 우리 파서가 틀린 것인지 판별할 수 없다. 44분을 쓴 run이 앞 300자만 남기고 끝난 일이 있었다.
codex 경로도 같다 — CLI가 쓴 답 파일을 그대로 남긴다.

**응답 형식이 깨진 것은 채점 결과가 아니라 전송 실패다**(`TransportFailure`). 그래서 위반으로 세지 않고
`--max-format-attempts`(기본 2)만큼 다시 부른다(`call_stage`). `ValueError`만 재시도하고 그 밖의 예외는
그대로 올린다 — CLI가 죽은 것이나 우리 코드의 버그를 재시도로 덮으면 안 된다.

| 단계 | 재시도 뒤에도 실패하면 |
| --- | --- |
| Eval | 다섯 축을 측정 불가로 적고(`unmeasured_eval`) 게이트 판정만으로 나아간다. 판정하지 못한 iteration은 PASS가 될 수 없다 |
| Critique | 비평 없이 나아간다. 판정자가 아니므로 없어도 루프가 돈다. refine은 게이트 위반만 받는다 |
| Gen · Refine | 초안이 없으면 루프가 이어지지 않으므로 `TransportFailure`로 끝낸다. 원문 경로를 메시지에 싣는다 |

실패한 자리는 `iter_00N/<단계>.transport-failure.json`과 원장 payload의 `transport_failures`에 남는다.

진단 메시지는 **네 가지를 가른다**(`response_reason`). `ok`는 건져지는 것, `truncated`는 끝까지 오지 않은 것,
`absent`는 JSON이 아예 없는 것, `syntax`는 균형은 맞고 끝까지 오는데 문법이 깨진 것이다. 하나로 부르면 다음에
같은 것을 보고도 무엇인지 모른다 — 끝쉼표 하나를 잘린 것으로 읽고 파서를 더 만질 뻔했다.
`describe_response`가 길이와 중괄호 여닫이 수와 앞뒤 500자를 함께 싣는다.

**뜻을 바꾸지 않는 문법 흠은 고쳐서 통과시킨다.** 닫는 괄호 앞의 쉼표가 그것이다(`strip_trailing_commas`).
**원문은 손대지 않고** 사본으로 파싱하며, 무엇을 고쳤는지 `on_note`로 진행 로그에 남긴다 — 자동으로 고친 것은
기록에 남아야 한다. 9분과 한 번의 호출을 끝쉼표 하나에 태우는 것은 이 하네스가 재려는 것과 무관하다.

## 저장된 원문을 다시 흘린다

`run_stage.py --from-raw <경로>`가 저장된 응답 원문을 그 단계의 응답으로 쓴다. 파싱·정규화·검증·산출 만들기까지
**LLM을 부르지 않고** 지나간다. 네 단계 모두에 걸린다.

진단 도구가 저장한 것을 스스로 읽을 수 없으면 저장의 값이 반쯤 버려진다. 모델이 실제로 낸 응답이 손에 있는데
다시 부르는 것은 돈만 드는 것이 아니라 정직하지도 않다 — 두 번째 호출은 다른 응답이고, 첫 번째가 문턱을
넘었는지 확인하는 일과 다른 일이 된다. 재생한 산출의 `model`은 **`raw-replay`** 로 남아, 원장을 읽는 사람이
이것이 새 호출에서 나온 것이 아님을 안다.

```powershell
python -B ./run_stage.py --stage eval --input <input.json> --run-dir <run> --iteration 001 `
  --from-raw <run>/iter_001/eval.raw.txt
```

## 원장과 리포트만 다시 만든다

`run_stage.py --stage record`가 run 폴더에 남은 iteration 산출물을 읽어 `run_ledger.json`과 `REPORT.md`를
만든다. **LLM을 부르지 않는다.** 단계별로 되살린 run에는 그 둘이 없는데, 그 리포트가 이 하네스의 제출물이므로
없으면 여기까지 온 것이 파일 몇 개로만 남는다.

```powershell
python -B ./run_stage.py --stage record --input <input.json> --run-dir <run> --iteration 001
```

- 있는 iteration을 번호 순으로 전부 넣는다. `draft.json`이 없는 iteration은 **넣지 않는다** —
  돌리지 않은 것과 실패한 것은 다르고, 돌리지 않은 것을 실패로 적으면 리포트가 거짓말을 한다.
- **빠진 산출물을 빈 값으로 채우지 않는다.** `eval.json`이 없으면 `eval`을 `None`으로 넘겨 원장이
  "Eval이 돌지 않아 축을 쟀는지 말할 수 없다"로 적게 한다. 그 구분이 합격선의 앞 문장이다.
- **원장을 처음부터 만든다.** 원장을 갱신하는 쪽은 기존 원장을 입력으로 읽어 이어 쓰므로, 남겨 두면 다시
  만드는 것이 아니라 두 번 세는 것이 된다. `--overwrite`면 기존 원장을 버리고 시작하고, 없으면 덮어쓰지 않고
  그 사실을 말하며 멈춘다. 조용히 이어 쓰면 항목 수가 늘어난 리포트가 나오고 읽는 사람은 그것을 알 수 없다.
  같은 run에 두 번 돌려 같은 리포트가 나오는 것이 이 단계의 회귀 확인이다.
- 되살린 run이라는 사실은 `record.json`에 남긴다(`continuous_run: false`, iteration별 `stage_models`).
  원장의 형식은 다른 조각이 소유하므로 모르는 키를 싣지 않고 이 자리에 적는다. `stage_models`에 `raw-replay`가
  있으면 그 산출은 저장된 원문을 다시 흘린 것이다.
- 게이트를 다시 돌릴 수 있는 run은 리포트도 다시 낼 수 있다. 옛 run은 `--stage gate`로 판정을 새 규칙으로
  갈아 낸 뒤 `--stage record`로 리포트를 다시 내면 된다.

## 게이트 호출

`gates/`가 구현하고 runner와 run_stage가 부른다. 세 함수에 **같은 규칙 카드**를 넘긴다 —
카드를 한 번만 읽어 넘기므로 어떤 초과를 위반으로 보고 어떤 초과를 기록으로 볼지가 run 안에서 갈리지 않는다.

```python
rules = gates.load_rules(<path>)                      # 부르는 쪽이 한 번 읽는다
gates.run_g0(project_dir, work_dir, java_home=None)   # project_dir=output/, work_dir=iter_00N/
gates.run_g1(derived_spec, contract, scope, decisions, rules, project_dir=None)
gates.check_contract_changes(prev_contract, curr_contract, changes, prev_path, curr_path,
                            rules=None, critique=None,      # critique=001..이번 직전까지의 비평 전부
                            baseline=None, accumulated=None,
                            spec=None)                      # spec=brief.requirement_spec
```

BUILD.md에서 세 군데가 늘었고, 그 셋이 다 판정의 구멍을 막는 자리다.

- **`run_g1`의 `project_dir`에 작업 폴더를 넘긴다.** 직렬화 재정의는 추출 스펙에 드러나지 않는다.
  스펙은 타입 선언만 보여주므로 계약이 널을 허용하면서 필수로 둔 필드의 키가 실제로 실리는지는 말하지 않는다.
  넘기지 않으면 `response.serialization_override` 검사가 조용히 빠진다.
- **`run_g0`는 `project_dir`에서 제자리에 빌드한다.** 스켈레톤 경로를 따로 받지 않는다 — `output/`이
  스켈레톤 사본이므로 추출기도 그 안에 있고, 경로를 둘 받으면 어느 추출기를 쓰는지가 두 곳이 된다.
  추출 스펙은 `work_dir`(`iter_00N/`)에 떨어진다. `output/target/`에 두면 `mvn clean`이 지우거나 다음
  iteration이 덮어서 원장이 iteration마다 되짚을 수 없다. 제자리 빌드라 두 번째 iteration부터 증분 빌드된다.
- **`run_g0`의 `java_home`에 `--java-home`을 그대로 넘긴다.** 스켈레톤은 JDK 17 이상을 요구한다.
  미지정이면 `CONTRACT_IMPL_JAVA_HOME` → `JAVA_HOME` 순으로 환경에 맡기고, 17 미만이면 G0가
  `g0.jdk_unusable`과 `status: ERROR`를 낸다.
- **`check_contract_changes`에 카드를 넘기고 `observations`도 받는다.** 판본 diff의 셈은 판정이 아니라
  기록이므로 REJECT로 이어지지 않고 리포트로 간다.
- **`critique`에는 이 판본이 만들어지기 전까지 이 run이 낸 비평 전부를 목록으로 넘긴다**(`load_source_critique`).
  계약 변경은 iteration을 넘어 살아남는다. `iter_002`가 한 변경을 003이 들고 가면 그 근거가 된 비평은 001이나
  002의 것이므로, 앞 iteration 하나만 넘기면 그 기록이 전부 `change.unjustified`로 뒤집힌다 — 지난 비평을
  되살릴 길이 없으니 모델이 만족시킬 수 없는 요구가 된다. 누적으로 넘기는 이유가 `accumulated`와 같다.
  **이번 iteration의 비평은 넣지 않는다.** 그것은 이 판본이 만들어진 뒤에 나온 것이라, 넣으면 결정이 내려질 때
  없던 정보로 그 결정을 정당화하게 되고 옳게 인용한 변경까지 뒤집힌다. iteration 001과 비평 파일이 하나도 없는
  run은 `None`이다 — 게이트는 "대조할 비평이 없다"와 "비평은 있는데 그 id가 없다"를 가르므로 빈 목록이 아니라
  `None`으로 넘겨야 앞의 것으로 읽힌다. 인용할 비평이 없으면 `basis`가 채워져 있어도 `change.unjustified`다.
  넘긴 자리는 `gate.json`의 `critique_source`에 `iter_001+iter_002` 꼴로 남는다.
- **`baseline`에 이 run의 기준선 계약을, `accumulated`에 001부터 이번까지의 `contract_changes` 전부를 넘긴다.**
  넘기지 않으면 `from`과 `path`가 같은 iteration에서 대조가 자기 자신과의 비교가 되어 차이가 0이다.
  생성기가 기준선 사본을 47,561바이트에서 13,202바이트로 줄인 것이 그 자리로 통과했다. 누적으로 넘기는 이유는
  기준선 대비 차이가 한 iteration의 기록으로 덮이지 않기 때문이다 — 001이 신고한 것이 002에서도 근거다.
  `gate.json`의 `baseline_compared`와 `accumulated_changes`에 무엇을 넘겼는지 남는다.
- **`spec`에 요구사항 명세 원문을 넘긴다.** 그러면 `spec_anchor`가 명세에 실재하는 요구사항 id를 가리키는지
  값으로 대조한다. 넘기지 않으면 빈 문자열만 아니면 통과하는 옛 동작으로 떨어져, 줄이려는 계약이 줄이는 근거가
  되는 문자열(`contract:security`)이 지나간다. 그 경우 게이트가 `change.anchor_uncompared` 관찰을 남기므로
  **판정하지 못한 것이 통과로 보이는 자리**가 로그에 드러난다. 이 하네스가 가장 자주 낸 실패가 그 모양이다.
- **G0가 깨져도 판본 대조는 돈다.** 판본 대조는 빌드가 필요 없다. 추출 스펙이 필요한 G1만 건너뛴다.
  빌드가 깨진 초안이 계약을 고쳐도 그 변경은 판정되어야 한다.

`status`가 `ERROR`인 G0 결과는 **위반으로 집계하지 않고** `gate.json`의 `errors`에 담아 run을 ERROR로
끝낸다(`raise_on_gate_error`). 환경 문제는 초안의 잘못이 아니라서 고칠 수 없는 것을 refine에 보내면
iteration만 태운다.

### 빌드 수리는 iteration을 태우지 않는다

G0가 `REJECT`면 같은 iteration 안에서 빌드만 고치는 시도를 `--max-build-attempts`회(기본 3) 한다
(`repair_build`). **이 시도는 `max_iterations`에서 세지 않는다.** iteration 예산은 계약 적합성을 좁히는 데
쓰는 것이고 컴파일을 맞추는 데 쓰는 것이 아니다. 컴파일 오류는 하나를 고치면 다음 하나가 드러나는 모양이라,
밖으로 돌리지 않으면 적합성 판정에 한 번도 닿지 못한다 — 세 iteration이 각각 오류 하나씩에 소진된 run이 있었다.

- **수리 입력은 빌드 실패 문장뿐이다**(`build_repair_request`). 열린 적합성 위반과 비평과 약한 축은 넣지 않는다.
  적합성은 아직 판정되지 않았으므로 고칠 것이 없고, 빌드를 고치며 구현을 크게 바꾸면 무엇이 무엇을 고쳤는지
  원장이 가를 수 없다. `critique`도 넘기지 않는다.
- 시도마다 커밋을 남기고(`build-repair-N`) 그 iteration의 초안 색인에 합친다(`amend_draft_for_repair`).
  시도 횟수와 매번의 실패 문장은 `gate.json`의 `build_attempts`와 원장에 남는다. 수리에 몇 번이 걸렸는지가
  초안의 품질 신호다.
- 시도가 성공해 G0가 통과하면 그 iteration은 계속 진행한다. G1과 critique와 eval이 그 뒤에 돈다.
- 한도를 넘기면 그 iteration을 REJECT로 닫고 나간다. G0 위반이 다음 iteration으로 합류하는 것은 그대로다.
- `--max-build-attempts 0`이면 수리를 끄고 예전처럼 곧장 refine으로 간다.

## 다른 조각에 기대는 자리

**게이트 공백은 통과가 아니다.** 판정 장치나 규칙 카드가 없으면 run을 `ERROR`로 끝낸다.
개발 중에만 `--allow-missing-gates`를 붙여 `SKIPPED`로 진행하고, 그 run은 결과의
`gate_coverage_incomplete`와 `gate.json`·`final.gate_result`의 `allow_missing_gates`가 사실을 들고 가므로
통과로 읽지 않는다.

| 기대하는 것 | 어디 | 없을 때 |
| --- | --- | --- |
| `run_g0`, `run_g1`, `check_contract_changes`, `load_rules` | `gates/` (`pipeline.gates`로 import한다) | `ERROR`. 플래그가 있으면 세 검사 모두 `SKIPPED` |
| 규칙 카드 | `../rules/conformance_rules.yaml` (`--rules`) | `ERROR`. 플래그가 있으면 카드 없이 돌고 `rule_cards`가 빈다 |
| 스켈레톤 | `phase2/taskE/task5/skeleton/` (`--skeleton-dir`) | G0가 `g0.skeleton_missing`으로 `ERROR`. 플래그가 있으면 G0 `SKIPPED` |
| JDK 17 이상 | `--java-home` 또는 `CONTRACT_IMPL_JAVA_HOME` | G0가 `g0.jdk_unusable`로 `ERROR` |
| 산문 슬롯 추출기 | `tools/prose_slots.py`의 `extract_prose_slots` 또는 `extract` | 빈 목록. `--prose-slots <path>`로 직접 넣을 수 있다 |
| 프롬프트 4벌 | `prompts/{gen,critique,eval,refine}_impl.md` | 그 단계가 LLM을 부르기 전에 멈춘다 |
| 루브릭 `impl:v1` | `rubrics/impl.rubric.yaml` | 전체 run은 멈춘다(하한 없이는 판정할 수 없다). `run_stage`는 gen·gate·refine만 돈다 |
| `update_ledger(run_dir, iteration, payload)` | `ledger.py` | 원장 기록을 건너뛴다 |
| `write_report(run_dir, ledger, result)` | `report.py` | 리포트 작성을 건너뛴다 |

**원장과 리포트는 기록 장치이므로 그것이 터져도 run을 버리지 않는다.** 판정은 이미 게이트와 루브릭이 끝냈다.
`record_ledger_and_report`가 두 호출을 각각 감싸 예외를 삼키고 `iter_00N/record.errors.json`에 종류와 문장과
트레이스백을 남긴다. 기록 장치의 고장도 고쳐야 할 것이므로 조용히 넘기지는 않는다.

`gates`·`tools`·`ledger`·`report`는 `import_pipeline_module`이 **`pipeline.<name>`으로 먼저** 가져온다.
조각 사이에서 상대 import(`from ..tools import ...`)를 쓰는 모듈이 있고 그것은 `pipeline`이 패키지로 잡혀야
풀린다. top-level로만 가져오면 장치가 있는데도 "없는 것"으로 보여 run이 멈춘다. 실패한 이유를 메시지에 싣는다.

판정 장치 확인은 gen보다 **먼저** 한다. 게이트가 없는데 gen을 먼저 돌리면 유료 호출을 쓰고도 판정하지 못한다.

## 실행

```powershell
cd .codex/skills/contract-impl-agent/pipeline

# 입력 만들기 (계약 원문을 싣고 sha256과 operation_ids를 기계로 뽑는다)
python -B ./intake_to_input.py --contract <계약> --spec <명세> --title <이름> `
  --provided-api <포트.java> --immutable-path "pom.xml;src/main/resources/application.yml" `
  --output-dir <dir>

python -B ./validate.py <input.json> --artifact input

# 단계 하나만 (작업 폴더가 없으면 스켈레톤을 복사하고 첫 커밋을 남긴다)
python -B ./run_stage.py --stage gate --input <input.json> --run-dir <run> --iteration 001 `
  --java-home "C:/Program Files/Amazon Corretto/jdk17.0.19_10"

# 전체
python -B ./run_draft.py <input.json> --max-iterations 3 --java-home <jdk17>
```

이 기계의 `JAVA_HOME`은 JDK 8을 가리키고 스켈레톤은 17로만 빌드되므로 `--java-home`을 넘기지 않으면
G0가 `g0.jdk_unusable`로 ERROR를 낸다. 쓸 JDK를 고르는 것은 사람의 판단이라 게이트가 찾아 나서지 않는다.

매니페스트의 금지 경로 검사는 input이 있어야 돌아간다.

```powershell
python -B ./validate.py <draft.json> --artifact draft --against-input <input.json> `
  --work-dir <run>/output --use-git-status
```

`--work-dir`만 주면 `declared_missing`까지, `--use-git-status`를 더하면 `undeclared_file`과
`forbidden_path`까지 본다. 파이프라인에서는 runner가 같은 것을 계산해 넘긴다.

문법만 볼 때는 `__pycache__`가 생기지 않게 `compile(...)`을 쓴다.

```powershell
python -B -c "from pathlib import Path; files=['runner.py','validate.py','run_stage.py']; [compile(Path(f).read_text(encoding='utf-8'), f, 'exec') for f in files]; print('syntax ok')"
```

## 스키마 — 모델용과 검증용 두 벌

**모델에게 주는 스키마와 판정하는 스키마를 가른다.** 두 쪽이 요구하는 것이 다르기 때문이다.
구조화출력(`--output-schema`)은 **모든 객체의 `required`가 `properties`의 모든 키를 담아야** 하고
`pattern`·길이·개수 제약을 받지 않는다. 선택 필드를 `required`에서 빼는 방식이 통하지 않으므로
선택은 널 허용 타입으로 표현한다. 그 제약에 맞춰 검증용을 느슨하게 고치면 판정의 엄격함이 모델의
형식 제약 때문에 낮아진다. 그래서 두 벌이다.

| 산출물 | 모델용 (`--output-schema`) | 검증용 (`validate_file`) |
| --- | --- | --- |
| gen / refine | `gen_output.codex.schema.json` | `gen_output.schema.json` |
| critique | `critique_output.codex.schema.json` | `critique_output.schema.json` |
| eval | `eval_output.impl.schema.json` (다섯 축 named/closed) | `eval_output.schema.json` (축 열거 없음) |
| input, draft, critique, eval, final | — | 각 `*.schema.json` |

`draft.schema.json`과 `final.schema.json`은 파일 내용을 담지 않는다. 색인과 커밋 해시만 담는다.

`pattern`·`minLength`·`minItems`와 선택 필드는 검증용에만 남는다. 모델이 지킬 형식은 프롬프트가 말한다.

**두 벌이 어긋나는 것을 막는 검사가 있다.** `validate.py --schema-pairs`가 셋을 한 번에 훑는다.

```powershell
python -B ./validate.py --schema-pairs
```

세 가지를 본다. 스키마를 고칠 때마다 먼저 이걸 돌린다 — 안 돌리면 같은 400에서 다시 막힌다.

- **구조화출력 규약**: 모든 객체의 `required`가 `properties` 전부를 담는지, `additionalProperties`가 닫혔는지,
  받지 않는 키워드가 남았는지.
- **모델용 ⊆ 검증용**: 모델용에만 있는 필드나 닫힌 값이 있으면 실패한다. 모델이 낼 수 있는 것이 검증용에
  없으면 그 산출물은 판정에서 떨어지거나 조용히 통과한다. 반대 방향은 보지 않는다 — 검증용이 더 엄격한 것은 의도다.
  검증용이 축을 열거하지 않고 `additionalProperties`로 열어 둔 자리는 모델이 이름을 박아도 맞는 것으로 본다.
- **널 일치**: 모델용이 널을 허용하는 자리는 검증용도 널을 허용하거나 그 필드가 선택이어야 한다.

`normalize_model_output`이 검증 전에 두 가지를 맞춘다. 널로 표현한 선택 필드를 지우고, 판본 경로의 철자를
실재하는 파일로 고친다. 고친 자리는 `{kind, pointer, from, to}`로 돌려주어 로그와 원장에 남는다.
자동으로 고친 것이 있으면 리포트가 그것을 말해야 한다.

모델용은 "없음"을 널로 표현하고 검증용은 필드가 없는 것으로 표현한다. 같은 뜻이므로 산출물에는 한 표현만
남긴다 — `normalize_model_output`이 검증 **전에** 그런 널을 지우고 지운 자리를 로그에 남긴다. 그러지 않으면
원장과 리포트가 널과 없음을 따로 다뤄야 한다. 검증용이 널을 직접 허용하는 자리(`supersedes`, `migration`)는
그대로 둔다.

축 집합이 루브릭과 일치하는지는 스키마가 아니라 `validate.validate_eval_contract`가 대조한다.

`eval_output`의 **`unmeasured_axes`** 는 채점자가 재지 못한 축의 이름이다. 그 축은 하한 대조에서 뺀다
(`is_unmeasured_threshold_error`) — 재지 못한 자리에 1점이 들어가 판정을 가르면 그것은 판정이 아니라 측정
실패다. `min_total`은 그 축들의 점수까지 섞은 값이라 함께 뺀다. **잰 축이 하한을 못 넘긴 것은 진짜 판정이므로
남긴다.** 잰 축만으로 품질을 다 말할 수는 없으므로 그 iteration에 PASS도 주지 않는다. 같은 iteration에서 eval을
한 번 다시 부르고(첫 산출은 `eval.unmeasured-N.json`으로 남는다), 두 번째도 못 재면 그 사실을 원장과 리포트에
남기고 게이트 판정만으로 진행한다. 측정 불가는 초안의 잘못이 아니므로 위반으로 세지 않는다.

`contract_changes[].representation_basis`는 표현 변경의 근거이고 닫힌 세 값(`interop`, `spec_implication`,
`contract_consistency`)과 빈 문자열을 받는다. 도구나 언어의 기본 표현은 근거가 아니므로 그 값이 없다.
문턱은 게이트가 셋으로 본다 — 약속 변경은 `basis`와 `spec_anchor`, 모양을 바꾼 표현 변경은 `basis`와
`representation_basis`, 더한 것뿐인 기록은 `basis`까지다.

`contract_changes[].basis`와 `spec_anchor`는 **빈 문자열을 허용한다.** 비어 있는 것을 스키마가 막으면
모델이 아무 글자나 채워 넣고, 그러면 근거의 유무를 셀 수 없다. 비어 있음은 G1이 `change.unjustified`로 잡는다.

## 외부 명령의 출력

**`subprocess`를 `text=True`·`encoding="utf-8"`로 열지 마라.** 이 기계의 윈도우 도구들은 한국어를 cp949로 내고,
그 바이트에서 디코딩이 리더 스레드 안에서 터지면 호출자가 손쓸 수 없다. 세 iteration을 돌린 run이 그렇게 끝났다.
외부 명령은 `stages/scripts/textio.run_command`로만 부른다. 바이트로 받아 utf-8 → cp949 → `replace` 순으로
푼다. utf-8이 먼저인 이유는 우리 산출물이 전부 utf-8이고 cp949는 도구 쪽 사정이기 때문이다.

같은 이유로 `ProgressReporter`가 로그 스트림에 `errors="replace"`를 건다(`harden_stream`). 로그를 쓰다 죽는 것은
판정과 아무 관계가 없으므로 글자를 잃는 쪽을 고른다.

## 실패 기록

`ERROR`로 끝난 run의 `failed.json`은 그 자체로 진단이 되어야 한다. 어느 iteration의 어느 단계에서 멈췄는지,
그때까지 세어 둔 것이 무엇인지 없으면 사람이 `iter_00N/gate.json`을 손으로 열어 보게 된다.

| 필드 | 무엇 |
| --- | --- |
| `stage`, `last_iteration` | 멈춘 자리 |
| `traceback` | 어느 줄에서 멈췄는지 |
| `failure_counts_by_category` | 그때까지 쌓인 실패의 분류별 개수 |
| `iteration_progress` | iteration마다 g0·g1·changes·위반 수·규칙 id·수리 횟수·비평/채점 여부 |
| `iteration_rejections` | 닫힌 iteration의 거절 기록 |

## 금지 행동

- LLM stage가 허용되지 않은 파일을 임의로 읽게 하지 않는다.
- Gen이 계약 개정을 발의하게 하지 않는다. Critique가 점수표를 만들게 하지 않는다.
- Eval이 critique나 게이트 결과를 읽게 하지 않는다. Refine에 총점 원문을 넘기지 않는다.
- 점수로 게이트를 뒤집지 않는다. 게이트 위반이 한 건이면 총점이 아무리 높아도 REJECT이고,
  REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.
- **측정하지 못한 것을 점수로 쓰지 않는다.** 게이트 공백을 통과로 세지 않는 것과 같은 이유로, 보지 못한 것을
  최저점으로도 세지 않는다.
- 게이트를 느슨하게 고쳐 확장을 얻지 않는다. 계약이 열면 게이트가 따라 연다.
- 모델용 스키마의 제약에 맞추려고 검증용 스키마를 느슨하게 고치지 않는다. 두 벌로 가른 이유가 그것이다.
- `--allow-missing-gates`를 기본으로 쓰지 않는다. 판정 장치가 보지 못한 자리는 미판정이고 미판정은 통과가 아니다.
- 외부 명령을 `subprocess.run`으로 직접 부르지 않는다. 인코딩 때문에 판정이 멈추는 일이 실제로 있었다.
- 같은 run artifact를 사용자 의도 없이 덮어쓰지 않는다. 재실행 덮어쓰기는 `--overwrite`가 있을 때만이다.
  단 `gate.json`과 `refine-request.json`은 runner가 매 실행에 다시 계산하는 파생물이므로 덮어쓴다.
