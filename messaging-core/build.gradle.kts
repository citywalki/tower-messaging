plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
}

description = "Tower Messaging Core"

dependencies {
    implementation(libs.jboss.logging)
    implementation(project(":common"))
    implementation(project(":schema:schema-model"))

    testImplementation(libs.jandex)
    testImplementation(project(":schema:schema-builder"))
}
