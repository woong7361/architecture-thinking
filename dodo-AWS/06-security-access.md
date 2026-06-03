# 06. 접근 제어와 보안: Security Group, IAM

## 판단 질문

누가 AWS 리소스에 접근할 수 있고, 리소스끼리는 어떤 통신이 허용되어 있는가?

## 기본 개념

Security Group은 AWS 리소스 앞의 방화벽이다. IAM은 AWS API와 콘솔을 사용할 수 있는 권한이다.

둘은 다르다.

```text
Security Group = 네트워크 접근 제어
IAM = AWS 작업 권한 제어
```

## Security Group 확인 화면

```text
EC2 > Security Groups
```

## 확인할 것

- `0.0.0.0/0`으로 열린 포트가 있는가?
- SSH `22`, RDP `3389`, DB 포트 `3306`, `5432`, Redis `6379`가 전체 공개되어 있지 않은가?
- ALB, ECS, RDS, Redis용 Security Group이 분리되어 있는가?
- 인바운드 규칙이 IP가 아니라 다른 Security Group을 참조하는가?

## 좋은 구조 예시

```text
ALB Security Group
- 80/443 from 0.0.0.0/0

ECS Security Group
- application port from ALB Security Group

RDS Security Group
- DB port from ECS Security Group

Redis Security Group
- 6379 from ECS Security Group
```

## 현재 확인된 Security Group 단서

### RDS dodoclass-pg

```text
RDS: dodoclass-pg
Engine: PostgreSQL
Port: 5432

Inbound:
- 172.31.0.0/16
- 10.0.0.0/16

Outbound:
- 0.0.0.0/0
```

현재 RDS는 전체 인터넷이 아니라 VPC 내부 대역과 peering 대상 대역에서 접근을 허용하는 것으로 보인다. 다만 Security Group 참조가 아니라 CIDR 대역 기반 허용이므로, 같은 대역 안의 다른 리소스도 DB 접근이 가능할 수 있다.

운영 관점에서는 `ECS task security group -> RDS security group`처럼 필요한 소스 보안 그룹만 허용하는 구조가 더 명확하다. 다만 기존 서비스 의존 관계를 확인하기 전에는 규칙을 수정하면 안 된다.

## IAM 확인 화면

```text
IAM > Users
IAM > Roles
IAM > Policies
IAM > Access analyzer
```

## IAM에서 확인할 것

- root 계정 MFA 여부
- 오래된 Access Key
- 사용하지 않는 사용자
- AdministratorAccess가 과도하게 부여되어 있는지
- ECS Task Role에 필요한 권한만 있는지

## 운영 포인트

- Security Group 이름만 믿으면 안 된다. 실제 인바운드/아웃바운드 규칙을 봐야 한다.
- 처음에는 보안 규칙을 바로 삭제하지 않는다. 먼저 어떤 서비스가 사용하는지 확인한다.
- IAM 권한은 삭제보다 비활성화, 영향 확인, 문서화 순서로 접근한다.
- RDS처럼 데이터 저장소에 연결되는 규칙은 변경 전에 ECS, EC2, peering VPC에서 실제로 누가 접근하는지 확인해야 한다.

## 아직 모르는 것

- ALB/ECS/RDS/Redis Security Group 이름
- 외부에 열린 포트
- IAM 사용자와 역할 현황
- RDS `dodoclass-pg`가 허용하는 실제 포트와 각 Security Group 상세 규칙
- `10.0.0.0/16`, `172.31.0.0/16` 접근 허용이 필요한 전체 리소스 목록
