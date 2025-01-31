# Deep Linking into the Engine Members Web experience

---
## Book a room for a given property

### URL
`https://members.engine.com/property/:propertyid?checkIn=:checkin&checkOut=:checkout&roomCount=:roomcount&guestCount=:guestcount`

### Path Parameters

| name       | required | description             | default |
|------------|----------|-------------------------|---------|
| propertyid | yes      | The Engine property ID. | none    |

### Query Parameters

| name       | required | description                                                                                      | default |
|------------|----------|--------------------------------------------------------------------------------------------------|---------|
| checkin    | yes      | The [ISO-8601 Calendar Date] in the property's local timezone that you will arrive at the hotel. | none    |
| checkout   | yes      | The [ISO-8601 Calendar Date] in the property's local timezone that you will leave the hotel.     | none    |
| roomcount  | no       | The number of rooms you wish to book.                                                            | 1       |
| guestcount | no       | The number of guests for the stay.                                                               | 2       |

---
[ISO-8601 Calendar Date]: https://en.wikipedia.org/wiki/ISO_8601#Calendar_dates