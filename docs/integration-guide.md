# Integration Guide

## Integration Choices

### gRPC
The Engine Partner APIs are available via [gRPC](https://grpc.io).
The proto definitions are available to compile any of the supported language bindings.
Engine publishes pre-compiled client bindings for the following language:
* JVM (Java/Kotlin)

Please contact the Engine Partner Integration team to request new client bindings libraries.

### HTTP/JSON
A subset of the Engine Partner APIs have REST-inspired HTTP/JSON implementations as defined in our published [Swagger Definitions](./HTTP/content-service-swagger.json).
HTTP/JSON provides a simpler integration at the cost of additional latency and increased payload sizes.

## Authentication
Authentication to the Engine Partner APIs is managed via [Mutual TLS (mTLS)](https://en.wikipedia.org/wiki/Mutual_authentication#mTLS).
Please contact the Engine Partner Integration team to have a private key provisioned for access to the APIs.
