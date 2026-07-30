# syntax=docker/dockerfile:1

# ── 1) 빌드 스테이지 ──────────────────────────────────────────────────────────
# JDK/Maven 버전을 이미지로 고정한다 → 로컬 JAVA_HOME/Maven 차이를 제거(재현성의 핵심).
# 이미지 빌드는 '실행 가능한 앱을 패키징'만 한다. 테스트 실행(인수테스트 + Testcontainers)은
# CI(GitHub Actions)에서 다룬다 — Task C-5.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 의존성 레이어 캐싱: pom만 먼저 복사해 오프라인 준비(소스만 바뀌면 이 레이어는 재사용).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B clean package -DskipTests

# ── 2) 런타임 스테이지 ────────────────────────────────────────────────────────
# JRE만 담은 슬림 이미지. curl은 compose 헬스체크(/actuator/health)용.
FROM eclipse-temurin:17-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/target/ticket-reservation-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
