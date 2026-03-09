plugins {
    id("tower.java-conventions")
    id("tower.maven-publish")
}

description = "Tower Schema Builder"

dependencies {
    implementation(libs.jboss.logging)
    implementation(project(":common"))
    implementation(project(":messaging-core"))
    implementation(project(":schema:schema-model"))
    implementation(libs.jandex)

    testCompileOnly(libs.jboss.logging)
}
