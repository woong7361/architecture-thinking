# Original User Input

https://www.onorca.dev/docs

여기서 pane 옮기는 법이 있어? 단축키가 필요해 같은 탭 내에서가아니라

# Checked Context

사용자는 Windows 환경으로 보인다. Orca에서 같은 terminal tab 내부 split pane이 아니라, 바깥 tab group/pane 사이를 키보드로 이동하거나 tab 자체를 옮기는 방법을 묻는다.
공식 문서 확인:
- Tabs, panes & split layouts: tab을 다른 group으로 옮기는 방법은 drag이며, split도 tab을 pane edge로 drag한다. https://www.onorca.dev/docs/model/tabs-panes-splits
- Quick Open & Jump Palette: Jump Palette는 모든 worktree와 open tab을 검색한다. https://www.onorca.dev/docs/model/quick-open
- Settings: 모든 shortcut은 remap 가능하다. https://www.onorca.dev/docs/settings
공식 저장소 main commit 8662e5a7ab448063f102888e7b00052cd6465080 확인:
- Windows/Linux worktree.palette 기본값은 Mod+Shift+J, 즉 Ctrl+Shift+J. src/shared/keybindings.ts:223-232
- terminal.focusNextPane/PreviousPane는 Mod+] / Mod+[이고 scope가 terminal이다. src/shared/keybindings.ts:949-962
- KeybindingActionId에 top-level tab group focus/move action은 없다. src/shared/keybindings.ts:29-112
- Jump Palette에서 open tab 선택 시 state.focusGroup(result.worktreeId, result.groupId)를 호출한다. src/renderer/src/lib/workspace-tab-palette-activation.ts:95
판단: 현재 외부 tab group 사이를 방향키처럼 직접 focus 이동하는 기본/등록 가능한 action은 보이지 않는다. 키보드 대안은 Ctrl+Shift+J로 Jump Palette를 열어 대상 open tab을 선택하는 것이다. tab 자체를 다른 group으로 보내는 것은 drag만 문서화되어 있다.
