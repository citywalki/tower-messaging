plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
}

description = "Tower Spring Boot Starter"

dependencies {
    api(project(":messaging-core"))

    implementation(libs.spring.boot.autoconfigure)
}
