# Changelog

파이프라인 component 변경 이력. slow loop proposal을 사람이 수락·적용할 때 여기에 한 줄을 추가한다.

형식:
```
## {component}:{vN} ({날짜})
- 변경: {무엇을}
- 근거: {왜 — 분석 신호 요약}
- 분석 run: {pending run hash들}
- 위험: 낮음 | 중 | 높음
- commit: {적용 커밋 해시}
```

과거 버전은 git이 관리한다. 별도 스냅샷을 만들지 않는다.

변경 경로는 둘이다. 느린 루프 proposal을 사람이 수락한 것과, 외부 학습을 근거로 사람이 직접 정한 것. 후자는 `분석 run` 대신 근거를 명시한다.

---

## intake-to-input:base-input (2026-08-25)
- 변경: 기존 input의 brief를 보존하고 승인된 context 블록만 교체하는 `--base-input` 경로 추가. `section_plan`이 들어오면 기존 `spine`은 제거하고 반대 경우도 동일하게 처리
- 겨냥: 같은 원문·독자 블록·제약·금지어를 유지한 채 이전 `spine` run과 신규 `section_plan` run을 비교하는 intake 재생성 경로
- 근거: 사용자가 이전 input으로 section plan을 적용해 비교 실행하도록 요청. 기존 CLI 인자만으로는 `forbidden_phrases`를 포함한 비교 조건을 손실 없이 재현할 수 없었음
- 검사: section plan 전체 테스트 9개 통과. 기존 raw text·constraints 보존과 `spine` → `section_plan` 교체 테스트 추가
- 위험: 낮음. 명시적 `--base-input`에서만 작동하며 생성 input은 기존 schema validator를 동일하게 통과함
- commit: 미커밋

## section-plan:v1 (2026-08-25)
- 변경: 승인된 절별 작성 계약 `brief.section_plan`을 추가. 절별 `id`, `heading_promise`, `purpose`, source·anchor·role 재료를 필수로 두고 `connection_to_next`는 선택으로 둠
- 겨냥: 소제목과 본문 범위가 어긋나고 절 사이가 나열처럼 느껴지는 문제
- 호환: 기존 `brief.spine`은 유지. 새 intake는 `section_plan`을 생성하고 두 필드의 동시 사용은 거부
- 검사: schema는 선택 연결이 있을 때만 형식을 검사. deterministic validator는 절 id 중복과 source anchor 존재만 검사하고 연결의 존재와 의미는 검사하지 않음
- 평가: Critique에 `section_reviews`를 추가하고 기존 `structure` 축을 절 적합성과 선언된 연결 준수 기준으로 구체화. 가중치와 통과선은 유지
- 근거: 사용자 승인. 모든 절에 이어받는 질문을 강제하지 않고 필요한 연결만 계약으로 남김
- 위험: 중간. section plan을 사용하는 신규 input의 절 범위가 강해지므로 기존 run과 함께 회귀 검증
- commit: 미커밋

## blog-profile:intake-eval-responsibility (2026-08-25)
- 변경: `blog-profile.md`에서 intake 필드 매핑과 발행 후 성공 신호를 제거. profile 적용 절차는 `SKILL.md`와 `references/intake-guide.md`로 이동하고, 초안에서 관찰 가능한 판단 과정은 기존 judgment·evidence·reader_fit 축과 critique 기준에 연결
- 겨냥: 독자 전략, intake 실행, 초안 품질 판정, 발행 후 성과의 책임 분리
- 근거: 사용자 승인. profile에 실행 규칙과 사후 성과가 함께 있어 eval·critique 책임과 겹친다는 검토
- 위험: 낮음. rubric 축, 가중치와 threshold는 변경하지 않음
- commit: 미커밋

## gen_system:v2 (2026-06-30)
- 변경: emphasis 주제를 비중 중심으로 삼고 다른 소재는 보조 역할로만 배치하는 지시 추가 (P1)
- 겨냥 axis: originality
- 근거: critique 반복 지적 — 1·2절이 emphasis 주제(multi-provider 확장)보다 비중이 큼; 분석 run 3930539b
- 분석 run: 2026-06-29_3930539b
- 위험: 중
- commit: ca6f266

