---
title: Versioning
permalink: /versioning.html
---

<!-- markdownlint-disable-next-line MD025 -->
# API Versioning

The Engine Partner API follows [Semantic Versioning 2.0.0](https://semver.org).

## Breaking Changes

When required, breaking changes to the Engine Partner API will be released along with a major version number increase.
Upon introduction of the breaking change, the API will be forked by version number such that the prior version will be available until the support window expires.
Please see the [Deprecation Policy] for full details.

Breaking changes include:

* Deletion of Messages or Services
* Moving a definition from one proto file to another
* Removing a field from a Message
* Removing an RPC from a Service
* Removing an enum constant
* Making an implicit field optional in a response message
* Making an optional field implicit in a request message

When it is necessary to remove Messages, Fields, Enums, or Enum constants, a minor release will precede the change marking the member as deprecated.

## Minor Changes

Minor and Patch changes will be introduced by the [Engine] team to add new features and functionality to our [Engine Partner API].

{: .attention}
Minor changes will always be wire compatible with all prior minor revisions of the current Major version.
However, depending on the language implementation in use, changes may be necessary for compilation to succeed when updating to new API clients.
For example, the addition of a new enum constant may violate branch completeness (kotlin, scala), and nullability changes may violate compilation checks for specific languages (kotlin, typescript).

Minor changes include:

* Introduction of new Services, or new RPCs to existing Services
* Introduction of new Messages
* Addition of new fields to Messages
* Addition of new enum constants
* Making an implicit request message field optional.
* Making an optional response message field implicit.

## Patch Changes

For the purposes of the [Engine Partner API], patch releases will include updates to documentation and comments only.

## Support for deprecated versions

[Engine Partner API]: https://github.com/engine-public/engine-partner-api
[Deprecation Policy]: #support-for-deprecated-versions