plugins {
    id("tower.java-platform-conventions")
}

description = "Tower Messaging Dependencies Platform"

dependencies {
    constraints {
        // JBoss Logging
        api(libs.jboss.logging)
        api(libs.jboss.logging.annotations)
        api(libs.jboss.logging.processor)

        // Jandex
        api(libs.jandex)

        // Jakarta EE 10
        api(libs.jakarta.cdi.api)
        api(libs.jakarta.inject.api)
        api(libs.microprofile.context.propagation.api)

        // Apache Commons
        api(libs.commons.lang3)
        api(libs.commons.collections4)

        // Google Guava
        api(libs.guava)

        // Testing
        api(libs.junit.jupiter.engine)
        api(libs.assertj.core)

        // Internal Modules
        api(project(":common"))
        api(project(":messaging-core"))
        api(project(":messaging-cdi"))
        api(project(":schema:schema-model"))
        api(project(":schema:schema-builder"))
        api(project(":support:quarkus:tower-quarkus"))
        api(project(":support:quarkus:tower-quarkus-deployment"))
    }

    // Import Quarkus BOM
    api(platform(libs.quarkus.bom))
}
