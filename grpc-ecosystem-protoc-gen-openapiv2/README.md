# grpc-ecosystem-protoc-gen-openapiv2

This subproject packages the proto definitions provided by the [:octocat: grpc-ecosystem/grpc-gateway](https://github.com/grpc-ecosystem/grpc-gateway)'s [OpenAPI protoc compiler](https://github.com/grpc-ecosystem/grpc-gateway?tab=readme-ov-file#6-optional-generate-openapi-definitions-using-protoc-gen-openapiv2) in a way that they may be used by other JVM projects.

At the time of this writing, the recommendation of the team was to copy the proto files into your own source to compile, but this puts a burden on API developers and consumers of our protos.
This jar provides, unmodified, and with the original license intact, a means to compile using the gradle-protobuf-plugin toolchain.