## gen_system:v3 (2026-06-30)
- 변경: raw_text의 핵심 기술 결정·선택 이유·설계 판단을 본문에서 생략 금지하는 지시 추가 (P2)
- 겨냥 axis: evidence
- 근거: critique 반복 지적 — build_prompt의 system/user 분리 결정이 draft에서 완전히 사라짐; 분석 run 3930539b
- 분석 run: 2026-06-29_3930539b
- 위험: 중
- commit: ca6f266

## gen_system:v4 (2026-06-30)
- 변경: 결론 지시를 "한계 나열 금지, 독자가 가져갈 판단·관점·실험 단서로 마무리" 로 교체 (P3)
- 겨냥 axis: originality
- 근거: critique 반복 지적 — 마지막 단락이 한계를 나열하지만 독자가 가져갈 단서가 없음; 분석 run 3930539b
- 분석 run: 2026-06-29_3930539b
- 위험: 중
- commit: ca6f266

## input.schema:reader/guide/judgment (2026-08-20)
- 변경: `brief`에 `reader`(욕망·악당·외적·내적·철학적·판돈), `guide`(공감·권위), `judgment`(버린 가설·깨지는 조건) 블록 신설. 전부 optional, `reader`를 넣으면 `desire`·`external`·`internal`은 함께 요구
- 겨냥: 저자 영역 경계를 프롬프트 지시가 아니라 계약으로 긋는다
- 근거: 사람 주도 변경 (taskD-0 저자 영역 경계, taskD-1 독자 3층 정의). 프롬프트로 "독자의 내적 문제를 다뤄라"만 넣으면 재료가 없을 때 모델이 지어낸다
- 함께 변경: `intake_to_input.py --context-file`, `gen_system.md` 입력 규칙 4줄, `references/intake-guide.md`, `SKILL.md`
- 위험: 낮음 (전부 additive, 기존 input 2건 재검증 PASS)
- commit: 미커밋

## craft.md:v1 (2026-08-20)
- 변경: 장르 기법 파일 신설 + `context.py`의 `load_craft_context(piece_type)`로 gen/refine에만 주입. critique/eval 미주입
- 겨냥 axis: sentence, purpose_fit
- 근거: 사람 주도 변경 (taskD-1 벤치마킹 4편의 적용할 점). 같은 input으로 돌린 비교 run에서 gen 단계 total 3.825 → 3.96, sentence 3.5 → 3.8, purpose_fit 3.5 → 4.0, 분량 4090자 → 3015자
- 분석 run: 2026-08-20_2db504b3 (baseline / craft 2회)
- 위험: 중 (기법이 저자 목소리를 덮을 수 있어 `soul.md` 우선 규칙을 파일에 명시)
- commit: 미커밋

## validate:content_envelope (2026-08-20)
- 변경: `content`가 파싱 가능한 JSON 객체·배열이면 REJECT. gen_output·refine_output·draft 세 지점에 적용
- 겨냥: 계약 게이트 결손
- 근거: run 2db504b3 iter_002에서 refine 모델이 출력을 이중으로 감싸 `content`가 `{"content":"..."}` 문자열이 됐는데, 스키마는 문자열이라 통과시켰고 LLM 평가자가 4.165점을 주고 PASS 판정했다. refine 프롬프트에는 이미 "`content`만 출력합니다"가 있었으므로 프롬프트 지시로는 막히지 않는다
- 분석 run: 2026-08-20_2db504b3
- 위험: 낮음 (오염 아티팩트 REJECT, 정상 아티팩트 PASS 확인)
- commit: 미커밋

## rubric:writing:v2 (2026-08-20)
- 변경: 5축 → 8축. 기존 structure·evidence·sentence·originality·purpose_fit 유지, judgment·reader_fit·grounding 신설. 서술어를 "좋음의 정도"에서 "확인 가능한 요소의 유무"로 교체. min_total 3.8 잠정, status=provisional
- 근거: 사람 주도 변경 (taskD-0·D-1). v1은 도입·문체·분량·결론이 다 다른 초안 세 편에 structure 4.0 / evidence 4.0 / originality 4.0을 세 번 똑같이 줬다(폭 0.5). v2는 같은 재료에서 3.8~4.8(폭 1.0)
- 중복 정리: evidence는 "무엇을 봤나", judgment는 "무엇을 어떻게 골랐고 어디서 깨지나". purpose_fit은 brief 제약 준수로 축소
- 함께 변경: eval.schema.json, eval_output.schema.json(축 목록을 rubric에서 생성), eval_system.md(축 하드코딩 제거), refine_system.md(weak_axes 8개)
- 위험: 중 (과거 eval 아티팩트는 새 스키마에서 거부된다. 앵커 셋을 쓰지 않기로 해 호환을 지키지 않음)
- commit: 미커밋

