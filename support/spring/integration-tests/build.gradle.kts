plugins {
    id("tower.java-conventions")
}

description = "Tower Spring Boot Integration Tests"

dependencies {
    implementation(project(":support:spring:tower-spring-boot-starter"))
    implementation(libs.spring.boot.starter)

    testImplementation(libs.spring.boot.starter.test)
}
