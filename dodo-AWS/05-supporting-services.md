# 05. 보조 서비스: ElastiCache, MQ

## 판단 질문

애플리케이션이 DB 외에 캐시나 메시지 큐를 사용하고 있는가?

## 기본 개념

ElastiCache는 보통 Redis/Memcached 캐시로 사용한다. Amazon MQ는 메시지를 임시로 저장하고 전달하는 큐 역할을 한다.

이 둘은 애플리케이션의 성능과 비동기 처리에 영향을 준다.

현재 AWS 계정에는 `nats-dodo-server`라는 EC2 인스턴스가 별도로 존재한다. 이름상 NATS 메시징 서버일 가능성이 있지만, 현재 EC2 요약 화면만으로 실제 NATS 서버라고 확정할 수는 없다.

## 현재 확인된 보조 서비스 후보

### nats-dodo-server

```text
서비스 후보: NATS
실행 위치: EC2
Instance name: nats-dodo-server
Instance ID: i-0e923f70e772480b4
Instance type: t3.nano
상태: 실행 중
VPC: vpc-05b8cced5f5c40bb1
Subnet: subnet-016d4334561f0a221
Private IPv4: 172.31.9.40
Public IPv4 / Elastic IP: 54.116.54.86
IAM Role: nats-dodo-server
```

`dodoclass-vpc`의 private subnet route table에는 `172.31.0.0/16 -> VPC Peering` 경로가 있다. 따라서 ECS 서비스나 다른 private 리소스가 이 EC2의 private IP `172.31.9.40`으로 접근하는 구조일 수 있다.

다만 아직 확인되지 않은 부분이 많다. 실제로 NATS가 실행 중인지, 어떤 포트를 사용하는지, ECS 서비스 환경변수에 이 주소가 들어 있는지 확인해야 한다.

## 확인할 화면

```text
ElastiCache > Redis caches
ElastiCache > Memcached caches
Amazon MQ > Brokers
EC2 > Instances > nats-dodo-server
```

## 확인할 것

- 엔진 종류: Redis, Memcached, ActiveMQ, RabbitMQ
- VPC와 Subnet group
- Security Group
- 연결 endpoint
- 어떤 ECS 서비스에서 접근하는지
- 장애 조치나 백업 설정이 있는지
- `nats-dodo-server`에서 실제 실행 중인 프로세스와 포트
- ECS Task Definition 환경변수에 `172.31.9.40` 또는 NATS endpoint가 있는지
- `nats-dodo-server` Security Group inbound가 어떤 소스에서 어떤 포트를 허용하는지

## 운영 포인트

- Redis는 캐시로만 쓰는지, 중요한 상태 저장소처럼 쓰는지 확인해야 한다.
- 캐시라고 해서 항상 삭제해도 되는 것은 아니다.
- MQ가 멈추면 비동기 작업, 알림, 주문 처리 같은 기능이 지연될 수 있다.
- Security Group에서 ECS만 접근하도록 제한되어 있는지 확인한다.
- `nats-dodo-server`는 Public IP가 있으므로 외부에서 직접 접근 가능한 포트가 열려 있는지 확인해야 한다.
- 이름이 NATS여도 실제 사용 여부는 ECS 환경변수, 애플리케이션 설정, 프로세스 확인 전까지 단정하면 안 된다.

## 기록 형식

```text
서비스 이름:
종류:
Endpoint:
VPC/Subnet:
Security Group:
접근하는 애플리케이션:
장애 영향:
```

```text
서비스 이름: nats-dodo-server
종류: NATS 추정
Endpoint:
- Private: 172.31.9.40
- Public: 54.116.54.86
VPC/Subnet:
- vpc-05b8cced5f5c40bb1
- subnet-016d4334561f0a221
Security Group: 확인 필요
접근하는 애플리케이션: 확인 필요
장애 영향: 확인 필요
```

## 아직 모르는 것

- ElastiCache 엔진
- Amazon MQ 브로커 종류
- ECS와의 연결 관계
- `nats-dodo-server`가 실제 NATS 서버인지 여부
- `nats-dodo-server`의 Security Group
- ECS 서비스가 사용하는 NATS endpoint
