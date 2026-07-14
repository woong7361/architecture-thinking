현재 Orca에는 같은 terminal tab 바깥의 tab group 사이를 좌우로 바로 포커스 이동하는 전용 단축키는 없습니다.

Windows 기준 Ctrl+[ / Ctrl+]는 terminal tab 내부 split pane 이동이라 질문하신 동작이 아닙니다. 키보드로 바깥 pane으로 가려면 Ctrl+Shift+J를 눌러 Jump Palette를 열고, 대상 open tab을 검색해 Enter를 누르는 방법이 가장 가깝습니다. 선택한 tab이 속한 바깥 tab group으로 포커스도 함께 이동합니다.

pane 사이로 tab 자체를 옮기려는 뜻이라면 현재 공식 문서에는 tab을 다른 group으로 드래그하는 방법만 나옵니다. Settings → Shortcuts에서 Switch worktree 단축키는 바꿀 수 있지만, Focus next outer pane 같은 별도 action은 현재 공식 keymap에 없습니다.
