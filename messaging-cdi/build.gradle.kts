plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
}

description = "Tower Messaging CDI Integration"

dependencies {
    implementation(libs.jboss.logging)

    api(project(":common"))
    api(project(":messaging-core"))
    implementation(project(":schema:schema-model"))

    compileOnly(libs.jakarta.cdi.api)
    compileOnly(libs.microprofile.context.propagation.api)

    testImplementation(libs.jboss.logging)
    testImplementation(libs.jandex)
    testImplementation(project(":schema:schema-builder"))

    // CDI testing
    testImplementation(libs.jakarta.cdi.api)
    testImplementation(libs.jakarta.inject.api)
    testImplementation("org.jboss.weld:weld-junit5:4.0.3.Final")
}
