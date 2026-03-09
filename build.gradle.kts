plugins {
    base
    id("org.jreleaser") version "1.17.0"
}

// Project Information
allprojects {
    group = "io.iamcyw.tower"
    version = findProperty("version")?.toString() ?: "0.1.0-SNAPSHOT"
}

// JReleaser Configuration
jreleaser {
    gitRootSearch.set(true)

    project {
        name.set("tower-messaging")
        description.set("Tower Messaging - A Java framework for command messaging patterns")
        license.set("Apache-2.0")
        copyright.set("2024 Yucheng W")
        authors.add("iamcyw")
        links {
            homepage.set("https://github.com/tower-projects/tower-messaging")
        }
    }

    release {
        generic {
            enabled.set(true)
            skipRelease.set(true)
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        mode.set(org.jreleaser.model.Signing.Mode.MEMORY)
        publicKey.set(propertyOrEnv("JRELEASER_GPG_PUBLIC_KEY"))
        secretKey.set(propertyOrEnv("JRELEASER_GPG_SECRET_KEY"))
        passphrase.set(propertyOrEnv("JRELEASER_GPG_PASSPHRASE"))
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                    username.set(propertyOrEnv("JRELEASER_MAVENCENTRAL_USERNAME"))
                    password.set(propertyOrEnv("JRELEASER_MAVENCENTRAL_PASSWORD"))
                    verifyPom.set(false)
                }
            }
        }
    }
}

fun propertyOrEnv(name: String): String {
    return providers.gradleProperty(name).orElse(providers.environmentVariable(name)).getOrElse("")
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
