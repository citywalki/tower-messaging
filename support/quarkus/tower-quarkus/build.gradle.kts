plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
    id("tower.quarkus-conventions")
}

description = "Tower Quarkus Extension Runtime"

java {
    registerFeature("metrics") {
        usingSourceSet(sourceSets.main.get())
    }
}

dependencies {
    implementation(libs.quarkus.core)
    implementation(libs.quarkus.arc)

    api(project(":messaging-cdi"))
}
