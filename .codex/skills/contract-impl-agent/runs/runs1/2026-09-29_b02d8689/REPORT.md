# 계약 적합성 run 리포트 — 2026-09-29_b02d8689

이 run이 낸 구현 초안과 그 초안이 서 있는 계약 판본, 그 사이에 내린 판단을 사람이 훑을 수 있게 적는다. 판정 결과만 알려 주면 판단을 기록한 뜻이 없으므로 통과한 run에서도 같은 여섯 절을 낸다.

작성 시각은 2026-09-29T23:46:18+09:00이고 기계가 읽는 판은 `run_ledger.json`이다.

## 1. 요약

이 run이 무엇을 남겼는지 한 자리에서 본다.

판정은 **REJECT**이고 iteration 003까지 왔다. 위반 5건 가운데 4건을 코드로 닫고 0건을 계약으로 닫았으며 1건이 아직 열려 있다. 서 있는 결정은 2건, 계약 변경 기록은 0건이다.

계약은 기준선 `tennis-alert-api.yaml`에서 출발해 `contract/tennis-alert-api-v1.yaml`까지 왔다. 바로 앞 판본은 `contract/tennis-alert-api-v1.yaml`이다.

`decision_risk`는 **low** 수준이다. 올릴 근거는 없다.

REJECT 사유는 게이트와 루브릭을 섞지 않고 따로 적는다.

게이트:
- gate:g0.compile_failed@None: [ERROR] COMPILATION ERROR : 
[ERROR] /C:/Users/PC-220627-03/AppData/Local/Temp/claude/c--Users-PC-220627-03-Desktop-project-task-architecture-thinking/862b7ff3-a396-4dc6-9b71-a200dcde2544/scratchpad/runs/2026-09-29_b02d8689/output/src/main/java/com/thinking/tennis/app/AvailabilityServiceConstructorWiringConfiguration.java:[17,21] cannot find symbol
  symbol:   method setAutowireMode(int)
  location: interface org.springframework.beans.factory.config.BeanDefinition
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project tennis-alert-skeleton: Compilation failure
[ERROR] /C:/Users/PC-220627-03/AppData/Local/Temp/claude/c--Users-PC-220627-03-Desktop-project-task-architecture-thinking/862b7ff3-a396-4dc6-9b71-a200dcde2544/scratchpad/runs/2026-09-29_b02d8689/output/src/main/java/com/thinking/tennis/app/AvailabilityServiceConstructorWiringConfiguration.java:[17,21] cannot find symbol
[ERROR]   symbol:   method setAutowireMode(int)
[ERROR]   location: interface org.springframework.beans.factory.config.BeanDefinition
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

색인과 실물이 어긋난 경로가 28곳이다. 게이트는 색인이 선언한 것을 판정하므로 선언 밖에 놓인 파일은 판정되지 않은 표면이 된다.

| 경로 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/api/AlertController.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiAuthentication.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiModels.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilityController.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValidationException.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityService.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/InMemoryAlertRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertRepository.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DomainExceptions.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/TimeWindow.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/UserId.java | 002 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AlertController.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiAuthentication.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiExceptionHandler.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/ApiModels.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/AvailabilityController.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/api/RequestValidationException.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/AvailabilityService.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/app/InMemoryAlertRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/Alert.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AlertRepository.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/AvailabilitySnapshot.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/DomainExceptions.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/TimeWindow.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |
| src/main/java/com/thinking/tennis/domain/UserId.java | 003 | 색인에 없는 파일이 작업 폴더에서 바뀌어 있다 |

판정되지 않은 검사가 있다: changes, g1. 건너뛴 단계의 이유는 g0_reject다. 미판정은 통과가 아니므로 이 리포트를 통과로 읽지 않는다.

| iteration | 판정 | G0 | G1 | 판본 대조 | 위반 | 기록 | 총점 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 001 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |
| 002 | REJECT | REJECT | SKIPPED | SKIPPED | 4 | 0 | — |
| 003 | REJECT | REJECT | SKIPPED | SKIPPED | 1 | 0 | — |

## 2. 계약을 이렇게 고쳤다

