---
description: Global instructions for GitHub Copilot reviewing PRs in this repository.
---
# GitHub Copilot Reviewer Instructions

## Repository Summary
This repository contains protobuf contracts and generated artifacts (Swagger/OpenAPI, protobuf descriptor sets, docs site) for partners integrating with Engine’s Partner API for lodging bookings.

The API contracts are defined using protobuf with a subset of the APIs supporting HTTP/JSON interactions using the gRPC-Gateway protoc-gen-openapiv2 protoc compiler to generate the OpenAPI specification according to the included `google.api.http` and `grpc.gateway.protoc_gen_openapiv2` annotations.

## Protobuf Modules
Each protobuf module should contain proto definition files in a path corresponding to the defined package. Packages are organized by domain (`shop`, `book`, `content`, `common`), by vertical (`lodging`, `common`), and api version (`v1`).
- `service/`: Module containing the gRPC service definitions with request, response, and error types.
- `book/`: Models comprising the Transact and Manage functionality in the Engine Partner API.
- `content/`: Models that define Content served by the Engine Partner API.
- `shop/`: Models comprising the Shop functionality in the Engine Partner API.
- `common/`: Common models shared by verticals in the Engine Partner API.

Examples would include `service/src/main/proto/engine/content/service/v1` and `common/src/main/proto/engine/common/v1`

## Other Modules
- `src`: The Jekyll templates and code used to generate the documentation site from proto definitions.
- `buildSrc/`: Custom build scripts.
- `grpc-ecosystem-protoc-gen-openapiv2/src/main/`: The source code for the gRPC-Gateway protoc-gen-openapiv2 protoc compiler. These files should not be modified.

## Agent Reviewer Workflow
- Read PR description and identify modules touched (e.g., `common/`, `service/`, `content/`).
- Check Gradle module wiring (`settings.gradle.kts`) and any `build.gradle.kts` edits for correctness.
- Validate changes against policies and best practices.
- Ensure `/.github/copilot-instructions.md` and other `/.github/*.instructions.md` agent reviewer files are updated to reflect new paths, steps, or policies.
- Leave concise comments with file paths and suggested minimal fixes.
