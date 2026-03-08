plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(provider { project.name })
                description.set(provider { project.description ?: "Tower Messaging - ${project.name}" })
                url.set("https://github.com/tower-projects/tower-messaging")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("iamcyw")
                        name.set("Yucheng W")
                        email.set("iamcyw@example.com")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/tower-projects/tower-messaging.git")
                    developerConnection.set("scm:git:ssh://git@github.com/tower-projects/tower-messaging.git")
                    url.set("https://github.com/tower-projects/tower-messaging")
                }
            }
        }
    }
}

signing {
    setRequired { !project.version.toString().endsWith("-SNAPSHOT") && gradle.taskGraph.hasTask("publish") }

    if (System.getenv("MAVEN_SIGNING_KEY") != null) {
        useInMemoryPgpKeys(
            System.getenv("MAVEN_SIGNING_KEY"),
            System.getenv("MAVEN_SIGNING_PASSWORD")
        )
    } else {
        useGpgCmd()
    }

    sign(publishing.publications["maven"])
}
