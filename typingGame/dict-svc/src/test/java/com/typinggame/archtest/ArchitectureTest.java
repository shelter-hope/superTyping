package com.typinggame.archtest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

/**
 * 后端结构护栏：与 {@code docs/backend-structure-spec.md} 第 4 节一一对应。
 *
 * <p>规则只约束「方向」，不约束命名；新增代码违反任意一条都会让 {@code mvn test} 失败。
 *
 * <p>规则允许空作用域（allowEmptyShould）：没有某层的服务不会被误报，但只要存在对应类就会真正检查。
 *
 * <p>注意：本文件在每个服务中都是同一份（api-gateway 无 JPA，版本少一条实体规则），修改时请同步全部 8 个服务。
 */
@AnalyzeClasses(packages = "com.typinggame", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule controller_must_not_depend_on_repository = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("controller 只做 HTTP 编排，数据访问必须经过 service")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule service_must_not_depend_on_controller = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("service 不得感知 HTTP 层，反向依赖会让事务边界和测试都变脏")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repository_must_not_depend_on_service_or_controller = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..")
            .because("repository 只做数据访问，业务编排属于 service")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entities_must_reside_in_entity_package = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..entity..")
            .because("JPA 实体必须集中在 entity 包，便于按服务划清表归属")
            .allowEmptyShould(true);
}
