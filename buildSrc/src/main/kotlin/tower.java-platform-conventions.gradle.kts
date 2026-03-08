plugins {
    `java-platform`
    `maven-publish`
}

javaPlatform {
    allowDependencies()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])

            pom {
                name.set(provider { project.name })
                description.set(provider { project.description ?: "Tower Messaging Platform - ${project.name}" })
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
