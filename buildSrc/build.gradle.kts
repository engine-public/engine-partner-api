plugins {
    `kotlin-dsl`
    // this is the version builtin to the version of gradle we're using, not the version we're building the app against
    // if this doesn't match, you'll see an error that looks like this:
    // The `embedded-kotlin` and `kotlin-dsl` plugins rely on features of Kotlin <the version you need to change the next line to> that might work differently than in the requested version <the version on the next line>.
    kotlin("jvm").version("1.9.20")
}

repositories {
    gradlePluginPortal()
}