package com.thinking.ticket;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 헥사고날 경계를 정적으로 검사한다. 인수테스트가 "동작하는가"를 본다면 이 테스트는
 * "의존 방향을 지켰는가"를 본다 — 동작이 맞아도 경계를 넘었으면 여기서 빨간불이 난다.
 *
 * <p>경계 점검을 사람의 리뷰가 아니라 테스트로 두는 이유는 셋이다.
 * <ul>
 *   <li>재현 가능하다. "위반 같다"는 의견이지만 빨간불은 사실이다.
 *   <li>실패 메시지를 그대로 되먹여 재작업을 지시할 수 있다.
 *   <li>어떤 경계 점검을 실제로 적용했는지가 코드로 남는다.
 * </ul>
 */
class HexagonalBoundaryTest {

    private static final String CORE = "com.thinking.ticket.core..";
    private static final String ADAPTER = "com.thinking.ticket.adapter..";

    private static JavaClasses classes;

    @BeforeAll
    static void importProductionClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.thinking.ticket");
    }

    @Test
    @DisplayName("검사 대상이 실제로 로드됐다 — 규칙이 공허하게 통과하지 않도록")
    void 규칙이_공허하지_않다() {
        // ArchUnit은 대상 클래스가 하나도 없으면 모든 금지 규칙을 통과시킨다.
        // Core는 항상 존재하는 계약이므로 여기서 최소 로드를 확인해 공허한 초록불을 막는다.
        assertThat(classes.that(resideInAPackage(CORE))).isNotEmpty();
    }

    @Test
    @DisplayName("Core는 Adapter를 모른다 — 의존은 항상 안쪽을 향한다")
    void core는_adapter를_모른다() {
        noClasses().that().resideInAPackage(CORE)
                .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                .because("의존 방향이 바깥으로 새면 어댑터를 교체할 때 Core를 고쳐야 한다")
                .check(classes);
    }

    @Test
    @DisplayName("Core는 프레임워크를 모른다 — Spring 타입 침투 금지")
    void core는_spring을_모른다() {
        noClasses().that().resideInAPackage(CORE)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .because("Core가 프레임워크에 묶이면 프레임워크 없이 도메인을 검증할 수 없다")
                .check(classes);
    }

    @Test
    @DisplayName("Core는 영속 기술을 모른다 — JPA 타입 침투 금지")
    void core는_jpa를_모른다() {
        noClasses().that().resideInAPackage(CORE)
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
                .because("도메인 모델과 영속 모델이 한 클래스가 되면 저장 기술이 도메인을 규정한다")
                .check(classes);
    }

    @Test
    @DisplayName("Inbound Adapter는 Outbound Adapter를 모른다 — 포트를 건너뛰지 않는다")
    void inbound는_outbound를_모른다() {
        // allowEmptyShould: 골격을 층 단위로 세우는 동안 인바운드 층이 아직 없을 수 있다.
        // 그때 이 규칙이 실패하면 "위반"이 아니라 "아직 그 층이 없음"을 빨간불로 오인하게 된다.
        // 층이 없을 때는 위반할 수도 없으므로 통과가 옳다. Core는 항상 존재하며 위 가드가 지킨다.
        noClasses().that().resideInAPackage("com.thinking.ticket.adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("com.thinking.ticket.adapter.out..")
                .because("인바운드가 아웃바운드를 직접 부르면 유스케이스를 건너뛴 우회 경로가 생긴다")
                .allowEmptyShould(true)
                .check(classes);
    }
}
