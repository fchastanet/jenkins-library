# Documentation

## `class Mail implements Serializable`

provide methods to send generic emails

### `void sendConditionalEmail( String to = null, String from, String gitDir = '' )`

Send an email following the result of the build emails sent can be SUCCESS, FAILURE, ABORTED or UNSTABLE Usage: post { always{ libMail.sendConditionalEmail(to, from) } }

### `private String getGenericBody(String status)`

Generates a generic body for the email displays the following data:

- build user email
- status of the build
- build parameters
- build context: build url, job name, build number

- **Parameters:** `status` — `String` status of the build
- **Private**
