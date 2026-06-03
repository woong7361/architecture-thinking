# 00. AWS를 처음 파악하는 방법

## 판단 질문

인수인계를 받지 못한 AWS 계정을 처음 볼 때, 무엇부터 확인해야 전체 구조를 빠르게 이해할 수 있는가?

## 전제 상황

- AWS 콘솔 접근 권한만 있다.
- 기존 담당자의 설명이나 구조도가 없다.
- 인프라를 처음 배우는 주니어지만, 실제 운영 상황에 대응해야 한다.
- 따라서 목표는 "모든 설정 이해"가 아니라 "운영 가능한 구조 파악"이다.

## 파악 순서

### 1. 외부 진입점 찾기

먼저 사용자가 어디로 들어오는지 찾는다.

확인 화면:

```text
Route 53
CloudFront
API Gateway
EC2 > Load Balancers
```

확인할 것:

- 도메인이 어디로 연결되는가?
- Load Balancer가 있는가?
- 80, 443 포트를 받는가?
- 요청을 어떤 Target Group으로 보내는가?

운영 포인트:

- 외부 진입점은 장애가 나면 사용자가 바로 체감하는 지점이다.
- 여기서부터 추적하면 실제 애플리케이션까지 이어지는 흐름을 찾기 쉽다.

### 2. 실제 애플리케이션 위치 찾기

다음은 요청을 실제로 처리하는 서버나 컨테이너를 찾는다.

확인 화면:

```text
ECS > Clusters
EC2 > Instances
Lambda > Functions
```

확인할 것:

- 어떤 서비스가 실행 중인가?
- 실행 개수는 몇 개인가?
- 어떤 이미지나 코드가 배포되어 있는가?
- Load Balancer와 연결되어 있는가?

운영 포인트:

- 장애 대응 시 "어떤 서비스가 죽었는지"를 알아야 한다.
- ECS라면 Cluster, Service, Task Definition, Task 상태를 봐야 한다.

### 3. 네트워크 경로 확인

애플리케이션이 어떤 네트워크 안에서 실행되는지 확인한다.

확인 화면:

```text
VPC > Your VPCs
VPC > Subnets
VPC > Route Tables
VPC > NAT Gateways
VPC > Endpoints
EC2 > Security Groups
```

확인할 것:

- public subnet과 private subnet이 나뉘어 있는가?
- private subnet에서 외부로 나갈 수 있는가?
- Security Group이 어떤 접근을 허용하는가?
- DB나 캐시가 외부에 직접 노출되어 있지는 않은가?

운영 포인트:

- 네트워크는 "누가 누구에게 접근할 수 있는가"를 설명한다.
- 이름만 보고 판단하면 안 된다. 반드시 라우팅과 보안그룹 규칙을 같이 봐야 한다.

### 4. 데이터 저장소 확인

애플리케이션이 사용하는 데이터 저장소를 찾는다.

확인 화면:

```text
RDS > Databases
S3 > Buckets
DynamoDB > Tables
ElastiCache
Amazon MQ
```

확인할 것:

- 어떤 DB 엔진을 쓰는가?
- public access가 켜져 있는가?
- 백업은 켜져 있는가?
- 어떤 Security Group에서 접근을 허용하는가?

운영 포인트:

- DB, 캐시, 큐는 삭제하거나 설정을 바꾸면 장애 영향이 크다.
- 처음에는 읽기 위주로 확인하고, 변경은 충분히 이해한 뒤 해야 한다.

### 5. 로그와 알람 확인

장애가 났을 때 어디서 확인해야 하는지 찾는다.

확인 화면:

```text
CloudWatch > Log groups
CloudWatch > Alarms
CloudWatch > Dashboards
```

확인할 것:

- ECS 로그가 어디 쌓이는가?
- 애플리케이션 에러 로그를 볼 수 있는가?
- CPU, Memory, DB 연결 수, 디스크 사용량 알람이 있는가?

운영 포인트:

- 운영에서 중요한 것은 "문제가 생겼을 때 알 수 있는가"이다.
- 로그가 있어도 보관 기간이 너무 짧거나 알람이 없으면 대응이 늦어진다.

## 최종 산출물

각 서비스를 확인하면서 아래 형식으로 기록한다.

```text
서비스 이름:
역할:
연결 대상:
콘솔 위치:
운영 확인 포인트:
아직 모르는 것:
```

최종적으로 다음 그림을 완성하는 것이 1차 목표다.

```text
User
-> DNS / CloudFront / Load Balancer
-> ECS / EC2 / Lambda
-> RDS / ElastiCache / MQ / S3
-> CloudWatch Logs / Alarms
```

