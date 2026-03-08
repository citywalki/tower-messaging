plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
}

description = "Tower Common Utilities"

dependencies {
    implementation(libs.jboss.logging)

    api(libs.guava)
    api(libs.commons.lang3)
    api(libs.commons.collections4)
}
