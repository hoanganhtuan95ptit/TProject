import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.simple.phonetics.ui.precompute.glide"
    compileSdk = 35

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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
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
    api(libs.glide.transformations)
    implementation(libs.auto.service)
    ksp(libs.auto.service.ksp)
}
