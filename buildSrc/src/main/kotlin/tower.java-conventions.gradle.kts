plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24

    withJavadocJar()
    withSourcesJar()
}

dependencies {
    // Platform BOM (exclude the platform project itself to avoid circular dependency)
    if (project.name != "messaging-dependencies") {
        implementation(platform(project(":messaging-dependencies")))
        annotationProcessor(platform(project(":messaging-dependencies")))
    }

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Annotation Processing
    compileOnly("org.jboss.logging:jboss-logging:3.6.1.Final")
    compileOnly("org.jboss.logging:jboss-logging-annotations:2.2.1.Final")
    annotationProcessor("org.jboss.logging:jboss-logging-processor:2.2.1.Final")
    annotationProcessor("org.jboss.logging:jboss-logging-annotations:2.2.1.Final")
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(24)
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "1024M"
    jvmArgs("--enable-preview")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Javadoc> {
    options {
        encoding("UTF-8")
        source(JavaVersion.VERSION_24.majorVersion)
        (this as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            tags("apiNote:a:API Note:", "implNote:a:Implementation Note:")
        }
    }
    // Exclude files using preview features from javadoc
    exclude("**/VirtualThreadBatchExecutor.java")
}
