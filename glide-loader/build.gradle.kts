import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.simple.phonetics.ui.precompute.glide"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

afterEvaluate {
    extensions.configure<PublishingExtension> {
        publications {
            register<MavenPublication>("release") {
                groupId = project.group.toString()
                artifactId = "glide-loader"
                version = project.version.toString()

                from(components["release"])
            }
        }
    }
}

dependencies {
    api(project(":node-engine"))
    api(libs.glide)
}
