plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
}

description = "Tower Messaging Core"

dependencies {
    implementation(libs.jboss.logging)
    implementation(project(":common"))

    testImplementation(libs.jandex)
}
