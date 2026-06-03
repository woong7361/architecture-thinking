# 03. 애플리케이션 실행 위치: ECS, EC2, Lambda

## 판단 질문

실제 애플리케이션은 어디에서 실행되고 있는가?

## 기본 개념

Compute는 애플리케이션 코드가 실행되는 곳이다.

현재 애플리케이션은 ECS Fargate에서 실행되는 것으로 확인되었다.

```text
User
-> ALB dodoclass
-> Target Group
-> ECS Cluster dodoclass-staging
-> ECS Fargate Service
-> Task
```

Fargate는 EC2 인스턴스를 직접 운영하지 않고 컨테이너를 실행하는 방식이다. 따라서 ECS 클러스터에 등록된 컨테이너 인스턴스가 없어도, Fargate 서비스가 정상 실행 중이면 문제가 아니다.

## 현재 확인된 ECS 구성

### Cluster

```text
Cluster 이름: dodoclass-staging
상태: 활성
Region: ap-northeast-2
Container Insights: 켜짐
등록된 컨테이너 인스턴스: 없음
Service 수: 2
실행 중 task 수: 2
```

`등록된 컨테이너 인스턴스`가 없는 것은 Fargate 방식에서는 정상이다. EC2 시작 유형을 쓰는 클러스터라면 컨테이너 인스턴스가 필요하지만, 현재 서비스는 Fargate로 실행된다.

### Service

```text
Service: dodoclass-staging-api
상태: 활성
스케줄링 전략: 복제본
시작 유형: Fargate
Task Definition: dodoclass-staging-api:24
플랫폼 버전: LATEST
플랫폼 패밀리: Linux
Task: 1/1 실행 중
마지막 배포: 완료됨
연결된 Target Group: dodoclass-staging-api
VPC: vpc-0a37cdcb3f351835c
Subnets:
- subnet-0ef0a980ff8eae441
- subnet-07f24fa1d105ec101
Security Group: sg-0feb0171e115e8c56
Public IP 자동 지정: 켜짐

Service: dodoclass-staging-web
상태: 활성
스케줄링 전략: 복제본
시작 유형: Fargate
Task Definition: dodoclass-staging-web:17
플랫폼 버전: 1.4.0
플랫폼 패밀리: Linux
Task: 1/1 실행 중
마지막 배포: 완료됨
연결된 Target Group: dodoclass-staging-web
VPC: vpc-0a37cdcb3f351835c
Subnets:
- subnet-059a770b9ceaa77dc
- subnet-0ef0a980ff8eae441
- subnet-03d86ae13f982fa30
- subnet-07f24fa1d105ec101
Security Group: sg-0feb0171e115e8c56
Public IP 자동 지정: 켜짐
```

현재 Desired task와 Running task가 각각 `1/1`로 맞기 때문에, ECS 서비스 수량 관점에서는 정상이다. 다만 이것은 컨테이너가 떠 있다는 뜻이지, 애플리케이션 기능과 DB 연결까지 정상이라는 뜻은 아니다.

ECS Task Definition은 Terraform으로 관리되는 것으로 확인되었다. 따라서 콘솔에서 Task Definition을 직접 수정하거나 새 revision을 수동 등록하면 Terraform 코드와 실제 AWS 상태가 어긋나는 drift가 생길 수 있다.

두 서비스 모두 생성자가 `arn:aws:iam::374604322063:user/opentofu`로 표시된다. 실제 운영 변경은 Terraform 코드와 배포 절차를 기준으로 확인해야 한다.

두 서비스 모두 Public IP 자동 지정이 켜져 있다. Public IP가 켜져 있다고 해서 곧바로 외부에서 접근 가능하다는 뜻은 아니지만, task security group inbound가 넓게 열려 있으면 ALB를 거치지 않는 직접 접근 경로가 생길 수 있다.

## 현재 확인된 EC2 인스턴스

ECS Fargate 서비스와 별개로 EC2 인스턴스 `nats-dodo-server`가 실행 중인 것으로 확인되었다.

```text
Instance name: nats-dodo-server
Instance ID: i-0e923f70e772480b4
상태: 실행 중
Instance type: t3.nano
Region: ap-northeast-2
VPC: vpc-05b8cced5f5c40bb1
Subnet: subnet-016d4334561f0a221
Availability Zone: ap-northeast-2a
Private IPv4: 172.31.9.40
Public IPv4: 54.116.54.86
Elastic IP: 54.116.54.86
Public DNS: ec2-54-116-54-86.ap-northeast-2.compute.amazonaws.com
IAM Role: nats-dodo-server
Auto Scaling Group: 없음
Managed: false
IMDSv2: Optional
```

