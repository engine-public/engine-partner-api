import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isSymbolicLink

plugins {
    id("com.google.protobuf")
}

description = "The Engine Partner API gRPC and OpenAPI service definitions."

dependencies {
    api(projects.enginePartnerApiContent)

    api(libs.grpc.core)
    api(libs.grpc.kotlin)
    api(libs.kotlinx.coroutines.core)
}

kotlin {
    explicitApi = ExplicitApiMode.Disabled
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
        logger.info("Resolved command $command to $resolvedPath")
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

val goInstallProtocGenOpenapiV2 = tasks.register<Exec>("goInstallProtocGenOpenapiV2") {
    environment(goEnvironment.mapValues { (_, dirProp) -> dirProp.get().asFile.absolutePath })
    commandLine(goCmd, "install", "github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-openapiv2@${libs.versions.protoc.gen.openapi.get()}")
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.grpc.protoc.java.get().toString()
        }
        create("grpckt") {
            artifact = "${libs.grpc.protoc.kotlin.get()}:jdk8@jar"
        }
        create("doc") {
            artifact = libs.grpc.protoc.genDoc.get().toString()
        }
        create("openapiv2") {
            path = goBin.map { dir -> dir.file("protoc-gen-openapiv2") }.get().asFile.absolutePath
        }
    }
    generateProtoTasks {
        all().forEach {
            it.dependsOn(goInstallProtocGenOpenapiV2)

            it.generateDescriptorSet = true
            it.descriptorSetOptions.includeImports = true

            it.plugins {
                create("grpc")
                create("grpckt")
                create("doc") {
                    option("html,${project.name}-${version}.html")
                }
                create("openapiv2")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.processResources.configure {
    dependsOn(tasks.generateProto)
    from(project.layout.buildDirectory.dir("generated/source/proto/main")) {
        include("descriptor_set.desc")
    }
    from(project.layout.buildDirectory.dir("generated/source/proto/main/openapiv2")) {
        include("**/service.swagger.json")
        eachFile {
            path = "${path.split("/")[1]}.swagger.json"
        }
    }
}

afterEvaluate {
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