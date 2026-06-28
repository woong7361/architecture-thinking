추천 설계는 collector는 결정론적 스냅샷 재생성 도구로 두고, proposal은 기본 수동 실행으로 둔다. 즉 collector는 여러 번 돌아도 같은 입력 run 집합이면 같은 latest report를 다시 만들고, 필요하면 timestamped report도 같이 남긴다. proposal은 collector 결과를 읽어 개선 후보를 만들지만 자동으로 기준 파일을 수정하지 않는다.

collector 실행 시점은 세 가지다. 첫째, 수동 실행이다. 사용자가 회고하려고 python collect.py를 실행한다. 둘째, Level 2 runner가 끝난 뒤 선택적으로 실행하는 방식이다. runner 기본 동작에 넣지는 않고 --collect 같은 옵션으로 둔다. 셋째, 나중에 주간 자동화나 CI 후보로 확장한다. 실전 MVP는 수동 실행 + runner 옵션이다. 매 run마다 무조건 collector를 돌리면 빠른 검토 흐름이 무거워질 수 있다.

파일은 두 종류로 만든다. reports/latest/summary.json과 trend.md는 계속 덮어쓰는 최신 스냅샷이다. reports/history/{timestamp}/summary.json과 trend.md는 보존용이다. 기본은 latest만 쓰고 --history를 주면 history도 남긴다. 이렇게 하면 여러 번 돌려도 사용자는 latest만 보면 되고, 실험 기록이 필요할 때만 history를 쌓을 수 있다.

수집 대상은 axis 축과 critique 문제점을 둘 다 포함한다. axis 쪽은 validation.json의 scores, weak_axes, weighted_score, gate_result와 eval.json 또는 validation.json의 score_reasons를 수집한다. critique 쪽은 critique.md의 문제 지점, 확인 필요, 수정 제안 섹션을 구조적으로 파싱하되, 처음에는 완벽한 자연어 분류보다 섹션별 bullet 수와 원문 bullet 목록을 저장한다. 이후 반복 키워드나 수동 태그를 붙일 수 있다.

병합 감지는 반드시 고려해야 한다. 같은 run의 attempt 1, 2, 3을 별도 run처럼 세면 실패가 과대 집계된다. collector의 기본 집계 단위는 run_id이고, attempt는 그 안의 revision history로 본다. run 단위 대표 attempt는 final_attempt를 정한다. final_attempt는 gate_result=pass인 마지막 attempt가 있으면 그것, 없으면 가장 마지막 attempt다. 통계에는 run-level과 attempt-level을 둘 다 분리해서 남긴다.

추가로 병합 감지는 두 층이 있다. 첫째, attempt 병합이다. 같은 run 안의 attempts를 하나의 case로 묶는다. 둘째, 중복 run 병합이다. 같은 사용자 입력과 비슷한 draft로 여러 run을 만든 경우 중복으로 볼 수 있다. MVP에서는 input.md의 Original User Input을 hash한 input_hash를 기록하고 같은 input_hash가 있으면 duplicate_candidates에만 표시한다. 자동 병합은 하지 않는다. 자동 병합은 잘못하면 실제로 다른 맥락의 run을 하나로 합칠 수 있기 때문이다.

proposal은 기본적으로 manual이다. collect.py를 실행한 뒤 사람이 python propose_improvements.py --from reports/latest/summary.json을 실행한다. 자동 루프는 나중 단계로 둔다. 다만 --auto-propose 옵션으로 collector 뒤에 proposal 생성을 붙일 수는 있다. 이 경우에도 proposal 파일만 만들고 SKILL.md, rubric.yaml, prompt, validate.py는 수정하지 않는다. apply는 별도 명령이나 수동 patch로 분리한다.

실전 흐름은 이렇게 잡는다. 평소에는 Level 2 run만 쌓인다. 회고하고 싶을 때 collect.py를 실행한다. summary에서 같은 weak_axis가 반복되거나 critique 문제 유형이 반복되면 propose_improvements.py를 수동 실행한다. proposal을 읽고 사람이 승인하면 그때만 기준 파일을 수정한다. 수정 뒤에는 대표 run 세트로 회귀 확인한다.
