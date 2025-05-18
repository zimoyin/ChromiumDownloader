import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

description = "Chromium Downloader"
group = "io.github.zimoyin"
version = "1.2.27"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.seleniumhq.selenium:selenium-java:4.29.0")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    if (!project.hasProperty("mavenCentralUsername")) {
        throw IllegalArgumentException("mavenCentralUsername is not set")
    } else if (!project.hasProperty("mavenCentralPassword")) {
        throw IllegalArgumentException("mavenCentralPassword is not set")
    } else if (!project.hasProperty("signing.keyId")) {
        throw IllegalArgumentException("signing.keyId is not set")
    } else if (!project.hasProperty("signing.password")) {
        throw IllegalArgumentException("signing.password is not set")
    }

    coordinates("io.github.zimoyin", "ChromiumDownloader", version.toString())

    pom {
        name.set("ChromiumDownloader")
        description.set("Headless Chrome")
        inceptionYear.set("2025")
        url.set("https://github.com/zimoyin/ChromiumDownloader/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("zimoyin")
                name.set("zimoyin")
                email.set("tianxuanzimo@qq.com")
                url.set("https://github.com/zimoyin")
            }
        }
        scm {
            url.set("ChromiumDownloader")
            connection.set("scm:git:git://github.com/zimoyin/ChromiumDownloader.git")
            developerConnection.set("scm:git:ssh://git@github.com/zimoyin/ChromiumDownloader.git")
        }
    }
}