계약 판본은 이 run에서 움직이지 않았다. 고칠 자리를 찾지 못했거나 고칠 필요가 없었다는 뜻이다.

## 3. 그 변경이 무엇을 바꾸는가

계약이 움직여서 무엇이 사라지고 무엇이 생겼는지 센다. 분모는 둘이다.

이 run의 기준선 `tennis-alert-api.yaml` 대비로는 좌표 0곳이 달라지고 깨는 변경 0건이 나왔다. 사람이 확정한 원본 `tennis-alert-api.yaml` 대비 누적으로는 좌표 0곳이 달라지고 깨는 변경 0건이다. run을 여러 번 돌리면 기준선이 스스로 멀어지므로 원본 대비를 함께 낸다.

| 분모 | 달라진 좌표 | 깨는 변경 | 사라진 판정 지점 |
| --- | --- | --- | --- |
| 앞 판본 | 0 | 0 | — |
| 이 run의 기준선 | 0 | 0 | — |
| 사람이 확정한 원본 | 0 | 0 | — |

## 4. 계약이 정하지 않아 내가 고른 것

계약과 명세가 값을 정하지 않아 구현이 고른 자리다. 신뢰도가 낮고 파급이 넓은 것이 위로 온다.

신고된 결정은 2건이고 그중 0건이 신뢰도 낮음이다. 이 절은 고른 것이 좋은 선택인지 말하지 않는다. 그 판단은 사람이 낸다.

| id | 질문 | 고른 것 | 근거 종류 | 신뢰도 | 되돌리는 비용 | 닿는 판정 지점 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| d_spring_constructor_selection | How should Spring instantiate AlertService when the service exposes multiple constructors and no constructor is explicitly annotated? | Set constructor autowiring on the existing alertService bean definition during bean-definition post-processing. | least_harm | medium | Remove the constructor-wiring configuration once AlertService has an explicit Spring constructor declaration. | boot:alertService | standing |
| d_spring_availability_constructor_selection | How should Spring instantiate AvailabilityService when its bean definition does not enable constructor autowiring? | Add a focused bean-definition post-processor that enables constructor autowiring for availabilityService. | least_harm | high | Remove the focused wiring configuration once AvailabilityService declares an explicit Spring constructor. | boot:availabilityService | standing |

`d_spring_constructor_selection` — The boot failure is caused by constructor selection, and constructor autowiring preserves the existing dependency graph and in-memory behavior.

`d_spring_availability_constructor_selection` — The startup failure is specifically caused by Spring searching for a no-argument constructor; enabling constructor autowiring preserves the existing dependency graph and keeps the change isolated to boot wiring.

## 5. 고친 것

위반마다 어느 자리를 고쳤는지, 코드로 닫았는지 계약으로 닫았는지, 다시 열렸는지를 적는다.

위반 5건 중 4건이 닫혔다. 굳은 것은 0건이다. 닫힘은 게이트 결과만 줄 수 있으므로 주장과 판정을 따로 싣는다.

| id | 규칙 | 판정 지점 | 좌표 | 상태 | 닫은 방법 | 재발 | 처음 본 iteration | 닫힌 iteration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v_579dfe | g0.compile_failed | — | mvnw test | open | — | 0 | 003 | — |
| v_271ee5 | manifest.contract_version_missing | — | iter_002 refine_output | closed | code | 0 | 002 | 003 |
| v_459e29 | manifest.undeclared_file | — | iter_002 refine_output | closed | code | 0 | 002 | 003 |
| v_5a5284 | manifest.contract_role_count | — | iter_002 refine_output | closed | code | 0 | 002 | 003 |
| v_cff1b6 | g0.extract_failed | — | dump_api_docs.py | closed | code | 0 | 001 | 003 |

각 위반이 왜 문제인지는 규칙 카드의 문장이 고정한다.

