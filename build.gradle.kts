import com.google.gson.GsonBuilder
import org.gradle.api.internal.provider.DefaultProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isSymbolicLink

plugins {
    id("org.jreleaser")

    id("com.google.protobuf").apply(false)
    kotlin("jvm").apply(false)
    id("org.jlleitschuh.gradle.ktlint").apply(false)
}

buildscript {
    dependencies {
        classpath(buildscript.gson)
    }
}

fun calculateVersion(): String {
    return System
        .getenv("ENGINE_BUILD_VERSION")
        ?.let {
            it.ifEmpty {
                null
            }
        }
        ?: "0.0.0-pre.0" // temporary fallback version
}

group = "com.engine"
version = calculateVersion()
description = "engine.com Partner API Definitions"

val distDir = project.layout.buildDirectory.dir("dist")
val stagingDir = project.layout.buildDirectory.dir("staging")
val mavenStagingDir = stagingDir.map { it.dir("maven-central") }
val documentationStagingDir = stagingDir.map { it.dir("documentation") }

configure<JReleaserExtension> {
    project {
        description = "Engine Partner API"
        copyright = "Copyright ${Calendar.getInstance().get(Calendar.YEAR)} HotelEngine, Inc., d/b/a Engine"
        license = "Apache-2.0"
    }
    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                val sonatype by creating {
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository(mavenStagingDir.get().asFile.relativeTo(rootDir).path)
                }
            }
        }
    }
}

val jreleaserCreateBuildDir = tasks.register("jreleaserCreateBuildDir") {
    doFirst { project.layout.buildDirectory.dir("jreleaser").get().asFile.mkdirs() }
}
val jreleaserDeployTask = tasks.jreleaserDeploy
jreleaserDeployTask {
    dependsOn(jreleaserCreateBuildDir)
}

val stageMavenCentral = tasks.register("stageMavenCentral") {
    group = "publishing"
}

subprojects {
    apply<IdeaPlugin>()
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of("21"))
            vendor.set(JvmVendorSpec.AMAZON)
        }
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    configure<KotlinJvmProjectExtension> {
        explicitApi()
    }

    val subproject = this
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set(subproject.name)
                    url.set(
                        "https://github.com/engine-public/engine-partner-api/blob/${version}/${
                            subproject.projectDir.toRelativeString(
                                rootDir
                            )
                        }/README.md"
                    )
                    inceptionYear.set("2025")
                    licenses {
                        license {
                            name = "Apache-2.0"
                            url.set("https://github.com/engine-public/engine-partner-api/blob/${version}/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            organizationUrl.set("https://github.com/engine-public")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/engine-public/engine-partner-api.git")
                        developerConnection.set("scm:git:https://github.com/engine-public/engine-partner-api.git")
                        url.set("https://github.com/engine-public/engine-partner-api")
                    }
                }
            }
        }
        repositories {
            maven {
                url = mavenStagingDir.get().asFile.toURI()
            }
        }
    }

    afterEvaluate {
        // for some reason, description isn't available in initial project evaluation, so we have to add it to the pom
        // after the subproject evaluation is complete
        configure<PublishingExtension> {
            (publications.getByName("maven") as MavenPublication).pom {
                description.set(subproject.description)
            }
        }
    }

    stageMavenCentral {
        dependsOn(tasks["publish"])
    }

    configure<KtlintExtension> {
        version.set("1.5.0")
        filter {
            /*
             * work around bug in the ktlint plugin that doesn't honor exclusions of
             * generated code (protobuf, etc.)
             */
            exclude {
                it.file.absolutePath.startsWith(layout.buildDirectory.get().asFile.absolutePath)
            }
        }
        reporters {
            reporter(ReporterType.CHECKSTYLE)
            reporter(ReporterType.HTML)
        }
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes(
                "Name" to project.name,
                "Specification-Title" to "Engine Partner API -- ${project.projectDir.name.toUpperCaseAsciiOnly()}",
                "Specification-Version" to version,
                "Specification-Vendor" to "HotelEngine, Inc., d/b/a Engine",
            )
        }
    }
}

