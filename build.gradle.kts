plugins {
    id("java")
    // Shadow bundles your code into one jar. FarmersDelight + CraftEngine are NOT bundled (compileOnly).
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "com.example.fdaddon"
version = "1.0.0"

// CraftEngine version selector — must be declared before `dependencies {}` uses it.
val ceVersion = providers.gradleProperty("ceVersion").orElse("26.7.4").get()
val pluginArchiveClassifier = if (ceVersion == "26.8") "ce268" else "ce2674"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/") // CraftEngine
    mavenLocal()
}

// The FarmersDelight api.** facade jar is prebuilt (from the closed-source FarmersDelight repo's :apiJar
// task) and committed into libs/, so this addon builds without any access to the FarmersDelight source.
// When the main plugin publishes a new API, replace libs/farmersdelight-1.0.0.jar and bump the version.

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // CraftEngine — two supported server-side versions. Pass -PceVersion=26.7.4 (default, maven) or
    // -PceVersion=26.8 (local jar shared from ../FarmersDelight/libs, since 26.8-SNAPSHOT is unpublished).
    if (ceVersion == "26.8") {
        compileOnly(files("../FarmersDelight/libs/craft-engine-26.8.jar"))
    } else {
        compileOnly("net.momirealms:craft-engine-core:26.7.4")
        compileOnly("net.momirealms:craft-engine-bukkit:26.7.4")
        compileOnly("net.momirealms:craft-engine-bukkit-proxy:26.7.4")
    }

    // FarmersDelight — the ONLY thing you may reference is its obfuscation-safe `api.**` facade.
    // FD's internals are repackaged/renamed by ProGuard; only `com.huidu.farmersdelight.api.**` keeps
    // stable names. This is an API-ONLY stub jar (just `com.huidu.farmersdelight.api.**`, no internals,
    // not a runnable plugin) — committed in libs/.
    // At runtime the real FarmersDelight plugin (a server dependency) provides the implementation.
    compileOnly(files("libs/farmersdelight-1.0.0.jar"))
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
    filesMatching("plugin.yml") { expand("version" to version) }
}

tasks.shadowJar {
    archiveBaseName.set("fdaddontemplate")
    archiveClassifier.set(pluginArchiveClassifier)
}

tasks.jar { enabled = false }
tasks.build { dependsOn(tasks.shadowJar) }
