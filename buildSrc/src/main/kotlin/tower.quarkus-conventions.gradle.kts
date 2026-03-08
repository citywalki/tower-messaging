plugins {
    `java-library`
}

dependencies {
    // Quarkus BOM
    implementation(platform("io.quarkus:quarkus-bom:3.31.2"))
    annotationProcessor(platform("io.quarkus:quarkus-bom:3.31.2"))

    // Quarkus Extension Processor
    annotationProcessor("io.quarkus:quarkus-extension-processor")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