allprojects {
    afterEvaluate {
        configurations.all {
            resolutionStrategy {
                // CVE-2024-12798: https://github.com/engine-public/engine-partner-api/security/dependabot/1
                // CVE-2024-12801: https://github.com/engine-public/engine-partner-api/security/dependabot/2
                force("ch.qos.logback:logback-core:[1.3.15,1.4[")
            }
        }
    }
}

val clean = tasks.register("clean", Delete::class) {
    group = "build"
    delete(rootProject.layout.buildDirectory)
}

tasks.register("writeVersion") {
    group = "build"
    doFirst {
        project
            .layout
            .buildDirectory
            .file("version.txt")
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(version.toString())
    }
}

afterEvaluate {
    // Find the projects that have proto files
    val protoProjects = project
        .subprojects
        .filter { p ->
            !fileTree(p.layout.projectDirectory.dir("src/main/proto")) {
                include("**/*.proto")
            }.isEmpty
        }

    val swaggerFiles = fileTree(projects.enginePartnerApiService.dependencyProject.layout.buildDirectory.dir("generated/source/proto/main/openapiv2")) {
        include("**/service.swagger.json")
    }

    val swaggerDir = project.layout.buildDirectory.dir("swagger")
    val baseSwaggerJson = swaggerDir.map { it.file("base.swagger.json") }
    val openApiMergeJson = swaggerDir.map { it.file("openapi-merge.json") }
    val enginePartnerApiSwaggerJson = swaggerDir.map { it.file("engine-partner-api.swagger.json") }

    val gson = DefaultProvider {
        GsonBuilder().setPrettyPrinting().create()
    }

    val generateBaseSwagger = tasks.register("generateBaseSwagger") {
        group = "swagger"
        description = "Generates the base swagger.json file containing the API metadata."
        outputs.file(baseSwaggerJson)
        doLast {
            baseSwaggerJson
                .get()
                .asFile
                .writeText(
                    gson
                        .get()
                        .toJson(
                            mapOf(
                                "swagger" to "2.0",
                                "info" to mapOf(
                                    "title" to "Engine Partner API",
                                    "description" to project.description,
                                    "version" to project.version.toString(),
                                    "contact" to mapOf(
                                        "name" to "Engine Partner API Support",
                                        "url" to "https://engine.com",
                                        "email" to "partner-api-support@engine.com"
                                    ),
                                    "license" to mapOf(
                                        "name" to "Apache License Version 2.0",
                                        "url" to "https://github.com/engine-public/engine-partner-api/blob/main/LICENSE"
                                    )
                                ),
                                "externalDocs" to "https://github.com/engine-public/engine-partner-api/releases/download/$version/documentation.zip",
                                "host" to "partner-api.engine.com",
                                "schemes" to listOf("https"),
                                "consumes" to listOf("application/json"),
                                "produces" to listOf("application/json")
                            )
                        )
                )
        }
    }

    val generateOpenApiMergeJson = tasks.register("generateOpenApiMergeJson") {
        group = "swagger"
        description = "Generates the configuration file used to merge OpenAPI definitions."

        // The swagger files are generated by protoc during the subprojects generateProto tasks
        protoProjects.forEach {
            dependsOn(it.tasks.findByName("generateProto"))
        }

        outputs.file(openApiMergeJson)

        doLast {
            openApiMergeJson
                .get()
                .asFile
                .writeText(
                    gson.get().toJson(
                        mapOf(
                            "inputs" to listOf(
                                baseSwaggerJson.get().asFile,
                                *swaggerFiles.toList().toTypedArray()
                            ).map {
                                mapOf("inputFile" to it.relativeTo(openApiMergeJson.get().asFile.parentFile).path)
                            },
                            "output" to enginePartnerApiSwaggerJson.get().asFile.relativeTo(enginePartnerApiSwaggerJson.get().asFile.parentFile).path
                        )
                    )
                )
        }
    }

    val mergeSwagger = tasks.register("mergeSwagger") {
        group = "swagger"
        description = "Merges all of the generated swagger service files into a single Engine Partner API swagger definition."

        protoProjects.forEach {
            dependsOn(it.tasks.findByName("generateProto"))
        }
        dependsOn(generateBaseSwagger)
        dependsOn(generateOpenApiMergeJson)

        inputs.file(baseSwaggerJson)
        inputs.file(openApiMergeJson)
        inputs.files(swaggerFiles)
        outputs.file(enginePartnerApiSwaggerJson)

        @Suppress("UNCHECKED_CAST")
        doLast {
            val services: List<Map<String, Any>> = swaggerFiles.map { gson.get().fromJson(it.readText(), Map::class.java) as Map<String, Any> }

            val allTags: Set<String> = services.flatMap { service ->
                (service["paths"] as Map<String, Map<String, Map<String, *>>>).flatMap { (_, methods) -> methods.flatMap { (_, keys) -> (keys["tags"] as List<String>)}}
            }.toSet()

            val allPaths: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
            services
                .map { service -> (service["paths"] as Map<String, Map<String, Any>>)}
                .forEach { servicePaths: Map<String, Map<String, *>> ->
                    servicePaths.forEach { (path, methods) ->
                        methods.forEach { (method, details) ->
                            if (allPaths.containsKey(path) && allPaths[path]!!.containsKey(method)) {
                                throw Exception("Conflicting definition of $path and $method:\n${allPaths[path]!![method]}\n-----\n${details}")
                            } else {
                                allPaths.computeIfAbsent(path) { mutableMapOf() }.set(method, details!!)
                            }
                        }
                    }
                }


            val allDefinitions = mutableMapOf<String, Any>()
            services
                .map { service -> service["definitions"] as Map<String, *> }
                .forEach { serviceDefs: Map<String, *> ->
                    serviceDefs.forEach { (definitionName, value) ->
                        if (allDefinitions.containsKey(definitionName)) {
                            if (allDefinitions[definitionName] != value) {
                                throw Exception("Conflicting definition of $definitionName: ${allDefinitions[definitionName]}\n!=\n${value}")
                            }
                        } else {
                            allDefinitions[definitionName] = value!!
                        }
                    }
                }

            val base = gson.get().fromJson(baseSwaggerJson.get().asFile.readText(), MutableMap::class.java) as MutableMap<String, Any>
            base["tags"] = allTags.map {
                mapOf("name" to it)
            }
            base["paths"] = allPaths
            base["definitions"] = allDefinitions

            enginePartnerApiSwaggerJson.get().asFile.writeText(
                gson.get().toJson(base)
            )
        }
    }

    val stageJsonContent = tasks.register<Copy>("stageJsonContent") {
        dependsOn(mergeSwagger)
        from(enginePartnerApiSwaggerJson)
        into(documentationStagingDir.map { it.dir("JSON") })
        includeEmptyDirs = false
    }

    val protosets = copySpec {
        // copy the descriptor set from the service project
        from(projects.enginePartnerApiService.dependencyProject.layout.buildDirectory.dir("generated/source/proto/main")) {
            include("**/*.desc")
        }
    }

    val protoStagingDir = project.layout.buildDirectory.dir("proto")

    val copyProtos = tasks.register<Copy>("copyProtos") {
        protoProjects.forEach { p ->
            dependsOn(p.tasks["generateProto"])
        }

        // copy all protos from all subprojects, maintaining namespace
        protoProjects.forEach { p ->
            from(p.layout.projectDirectory.dir("src/main/proto")) {
                include("**/*.proto")
            }
        }

        // copy the grpc-ecosystem/grpc-gateway license file
        from(projects.grpcEcosystemProtocGenOpenapiv2.dependencyProject.layout.projectDirectory.dir("src/main/resources")) {
            include("LICENSE")
            eachFile {
                this.path = "protoc-gen-openapiv2/LICENSE"
            }
        }

        into(protoStagingDir)
        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
    }

    tasks.jreleaserDeploy {
        dependsOn(stageMavenCentral)
    }

    val buildMarkdown = project.layout.buildDirectory.dir("markdown")
    val buildMarkdownStatic = buildMarkdown.map{ it.dir("static") }
    val buildMarkdownGenerated = buildMarkdown.map{ it.dir("generated") }

    val copyGeneratedMarkdown = tasks.register<Copy>("copyGeneratedMarkdown") {
        protoProjects.forEach { p ->
            dependsOn(p.tasks["generateProto"])
            from(p.layout.buildDirectory.dir("generated/source/proto/main/doc")) {
                include("**/*.html", "**/*.md")
            }
        }
        into(buildMarkdownGenerated.map { it.dir("gRPC") })
        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
    }

    val copyStaticMarkdown = tasks.register<Copy>("copyStaticMarkdown") {
        from("src/main/markdown") {
            include("**/*.md")
            exclude("**/_*.md")
        }
        into(buildMarkdownStatic)
        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
    }

    val addLinkFooters = tasks.register("addLinkFooters") {
        val linkFooter = project.layout.projectDirectory.file("src/main/markdown/_link_footer.md")
        dependsOn(copyStaticMarkdown, copyGeneratedMarkdown)
        inputs.file(linkFooter)
        inputs.dir(buildMarkdown)
        doLast {
            val footerText = linkFooter.asFile.readText().replace("{version}", project.version.toString())
            listOf(buildMarkdownStatic, buildMarkdownGenerated)
                .forEach { root ->
                    fileTree(root)
                        .filterNot { it.name.startsWith("_") }
                        .filter { it.isFile }
                        .forEach {
                            val relpath = root.get().asFile.relativeTo(it.parentFile).path.ifEmpty { "." }
                            it.appendText(footerText.replace("{relpath}", "$relpath/"))
                        }
                }
        }
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

    val fixGeneratedMarkdown = tasks.register("fixGeneratedMarkdown") {
        dependsOn(addLinkFooters)
        inputs.dir(buildMarkdownGenerated)
        outputs.dir(buildMarkdownGenerated)
        doLast {
            val patterns = mapOf(
                "engine-partner-api-book-lodging-$version.md" to Regex("""\[(engine\.book\.lodging\.v1\.(?:.*?))\]\((#.*?)\)"""),
                "engine-partner-api-common-$version.md" to Regex("""\[(engine\.common\.v1\.(?:.*?))\]\((#.*?)\)"""),
                "engine-partner-api-content-$version.md" to Regex("""\[(engine\.content\.v1\.(?:.*?))\]\((#.*?)\)"""),
                "engine-partner-api-service-$version.md" to Regex("""\[(engine\.service\.v1\.(?:.*?))\]\((#.*?)\)"""),
                "engine-partner-api-shop-lodging-$version.md" to Regex("""\[(engine\.shop\.lodging\.v1\.(?:.*?))\]\((#.*?)\)"""),
            )

            buildMarkdownGenerated.get().asFileTree.forEach {
                val oldText = it.readText()

                val newText = patterns
                    .entries
                    .fold(oldText
                        // fix bad table headers for "go" which is only two - instead of a minimum of 3
                        .replace(
                            Regex(""" -{1,2} \|"""),
                            " --- |"
                        )
                        // fix missing links out to google rpc models
                        .replace(
                            Regex("""\[google\.rpc\.Status\]\(#.*?\)"""),
                            "[google.rpc.Status](https://cloud.google.com/tasks/docs/reference/rpc/google.rpc#google.rpc.Status)"
                        )
                        // fix missing links out to google protobuf well known types
                        .replace(
                            Regex("""\[google\.protobuf\.Value\]\(#.*?\)"""),
                            "[google.protobuf.Value](https://protobuf.dev/reference/protobuf/google.protobuf/#value)"
                        )
                        // fix broken tables caused by line breaks within a cell
                        .replace(Regex("""\| ([^|]*?)\n+([^|]*?) \|""", RegexOption.MULTILINE), "| <p>$1</p><p>$2</p> |")
                    ) { acc, (fileName, pattern) ->
                        if (it.name == fileName) {
                            logger.info("No changes necessary to ${it.name}")
                            acc
                        } else {
                            logger.info("Changes are necessary for ${it.name}")
                            acc.replace(pattern, "[$1](./$fileName$2)")
                        }
                    }

                if (oldText != newText) {
                    // TODO show diff
                    it.writeText(newText)
                }
            }
        }
    }

    val npxCommand = resolveCommand("npx")

    val mdlUlIndent = "MD007"
    val mdlNoTrailingSpaces = "MD009"
    val mdlNoMultipleBlanks = "MD012"
    val mdlLineLength = "MD013"
    val mdlBlanksAroundHeadings = "MD022"
    val mdlNoDuplicateHeading = "MD024"
    val mdlNoInlineHtml = "MD033"
    val mdlNoBareUrls = "MD034"
    val mdlLinkImageReferenceDefinitions = "MD053"
    val mdlBlanksAroundTables = "MD058"

    val runMarkdownLinterOnStatic = tasks.register<Exec>("runMarkdownLinterOnStatic") {
        dependsOn(addLinkFooters)
        commandLine(
            npxCommand,
            "markdownlint-cli",
            "--disable",
            mdlLineLength,
            mdlLinkImageReferenceDefinitions,
            "--",
            buildMarkdownStatic.get().asFile.absolutePath
        )
    }

    val runMarkdownLinterOnGenerated = tasks.register<Exec>("runMarkdownLinterOnGenerated") {
        dependsOn(fixGeneratedMarkdown)
        commandLine(
            npxCommand,
            "markdownlint-cli",
            "--disable",
            mdlUlIndent,
            mdlNoMultipleBlanks,
            mdlLineLength,
            mdlBlanksAroundHeadings,
            mdlNoInlineHtml,
            mdlNoTrailingSpaces,
            mdlLinkImageReferenceDefinitions,
            mdlNoBareUrls,
            mdlNoDuplicateHeading,
            mdlBlanksAroundTables,
            "--",
            buildMarkdown.map{ it.dir("generated") }.get().asFile.absolutePath
        )
    }

    val stageDocContent = tasks.register<Copy>("stageDocContent") {
        dependsOn(runMarkdownLinterOnStatic, runMarkdownLinterOnGenerated, copyProtos)

        from("src/main/compose")
        from(protoStagingDir) {
            into("gRPC/proto")
        }
        from(buildMarkdownStatic)
        from(buildMarkdownGenerated)
        from("LICENSE")
        from("README.md")

        into(documentationStagingDir)

        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
    }

    tasks.register<Zip>("buildDocumentationDistribution") {
        group = "publishing"
        archiveFileName.set("documentation.zip")
        destinationDirectory.set(distDir)
        from(documentationStagingDir)
        dependsOn(copyProtos, stageJsonContent, stageDocContent)
    }

    val buildProtoDistribution = tasks.register<Zip>("buildProtoDistribution") {
        group = "publishing"
        archiveFileName.set("proto.zip")
        destinationDirectory.set(distDir)
        from(protoStagingDir)
        from(project.layout.projectDirectory.file("LICENSE")) {
            into("engine")
        }
        dependsOn(copyProtos)
    }

    tasks.register<Copy>("buildApiDefinitionDistribution") {
        group = "publishing"
        protoProjects.forEach {
            dependsOn(it.tasks.findByName("generateProto"))
        }
        // copy protos, too
        dependsOn(mergeSwagger, buildProtoDistribution)
        from(enginePartnerApiSwaggerJson)
        with(protosets)
        into(distDir)
        includeEmptyDirs = false
    }
}
