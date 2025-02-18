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

## Curl example
---
```bash
curl --verbose --key /path/to/private.key --cert /path/to/cert.pem 'https://partner-api.engine.com/content/v1/property?request.criteria.radius.coordinates.latitude=30.361589&request.criteria.radius.coordinates.longitude=-97.747976&request.criteria.radius.radius.value=5&request.pageSize=1' -H 'accept: application/json'
```
### Headers
```
< ratelimit-limit: 400
< ratelimit-remaining: 399
< ratelimit-reset: 48
< com-engine-request-id: 0833dc12-5347-9938-bb61-7d74bc264c9e
```

### Body
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