package com.example.tasks;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .importPackages("com.example.tasks");

    @Nested
    @DisplayName("Hexagonal Architecture Rules")
    class HexagonalArchitectureTests {

        @Test
        @DisplayName("Domain layer should not depend on any other layer")
        void domainShouldNotDependOnOtherLayers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .and().areNotAnnotatedWith("org.junit.jupiter.api.Test")
                    .and().haveSimpleNameNotContaining("Test")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..adapters..", "..config..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Application layer should only depend on domain")
        void applicationShouldOnlyDependOnDomain() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .and().haveSimpleNameNotContaining("Test")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..adapters..", "..config..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Adapters should depend on application ports, not on each other")
        void adaptersShouldNotDependOnEachOther() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..adapters.inbound..")
                    .and().haveSimpleNameNotContaining("Test")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapters.outbound..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Inbound adapters should only use allowed dependencies")
        void inboundAdaptersShouldOnlyUseAllowedDependencies() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..adapters.inbound..")
                    .and().haveSimpleNameNotContaining("Test")
                    .and().areNotAnnotatedWith("org.junit.jupiter.api.Test")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("..application.port.inbound..", "..domain..",
                                      "java..", "org.springframework..", "..dto..",
                                      "jakarta..", "com.example.tasks.application.service..",
                                      "..infrastructure..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Outbound adapters should implement outbound ports")
        void outboundAdaptersShouldImplementOutboundPorts() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..adapters.outbound..")
                    .and().haveSimpleNameNotContaining("Test")
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().beAssignableTo("com.example.tasks.application.port.outbound.TaskRepositoryPort");

            rule.allowEmptyShould(true).check(importedClasses);
        }
    }

    @Nested
    @DisplayName("DDD Architecture Rules")
    class DDDArchitectureTests {

        @Test
        @DisplayName("Domain entities should not have setters")
        void domainEntitiesShouldNotHaveSetters() {
            ArchRule rule = noMethods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
                    .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("Test")
                    .and().areNotPrivate()
                    .and().haveNameNotMatching("setup")
                    .should().haveNameMatching("set.*");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Domain services should be in domain package")
        void domainServicesShouldBeInDomainPackage() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("DomainService")
                    .should().resideInAPackage("..domain..")
                    .allowEmptyShould(true); // Allow empty since we simplified and removed domain services

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Repository interfaces should be in domain")
        void repositoryInterfacesShouldBeInDomain() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .and().areInterfaces()
                    .should().resideInAPackage("..domain..");

            rule.allowEmptyShould(true).check(importedClasses);
        }

        @Test
        @DisplayName("Domain exceptions should be in domain package")
        void domainExceptionsShouldBeInDomainPackage() {
            ArchRule rule = classes()
                    .that().areAssignableTo(RuntimeException.class)
                    .and().resideInAPackage("..domain..")
                    .should().resideInAPackage("..domain.exception..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Value objects should be immutable")
        void valueObjectsShouldBeImmutable() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..domain..")
                    .and().haveSimpleNameEndingWith("Id")
                    .should().haveOnlyFinalFields();

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Layer Architecture Validation")
    class LayerArchitectureTests {

        @Test
        @DisplayName("Should follow layered architecture pattern")
        void shouldFollowLayeredArchitecturePattern() {
            ArchRule rule = layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Adapters").definedBy("..adapters..")
                    .layer("Config").definedBy("..config..")

                    .whereLayer("Domain").mayNotAccessAnyLayer()
                    .whereLayer("Application").mayOnlyAccessLayers("Domain")
                    .whereLayer("Adapters").mayOnlyAccessLayers("Application", "Domain")
                    .whereLayer("Config").mayOnlyAccessLayers("Application", "Domain");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Package Dependencies")
    class PackageDependencyTests {

        @Test
        @DisplayName("Should have no circular dependencies")
        void shouldHaveNoCircularDependencies() {
            ArchRule rule = SlicesRuleDefinition.slices()
                    .matching("com.example.tasks.(*)..")
                    .should().beFreeOfCycles();

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Port and Adapter Pattern")
    class PortAdapterPatternTests {

        @Test
        @DisplayName("All ports should be interfaces")
        void allPortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.port..")
                    .should().beInterfaces();

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("UseCase interfaces should end with UseCase")
        void useCaseInterfacesShouldEndWithUseCase() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.port.inbound..")
                    .should().haveSimpleNameEndingWith("UseCase");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Port interfaces should end with Port")
        void portInterfacesShouldEndWithPort() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.port.outbound..")
                    .and().areInterfaces()
                    .should().haveSimpleNameEndingWith("Port");

            rule.allowEmptyShould(true).check(importedClasses);
        }

        @Test
        @DisplayName("Application services should be annotated with @Service")
        void applicationServicesShouldBeAnnotatedWithService() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.service..")
                    .and().haveSimpleNameNotContaining("Test")
                    .should().beAnnotatedWith("org.springframework.stereotype.Service");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Injection Rules")
    class DependencyInjectionTests {

        @Test
        @DisplayName("No field injection should be used")
        void noFieldInjectionShouldBeUsed() {
            ArchRule rule = noFields()
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

            rule.check(importedClasses);
        }
    }
}
