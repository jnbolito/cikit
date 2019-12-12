import com.jfrog.bintray.gradle.BintrayExtension
import java.util.Date

val cikitVersion = "0.1.0"
group = "io.bolito"
version = cikitVersion

// TODO: Place kotlin version in gradle properties
plugins {
    kotlin("jvm") version "1.3.60"
    maven
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4" apply true
    id("org.jetbrains.dokka") version "0.9.17"
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.60")
    implementation(gradleApi())
}

tasks {
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }


    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
    }

    val javadocJar = task<Jar>("javadocJar") {
        dependsOn("dokka")
        archiveClassifier.set("javadoc")
        from(dokka.get().outputDirectory)
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }


    publishing {
        publications {
            create<MavenPublication>("CikitPublication") {
                groupId = "io.bolito"
                artifactId = project.name
                version = cikitVersion

                from(project.components["java"])
                artifact(sourcesJar)
                artifact(javadocJar)
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_API_KEY")
    publish = false
    override = true
    dryRun = false
    setPublications("CikitPublication")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven-oss"
        name = project.name
        vcsUrl = "https://github.com/jnbolito/cikit"
        setLabels("kotlin")
        setLicenses("LGPL-2.1")
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = cikitVersion
            released = Date().toString()
        })
    })
}
