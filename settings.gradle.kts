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
val farmersDelightApiVersion = "1.0.3"
val farmersDelightRepo = "https://github.com/IOVEYOUMC0/Farmersdelight-Plugin.git"
val farmersDelightCheckout = file("../FarmersDelight")

sourceControl {
    gitRepository(uri(farmersDelightRepo)) {
        producesModule("com.huidu.farmersdelight:farmersdelight-plugin")
    }
}

if (farmersDelightCheckout.isDirectory) {
    includeBuild(farmersDelightCheckout) {
        name = "farmersdelight-plugin"
    }
}

gradle.beforeProject {
    // The dependency has to be added after this project's build script has been evaluated: only then is
    // the java plugin applied and its `compileOnly` configuration present.
    afterEvaluate {
        if (configurations.findByName("compileOnly") != null) {
            dependencies.add("compileOnly", "com.huidu.farmersdelight:farmersdelight-plugin:$farmersDelightApiVersion")
        }
    }
}
