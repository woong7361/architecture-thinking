# 확인한 프로젝트 문맥

- `task2/assignments/taskB-0.md` 부터 `taskB-7.md`까지 모든 Markdown 파일에 `<!-- notion-feedback:begin -->` 주석이 있다.
- 각 파일의 피드백 섹션에 `<!--`와 `-->`로 감싼 메타데이터 주석도 있다.
- `task1/assignments` Markdown에서는 같은 HTML 주석이 검색되지 않았다.
- `git blame` 결과, `taskB-0.md`의 해당 주석은 커밋 `11ef0db`에서 추가되었다.
- 현재 Codex 공식 메뉴얼에서 해당 경고 문구나 Markdown 편집기의 HTML/JSX/MDX 판별 규칙은 확인하지 못했다. 공식 OpenAI 도메인 검색에서도 같은 문구는 나오지 않았다.
- `PROBLEM.md`의 열린 문제는 이 편집 경고와 관련이 없다.

# 질문 범위

파일을 수정하지 않고 경고가 나오는 원인과 사용자가 확인할 방법만 설명한다.