| id | 왜 문제인가 | 주장한 수정 | 확인된 수정 |
| --- | --- | --- | --- |
| v_579dfe | [ERROR] COMPILATION ERROR :  [ERROR] /C:/Users/PC-220627-03/AppData/Local/Temp/claude/c--Users-PC-220627-03-Desktop-project-task-architecture-thinking/862b7ff3-a396-4dc6-9b71-a200dcde2544/scratchpad/runs/2026-09-29_b02d8689/output/src/main/java/com/thinking/tennis/app/AvailabilityServiceConstructorWiringConfiguration.java:[17,21] cannot find symbol   symbol:   method setAutowireMode(int)   location: interface org.springframework.beans.factory.config.BeanDefinition [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project tennis-alert-skeleton: Compilation failure [ERROR] /C:/Users/PC-220627-03/AppData/Local/Temp/claude/c--Users-PC-220627-03-Desktop-project-task-architecture-thinking/862b7ff3-a396-4dc6-9b71-a200dcde2544/scratchpad/runs/2026-09-29_b02d8689/output/src/main/java/com/thinking/tennis/app/AvailabilityServiceConstructorWiringConfiguration.java:[17,21] cannot find symbol [ERROR]   symbol:   method setAutowireMode(int) [ERROR]   location: interface org.springframework.beans.factory.config.BeanDefinition [ERROR] -> [Help 1] [ERROR]  [ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch. [ERROR] Re-run Maven using the -X switch to enable full debug logging. [ERROR]  [ERROR] For more information about the errors and possible solutions, please read the following articles: [ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException | — | — |
| v_271ee5 | manifest.contract_version_missing: contract/tennis-alert-api-v1.yaml is not in the file manifest | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_459e29 | 색인에 없는 파일이 작업 폴더에서 바뀌었다. 판정 대상 밖에 조용히 놓인 파일은 판정되지 않은 표면이 된다. | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_5a5284 | manifest.contract_role_count: expected exactly one file with role=contract, found [] | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |
| v_cff1b6 | 부팅이 실패했거나 제한 시간 안에 포트를 열지 못했다: 	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:625) ~[spring-context-6.1.13.jar!/:6.1.13] 	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.3.4.jar!/:3.3.4] 	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.3.4.jar!/:3.3.4] 	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.3.4.jar!/:3.3.4] 	at org.springframework.boot.SpringApplication.run(SpringApplication.java:335) ~[spring-boot-3.3.4.jar!/:3.3.4] 	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1363) ~[spring-boot-3.3.4.jar!/:3.3.4] 	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1352) ~[spring-boot-3.3.4.jar!/:3.3.4] 	at com.thinking.tennis.TennisAlertApplication.main(TennisAlertApplication.java:17) ~[!/:1.0.0] 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na] 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na] 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na] 	at java.base/java.lang.reflect.Method.invoke(Method.java:569) ~[na:na] 	at org.springframework.boot.loader.launch.Launcher.launch(Launcher.java:102) ~[tennis-alert-skeleton-1.0.0.jar:1.0.0] 	at org.springframework.boot.loader.launch.Launcher.launch(Launcher.java:64) ~[tennis-alert-skeleton-1.0.0.jar:1.0.0] 	at org.springframework.boot.loader.launch.JarLauncher.main(JarLauncher.java:40) ~[tennis-alert-skeleton-1.0.0.jar:1.0.0] Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'alertService' defined in URL [jar:nested:/C:/Users/PC-220627-03/AppData/Local/Temp/claude/c--Users-PC-220627-03-Desktop-project-task-architecture-thinking/862b7ff3-a396-4dc6-9b71-a200dcde2544/scratchpad/runs/2026-09-29_b02d8689/output/target/tennis-alert-skeleton-1.0.0.jar/!BOOT-INF/classes/!/com/thinking/tennis/app/AlertService.class]: Failed to instantiate [com.thinking.tennis.app.AlertService]: No default constructor found 	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateBean(AbstractAutowireCapableBeanFactory.java:1337) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1222) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:337) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:335) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:254) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1443) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1353) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:904) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:782) ~[spring-beans-6.1.13.jar!/:6.1.13] 	... 26 common frames omitted Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.thinking.tennis.app.AlertService]: No default constructor found 	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:90) ~[spring-beans-6.1.13.jar!/:6.1.13] 	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateBean(AbstractAutowireCapableBeanFactory.java:1331) ~[spring-beans-6.1.13.jar!/:6.1.13] 	... 38 common frames omitted Caused by: java.lang.NoSuchMethodException: com.thinking.tennis.app.AlertService.<init>() 	at java.base/java.lang.Class.getConstructor0(Class.java:3587) ~[na:na] 	at java.base/java.lang.Class.getDeclaredConstructor(Class.java:2756) ~[na:na] 	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:86) ~[spring-beans-6.1.13.jar!/:6.1.13] 	... 39 common frames omitted --- 로그 끝 --- | — | 이 위반의 좌표를 언급하는 파일 변경이 없다 |

