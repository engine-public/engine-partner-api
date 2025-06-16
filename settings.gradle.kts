rootProject.name = "engine-partner-api"


pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm").version(extra["kotlinVersion"] as String)

        id("com.google.protobuf").version(extra["version.plugin.protobuf"] as String)
        id("org.jlleitschuh.gradle.ktlint").version(extra["version.plugin.ktlint"] as String)
        id("org.jreleaser").version(extra["version.plugin.jreleaser"] as String)
    }
}

dependencyResolutionManagement {
    val grpc = "1.60.0"
    val grpc_kotlin = "1.4.1"
    versionCatalogs {
        create("buildscript") {
            library("gson", "com.google.code.gson:gson:2.13.1")
        }
        create("libs") {
            version("protoc-gen-openapi", "v2.25.1")
            val grpc = version("grpc", grpc)
            val grpc_kotlin = version("grpc.kotlin", grpc_kotlin)
            val kotlinx_coroutines = version("kotlinx.coroutines", "1.7.3")
            val protobuf = version("protobuf", "3.25.5")

            library("google.api.grpc.common.protos", "com.google.api.grpc", "proto-google-common-protos").version("2.22.0")

            library("grpc.api", "io.grpc", "grpc-api").versionRef(grpc)
            library("grpc.core", "io.grpc", "grpc-protobuf").versionRef(grpc)
            library("grpc.kotlin", "io.grpc", "grpc-kotlin-stub").versionRef(grpc_kotlin)
            library("grpc.protoc.java", "io.grpc", "protoc-gen-grpc-java").versionRef(grpc)
            library("grpc.protoc.kotlin", "io.grpc", "protoc-gen-grpc-kotlin").versionRef(grpc_kotlin)
            library("grpc.protoc.genDoc", "io.github.pseudomuto", "protoc-gen-doc").version("1.5.1")

            library("kotlinx.coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(kotlinx_coroutines)

            library("protobuf.java", "com.google.protobuf", "protobuf-java").versionRef(protobuf)
            library("protobuf.kotlin", "com.google.protobuf", "protobuf-kotlin").versionRef(protobuf)
            library("protobuf.protoc", "com.google.protobuf", "protoc").versionRef(protobuf)
        }

        create("testLibs") {
            val kotest = version("kotest", "5.8.1")

            library("kotest.runner", "io.kotest", "kotest-runner-junit5").versionRef(kotest)
            library("kotest.assertions.core", "io.kotest", "kotest-assertions-core").versionRef(kotest)
        }
    }
}

rootDir
    .walkTopDown()
    .filter { it != rootDir }
    .filter { it.isDirectory }
    .filterNot { it.name.startsWith(".") }
    .filterNot { setOf("build", "buildSrc", "tmp", "scratch").contains(it.name) }
    .filterNot { it.resolve(".gradle_ignore").exists() }
    .filter { it.resolve("build.gradle.kts").let { it.exists() && it.isFile } }
    .forEach {
        val relativePath = it.relativeTo(rootDir)
        val projectName = ":${rootProject.name}-${relativePath.path.replace("/", "-")}"
        include(projectName)
        project(projectName).projectDir = it
    }

// manual include to set explicit naming instead of relying on auto-import
include(":grpc-ecosystem-protoc-gen-openapiv2")
project(":grpc-ecosystem-protoc-gen-openapiv2").projectDir = rootDir.resolve("grpc-ecosystem-protoc-gen-openapiv2")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
