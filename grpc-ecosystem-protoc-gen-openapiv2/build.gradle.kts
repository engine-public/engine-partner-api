import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isSymbolicLink

plugins {
    id("com.google.protobuf")
}

description = "Packaged protoc-gen-openapiv2 protos to support Gradle Protobuf Plugin protoc invocation."

dependencies {
    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)
}

fun resolveCommand(command: String): String {
    val originalPath = ProcessBuilder().command("which", command).start().let { process ->
        process.inputReader(Charsets.UTF_8).use { reader ->
            Path(reader.readText().trim())
        }
    }
    if (!originalPath.isSymbolicLink()) {
        return command
    }
    var resolvedPath = originalPath
    while (resolvedPath.isSymbolicLink()) {
        Files.readSymbolicLink(resolvedPath).let {
            resolvedPath = if (it.isAbsolute) it else resolvedPath.parent.resolve(it)
        }
    }
    return resolvedPath.absolutePathString().also {
        logger.info("Resolved command ${command} to $resolvedPath")
    }
}

val goCmd = resolveCommand("go")

val goWorkspace = project.layout.buildDirectory.dir("go")
val goModCache = goWorkspace.map { it.dir("mod_cache") }
val goBin = goWorkspace.map { it.dir("bin") }
val goEnvironment = mapOf(
    "GOWORKSPACE" to goWorkspace,
    "GOMODCACHE" to goModCache,
    "GOBIN" to goBin,
)

val goModDownload = tasks.register<Exec>("goModDownload") {
    group = "golang"
    description = "Download the grpc-gateway go module."

    workingDir(project.projectDir)
    environment(goEnvironment.mapValues { (_, dirProp) -> dirProp.get().asFile.absolutePath })
    commandLine(goCmd, "mod", "download", "github.com/grpc-ecosystem/grpc-gateway/v2@${libs.versions.protoc.gen.openapi.get()}")
}

val grpcGatewayBaseDir = goModCache.map { it.dir("github.com/grpc-ecosystem/grpc-gateway/v2@${libs.versions.protoc.gen.openapi.get()}") }
val goStageProtocGenOpenapiV2Protos = tasks.register<Copy>("goStageProtocGenOpenapiV2Protos") {
    group = "golang"
    description = "Extracts the swagger proto extensions from the grpc-gateway go module."

    dependsOn(goModDownload)

    from(grpcGatewayBaseDir.map { it.dir("protoc-gen-openapiv2") }) {
        include("**/*.proto")
    }

    into("src/main/proto/protoc-gen-openapiv2")

    fileMode = "0666".toInt(8)
    dirMode = "0777".toInt(8)
    includeEmptyDirs = false
}

val goStageLicenseFile = tasks.register<Copy>("goStageLicenseFile") {
    group = "golang"
    description = "Extracts the license file from the grpc-gateway go module."

    dependsOn(goModDownload)
    from(grpcGatewayBaseDir) {
        include("LICENSE")
    }

    into("src/main/resources")

    fileMode = "0666".toInt(8)
    dirMode = "0777".toInt(8)
    includeEmptyDirs = false
}

tasks.processResources {
    dependsOn(goStageProtocGenOpenapiV2Protos, goStageLicenseFile)
}

tasks.sourcesJar {
    dependsOn(goStageLicenseFile)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        create("doc") {
            artifact = libs.grpc.protoc.genDoc.get().toString()
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.dependsOn(goStageProtocGenOpenapiV2Protos)
            task.builtins {
                create("kotlin")
            }
            task.plugins {
                create("doc") {
                    option("markdown,${project.name}-$version.md")
                }
            }
        }
    }
}

afterEvaluate {
    // override the default license information in the POM
    configure<PublishingExtension> {
        (publications.getByName("maven") as MavenPublication).pom {
            licenses {
                this.license {
                    name = "BSD-3-Clause"
                    url.set("https://github.com/grpc-ecosystem/grpc-gateway/blob/${libs.versions.protoc.gen.openapi.get()}/LICENSE")
                }
            }
        }
    }

    // golang downloads its module cache as readable, but not writable.
    // gradle can't delete these files...
    // on os that can, reset perms, so clean can succeed.
    val fixGoPermissions = tasks.register<Exec>("fixGoPermissions") {
        group = "golang"
        commandLine("chmod", "-Rf", "+w", goWorkspace.get().asFile.absolutePath)
        enabled = (osdetector.os == "osx") && goWorkspace.get().asFile.exists()
    }
    tasks.clean {
        dependsOn(fixGoPermissions)
    }
}
