# Documentation

## `class Utils implements Serializable`

provide delete files and aws utilities

### `static Map deepMerge(Map original, Map newMap)`

merge newMap inside original

* **Parameters:**
  * `original` — `Map`
  * `newMap` — `Map`

### `void deleteDirAsRoot(String baseDir, String relativeDirToRemove)`

**!!!!!!!! Use with caution !!!!!**

ability to remove directory using root rights

* **Parameters:**
  * `baseDir` — `String` could ./ for workspace dir
  * `relativeDirToRemove` — `String` the directory to remove relative to baseDir

### `void initAws(String prodRoleArn, String roleSessionName)`

Initialize AWS environment variables for given role arn:

* AWS_ACCESS_KEY_ID
* AWS_SECRET_ACCESS_KEY
* AWS_SESSION_TOKEN

* **Parameters:** `prodRoleArn` — `String` the aws role to use (eg: 'arn:aws:iam::786687:role/JenkinsSlave')
