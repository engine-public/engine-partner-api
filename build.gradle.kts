import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import java.util.Calendar

plugins {
    id("org.jreleaser")

    kotlin("jvm").apply(false)
    id("org.jlleitschuh.gradle.ktlint").apply(false)
}

fun calculateVersion(): String {
    return System
        .getenv("ENGINE_BUILD_VERSION")
        ?: "0.0.0-pre.0" // temporary fallback version
}

group = "com.engine"
version = calculateVersion()

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

tasks.register("clean", Delete::class) {
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

val protoProjects = listOf(
    projects.grpcEcosystemProtocGenOpenapiv2,
    projects.enginePartnerApiCommon,
    projects.enginePartnerApiContent,
    projects.enginePartnerApiService
)

afterEvaluate {
    val protosets = copySpec {
        // copy the descriptor set from the service project
        from(projects.enginePartnerApiService.dependencyProject.layout.buildDirectory.dir("generated/source/proto/main")) {
            include("**/*.desc")
        }
    }

    val stageProtoContent = tasks.register<Copy>("stageProtoContent") {
        protoProjects.forEach { p ->
            dependsOn(p.dependencyProject.tasks["generateProto"])
        }

        // copy all protos and docs from all subprojects, maintaining namespace
        protoProjects.forEach { p: DelegatingProjectDependency ->
            from(p.dependencyProject.layout.projectDirectory.dir("src/main/proto")) {
                include("**/*.proto")
            }
            from(p.dependencyProject.layout.buildDirectory.dir("generated/source/proto/main/doc")) {
                include("**/*.html", "**/*.md")
            }
        }

        // copy the grpc-ecosystem/grpc-gateway license file
        from(projects.grpcEcosystemProtocGenOpenapiv2.dependencyProject.layout.projectDirectory.dir("src/main/resources")) {
            include("LICENSE")
            eachFile {
                this.path = "protoc-gen-openapiv2/LICENSE"
            }
        }

        with(protosets)

        into(documentationStagingDir.map { it.dir("gRPC") })
        fileMode = "0666".toInt(8)
        dirMode = "0777".toInt(8)
        includeEmptyDirs = false
    }

    val swaggers = copySpec {
        from(projects.enginePartnerApiService.dependencyProject.layout.buildDirectory.dir("generated/source/proto/main/openapiv2")) {
            include("**/service.swagger.json")
            eachFile {
                path = "${path.split("/")[1]}.swagger.json"
            }
        }
    }

    val stageJsonContent = tasks.register<Copy>("stageJsonContent") {
        protoProjects.forEach {
            dependsOn(it.dependencyProject.tasks.findByName("generateProto"))
        }
        with(swaggers)
        into(documentationStagingDir.map { it.dir("JSON") })
        includeEmptyDirs = false
    }

    val stageDocContent = tasks.register<Copy>("stageDocContent") {
        from("docs/")
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
        dependsOn(stageProtoContent, stageJsonContent, stageDocContent)
    }

    tasks.register<Copy>("buildApiDefinitionDistribution") {
        group = "publishing"
        protoProjects.forEach {
            dependsOn(it.dependencyProject.tasks.findByName("generateProto"))
        }
        with(swaggers)
        with(protosets)
        into(distDir)
        includeEmptyDirs = false
    }

    tasks.jreleaserDeploy {
        dependsOn(stageMavenCentral)
    }
}

