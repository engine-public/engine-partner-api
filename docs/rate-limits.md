# Rate limits

Rate limiting is executed on a per-endpoint basis.
Rate limits can be adjusted by contacting the Engine Partner Integration team.

## Responding to rate limits

When a rate limit has been exceeded, subsequent requests will fail with an HTTP 429.
Each rate limit defines a burst (per minute) rate and a sustained (per hour) rate.
The burst rate allows high throughput for shorter periods of time, but still counts against the hourly total rate.

Each request will have three headers populated:
* `ratelimit-reset: S`, where S is the number of seconds remaining in the current reset period
* `ratelimit-limit: N`, where N is the number of requests allowed within the reset period
* `ratelimit-remaining: R`, where R is the number of requests you have left within the reset period

## Default Rate Limits

| API              | Burst Count | Burst Period | Sustained Count | Sustained Period |
|------------------|------------:|--------------|----------------:|------------------|
| Property Listing |         400 | minute       |          18,000 | hour             |
