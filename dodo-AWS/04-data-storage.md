# 04. 데이터 저장소: RDS, S3

## 판단 질문

서비스의 데이터는 어디에 저장되고, 장애나 삭제에 대비되어 있는가?

## 기본 개념

데이터 저장소는 장애 영향이 가장 큰 영역이다. 처음 보는 계정에서는 변경보다 확인이 우선이다.

현재 RDS PostgreSQL 인스턴스 `dodoclass-pg` 1개와 S3 버킷 5개가 확인되었다.

## 현재 확인된 RDS 구성

### dodoclass-pg

```text
DB identifier: dodoclass-pg
역할: 인스턴스
상태: 사용 가능
Engine: PostgreSQL
Engine version: 18.2
Instance class: db.t4g.micro
vCPU / RAM: 2 vCPU / 1 GB
Region / AZ: ap-northeast-2b
현재 연결: 6 connections
CPU: 약 3.19%
Endpoint: dodoclass-pg.c3c84k6a2gyb.ap-northeast-2.rds.amazonaws.com
Port: 5432
Database name: dodoclass
DB user: dodoclass
Internet access gateway: 비활성화됨
IAM 인증: 비활성화됨
IAM Role: 없음
Multi-AZ: 아니오
Deletion protection: 비활성화됨
Option group: default:postgres-18
Parameter group: default.postgres18
Created: 2026-03-17 16:40 KST
권장 사항: 3개 있음
```

현재 화면 기준으로 RDS는 `사용 가능` 상태이며 PostgreSQL 엔진을 사용한다. 연결 예시에는 `sslmode=verify-full`과 RDS CA bundle을 사용하는 방식이 표시되어 있으므로, 클라이언트 연결 시 TLS 검증을 사용하는 구성이 권장되는 것으로 보인다.

```text
psql "host=$RDSHOST port=5432 dbname=dodoclass user=dodoclass sslmode=verify-full sslrootcert=./global-bundle.pem"
```

### 스토리지와 모니터링

```text
Storage type: 범용 SSD(gp2)
Allocated storage: 20 GiB
Storage autoscaling: 활성화됨
Max storage threshold: 50 GiB
Encryption: 활성화됨
KMS key: AWS managed key aws/rds
Database Insights: 표준
Performance Insights: 비활성화됨
Enhanced monitoring: 비활성화됨
```

스토리지 자동 조정은 켜져 있으므로 사용량이 늘면 최대 50 GiB까지 확장될 수 있다. 다만 Multi-AZ가 꺼져 있고 deletion protection도 꺼져 있으므로 장애 복원력과 실수 방지 관점에서는 추가 확인이 필요하다.

현재 화면에는 `DB 인스턴스 파라미터 그룹`은 보이지만, `DB subnet group`과 VPC 정보는 보이지 않는다. RDS의 VPC와 subnet group은 보통 `연결 및 보안` 탭의 네트워크 영역에서 확인한다.

### Security Group 규칙

```text
Inbound:
- cemware-vpc-peering (sg-051e2f9d41154e6a5): 172.31.0.0/16
- dodoclass-rds-20260317073654296400000001 (sg-08a4bdd58f234fa6b): 10.0.0.0/16

Outbound:
- dodoclass-rds-20260317073654296400000001 (sg-08a4bdd58f234fa6b): 0.0.0.0/0
```

RDS 인바운드는 전체 인터넷이 아니라 `10.0.0.0/16`과 `172.31.0.0/16` 대역에서 들어오는 트래픽을 허용한다. 즉 `dodoclass-vpc` 내부 리소스와 peering 대상 VPC 리소스가 DB에 접근할 수 있는 구조로 보인다.

다만 이 규칙은 Security Group 참조가 아니라 CIDR 대역 허용이다. 해당 VPC 대역 안의 어떤 리소스든 DB 포트 접근이 가능할 수 있으므로, 실제 포트와 소스 범위를 더 좁힐 수 있는지 검토해야 한다.

### 복제와 IAM

```text
복제: 1개 표시됨
복제 원본: 없음
복제 상태: 없음
IAM 역할: 없음
```

현재 화면의 복제 섹션에는 `dodoclass-pg`가 1개 표시되지만, 복제 원본과 복제 상태가 비어 있다. 읽기 복제본이 구성되어 있다고 단정하면 안 된다. Multi-AZ, read replica, 백업 보관 기간은 추가 탭에서 확인해야 한다.

## 현재 확인된 S3 버킷

현재 S3에는 서울 리전(`ap-northeast-2`)의 범용 버킷 5개가 있다.

```text
Bucket: dodoclass
Region: ap-northeast-2
Created: 2026-03-11 18:54:26 KST
용도: 확인 필요

Bucket: dodoclass-cdn
Region: ap-northeast-2
Created: 2026-03-20 10:54:35 KST
용도: CDN 정적 파일 저장소로 추정, 확인 필요

Bucket: dodoclass-logs
Region: ap-northeast-2
Created: 2026-03-19 11:57:33 KST
용도: 로그 저장소로 추정, 확인 필요

Bucket: dodoclass-maintenance
Region: ap-northeast-2
Created: 2026-03-19 12:25:25 KST
용도: 점검/maintenance 페이지 또는 관련 파일 저장소로 추정, 확인 필요

Bucket: dodoclass-terraform-state
Region: ap-northeast-2
Created: 2026-03-11 18:42:33 KST
용도: Terraform state 저장소로 추정
```

