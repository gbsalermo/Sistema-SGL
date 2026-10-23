package com.sgl.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Barreiras arquiteturais mínimas do MVP.
 *
 * Regras incrementais: não tentam impor uma estrutura nova ao legado e não
 * substituem os testes funcionais de estoque, autorização e tenant.
 */
@AnalyzeClasses(packages = "com.sgl", importOptions = ImportOption.DoNotIncludeTests.class)
class SglArchitectureTest {

    @ArchTest
    static final ArchRule dominio_nao_depende_de_controllers =
        noClasses().that().resideInAPackage("..model..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule repositorios_nao_dependem_de_controllers =
        noClasses().that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule dtos_nao_dependem_de_controllers =
        noClasses().that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule dtos_nao_dependem_de_repositorios =
        noClasses().that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule servicos_nao_dependem_de_controllers =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");
}
