package com.astro.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.astro", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    // --- Reglas de Capas (Definidas Manualmente para Mayor Precisión) ---

    @ArchTest
    public static final ArchRule servicesShouldOnlyBeAccessedByControllersOrOtherServices =
            classes()
                    .that().resideInAPackage("..service..")
                    .or().resideInAPackage("..handler..")
                    .or().resideInAPackage("..creation..")
                    .or().resideInAPackage("..retrieval..")
                    .or().resideInAPackage("..update..")
                    .or().resideInAPackage("..generator..")
                    .or().resideInAPackage("..protection..")
                    .or().resideInAPackage("..scheduler..")
                    .or().resideInAPackage("..validation..")
                    .or().resideInAPackage("..mapper..")
                    .or().resideInAPackage("..helper..")
                    .should().onlyBeAccessed().byAnyPackage(
                            "..controller..",
                            "..service..",
                            "..handler..",
                            "..creation..",
                            "..retrieval..",
                            "..update..",
                            "..generator..",
                            "..protection..",
                            "..scheduler..",
                            "..validation..",
                            "..mapper..",
                            "..helper..",
                            "..config..", // La configuración puede necesitar acceder a servicios
                            "..security.." // La seguridad puede necesitar acceder a servicios
                    );

    @ArchTest
    public static final ArchRule repositoriesShouldOnlyBeAccessedByServices =
            classes()
                    .that().resideInAPackage("..repository..")
                    .should().onlyBeAccessed().byClassesThat().resideInAnyPackage(
                            "..service..",
                            "..handler..",
                            "..creation..",
                            "..retrieval..",
                            "..update..",
                            "..generator..",
                            "..protection..",
                            "..scheduler..",
                            "..validation..",
                            "..mapper..",
                            "..helper..",
                            "..auth.service.." // CustomUserDetailsService necesita el repositorio
                    );

    // --- Reglas de Nomenclatura y Ubicación ---

    @ArchTest
    public static final ArchRule servicesShouldBeAnnotated =
            classes()
                    .that().resideInAPackage("..service..")
                    .should().beAnnotatedWith(Service.class);

    @ArchTest
    public static final ArchRule repositoriesShouldBeAnnotated =
            classes()
                    .that().resideInAPackage("..repository..")
                    .and().areNotInterfaces() // Las interfaces no necesitan la anotación
                    .should().beAnnotatedWith(Repository.class);


    // --- Reglas de Dependencias entre Módulos ---

    @ArchTest
    public static final ArchRule authModuleIsolation =
            noClasses()
                    .that().resideInAPackage("com.astro.auth..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.astro.stats..",
                            "com.astro.report..",
                            "com.astro.url.."
                    );

    @ArchTest
    public static final ArchRule urlModuleIsolation =
            noClasses()
                    .that().resideInAPackage("com.astro.url..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.astro.stats..",
                            "com.astro.report..",
                            "com.astro.auth.."
                    );
}