버킷 이름만으로 실제 용도를 확정하면 안 된다. 각 버킷의 `Properties`, `Permissions`, `Objects`, `Management` 탭에서 public access 차단, bucket policy, versioning, lifecycle, encryption, access logging을 확인해야 한다.

`dodoclass-terraform-state`는 Terraform state 저장소일 가능성이 높다. 이 버킷은 인프라 상태 정보를 담을 수 있으므로 삭제, 비우기, 퍼블릭 공개, 권한 변경을 특히 조심해야 한다.

## 확인할 화면

```text
RDS > Databases
RDS > Snapshots
S3 > Buckets
```

## RDS에서 확인할 것

- DB identifier
- Engine: MySQL, PostgreSQL 등
- Instance class
- Multi-AZ 여부
- Public access 여부
- VPC와 Subnet group
- Security Group
- Backup retention period
- Deletion protection
- Endpoint와 port
- IAM DB 인증 사용 여부
- Performance Insights 또는 모니터링 설정
- 권장 사항 3개 내용
- Storage autoscaling 최대값
- 암호화 KMS key

## S3에서 확인할 것

- Bucket 이름
- Public access 차단 여부
- Versioning 여부
- Lifecycle rule
- 어떤 서비스가 S3를 사용하는지
- Bucket policy
- Server-side encryption
- Access logging
- `dodoclass-terraform-state`의 versioning과 접근 권한

## 운영 포인트

- RDS public access가 켜져 있어도 Security Group이 막고 있으면 바로 공개는 아닐 수 있다. 반대로 Security Group까지 열려 있으면 위험하다.
- 백업이 없으면 장애 대응이 매우 어렵다.
- Deletion protection이 꺼져 있으면 실수로 삭제될 위험이 있다.
- S3 public access는 개별 버킷 정책까지 같이 봐야 한다.
- RDS Security Group이 CIDR 대역 `10.0.0.0/16`, `172.31.0.0/16`을 허용한다. 운영상 동작은 편하지만, 최소 권한 관점에서는 ECS task security group이나 필요한 소스만 허용하는 구조가 더 안전할 수 있다.
- DB endpoint, 사용자명, database name은 민감한 운영 정보이므로 외부 공유 문서에는 노출 범위를 조심해야 한다.
- RDS 권장 사항 3개는 성능, 보안, 비용 중 어떤 항목인지 확인해야 한다.
- 현재 RDS는 Multi-AZ가 꺼져 있다. 단일 AZ 장애가 발생하면 DB 가용성에 영향이 있을 수 있다.
- 현재 RDS는 deletion protection이 꺼져 있다. 실수 삭제 방지 관점에서는 위험할 수 있으므로 운영 정책에 맞는지 확인해야 한다.
- 스토리지 자동 조정은 켜져 있지만 최대값이 50 GiB이므로, 사용량 증가 추이를 모니터링해야 한다.
- `dodoclass-terraform-state`는 Terraform state 저장소일 가능성이 높으므로 versioning, encryption, public access block, 접근 권한을 우선 확인해야 한다.
- `dodoclass-cdn`이 실제 CDN 원본이라면 CloudFront와 연결되어 있는지 확인해야 한다.
- `dodoclass-logs`가 ALB/S3 로그 저장소라면 보관 기간과 lifecycle rule을 확인해야 한다.

## 기록 형식

```text
DB 이름: dodoclass-pg
엔진: PostgreSQL
엔진 버전: 18.2
클래스: db.t4g.micro
vCPU/RAM: 2 vCPU / 1 GB
AZ: ap-northeast-2b
Endpoint: dodoclass-pg.c3c84k6a2gyb.ap-northeast-2.rds.amazonaws.com
Port: 5432
Database/User: dodoclass / dodoclass
Storage: gp2 20 GiB, autoscaling max 50 GiB
Encryption: enabled, aws/rds
Multi-AZ: 아니오
Deletion protection: 비활성화됨
Security Group:
- sg-051e2f9d41154e6a5: 172.31.0.0/16 inbound
- sg-08a4bdd58f234fa6b: 10.0.0.0/16 inbound, 0.0.0.0/0 outbound
접근하는 애플리케이션: 확인 필요
백업: 확인 필요
삭제 보호:
```

```text
S3 Buckets:
- dodoclass
- dodoclass-cdn
- dodoclass-logs
- dodoclass-maintenance
- dodoclass-terraform-state
Region: ap-northeast-2
Public access block: 확인 필요
Versioning: 확인 필요
Encryption: 확인 필요
Lifecycle: 확인 필요
Bucket policy: 확인 필요
```

## 아직 모르는 것

- ECS와 RDS 연결 관계
- RDS VPC와 subnet group
- Public access 여부
- Backup retention period
- RDS Security Group의 실제 허용 포트
- 권장 사항 3개 내용
- 각 S3 버킷의 실제 용도
- 각 S3 버킷의 public access block 여부
- 각 S3 버킷의 versioning, encryption, lifecycle 설정
- `dodoclass-cdn`과 CloudFront 연결 여부
- `dodoclass-logs`에 어떤 로그가 저장되는지
