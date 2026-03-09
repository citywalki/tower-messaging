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
}
