import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("com.google.protobuf")
}

description = "Notification event model and per-channel delivery types (e.g. webhook subscriptions and delivery attempts)."

dependencies {
    api(projects.grpcEcosystemProtocGenOpenapiv2)
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