열린 위반과 무관한 파일이 달라진 자리다. 막지 않고 기록만 한다.

| 파일 | iteration | 관찰 |
| --- | --- | --- |
| src/main/java/com/thinking/tennis/app/AlertService.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/SpringConstructorWiringConfiguration.java | 002 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |
| src/main/java/com/thinking/tennis/app/AvailabilityServiceConstructorWiringConfiguration.java | 003 | 열린 위반의 좌표를 언급하지 않는 파일이 달라졌다 |

## 6. 주장과 판정이 어긋난 곳

모델이 스스로 한 말과 장치의 판정이 갈린 자리를 모은다. 이 절이 비면 둘이 같은 말을 했다는 뜻이다.

어긋난 자리는 모두 70건이다.

### 충족 주장과 게이트 판정 (61건)

주장은 판정이 아니다. 어긋난 자리만 남긴다.

| iteration | point | 관찰 |
| --- | --- | --- |
| 001 | getCourtAvailability:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:502 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:504 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:COURT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:UPSTREAM_RESPONSE_UNREADABLE | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:UPSTREAM_UNAVAILABLE | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:UPSTREAM_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getCourtAvailability:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | listAlerts:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:201 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:409 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:422 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:VALIDATION_FAILED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:UNAUTHENTICATED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:IDEMPOTENCY_KEY_REUSED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:COURT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:SLOT_NOT_SUPPORTED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:ALERT_WINDOW_CLOSED | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:CONCURRENT_UPDATE_CONFLICT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | createAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | getAlert:ALERT_NOT_FOUND | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:200 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:400 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:401 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:404 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:500 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:503 | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:ALERT_NOT_FOUND | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:CONCURRENT_UPDATE_CONFLICT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:STORAGE_TIMEOUT | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | cancelAlert:INTERNAL_ERROR | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 001 | contract:prose | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 002 | boot:alertService | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 003 | boot:alertService | G1이 미판정이라 이 주장을 대조할 수 없다 |
| 003 | boot:availabilityService | G1이 미판정이라 이 주장을 대조할 수 없다 |

### 말없는 되돌림 (9건)

서 있는 결정을 바꾸려면 `supersedes`에 이전 id와 이유를 적어야 한다.

| iteration | decision_id | 관찰 |
| --- | --- | --- |
| 002 | d_clock_source | 서 있던 결정 d_clock_source이 supersedes 없이 사라졌다 |
| 002 | d_user_identity | 서 있던 결정 d_user_identity이 supersedes 없이 사라졌다 |
| 002 | d_availability_validation_source | 서 있던 결정 d_availability_validation_source이 supersedes 없이 사라졌다 |
| 002 | d_refresh_failure_coalescing | 서 있던 결정 d_refresh_failure_coalescing이 supersedes 없이 사라졌다 |
| 002 | d_condition_atomicity | 서 있던 결정 d_condition_atomicity이 supersedes 없이 사라졌다 |
| 002 | d_lazy_expiry | 서 있던 결정 d_lazy_expiry이 supersedes 없이 사라졌다 |
| 002 | d_stale_creation_validation | 서 있던 결정 d_stale_creation_validation이 supersedes 없이 사라졌다 |
| 002 | d_problem_trace_generation | 서 있던 결정 d_problem_trace_generation이 supersedes 없이 사라졌다 |
| 002 | d_validation_precedence | 서 있던 결정 d_validation_precedence이 supersedes 없이 사라졌다 |
