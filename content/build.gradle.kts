import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("com.google.protobuf")
}

description = "Models that define Content served by the Engine Partner API."

dependencies {
    api(projects.enginePartnerApiCommon)
    api(projects.grpcEcosystemProtocGenOpenapiv2)

    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)
    api(libs.google.api.grpc.common.protos)
}

kotlin {
    explicitApi = ExplicitApiMode.Disabled
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
    }
    generateProtoTasks {
        all().forEach {

            it.generateDescriptorSet = true
            it.descriptorSetOptions.includeImports = true

            it.plugins {
                create("grpc")
                create("grpckt")
                create("doc") {
                    option("html,${project.name}-${version}.html")
                }
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}
