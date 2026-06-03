# 07. 운영 확인: CloudWatch, 알람, 백업

## 판단 질문

장애가 났을 때 어디에서 확인하고, 어떤 기준으로 복구 판단을 할 수 있는가?

## 기본 개념

운영 관점에서 중요한 것은 리소스가 존재한다는 사실보다 다음 질문이다.

```text
문제가 생기면 알 수 있는가?
로그를 볼 수 있는가?
복구할 백업이 있는가?
누가 어떤 순서로 대응하는가?
```

## 확인할 화면

```text
CloudWatch > Log groups
CloudWatch > Alarms
CloudWatch > Metrics
CloudWatch > Dashboards
RDS > Snapshots
ECS > Services > Events
S3 > Buckets > dodoclass-terraform-state
S3 > Buckets > dodoclass-logs
Terraform 코드 저장소
Terraform state 저장 위치
```

## CloudWatch Logs에서 확인할 것

- ECS 로그 그룹 이름
- 애플리케이션 에러 로그 위치
- 로그 보관 기간
- 최근 에러 발생 여부

## CloudWatch Alarms에서 확인할 것

- ECS CPU/Memory 알람
- ALB 5xx 알람
- Target Group unhealthy 알람
- RDS CPU, Storage, Connection 알람
- RDS `dodoclass-pg` 연결 수, CPU, FreeStorageSpace 알람
- 알람 수신 대상

## 백업에서 확인할 것

- RDS 자동 백업 보관 기간
- 수동 스냅샷 존재 여부
- RDS `dodoclass-pg` 삭제 보호 여부
- RDS `dodoclass-pg` Multi-AZ 여부
- RDS `dodoclass-pg` 스토리지 자동 조정 최대값과 사용량 추이
- S3 versioning 여부
- S3 `dodoclass-terraform-state` versioning 여부
- S3 `dodoclass-terraform-state` encryption 여부
- 복구 절차 문서 존재 여부

## 배포와 변경 관리에서 확인할 것

- ECS Task Definition을 관리하는 Terraform 코드 위치
- Terraform state 저장 위치
- Terraform state S3 bucket: `dodoclass-terraform-state`로 추정
- Terraform plan/apply를 누가, 어디서 실행하는지
- 이미지 태그 변경 방식
- ECS Service 배포가 Terraform으로 발생하는지, 별도 CI/CD에서 발생하는지
- 콘솔에서 직접 변경한 drift가 있는지

## 운영 포인트

- 로그가 있어도 알람이 없으면 장애를 늦게 알 수 있다.
- 알람이 있어도 수신 대상이 퇴사자 이메일이면 운영상 의미가 없다.
- 백업은 존재 여부보다 복구 가능 여부가 중요하다.
- 복구 테스트 기록이 없으면 실제 장애 때 복구 시간을 예측하기 어렵다.
- ECS Task Definition은 Terraform으로 관리되므로 콘솔에서 직접 새 revision을 등록하거나 서비스 업데이트를 하면 drift가 생길 수 있다.
- 장애 대응 중 긴급 변경이 필요하더라도, 변경 후 Terraform 코드와 실제 AWS 상태를 반드시 맞춰야 한다.
- RDS `dodoclass-pg`에는 권장 사항 3개가 표시된다. 권장 사항이 보안, 백업, 성능, 비용 중 무엇인지 확인하고 영향도를 기록해야 한다.
- RDS `dodoclass-pg`는 Multi-AZ가 꺼져 있고 deletion protection도 꺼져 있다. 장애 복원력과 실수 삭제 방지 관점에서 운영 정책에 맞는지 확인해야 한다.
- RDS `dodoclass-pg`는 스토리지 자동 조정이 켜져 있고 최대 50 GiB까지 확장된다. FreeStorageSpace 알람이 필요하다.
- S3 `dodoclass-terraform-state`는 Terraform state 저장소일 가능성이 높다. versioning과 encryption이 꺼져 있으면 state 손상이나 유실 시 복구가 어려워질 수 있다.
- S3 `dodoclass-logs`가 로그 저장소라면 lifecycle rule과 보관 기간을 확인해야 한다.

## 기록 형식

```text
로그 그룹:
알람 이름:
알람 조건:
수신 대상:
백업 위치:
복구 절차:
Terraform 코드:
Terraform state: dodoclass-terraform-state 추정
배포 절차:
RDS 백업 보관 기간:
RDS 삭제 보호: 비활성화됨
RDS Multi-AZ: 아니오
RDS 권장 사항:
S3 로그 버킷:
S3 state 버킷:
```

## 아직 모르는 것

- ECS 로그 그룹
- 장애 알람 존재 여부
- RDS 백업 설정
- RDS `dodoclass-pg` 권장 사항 3개 내용
- RDS `dodoclass-pg` deletion protection 비활성화가 의도된 설정인지 여부
- RDS `dodoclass-pg` 단일 AZ 구성이 운영 정책상 허용되는지 여부
- 실제 장애 대응 절차
- Terraform 코드 저장소
- Terraform state 저장 위치와 state key
- `dodoclass-terraform-state` versioning/encryption 설정
- `dodoclass-logs`의 실제 로그 종류와 보관 기간
- ECS Task Definition 배포 절차
