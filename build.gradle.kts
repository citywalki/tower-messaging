plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

// Project Information
allprojects {
    group = "io.iamcyw.tower"
    version = findProperty("version")?.toString() ?: "0.1.0-SNAPSHOT"
}

// Nexus Publishing Configuration
nexusPublishing {
    repositories {
        sonatype {
            // For users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

            username.set(providers.gradleProperty("MAVEN_USERNAME").orElse(providers.environmentVariable("MAVEN_USERNAME")))
            password.set(providers.gradleProperty("MAVEN_PASSWORD").orElse(providers.environmentVariable("MAVEN_PASSWORD")))
        }
    }
}

// Wrapper Task Configuration
tasks.wrapper {
    gradleVersion = "9.0"
    distributionType = Wrapper.DistributionType.BIN
}

// Aggregate Reports for Root Project
subprojects {
    // Empty configuration for subprojects
}
