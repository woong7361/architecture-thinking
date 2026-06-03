# dodo AWS 인수인계 문서

이 문서는 AWS 콘솔만 받은 상태에서 인프라 구조를 역으로 파악하기 위한 작업 기록이다.

목표는 모든 AWS 서비스를 한 번에 외우는 것이 아니라, 다음 질문에 답할 수 있는 구조도를 만드는 것이다.

```text
사용자는 어디로 들어오는가?
실제 애플리케이션은 어디서 실행되는가?
데이터는 어디에 저장되는가?
각 리소스는 서로 어떻게 연결되는가?
장애가 나면 어디서 확인하고 어떻게 복구하는가?
```

## 문서 목록

- [00. AWS를 처음 파악하는 방법](./00-aws-reading-guide.md)
- [01. VPC와 네트워크](./01-vpc-network.md)
- [02. 외부 진입점: DNS, Load Balancer](./02-entrypoints.md)
- [03. 애플리케이션 실행 위치: ECS, EC2, Lambda](./03-compute.md)
- [04. 데이터 저장소: RDS, S3](./04-data-storage.md)
- [05. 보조 서비스: ElastiCache, MQ](./05-supporting-services.md)
- [06. 접근 제어와 보안: Security Group, IAM](./06-security-access.md)
- [07. 운영 확인: CloudWatch, 알람, 백업](./07-operations.md)

## 현재까지의 큰 추정

현재 비용표와 VPC 화면 기준으로는 다음 구조일 가능성이 있다.

```text
User
-> Load Balancer
-> ECS
-> RDS / ElastiCache / MQ
-> S3
```

단, 이것은 아직 추정이다. 실제 구조는 Load Balancer, ECS 서비스, Security Group, RDS 연결 정보를 확인해야 확정할 수 있다.

