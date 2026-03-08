plugins {
    id("java")
    alias(libs.plugins.quarkus)
    id("tower.java-conventions")
}

description = "Tower Quarkus Integration Tests"

dependencies {
    implementation(platform(libs.quarkus.bom))

    implementation(libs.quarkus.reactive.routes)
    implementation(libs.quarkus.arc)

    implementation(project(":common"))
    implementation(project(":support:quarkus:tower-quarkus"))
    implementation(project(":support:quarkus:tower-quarkus-deployment"))

    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.rest.assured)
}