## validate:content_contract (2026-08-20)
- 변경: target_length 범위, forbidden_phrases(신규 리터럴 필드), craft 기법 용어 노출, 귀속된 날조 인용을 계약 검사로 추가. eval 검증 결과에 병합해 refine으로 보낸다
- 근거: 길이·금칙어 검사가 아예 없었다. refine_system.md에 "contract_errors에 길이 문제가 있으면"이 있는데 그 에러를 만드는 코드가 없는 죽은 분기였다. 실제 초안에서 4090자(3000-4000 요청)와 기법 용어 "악당" 노출을 검출
- 한계: avoid와 must_include는 값이 의미 서술이라 결정적으로 못 잡는다. purpose_fit 축에 남겼다
- 인용 검사는 처음 3건이 전부 오탐(패러프레이즈)이라 귀속 표지가 붙은 인용만 보도록 좁혔다
- 위험: 낮음
- commit: 미커밋

## validate:weighted_total 검산 (2026-08-20)
- 변경: 가중치를 rubric과 대조하고, 총점을 rubric 가중치로 재계산해 판정에 쓴다. 보고값 불일치는 errors가 아니라 notes로 남긴다
- 근거: 총점을 모델이 계산하고 아무도 검산하지 않았다. 과거 아티팩트 14개 중 8개가 어긋났다(3.43↔3.425, 3.85↔3.825, 4.31↔4.329). 합격선이 평가자의 산수에 의존하고 있었다
- 설계: 산수 실수는 초안의 결함이 아니므로 refine으로 보내지 않는다
- 위험: 낮음
- commit: 미커밋

## runner:stage 격리 (2026-08-20)
- 변경: 모든 스테이지를 빈 임시 디렉터리에서 실행. codex는 --dangerously-bypass-approvals-and-sandbox → --sandbox read-only, -C를 빈 디렉터리로. claude는 cwd 격리 + 파일·네트워크 도구 차단. project_dir 배선 전면 제거
- 근거: 스테이지가 pipeline/ 전체를 읽을 수 있었다. 초안이 validator·generator·refiner와 그 동작을 brief 없이 1인칭으로 서술했고, 평가자는 자기가 대조당하는 min_total을 읽을 수 있었다
- 검증: 격리 후 brief에 없던 용어만 정확히 사라지고(validator·refiner·evaluator·금칙어), brief에 있던 것은 남았다(critique·Gherkin·run 해시)
- 관측: 날조를 못 하게 하니 총점이 4.31 → 4.06으로 내려갔다. 지어낸 구체성이 점수를 벌고 있었다
- 위험: 중 (스테이지에서 셸이 닫힌다. gen·critique·eval은 프롬프트만 필요하므로 영향 없음)
- commit: 미커밋

## runner:envelope 재시도 (2026-08-20)
- 변경: gen·refine 출력이 JSON envelope이면 해당 스테이지를 1회만 재생성. draft metadata에 envelope_retry 스탬프
- 근거: 오늘 run 3번 중 2번에서 발생했고 그중 한 번은 run을 죽였다. 포맷 실수 하나로 성공한 스테이지를 전부 버리게 된다. 실전에서 재시도가 걸려 run이 통과했다
- 위험: 낮음 (두 번째 실패는 그대로 종료)
- commit: 미커밋

## critique:unsupported_claims (2026-08-20)
- 변경: critique가 초안의 1인칭 경험·수치·인용·구성 요소 서술을 brief와 대조해 근거 없는 것을 목록으로 낸다. 계약 에러로 병합되어 PASS를 막고 refine이 삭제 방향으로만 반영한다
- 겨냥 axis: grounding
- 근거: 루브릭만으로는 못 잡는다. 날조가 있는 초안이 grounding 4.8, 없는 초안이 4.5로 방향이 반대였다. 구체적으로 쓰인 거짓이 모호하게 쓰인 진실보다 근거 있어 보인다
- 검증: 날조 있는 초안 4건 검출(손으로 잡은 1건 + 3건), 깨끗한 초안 0건
- 위험: 중 (critique 오탐이 PASS를 막을 수 있어 프롬프트에 정밀도 편향을 넣었다)
- commit: 미커밋

