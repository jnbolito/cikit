import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.dokka.gradle.DokkaTask
import java.time.Instant

val cikitVersion = "0.1.0"

// TODO: Place kotlin version in gradle properties
plugins {
    kotlin("jvm") version "1.3.60"
    maven
    `maven-publish`
    id("com.jfrog.bintray") version "1.7.3" apply true
    id("org.jetbrains.dokka") version "0.9.17"
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.60")
    implementation(gradleApi())
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_API_KEY")
    publish = true
    override = true
    dryRun = false
    setPublications("CikitPublication")
    setConfigurations("archives")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "other"
        name = "cikit"
        vcsUrl = "https://github.com/jnbolito/cikit"
        setLabels("kotlin")
        setLicenses("LGPL-2.1")
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = cikitVersion
            released = Instant.now().toString()
        })
    })
}

val sourcesJar = task<Jar>("sourcesJar") {
    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}


tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val javadocJar = task<Jar>("javadocJar") {
    dependsOn("dokka")
    archiveClassifier.set("javadoc")
    from(tasks.dokka.get().outputDirectory)
}

artifacts {
    add("archives", sourcesJar)
    add("archives", javadocJar)
}


publishing {
    publications {
        create<MavenPublication>("CikitPublication") {
            groupId = "io.bolito"
            artifactId = "cikit"
            version = cikitVersion

            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")
                configurations.implementation.allDependencies.forEach {
                    // Ensure dependencies such as fileTree are not included in the pom.
                    if (it.name != "unspecified") {
                        val dependencyNode = dependenciesNode.appendNode ("dependency")
                        dependencyNode.appendNode("groupId", it.group)
                        dependencyNode.appendNode("artifactId", it.name)
                        dependencyNode.appendNode("version", it.version)
                    }
                }
            }
        }
    }
}
