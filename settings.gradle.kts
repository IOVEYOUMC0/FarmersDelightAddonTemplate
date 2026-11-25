rootProject.name = "fdaddontemplate"

// The FarmersDelight API dependency (com.huidu.farmersdelight:farmersdelight-plugin) has two channels.
//
// 1. Local checkout (preferred). When ../FarmersDelight exists the build is a Gradle composite build:
//    :apiJar is built from that checkout and substituted for the coordinate below. No fetch, works
//    offline, and an api change is picked up immediately.
// 2. No checkout. Gradle falls back to a source dependency on the git repository, checks out the pinned
//    version, builds its api jar and resolves the same coordinate from there. That channel needs network.
//
// Both channels hand over the same artifact: the api-only jar (com/huidu/farmersdelight/api/**).
// Keep the pinned version in step with the version the addon is written against.
val farmersDelightApiVersion = "1.0.4"
val farmersDelightRepo = "https://github.com/IOVEYOUMC0/Farmersdelight-Plugin.git"
val farmersDelightCheckout = file("../FarmersDelight")

val localApiJar = File(System.getProperty("user.home"),
        ".m2/repository/com/huidu/farmersdelight/farmersdelight-plugin/$farmersDelightApiVersion/"
                + "farmersdelight-plugin-$farmersDelightApiVersion.jar")

// The Git source-dependency repository takes precedence over mavenLocal, so only register it when neither
// the sibling checkout nor a locally published 1.0.4 api jar exists. A workspace that ran
// `FarmersDelight/gradlew publishToMavenLocal` therefore resolves from ~/.m2 even before the 1.0.4 tag is
// pushed; a truly standalone checkout still uses the source-dependency channel.
if (!farmersDelightCheckout.isDirectory && !localApiJar.isFile) {
    sourceControl {
        gitRepository(uri(farmersDelightRepo)) {
            producesModule("com.huidu.farmersdelight:farmersdelight-plugin")
        }
    }
}

if (farmersDelightCheckout.isDirectory) {
    includeBuild(farmersDelightCheckout) {
        name = "farmersdelight-plugin"
    }
}

gradle.beforeProject {
    // Fallback for a workspace without the sibling checkout: publish the api once with
    // `FarmersDelight/gradlew publishToMavenLocal`, then this repository resolves 1.0.4 from ~/.m2.
    // The git source-dependency channel still needs a published tag, so mavenLocal keeps local builds
    // deterministic before that tag exists.
    repositories.mavenLocal {
        content { includeGroup("com.huidu.farmersdelight") }
    }
    // The dependency has to be added after this project's build script has been evaluated: only then is
    // the java plugin applied and its `compileOnly` / `testImplementation` configurations present.
    afterEvaluate {
        if (configurations.findByName("compileOnly") != null) {
            dependencies.add("compileOnly", "com.huidu.farmersdelight:farmersdelight-plugin:$farmersDelightApiVersion")
        }
        if (configurations.findByName("testImplementation") != null) {
            dependencies.add("testImplementation", "com.huidu.farmersdelight:farmersdelight-plugin:$farmersDelightApiVersion")
        }
    }
}
