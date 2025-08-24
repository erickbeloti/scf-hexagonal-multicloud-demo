package com.example.tasks;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.tasks")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainShouldBeIndependent = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapters..");

    @ArchTest
    static final ArchRule applicationShouldNotDependOnAdapters = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..adapters..");

    @ArchTest
    static final ArchRule inboundShouldNotDependOnOutbound = noClasses()
        .that().resideInAPackage("..adapters.inbound..")
        .should().dependOnClassesThat().resideInAPackage("..adapters.outbound..");

    @ArchTest
    static final ArchRule outboundShouldNotDependOnInbound = noClasses()
        .that().resideInAPackage("..adapters.outbound..")
        .should().dependOnClassesThat().resideInAPackage("..adapters.inbound..");
}
