plugins {
    id("java")
    // Shadow bundles your code into one jar. FarmersDelight + CraftEngine are NOT bundled (compileOnly).
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "com.example.fdaddon"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/") // CraftEngine
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // CraftEngine — your addon talks to CE for custom items/blocks/recipes at runtime.
    compileOnly("net.momirealms:craft-engine-core:26.5.3")
    compileOnly("net.momirealms:craft-engine-bukkit:26.5.3")

    // FarmersDelight — the ONLY thing you may reference is its obfuscation-safe `api.**` facade.
    // FD's internals are repackaged/renamed by ProGuard; only `com.huidu.farmersdelight.api.**` keeps
    // stable names. This is an API-ONLY stub jar (just `com.huidu.farmersdelight.api.**`, no internals,
    // not a runnable plugin) — build it in the FarmersDelight repo with `gradlew apiJar` and copy
    // build/libs/farmersdelight-plugin-*-api.jar into this repo's libs/ (renamed).
    // At runtime the real FarmersDelight plugin (a server dependency) provides the implementation.
    compileOnly(files("libs/farmersdelight-api-1.0.0.jar"))
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
    archiveClassifier.set("")
}

tasks.jar { enabled = false }
tasks.build { dependsOn(tasks.shadowJar) }
