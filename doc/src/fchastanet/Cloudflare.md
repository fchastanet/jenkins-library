# Documentation

## `class Cloudflare implements Serializable`

Provides methods allowing cloudflare interaction

### `void zonePurge( String cloudflareZoneId, Map data, String credentialsId = 'cloudflare-workers-deploy', String usernameVariable = 'CLOUDFLARE_ACCOUNT_ID', String passwordVariable = 'CLOUDFLARE_API_TOKEN' )`

The file url to indicate to cloudflare must be the source file and not asset file so in our case aws file

* **Parameters:** `data` — `Map` cloudflare api data
    see <https://api.cloudflare.com/#zone-purge-files-by-url>
    eg: ["files":["https://project.s3.amazonaws.com/project/${instance}/index.html"]]
