import com.google.gson.GsonBuilder
import org.apache.tools.ant.filters.ConcatFilter
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.internal.provider.DefaultProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import java.nio.file.Files
import java.time.Instant
import java.util.*
import kotlin.apply
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isSymbolicLink
import kotlin.io.writeText

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
    group = "publishing"
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
            val mavenUser = System.getenv("MAVEN_USERNAME")
            val mavenPassword = System.getenv("MAVEN_PASSWORD")
            val mavenUrl = System.getenv("MAVEN_DEPLOY_URL")
            maven {
                url = mavenUrl?.let { uri(it) } ?: mavenStagingDir.get().asFile.toURI()
                if (mavenUser != null) {
                    credentials {
                        username = mavenUser
                        password = mavenPassword
                    }
                }
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

tasks.register("clean", Delete::class) {
    group = "build"
    delete(rootProject.layout.buildDirectory)
}

val writeVersion = tasks.register("writeVersion") {
    val versionFile = project.layout.buildDirectory.map { it.file("version.txt") }
    group = "build"
    outputs.file(versionFile)
    outputs.upToDateWhen { !versionFile.get().asFile.exists() || versionFile.get().asFile.readText() != version.toString() }
    doFirst {
        versionFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(version.toString())
    }
}

val writeDateGenerated = tasks.register("writeDateGenerated") {
    val dateGeneratedFile = project.layout.buildDirectory.map { it.file("date_generated.txt") }
    group = "build"
    outputs.file(dateGeneratedFile)
    outputs.upToDateWhen { false }
    doFirst {
        dateGeneratedFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(Instant.now().epochSecond.toString())
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

    val swaggerDir = project.layout.buildDirectory.dir("swagger")
    val baseSwaggerJson = swaggerDir.map { it.file("base.swagger.json") }
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
                                        "url" to "https://engine-public.github.io/engine-partner-api",
                                        "email" to "partner-api-support@engine.com"
                                    ),
                                    "license" to mapOf(
                                        "name" to "Apache License Version 2.0",
                                        "url" to "https://github.com/engine-public/engine-partner-api/blob/main/LICENSE"
                                    )
                                ),
                                "externalDocs" to "https://engine-public.github.io/engine-partner-api",
                                "host" to "partner-api.engine.com",
                                "schemes" to listOf("https"),
                                "consumes" to listOf("application/json"),
                                "produces" to listOf("application/json")
                            )
                        )
                )
        }
    }

    val mergeSwagger = tasks.register("mergeSwagger") {
        group = "swagger"
        description = "Merges all of the generated swagger service files into a single Engine Partner API swagger definition."

        val swaggerFiles = fileTree(projects.enginePartnerApiService.dependencyProject.layout.buildDirectory.dir("generated/source/proto/main/openapiv2")) {
            include("**/service.swagger.json")
        }

        protoProjects.forEach {
            dependsOn(it.tasks.findByName("generateProto"))
        }
        dependsOn(generateBaseSwagger)

        inputs.file(baseSwaggerJson)
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


    val copyGeneratedMarkdown = tasks.register<Copy>("copyGeneratedMarkdown") {
        group = "markdown"
        protoProjects.forEach { p ->
            dependsOn(p.tasks["generateProto"])
            from(p.layout.buildDirectory.dir("generated/source/proto/main/doc")) {
                include("**/*.html", "**/*.md")
            }
        }
        into(project.layout.buildDirectory.dir("markdown/generated/copy"))
        eachFile {
            path = "api/grpc/" + path
        }
        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
    }

    val fixGeneratedMarkdown = tasks.register<Copy>("fixGeneratedMarkdown") {
        group = "markdown"
        dependsOn(copyGeneratedMarkdown)

        from(copyGeneratedMarkdown)
        into(project.layout.buildDirectory.dir("markdown/generated/fixed"))
        includeEmptyDirs = false

        val patterns = mapOf(
            "engine-partner-api-book-lodging-$version.md" to Regex("""\[(engine\.book\.lodging\.v1\.(?:.*?))\]\((#.*?)\)"""),
            "engine-partner-api-common-$version.md" to Regex("""\[(engine\.common\.v1\.(?:.*?))\]\((#.*?)\)"""),
            "engine-partner-api-content-$version.md" to Regex("""\[(engine\.content\.v1\.(?:.*?))\]\((#.*?)\)"""),
            "engine-partner-api-service-$version.md" to Regex("""\[(engine\.service\.v1\.(?:.*?))\]\((#.*?)\)"""),
            "engine-partner-api-shop-lodging-$version.md" to Regex("""\[(engine\.shop\.lodging\.v1\.(?:.*?))\]\((#.*?)\)"""),
        )

        eachFile {
            filter { line ->
                line
                    // fix bad table headers for "go" and "c#" which only have two dashes instead of a minimum of 3 to be a table header
                    .replace(
                        Regex(""" -{1,2} \|"""),
                        " --- |"
                    )
                    // fix missing links out to google rpc models
                    .replace(
                        Regex("""\[\.?google\.rpc\.Status\]\(#.*?\)"""),
                        "[google.rpc.Status](https://cloud.google.com/tasks/docs/reference/rpc/google.rpc#google.rpc.Status)"
                    )
                    // fix missing links out to google api well known types
                    .replace(
                        Regex("""\[\.?google\.api\.HttpBody\]\(#.*?\)"""),
                        "[google.api.HttpBody](https://cloud.google.com/tasks/docs/reference/rpc/google.api#google.api.HttpBody)"
                    )
                    // fix missing links out to google protobuf well known types
                    .replace(
                        Regex("""\[\.?google\.protobuf\.Value\]\(#.*?\)"""),
                        "[google.protobuf.Value](https://protobuf.dev/reference/protobuf/google.protobuf/#value)"
                    )
            }

            patterns
                .entries
                .forEach { (fileName, pattern) ->
                    if (name == fileName) {
                        logger.info("No changes necessary for $this")
                    } else {
                        filter { line ->
                            line.replace(pattern, "[$1](./$fileName$2)")
                        }
                    }
                }

            // protoc-gen-doc has a bug in its table rendering that breaks markdown tables if the comment on an RPC method
            // has more than one line.  This filter replaces those line breaks with <p/> tags.
            filter(FixMarkdownTablesFilter::class)
        }
    }

    val copyStaticMarkdown = tasks.register<Copy>("copyStaticMarkdown") {
        group = "markdown"
        from("src/main/markdown") {
            include("**/*.md")
            exclude("**/_*.md")
        }
        into(project.layout.buildDirectory.dir("markdown/static/copy"))
        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
    }

    val baseUrl = System.getenv("JEKYLL_BASE_URL") ?: "/engine-partner-api"

    val baseUrlCacheBuster = tasks.register("baseUrlCacheBuster") {
        val file = project.layout.buildDirectory.file("base_url")
        group = "site"
        outputs.file(file)
        outputs.upToDateWhen {
            file.get().asFile.run {
                exists() && readText().trim() == baseUrl
            }
        }
        doFirst {
            file.get().asFile.apply {
                parentFile.mkdirs()
                writeText(baseUrl)
            }
        }
    }

    val appendFootersTaskConfig: Copy.(contentType: String, dependsOnTask: TaskProvider<Copy>) -> Unit = { contentType, dependsOnTask ->
        group = "markdown"
        dependsOn(dependsOnTask)
        dependsOn(baseUrlCacheBuster)

        inputs.file(project.layout.projectDirectory.dir("src/main/markdown/partials").file("_link_footer.md"))

        from(dependsOnTask)
        into(project.layout.buildDirectory.dir("markdown/$contentType/footers"))
        includeEmptyDirs = false
        filter(
            mapOf(
                "append" to project.layout.projectDirectory.dir("src/main/markdown/partials").file("_link_footer.md").asFile
            ), ConcatFilter::class.java
        )
        filter(
            ReplaceTokens::class,
            "tokens" to mapOf("JEKYLL_BASE_URL" to baseUrl)
        )
    }

    val appendFootersToStaticMarkdown = tasks.register<Copy>("addLinkFootersToStatic") {
        appendFootersTaskConfig("static", copyStaticMarkdown)
    }

    val appendFootersToGeneratedMarkdown = tasks.register<Copy>("addLinkFootersToGenerated") {
        appendFootersTaskConfig("generated", fixGeneratedMarkdown)
    }

    val prependFrontMatterToGeneratedMarkdown = tasks.register<Copy>("writeFrontMatterToGenerated") {
        group = "markdown"
        dependsOn(appendFootersToGeneratedMarkdown)
        inputs.file(project.layout.projectDirectory.dir("src/main/markdown/templates").file("_front_matter.md"))

        from(appendFootersToGeneratedMarkdown)
        into(project.layout.buildDirectory.dir("markdown/generated/frontmatter"))
        eachFile {
            filter(
                ConcatFilter::class,
                mapOf(
                    "prepend" to project.layout.projectDirectory.dir("src/main/markdown/templates").file("_front_matter.md").asFile,
                )
            )
            filter(
                ReplaceTokens::class,
                "tokens" to mapOf(
                    "TITLE" to relativePath.segments.last().removeSuffix(".md").replace("-$version", ""),
                    "PARENT" to "gRPC",
                    "PERMALINK" to "/" + relativePath.toString().replace(".md", ".html"),
                )
            )
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
    val mdlTableColumnStyle = "MD060"

    val runMarkdownLinterOnStaticMarkdown = tasks.register<Exec>("runMarkdownLinterOnStaticMarkdown") {
        group = "formatting"
        dependsOn(appendFootersToStaticMarkdown)
        inputs.files(appendFootersToStaticMarkdown)
        commandLine(
            npxCommand,
            "markdownlint-cli",
            "--disable",
            mdlLineLength,
            mdlLinkImageReferenceDefinitions,
            "--",
            *appendFootersToStaticMarkdown.get().outputs.files.map { it.absolutePath }.toTypedArray()
        )
    }

    val runMarkdownLinterOnGeneratedMarkdown = tasks.register<Exec>("runMarkdownLinterOnGeneratedMarkdown") {
        group = "formatting"
        // it is intentional that we're validating the markdown before frontmatter
        dependsOn(appendFootersToGeneratedMarkdown)
        inputs.files(appendFootersToGeneratedMarkdown)
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
            mdlTableColumnStyle,
            "--",
            *appendFootersToGeneratedMarkdown.get().outputs.files.map { it.absolutePath }.toTypedArray()
        )
    }

    val artifactRenamer = { originalName: String ->
        val allowedExtensions = listOf(".zip", ".swagger.json", ".desc")
        val foundExtension = checkNotNull(allowedExtensions.find { extension -> originalName.endsWith(extension) }) {
            "$originalName doesn't have an extension for $allowedExtensions"
        }
        val baseName = originalName.removePrefix("engine-partner-api").removePrefix("-").removeSuffix(foundExtension).replace("-$version", "")

        listOfNotNull(
            "engine-partner-api",
            baseName.ifEmpty { null },
            version
        )
            .joinToString("-") + foundExtension
    }

    val stageProtoDistribution = tasks.register<Zip>("buildProtoDistribution") {
        group = "publishing"

        dependsOn(projects.grpcEcosystemProtocGenOpenapiv2.dependencyProject.tasks["goStageProtocGenOpenapiV2Protos"])

        // copy all protos from all subprojects, maintaining namespace
        protoProjects.forEach { p ->
            from(p.layout.projectDirectory.dir("src/main/proto")) {
                include("**/*.proto")
            }
        }

        from(project.layout.projectDirectory.file("LICENSE")) {
            into("engine")
        }

        from(projects.grpcEcosystemProtocGenOpenapiv2.dependencyProject.tasks["goStageLicenseFile"]) {
            into("protoc-gen-openapiv2")
        }

        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
        archiveFileName = artifactRenamer("protos.zip")
        destinationDirectory.set(stagingDir.map { it.dir("proto") })
    }

    val protoset = projects.enginePartnerApiService.dependencyProject.layout.buildDirectory.dir("generated/source/proto/main").map { it.file("descriptor_set.desc") }

    tasks.register<Copy>("buildApiDefinitionDistribution") {
        group = "publishing"
        dependsOn(mergeSwagger, stageProtoDistribution)
        from(stageProtoDistribution) {
            rename(artifactRenamer)
        }
        from(enginePartnerApiSwaggerJson) {
            rename(artifactRenamer)
        }
        from(protoset) {
            rename(artifactRenamer)
        }
        into(distDir)
        includeEmptyDirs = false
    }

    val stagingSiteDir = stagingDir.map { it.dir("site") }

    val stageSiteDownloads = tasks.register<Copy>("stageSiteDownloads") {
        group = "site"
        from(protoset)
        from(stageProtoDistribution)
        from(mergeSwagger)
        rename(artifactRenamer)
        into(stagingSiteDir.map { it.dir("downloads") })
    }

    val stageSiteLiquidIncludes = tasks.register<Copy>("stageSiteLiquidIncludes") {
        group = "site"
        from(writeVersion)
        from(writeDateGenerated)
        from(project.layout.projectDirectory.file("README.md"))
        into(stagingSiteDir.map { it.dir("_includes") })
    }

    val stageSiteMarkdown = tasks.register<Copy>("stageSiteContent") {
        group = "site"
        dependsOn(runMarkdownLinterOnStaticMarkdown, runMarkdownLinterOnGeneratedMarkdown)
        dependsOn(appendFootersToStaticMarkdown, prependFrontMatterToGeneratedMarkdown)
        from(appendFootersToStaticMarkdown)
        from(prependFrontMatterToGeneratedMarkdown)
        into(stagingSiteDir.map { it.dir("content") })
    }

    val stageSiteGrpcDocRedirects = tasks.register<Copy>("stageSiteGrpcDocRedirects") {
        group = "site"
        dependsOn(copyGeneratedMarkdown)
        inputs.file(project.layout.projectDirectory.dir("src/main/html/templates").file("_redirect.html"))

        from(copyGeneratedMarkdown)
        into(stagingSiteDir.map { it.dir("content") })
        eachFile {
            val redirect = "/" + relativePath.toString().replace(".md", ".html")
            val permalink = redirect.replace("-$version", "")
            filter(
                WriteRedirectFilter::class,
                "permalink" to permalink,
                "redirectUrl" to redirect,
                "templateFile" to project.layout.projectDirectory.dir("src/main/html/templates").file("_redirect.html").asFile
            )
        }
        rename { it.replace("-$version", "").replace(".md", ".html") }
    }

    val stageSiteHtml = tasks.register<Copy>("stageSiteHtml") {
        group = "site"
        from(project.layout.projectDirectory.dir("src/main/html")) {
            include("**/*.html")
            exclude("**/_*.html")
        }
        into(stagingSiteDir)
        includeEmptyDirs = false
    }

    val stageSiteJekyll = tasks.register<Copy>("stageSiteJekyll") {
        group = "site"
        from(project.layout.projectDirectory.dir("src/main/jekyll"))
        into(stagingSiteDir)
        includeEmptyDirs = false
    }

    val stageSite = tasks.register("stageSite") {
        group = "site"
        dependsOn(stageSiteDownloads)
        dependsOn(stageSiteMarkdown)
        dependsOn(stageSiteHtml)
        dependsOn(stageSiteLiquidIncludes)
        dependsOn(stageSiteGrpcDocRedirects)
        dependsOn(stageSiteJekyll)
    }

    val bundleCommand = resolveCommand("bundle")

    val bundleInstall = tasks.register<Exec>("bundleInstall") {
        group = "jekyll"
        dependsOn(stageSite)
        workingDir(stagingSiteDir)
        commandLine(bundleCommand, "install")
    }

    tasks.register<Exec>("serveSite") {
        group = "jekyll"
        dependsOn(bundleInstall)
        dependsOn(stageSite)
        workingDir(stagingSiteDir)
        commandLine(
            bundleCommand,
            "exec",
            "jekyll",
            "serve",
            "--baseurl", baseUrl,
        )
    }

    tasks.register<Exec>("buildSite") {
        group = "publishing"
        dependsOn(bundleInstall)
        dependsOn(stageSite)
        workingDir(stagingSiteDir)
        commandLine(
            bundleCommand,
            "exec",
            "jekyll",
            "build",
            "--destination",
            distDir.map { it.dir("site") }.get().asFile.absolutePath,
            "--baseurl", baseUrl,
        )
    }
}
