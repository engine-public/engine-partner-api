---
title: Integration paths
permalink: /integration-paths.html
parent: Getting Started
nav_order: 2
has_children: true
---

<!-- markdownlint-disable-next-line MD025 -->
# Integration paths

The Omni API offers two integration approaches — [Omni Swift] and [Omni Halo].
Choosing the right path early is critical — it determines your architecture, timeline, and engineering scope.

<!-- markdownlint-capture -->
<!-- markdownlint-disable MD033 -->
<details markdown="block">
  <summary>
    Table of contents
  </summary>
  {: .text-delta }
1. TOC
{:toc}
</details>
<!-- markdownlint-restore -->

## Path decision guide

```mermaid
---
title: Path Decision Guide
---
flowchart TB
    q{"Do you want to build your own shopping,<br/>checkout, and booking UI from scratch?"}
    swift["<b>Omni Swift</b><br/><i>Deep linking to Engine's<br/>checkout experience</i>"]
    halo["<b>Omni Halo</b><br/><i>Complete control via gRPC<br/>or HTTP/JSON</i>"]

    q -- No --> swift
    q -- Yes --> halo

    classDef swiftStyle fill:#c8d84e,stroke:#27262b,color:#27262b
    classDef haloStyle fill:#27262b,stroke:#27262b,color:#fff
    class swift swiftStyle
    class halo haloStyle
```

## The two paths

### Omni Swift — turnkey deep linking

Generate deep links to Engine's checkout experience.
You control hotel discovery, while Engine handles room selection, checkout, payments, and cancellations.

[Learn more about Omni Swift →][Omni Swift]

### Omni Halo — full API integration

Full control over shopping, booking, and management via gRPC or HTTP/JSON.
Build the entire experience within your platform.

[Learn more about Omni Halo →][Omni Halo]

## Side-by-side comparison

|                       | Omni Swift                                                  | Omni Halo                                          |
|-----------------------|-------------------------------------------------------------|----------------------------------------------------|
| **What you build**    | Discovery UI + redirect to Engine for checkout              | Full end-to-end booking experience                 |
| **Payment handling**  | Engine handles payments                                     | You handle payments & compliance                   |
| **UI control**        | Discovery: yours. Checkout: Engine-hosted                   | Complete control over every screen                 |
| **Post-booking**      | Engine manages cancellations & disputes                     | You manage via API                                 |
| **Best for**          | Fast time-to-market, non-travel-native                      | Travel platforms, full brand control               |
| **API surface**       | Content + deep link URL construction                        | Content, Shopping, Booking, Management             |

## Not sure which to choose?

Contact [omni-partnerships@engine.com](mailto:omni-partnerships@engine.com) and our team will help you align on the right approach for your product, timeline, and engineering scope.
