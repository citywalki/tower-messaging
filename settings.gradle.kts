rootProject.name = "tower-messaging"

// Plugin Management
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Dependency Resolution Management
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

// Project Structure
include("common")
include("messaging-core")
include("messaging-cdi")

include("support:quarkus:tower-quarkus")
include("support:quarkus:tower-quarkus-deployment")
include("support:quarkus:integration-tests")

