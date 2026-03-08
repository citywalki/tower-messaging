plugins {
    `java-platform`
    id("java-conventions")
    id("maven-deploy")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api("org.jboss.logging:jboss-logging:3.6.1.Final")
        api("org.jboss.logging:jboss-logging-annotations:2.2.1.Final")
        api("org.jboss.logging:jboss-logging-processor:2.2.1.Final")

        // Jakarta EE 10 - jakarta 命名空间
        api("jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0")
        api("jakarta.inject:jakarta.inject-api:2.0.1")
        api("org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.3")

        api("com.google.guava:guava:33.3.1-jre")

        api("org.apache.commons:commons-collections4:4.4")
        api("org.apache.commons:commons-lang3:3.17.0")
        api("org.jboss:jandex:3.2.3")

        api("org.junit.jupiter:junit-jupiter-engine:5.11.4")
        api("org.assertj:assertj-core:3.27.3")

        api(project(":common"))
        api(project(":messaging-core"))
        api(project(":support:quarkus:tower-quarkus"))
        api(project(":support:quarkus:tower-quarkus-deployment"))
    }
}