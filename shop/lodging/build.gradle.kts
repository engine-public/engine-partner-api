import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("com.google.protobuf")
}

description = "Models comprising the Lodging Shop functionality in the Engine Partner API."

dependencies {
    api(projects.grpcEcosystemProtocGenOpenapiv2)
    api(projects.enginePartnerApiCommon)
    api(projects.enginePartnerApiContent)
    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)
}

kotlin {
    explicitApi = ExplicitApiMode.Disabled
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
        all().forEach {
            it.generateDescriptorSet = true
            it.descriptorSetOptions.includeImports = true

            it.plugins {
                create("doc") {
                    option("markdown,${project.name}-${version}.md")
                }
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}