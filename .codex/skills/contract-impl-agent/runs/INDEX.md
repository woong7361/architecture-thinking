# 저장한 run 목록

판정 장치를 고칠 때마다 저장된 run에 되돌려 대조하려고 남긴다. 빌드 산출물(`output/target`)은 뺐고,
iteration마다 찍힌 git 스냅샷은 `output.bundle` 한 파일로 묶었다(`git clone output.bundle`로 되살린다).

**표의 게이트 판정은 그 run이 돌던 때의 규칙이 낸 값이다.** 지금 규칙으로 다시 판정하려면 그 run 폴더에
`--stage gate`를 돌려 `gate.json`을 새로 쓴 뒤 `--stage record`로 원장과 리포트를 다시 만든다. `output/`과
계약 판본이 남아 있으므로 그 길이 열려 있다. 특히 codex run들의 위반 수에는 뒤에 오판으로 판명된
`response.field_differs`(응답에서 값을 좁힌 것)가 섞여 있다.

`runs12`는 한 가지를 더 안고 있다. **그 run이 도는 동안 판정 규칙이 바뀌었다.** runner가 iteration마다
디스크에서 게이트 모듈을 가져오므로 001·002·003이 서로 다른 판본으로 판정됐다. 그래서 이 run은 뒤에 재판정했고
표에는 재판정한 값을 적는다(비고 참고). 그때의 판정은 git 이력에 남아 있다.

판정 칸은 `G0/G1/판본대조`를 앞글자로 적는다(P=PASS, R=REJECT, S=SKIPPED). `v`는 위반, `o`는 관찰,
뒤의 숫자는 루브릭 총점이다. 번호에 빈 자리(runs2·runs3)가 있는 것은 그 이름으로 돌린 run이 남아 있지 않기
때문이다.

| run | run_id | 공급자 | 모델 | 끝난 이유 | iter | 판정 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| runs1 | 2026-09-29_508c928d | codex | gpt-5.5 | stage_error | 3 | 001:P/R/P v7 o5 3.20  002:P/R/R v136 o64 4.45  003:돌지 않음 |  |
| runs1 | 2026-09-29_745183bc | codex | gpt-5.5 | stage_error | 2 | 001:P/R/P v46 o4 4.20  002:돌지 않음 |  |
| runs1 | 2026-09-29_b02d8689 | codex | gpt-5.5 | max_iteration_exceeded | 3 | 001:R/S/S v1 o0  002:R/S/S v4 o0  003:R/S/S v1 o0 |  |
| runs4 | 2026-09-29_b02d8689 | codex | gpt-5.5 | max_iteration_exceeded | 3 | 001:P/R/P v56 o51 4.60  002:R/S/S v1 o0  003:P/R/P v57 o14 1.45 |  |
| runs5 | 2026-09-30_b02d8689 | codex | gpt-5.5 | max_iteration_exceeded | 3 | 001:R/S/S v1 o0  002:R/S/S v1 o0  003:R/S/S v1 o0 |  |
| runs6 | 2026-09-30_b02d8689 | codex | gpt-5.5 | stage_error | 3 | 001:R/S/S v1 o0  002:P/R/P v523 o4 1.75  003:R/S/S v1 o0 |  |
| runs7 | 2026-09-30_b02d8689 | codex | gpt-5.5 | stage_error | 2 | 001:P/R/P v45 o4 1.00  002:R/S/S v1 o0 |  |
| runs8 | 2026-09-30_b02d8689 | codex | gpt-5.5 | stage_error | 3 | 001:P/R/P v31 o65 2.60  002:R/S/S v1 o0  003:P/R/R v342 o9 |  |
| runs9 | 2026-09-30_b02d8689 | codex | gpt-5.5 | stage_error | 1 | 001:돌지 않음 |  |
| runs10 | 2026-09-30_b02d8689 | 기록 없음 | 기록 없음 | 사람이 중단(iter_003 도중) | 3 | 001:P/R/P v94 o7 2.25  002:R/S/P v1 o0  003:돌지 않음 |  |
| runs11 | 2026-09-30_b02d8689 | claude | claude-opus-5 | stage_error | 1 | 001:P/P/P v0 o31 4.20 |  |
| runs12 | 2026-09-30_b02d8689 | claude | claude-opus-5 | max_iteration_exceeded (재판정 후 003 PASS) | 3 | 001:P/R/P v2 o31 4.20  002:P/P/P v0 o42 3.65  003:P/P/P v0 o43 4.00 | 판정 규칙이 run 도중에 바뀌었다. 뒤에 세 iteration의 게이트를 지금 규칙으로 다시 돌렸고(추출 스펙은 그때와 같다), iter 003의 critique·eval은 판본 기준 채점으로 다시 돌렸으며, 원장과 REPORT를 `--stage record`로 새로 냈다. iter 001·002의 critique·eval은 그때의 것이다. 표의 판정이 이 재판정의 값이다 |
