package com.thinking.tennis.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 패키지 경계를 정적으로 검사한다.
 *
 * <p>이 테스트는 사람이 소유한다. 생성 코드는 이 파일을 고치지 않는다. 경계 위반을 리뷰어의 눈이 아니라
 * 빨간불로 판정하려는 것이고, 실패 메시지를 그대로 생성기에 되먹일 수 있어야 한다.
 *
 * <p>규칙마다 {@code allowEmptyShould(true)} 를 켠다. ArchUnit은 검사 대상이 하나도 없으면 규칙이 헛돈
 * 것으로 보고 실패시키는데, 이 스켈레톤은 생성 코드가 아직 없는 빈 상태로도 돌아야 한다.
 * 빈 상태에서 빨간불이 켜지면 "경계를 어겼다"와 "아직 아무것도 없다"를 구별할 수 없다.
 */
class LayerBoundaryTest {

    /** HTTP와 직렬화 기술의 어휘. 안쪽이 이것을 참조하면 기술이 업무 규칙으로 번진다. */
    private static final String[] TECHNOLOGY_PACKAGES = {
            "jakarta.servlet..",
            "org.springframework.web..",
            "com.fasterxml.jackson.."
    };

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.thinking.tennis");
    }

    @Test
    @DisplayName("domain은 HTTP·직렬화 타입을 참조하지 않는다")
    void domainDoesNotDependOnHttpOrSerialization() {
        check(noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(TECHNOLOGY_PACKAGES)
                .because("도메인은 요청을 받는 방식과 직렬화 형식을 모르는 채로 실행되고 테스트돼야 한다"));
    }

    @Test
    @DisplayName("app은 HTTP·직렬화 타입을 참조하지 않는다")
    void appDoesNotDependOnHttpOrSerialization() {
        check(noClasses()
                .that().resideInAPackage("..app..")
                .should().dependOnClassesThat().resideInAnyPackage(TECHNOLOGY_PACKAGES)
                .because("유스케이스는 포트를 엮는 순서만 소유하므로 프로토콜과 표현 형식을 몰라야 한다"));
    }

    @Test
    @DisplayName("api는 adapter를 참조하지 않는다")
    void apiDoesNotDependOnAdapter() {
        check(noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("인바운드 어댑터가 아웃바운드 어댑터를 직접 부르면 유스케이스를 건너뛰는 우회 경로가 생긴다"));
    }

    @Test
    @DisplayName("domain은 api를 참조하지 않는다")
    void domainDoesNotDependOnApi() {
        check(noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .because("의존은 항상 안쪽을 향한다. 도메인이 컨트롤러와 DTO를 알면 방향이 뒤집힌다"));
    }

    private static void check(ArchRule rule) {
        rule.allowEmptyShould(true).check(productionClasses);
    }
}
