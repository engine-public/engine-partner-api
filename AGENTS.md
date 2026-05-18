# AGENTS.md

## Repository Summary
This repository contains protobuf contracts and generated artifacts (Swagger/OpenAPI, protobuf descriptor sets, docs site) for partners integrating with Engine's Partner API for lodging bookings.

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

## Reviewer Workflow
- Read PR description and identify modules touched (e.g., `common/`, `service/`, `content/`).
- Check Gradle module wiring (`settings.gradle.kts`) and any `build.gradle.kts` edits for correctness.
- Validate changes against policies and best practices.
- Ensure agent instruction files (`AGENTS.md`, `.github/copilot-instructions.md`, and `.github/*.instructions.md`) are updated to reflect new paths, steps, or policies.
- Leave concise comments with file paths and suggested minimal fixes.

## Protobuf and API Change Policies and Best Practices

These policies apply to all `*.proto` files in this repository.

### Deprecation and breaking changes
- Do not remove existing fields/types, or make breaking API changes. Deprecate instead unless a version bump justifies removal.
- Deprecations require documentation outlining the alternative to use and a timeline for removal.

### Documentation
- Doc comments power public documentation. All types and fields should have well-formatted, descriptive comments.
- Doc comments include constraints (e.g. minimum, maximum), form (e.g. string format, standards, example values), and function of each field. 
- Optional fields should specify the conditions they are expected to be present or absent.
- Only C++ style non-block comments (`//`) should be used for documentation. C style block comments (`/* ... */`) are not allowed.
- Message types referenced in doc comments should be formatted as markdown footer links.
- Each new type added must be linked in `src/main/markdown/partials/_link_footer.md` so generated docs link correctly.
- Use `google.api.http` and `grpc.gateway.protoc_gen_openapiv2` annotations to support HTTP/JSON translation. Documenting titles and RPC descriptions and errors using values that match the name and package of the type.
  For example:
  ```protobuf
  package engine.book.lodging.v1;
  
  message ReservationDetails {
    option (grpc.gateway.protoc_gen_openapiv2.options.openapiv2_schema) = {
      json_schema: {
        title: "Book_Lodging_ReservationDetails_v1"
      }
    };
  }
  ```
- All references to standards (e.g. ISO-8601, IANA) should include a link to the relevant documentation.  For example "date times conform to ISO-8601 see https://en.wikipedia.org/wiki/ISO_8601#Times"
- Each file should contain a copyright header with the Apache 2.0 license.

### Type definition and file structure
- Place all types in the appropriate package and module with correct granularity and naming (e.g. repeated fields should use a plural name).
- All proto definitions should have one top-level entity (message, enum) per file. This promotes simpler refactoring and reduces transitive dependencies for faster builds and smaller binaries. Use good judgement when diverging from this rule to avoid circular dependencies or easier reading of inherently coupled messages.
- Only scalar fields should be marked as optional. Message types are implicitly optional and should not have an explicit optional annotation.
- Reuse common types whenever possible.

### Naming conventions and style
- Message, Services, and RPC names use PascalCasing.
- Fields use snake_casing.
- Enum values should be prefixed with the enum type name to avoid collisions (e.g. `AMENITY_AVAILABILITY_UNKNOWN`, `AMENITY_AVAILABILITY_INCLUDED` for an enum named `AmenityAvailability`).
- The first enum values should always define a safe default with tag 0 that indicates an unknown or unspecified value.
- Use fully qualified type names (`.engine.content.service.v1.GetPropertiesRequest`) for all fields to ensure schema registry compatibility.
- Do not use excessive blank lines.

## Tool-Specific Instruction Files
- GitHub Copilot: See `.github/copilot-instructions.md`
- GitHub Copilot path-scoped reviews: See `.github/proto.instructions.md`
