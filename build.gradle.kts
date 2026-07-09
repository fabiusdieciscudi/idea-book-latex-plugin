/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    // Must be >= the Kotlin used to build everything we compile against: the 2026.1 platform ships metadata 2.3.0,
    // TeXiFy 1.0.1 ships 2.4.0. A compiler reads its own metadata version or older, never newer.
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.17.0"
}

group = "name.fabiusdieciscudi"
// The single source of truth for the version is gradle.properties, so a release is one line to change there.
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        // Includes releases() and marketplace(), the latter being where the TeXiFy artifact below is resolved from.
        defaultRepositories()
    }
}

// Path to an already-installed IDE, used instead of downloading the platform. Keep it out of the repository: set it
// in ~/.gradle/gradle.properties as
//     localIdePath=/Applications/IntelliJ IDEA.app
// When unset (CI, fresh checkout) the build falls back to the remote artifact.
val localIdePath: String? = providers.gradleProperty("localIdePath").orNull

dependencies {
    intellijPlatform {
        if (localIdePath != null) {
            local(localIdePath)
        } else {
            intellijIdea("2026.1.4")
        }

        // TeXiFy IDEA. The plugin ID is the historical `nl.rubensten.*`, kept for backwards compatibility
        plugin("nl.rubensten.texifyidea", "1.0.1")

        // Puts the platform, and the Kotlin stdlib it bundles, on the test classpath. Without it the unit tests have
        // no runtime.
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

// The IntelliJ Platform requires JDK 21 from 2024.2 onwards.
kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
            // No upper bound: the plugin keeps working across IDE updates.
            untilBuild = provider { null }
        }
    }

//    // Plugin signing. The key material never lives in the repository: it is read from the environment, set as CI
//    // secrets when publishing runs. Leaving the variables unset simply disables signing.
//    signing {
//        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
//        privateKey = providers.environmentVariable("PRIVATE_KEY")
//        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
//    }
//
//    // Marketplace publishing. The token is an environment variable for the same reason. The channel follows the
//    // version: a plain `1.0.0` goes to the default stable channel, a `1.0.0-eap` to an `eap` one.
//    publishing {
//        token = providers.environmentVariable("PUBLISH_TOKEN")
//        channels = providers.gradleProperty("pluginVersion").map {
//            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
//        }
//    }
}

tasks {
    // Nothing to index yet, and it is slow.
    buildSearchableOptions {
        enabled = false
    }

    test {
        useJUnit()
    }
}
