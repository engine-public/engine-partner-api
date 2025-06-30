---
title: Integration Guide
permalink: /integration-guide.html
---

<!-- markdownlint-disable-next-line MD025 -->
# Integration Guide

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

## Authentication

Authentication to the Engine Partner APIs is managed via [Mutual TLS (mTLS)](https://en.wikipedia.org/wiki/Mutual_authentication#mTLS).
Please contact the Engine Partner Integration team to have a private key provisioned for access to the APIs.

## Integration Choices

---

### gRPC

The Engine Partner APIs are available via [gRPC](https://grpc.io).
The proto definitions are available to compile any of the supported language bindings.
Engine publishes pre-compiled client bindings for the following language:

* [JVM](https://central.sonatype.com/artifact/com.engine/engine-partner-api-service) (Java/Kotlin)

Please contact the Engine Partner Integration team to request new client bindings libraries.

For more information:

* [gRPC API Definition]

#### grpcurl example

```bash
grpcurl -protoset descriptor_set.desc -key /path/to/private.key -cert /path/to/cert.pem -d '{
    "request": {
        "criteria": {
            "radius": {
                "coordinates": {
                    "latitude": 30.361589,
                    "longitude": -97.747976
                }
            }
        },
        "page_size": 1
    }
}' partner-api.engine.com:443 engine.content.api.v1.ContentServiceV1.ListProperties
```

#### gRPC Response Headers

```text
com-engine-request-id: 5e80f344-09b9-92f3-bdec-1db632f4a75e
content-type: application/grpc
date: Fri, 21 Feb 2025 16:41:08 GMT
grpc-accept-encoding: gzip
ratelimit-limit: 400
ratelimit-remaining: 399
ratelimit-reset: 52
server: istio-envoy
x-envoy-upstream-service-time: 80
```

#### gRPC Response Body (formatted as JSON)

```json
{
  "properties": [
    {
      "property": {
        "id": "P0000000000000102095",
        "name": "DoubleTree by Hilton Hotel Austin Northwest Arboretum",
        "physicalAddress": {
          "addressLine": [
            "8901 Business Park Dr"
          ],
          "administrativeArea": "TX",
          "locality": "Austin",
          "postalCode": "78759",
          "countryCode": "US"
        },
        "coordinates": {
          "latitude": 30.379014215153,
          "longitude": -97.740766025294
        },
        "heroImageUri": "https://i.travelapi.com/lodging/1000000/10000/3700/3686/e6110757_z.jpg",
        "description": "Close to major motorways and a short drive from local attractions and Austin city center, DoubleTree by Hilton Austin Northwest - Arboretum features on-site dining options and many modern facilities, including a 24-hour fitness center. Guests at the DoubleTree by Hilton Austin Northwest - Arboretum can start the day with a cup of coffee from in-room coffeemakers, or take advantage of in-room microwaves and small refrigerators. The hotel also features an outdoor pool and a modern business center. Area points of interest, including the Arboretum Entertainment and Shopping District can be found near the DoubleTree by Hilton Austin Northwest-Arboretum. Scenic Lake Travis and the Texas State Capitol are also nearby."
      },
      "distance": {
        "value": 1.2752452698971317
      }
    }
  ],
  "nextPageToken": "00C0586FDED6BA8C58403E5C9118C197E541200000000010000000001"
}
```

---

### HTTP/JSON

A subset of the Engine Partner APIs have REST-inspired HTTP/JSON implementations as defined in our published [Swagger Definitions](./HTTP/content-service-swagger.json).
HTTP/JSON provides a simpler integration at the cost of additional latency and increased payload sizes.

For more information:

* [Swagger API Definition]

#### curl example

```bash
curl --verbose --key /path/to/private.key --cert /path/to/cert.pem 'https://partner-api.engine.com/content/v1/property?request.criteria.radius.coordinates.latitude=30.361589&request.criteria.radius.coordinates.longitude=-97.747976&request.pageSize=1' -H 'accept: application/json'
```

#### HTTP Headers

```text
< ratelimit-limit: 400
< ratelimit-remaining: 399
< ratelimit-reset: 48
< com-engine-request-id: 0833dc12-5347-9938-bb61-7d74bc264c9e
```

#### HTTP Response Body

```json
{
  "properties": [
    {
      "property": {
        "id": "P0000000000000102095",
        "name": "DoubleTree by Hilton Hotel Austin Northwest Arboretum",
        "physicalAddress": {
          "recipients": [],
          "addressLine": [
            "8901 Business Park Dr"
          ],
          "administrativeArea": "TX",
          "locality": "Austin",
          "postalCode": "78759",
          "countryCode": "US"
        },
        "coordinates": {
          "latitude": 30.379014215153,
          "longitude": -97.740766025294
        },
        "heroImageUri": "https://i.travelapi.com/lodging/1000000/10000/3700/3686/e6110757_z.jpg",
        "description": "Close to major motorways and a short drive from local attractions and Austin city center, DoubleTree by Hilton Austin Northwest - Arboretum features on-site dining options and many modern facilities, including a 24-hour fitness center. Guests at the DoubleTree by Hilton Austin Northwest - Arboretum can start the day with a cup of coffee from in-room coffeemakers, or take advantage of in-room microwaves and small refrigerators. The hotel also features an outdoor pool and a modern business center. Area points of interest, including the Arboretum Entertainment and Shopping District can be found near the DoubleTree by Hilton Austin Northwest-Arboretum. Scenic Lake Travis and the Texas State Capitol are also nearby."
      },
      "distance": {
        "value": 1.2752452698971317,
        "unit": "DISTANCE_UNIT_MILE"
      }
    }
  ],
  "nextPageToken": "00C0586FDED6BA8C58403E5C9118C197E540A00000000010000000001"
}
```
