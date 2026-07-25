# Task C-4: 재현 가능한 환경 (Docker, Stateless, docker-compose)

(Grit's Why): '제 컴퓨터에선 됐는데요'는 시스템이 아닙니다. 어디서든 같게 도는 환경이 곧 신뢰입니다.

### 수행 내용

1. 애플리케이션을 Dockerfile로 이미지화하고, docker-compose로 앱 + DB(+ 필요 시 cache/queue)를 한 번에 띄우세요. docker compose up 한 줄로 기동되어야 합니다.
2. 상태를 외부화(Stateless)해서 수평 확장이 가능한 구조로 두세요. 무엇을 왜 외부로 뺐는지 적으세요.
3. Spring Boot 헬스 체크 엔드포인트를 두고, 컨테이너가 정상 기동했는지 확인하는 방법을 적으세요.

### 제출물

- [ ]  Dockerfile + docker-compose.yml을 GitHub에.
- [ ]  docker compose up으로 1-command 기동되는 화면 또는 로그.
- [ ]  Stateless 설계 메모(무엇을 왜 외부화했는가). (최소 300자)
