---
description: 'Policies and best practices for changes to the protobuf API definitions in this repository.'
apply-to: '**/*.proto'
---
# Protobuf and API Change Policies and Best Practices
## Deprecation and breaking changes
- Do not remove existing fields/types, or make breaking API changes. Deprecate instead unless a version bump justifies removal.
- Deprecations require documentation outlining the alternative to use and a timeline for removal.

## Documentation
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

## Type definition and file structure
- Place all types in the appropriate package and module with correct granularity and naming (e.g. repeated fields should use a plural name).
- All proto definitions should have one top-level entity (message, enum) per file. This promotes simpler refactoring and reduces transitive dependencies for faster builds and smaller binaries. Use good judgement when diverging from this rule to avoid circular dependencies or easier reading of inherently coupled messages.
- Only scalar fields should be marked as optional. Message types are implicitly optional and should not have an explicit optional annotation.
- Reuse common types whenever possible.

## Naming conventions and style
- Message, Services, and RPC names use PascalCasing.
- Fields use snake_casing.
- Enum values should be prefixed with the enum type name to avoid collisions (e.g. `AMENITY_AVAILABILITY_UNKNOWN`, `AMENITY_AVAILABILITY_INCLUDED` for an enum named `AmenityAvailability`).
- The first enum values should always define a safe default with tag 0 that indicates an unknown or unspecified value.
- Use fully qualified type names (`.engine.content.service.v1.GetPropertiesRequest`) for all fields to ensure schema registry compatibility.
- Do not use excessive blank lines.
