plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
    id("tower.quarkus-conventions")
}

description = "Tower Quarkus Extension Deployment"

dependencies {
    api(project(":support:quarkus:tower-quarkus"))
    api(project(":schema:schema-builder"))

    api(libs.quarkus.core.deployment)
    api(libs.quarkus.arc.deployment)

    testImplementation(libs.quarkus.junit5.internal)
}

tasks.test {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    systemProperty("platform.quarkus.native.builder-image", "false")
    filter {
        failOnNoDiscoveredTests = false
    }
}
