# task1/task1-4-history — Cucumber 실행 플레이북

> 이 파일은 `task1/CLAUDE.md`를 상속한다.
> 이 폴더는 task1-4 수행내용 2번, 즉 Gherkin Feature를 Cucumber-JVM Step Definition과 도메인 코드로 실행하는 제출용 작업 폴더다.

## 실행 규칙

- Feature는 `src/test/resources/features/refund.feature`에 둔다.
- Step Definition은 `src/test/java/com/thinking/payment/steps`에 둔다.
- Cucumber 실행 진입점은 `CucumberAcceptanceTest`다.
- 실행은 `JAVA_HOME`을 Corretto 17로 맞춘 뒤 `./mvnw test`로 한다.

## 검증 관점

- Feature는 사람 언어로 쓰인 인수 조건이고, Step Definition은 그 문장을 도메인 코드 호출로 연결한다.
- 이 폴더에서는 Cucumber 인수테스트를 실행 게이트로 삼는다.
- 도메인 로직을 검증할 때는 Cucumber 문장의 Given/When/Then이 실제 상태와 결과를 단언하는지 먼저 확인한다.
