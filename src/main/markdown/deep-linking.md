---
title: Deep Linking
permalink: /deep-linking.html
---

<!-- markdownlint-disable-next-line MD025 -->
# Deep Linking to Engine Web Experiences

<!-- markdownlint-capture -->
<!-- markdownlint-disable MD033 -->
<details markdown="block">
  <summary>
    Table of contents
  </summary>
1. TOC
{:toc}
</details>
<!-- markdownlint-restore -->

## Deep Linking into the Engine Members Web experience

---

### Book a room for a given property

This link will take you directly to the room and rate selection page within our Members web experience.

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="book-a-room-for-a-given-property-url" />URL</h4>

`https://members.engine.com/properties/:propertyId`

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="book-a-room-for-a-given-property-path-parameters" />Path Parameters</h4>

| name       | required | description             | default |              example |
|------------|---------:|-------------------------|--------:|---------------------:|
| propertyId |      yes | The Engine property ID. |    none | P0000000000000102095 |

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="book-a-room-for-a-given-property-query-parameters" />Query Parameters</h4>

| name       | required | description                                                                             | default | constraints             |    example |
|------------|---------:|-----------------------------------------------------------------------------------------|--------:|-------------------------|-----------:|
| checkIn    |      yes | The [ISO-8601 Calendar Date] in the property's local timezone that the stay will begin. |    none | `checkIn > now`         | 2024-12-25 |
| checkOut   |      yes | The [ISO-8601 Calendar Date] in the property's local timezone that the stay will end.   |    none | `checkOut >= checkIn`   | 2024-12-27 |
| roomCount  |       no | The number of rooms you wish to book.                                                   |       1 | `1 <= roomCount <= 8`   |          2 |
| guestCount |       no | The number of guests for the stay.                                                      |       2 | `1 <= guestCount <= 16` |          4 |

---

### Signup or Signin with your Custom Landing Experience

This link will allow you to direct your customers to your custom landing page.
Once the customer has signed in or created their new account, they will be redirected to the provided URL or to their dashboard.

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="signup-or-signin-with-your-custom-landing-experience-url" />URL</h4>

`https://members.engine.com/join/:slug`

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="signup-or-signin-with-your-custom-landing-experience-path-parameters" />Path Parameters</h4>

| name | required | description                                           | default |
|------|----------|-------------------------------------------------------|--------:|
| slug | yes      | The slug provided for your custom landing experience. |    none |

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="signup-or-signin-with-your-custom-landing-experience-query-parameters" />Query Parameters</h4>

| name         | required | description                                                                                                                                        | default                                                                       |
|--------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| redirect_url | no       | A URL-encoded deep link into the Engine Members Web Experience, for example, [Book a room for a given property](#book-a-room-for-a-given-property) | If no `redirect_url` is provided, the customer will be sent to the Dashboard. |

---

## Groups, Events, and RFPs

---

### Begin a new RFP Submission

This link allows you to prefill parameters in the RFP Submission process.

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="begin-a-new-rfp-url" />URL</h4>

`http://groups.engine.com/new-trip`

<!-- markdownlint-disable-next-line MD033 -->
<h4><a name="begin-a-new-rfp-query-parameters" />Query Parameters</h4>

<!-- markdownlint-capture -->
<!-- markdownlint-disable MD033 -->

| name     | aliases               |        required | description                                                                                        | default |          constraints |                                example |
|----------|-----------------------|----------------:|----------------------------------------------------------------------------------------------------|--------:|---------------------:|---------------------------------------:|
| checkin  | CheckIn               |              no | The `MM/DD/YYYY` or [ISO-8601 Calendar Date] in the destination timezone that the stay will begin. |    none |      `checkin > now` |              12/24/2024<br/>2024-12-24 |
| checkout | CheckOut              |              no | The `MM/DD/YYYY` or [ISO-8601 Calendar Date] in the destination timezone that the stay will end.   |    none | `checkout > checkin` |              12/27/2024<br/>2024-12-27 |
| lat      |                       | if lng provided | The latitude of your point of interest for your stay.                                              |    none |   `-90 <= lat <= 90` |                        30.379014215153 |
| lng      |                       | if lat provided | The longitude of your point of interest for your stay.                                             |    none | `-180 <= lng <= 180` |                       -97.740766025294 |
| city     | City<br/>description  |              no | The textual description of a city or point of interest for your stay.                              |    none |                      | Austin, TX<br/>New Jersey, USA<br/>NYC |
| sc       |                       |              no | Custom search attribution data provided by you.                                                    |    none |                      |                 (Any short text value) |

<!-- markdownlint-restore -->

[ISO-8601 Calendar Date]: https://en.wikipedia.org/wiki/ISO_8601#Calendar_dates
