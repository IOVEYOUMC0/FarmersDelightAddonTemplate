plugins {
    id("java")
    // Shadow bundles your code into one jar. FarmersDelight + CraftEngine are NOT bundled (compileOnly).
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "com.example.fdaddon"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/") // CraftEngine
    mavenLocal()
}

// The FarmersDelight api dependency is wired in settings.gradle.kts: it resolves to the sibling
// ../FarmersDelight composite build when that checkout exists, and otherwise to a Gradle source
// dependency on the git repository. Either way the coordinate is
// com.huidu.farmersdelight:farmersdelight-plugin:<version>, added there as compileOnly.
// CraftEngine is resolved from Maven. Overridable so a compatibility check can build the same sources
// against another release without editing this file:  gradlew build -PceVersion=26.8.2
val ceVersion = providers.gradleProperty("ceVersion").getOrElse("26.9.1")

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // CraftEngine from the official Maven repository.
    compileOnly("net.momirealms:craft-engine-bukkit:$ceVersion")
    // The bukkit artifact no longer bundles core, so the core classes come from their own jar.
    compileOnly("net.momirealms:craft-engine-core:$ceVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") { expand("version" to version) }
}

tasks.shadowJar {
    archiveBaseName.set("fdaddontemplate")
    // Drop Shadow's default "-all" classifier so the artifact is the plain documented file name.
    archiveClassifier.set("")
}

tasks.jar { enabled = false }
tasks.build { dependsOn(tasks.shadowJar) }
