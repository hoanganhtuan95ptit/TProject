import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.simple.phonetics.ui.precompute"
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

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
                groupId = "com.github.hoanganhtuan95ptit.core"
                artifactId = "node-engine"
                version = "1.0.0"

                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation(libs.glide)
    implementation(libs.androidx.core.ktx)
    compileOnly(libs.google.auto.service.annotations)
    annotationProcessor(libs.google.auto.service)
}
