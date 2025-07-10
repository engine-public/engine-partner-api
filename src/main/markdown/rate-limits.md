---
title: Rate Limits
permalink: /rate-limits.html
---

<!-- markdownlint-disable-next-line MD025 -->
# Rate limits

Rate limiting is executed on a per-endpoint basis.

## Responding to rate limits

When a rate limit has been exceeded, subsequent requests will fail with an HTTP 429.
Each rate limit defines a burst (per minute) rate and a sustained (per hour) rate.
The burst rate allows high throughput for shorter periods of time, but still counts against the hourly total rate.

Each request will have three headers populated:

* `ratelimit-reset: S`, where S is the number of seconds remaining in the current reset period
* `ratelimit-limit: N`, where N is the number of requests allowed within the reset period
* `ratelimit-remaining: R`, where R is the number of requests you have left within the reset period

{: .attention}
Both burst and sustained rate limits are communicated via the same headers described above.
The `rate-limit-remaining` header will always accurately reflect the number of calls you have remaining, even if that number is less than the burst allotment.
The `rate-limit-reset` header will accurately represent the next refill period, but that refill period may be less than the burst count when you near the sustained count.
There is currently no way to observe the remaining sustained count or its reset time if your sustained count exceeds the current burst count.

## Default Rate Limits

<!-- markdownlint-capture -->
<!-- markdownlint-disable MD033 -->

| API                                                                                          | Burst Count | Burst Period | Sustained Count | Sustained Period |
|----------------------------------------------------------------------------------------------|------------:|--------------|----------------:|------------------|
| [ContentService.ListProperties]                                                              |         400 | minute       |          18,000 | hour             |
| [LodgingShoppingService.FindBestOffers]<br/>[LodgingShoppingService.FindBestOffersStreaming] |          50 | minute       |           2,250 | hour             |
| [LodgingShoppingService.FindAvailability]                                                    |         100 | minute       |           4,500 | hour             |
| [LodgingBookingService.ConfirmOffer]                                                         |         100 | minute       |           4,500 | hour             |
| [LodgingBookingService.Book]                                                                 |          20 | minute       |             900 | hour             |
| [LodgingBookingService.GetBookings]<br/>[LodgingBookingService.GetBookingsStreaming]         |          20 | minute       |             900 | hour             |
| [LodgingBookingService.PreviewCancellation]                                                  |          20 | minute       |             900 | hour             |
| [LodgingBookingService.SubmitCancellation]                                                   |          10 | minute       |             450 | hour             |

<!-- markdownlint-restore -->