이 인스턴스는 ECS 클러스터 `dodoclass-staging`에 등록된 컨테이너 인스턴스가 아니다. 따라서 ECS 서비스의 실행 노드로 해석하면 안 된다.

Private IP가 `172.31.9.40`이고 VPC가 `vpc-05b8cced5f5c40bb1`이므로, `dodoclass-vpc`의 private subnet route table에 있는 `172.31.0.0/16 -> VPC Peering` 경로와 관련된 별도 VPC의 리소스로 보인다.

이름상 NATS 서버일 가능성이 있지만, 현재 화면만으로 실제 NATS 프로세스가 실행 중인지, 애플리케이션이 어떤 포트로 접근하는지는 확정할 수 없다. 보조 서비스 문서에서 별도로 추적한다.

## 확인할 화면

```text
ECS > Clusters
ECS > Task definitions
EC2 > Instances
Lambda > Functions
```

## ECS에서 확인할 것

- Cluster 이름
- Service 이름
- Desired tasks / Running tasks
- Launch type: Fargate 또는 EC2
- Task Definition
- Container image
- Environment variables
- 연결된 Load Balancer와 Target Group
- 로그 설정
- Task가 배치된 subnet
- Task에 연결된 Security Group
- Public IP 할당 여부

## 운영 포인트

- Desired tasks와 Running tasks가 다르면 서비스가 정상 유지되지 못하는 상태일 수 있다.
- Task가 계속 재시작되면 CloudWatch Logs에서 애플리케이션 에러를 봐야 한다.
- Task Definition을 변경하면 새 배포가 발생할 수 있으므로 처음에는 수정하지 않는다.
- 환경변수에는 DB 주소, Redis 주소, MQ 주소 같은 연결 정보가 들어 있을 수 있다.
- Fargate 서비스는 EC2 인스턴스가 없어도 정상일 수 있다.
- `1/1 running`은 최소 실행 수량이 맞다는 뜻이지, 서비스 기능 전체가 정상이라는 뜻은 아니다.
- ALB Target Group Health와 CloudWatch Logs를 함께 봐야 실제 서비스 상태를 판단할 수 있다.
- Public IP 자동 지정이 켜진 Fargate task는 task security group inbound rule을 반드시 확인해야 한다.
- ECS Task Definition은 Terraform으로 관리되므로 콘솔에서 직접 수정하지 않는다.
- 생성자가 `opentofu`로 표시되므로 실제 Terraform 실행 주체와 코드 저장소를 확인해야 한다.
- 서비스 업데이트는 새 배포를 유발할 수 있으므로, 처음 인수인계 단계에서는 읽기 위주로 확인한다.
- `nats-dodo-server`는 Public IP와 Elastic IP가 있으므로 Security Group inbound rule을 확인해야 한다.
- `nats-dodo-server`의 IMDSv2가 Optional이므로, 운영 보안 기준에서는 Required로 전환 가능한지 검토해야 한다. 단, 먼저 애플리케이션 영향 여부를 확인해야 한다.

## 기록 형식

```text
Cluster: dodoclass-staging
Service:
- dodoclass-staging-api
- dodoclass-staging-web
Task Definition:
- dodoclass-staging-api:24
- dodoclass-staging-web:17
실행 방식: Fargate
실행 개수:
- api: 1/1 running
- web: 1/1 running
Load Balancer 연결: ALB dodoclass
Target Group:
- dodoclass-staging-api
- dodoclass-staging-web
Service Security Group: sg-0feb0171e115e8c56
Public IP 자동 지정: 켜짐
로그 그룹:
주요 환경변수:
```

## 아직 모르는 것

- 컨테이너 이미지 저장소
- 배포 방식
- CloudWatch Logs 로그 그룹
- Task Security Group `sg-0feb0171e115e8c56` inbound rule
- 주요 환경변수
- Terraform 코드 위치
- Terraform state 관리 위치
- ECS Task Definition 변경 및 배포 절차
- `nats-dodo-server`의 Security Group
- `nats-dodo-server`에서 실제 실행 중인 서비스와 포트
- ECS 서비스가 `nats-dodo-server`에 접근하는지 여부
