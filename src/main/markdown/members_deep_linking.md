# Deep Linking into the Engine Members Web experience

---

## Book a room for a given property

This link will take you directly to the room and rate selection page within our Members web experience.

<!-- markdownlint-disable-next-line MD033 -->
<h3><a name="book-a-room-for-a-given-property-url" />URL</h3>

`https://members.engine.com/properties/:propertyid?checkIn=:checkin&checkOut=:checkout&roomCount=:roomcount&guestCount=:guestcount`

<!-- markdownlint-disable-next-line MD033 -->
<h3><a name="book-a-room-for-a-given-property-path-parameters" />Path Parameters</h3>

| name       | required | description             | default |
|------------|----------|-------------------------|---------|
| propertyid | yes      | The Engine property ID. | none    |

<!-- markdownlint-disable-next-line MD033 -->
<h3><a name="book-a-room-for-a-given-property-query-parameters" />Query Parameters</h3>

| name       | required | description                                                                                      | default |
|------------|----------|--------------------------------------------------------------------------------------------------|---------|
| checkin    | yes      | The [ISO-8601 Calendar Date] in the property's local timezone that you will arrive at the hotel. | none    |
| checkout   | yes      | The [ISO-8601 Calendar Date] in the property's local timezone that you will leave the hotel.     | none    |
| roomcount  | no       | The number of rooms you wish to book.                                                            | 1       |
| guestcount | no       | The number of guests for the stay.                                                               | 2       |

---

## Signup or Signin with your Custom Landing Experience

This link will allow you to direct your customers to your custom landing page.
Once the customer has signed in or created their new account, they will be redirected to the provided URL or to their dashboard.

<!-- markdownlint-disable-next-line MD033 -->
<h3><a name="signup-or-signin-with-your-custom-landing-experience-url" />URL</h3>

`https://members.engine.com/join/:slug?redirect_url=:members_deep_link_url`

<!-- markdownlint-disable-next-line MD033 -->
<h3><a name="signup-or-signin-with-your-custom-landing-experience-path-parameters" />Path Parameters</h3>

| name | required | description                                           | default |
|------|----------|-------------------------------------------------------|---------|
| slug | yes      | The slug provided for your custom landing experience. | none    |

<!-- markdownlint-disable-next-line MD033 -->
<h3><a name="signup-or-signin-with-your-custom-landing-experience-query-parameters" />Query Parameters</h3>

| name                  | required | description                                                                                                                                        | default                                                                                        |
|-----------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| members_deep_link_url | no       | A URL-encoded deep link into the Engine Members Web Experience, for example, [Book a room for a given property](#book-a-room-for-a-given-property) | If no `members_deep_link_url` is provided, the customer will be sent to the Members Dashboard. |

[ISO-8601 Calendar Date]: https://en.wikipedia.org/wiki/ISO_8601#Calendar_dates
