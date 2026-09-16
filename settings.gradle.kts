rootProject.name = "fdaddontemplate"

// Composite build: FarmersDelight is included so its :apiJar is built from source instead of being
// copied in by hand. The stub in libs/ is a build output, not something to keep in sync manually --
// that is exactly how five copies of it silently went stale.
includeBuild("../FarmersDelight") {
    name = "farmersdelight-plugin"
}
