// TODO: Place kotlin version in gradle properties

plugins {
    kotlin("jvm") version "1.3.60"
    `maven-publish`
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.60")
    implementation(gradleApi())
}



publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.bolito"
            artifactId = "cikit"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}

