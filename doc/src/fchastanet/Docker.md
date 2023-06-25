# Documentation

## `class Docker implements Serializable`

Provides method to build docker images

### `String getTagCompatibleFromBranch(String branchName, String separator = '-')`

* **Returns:** String a string compatible with docker tag format

### `void pullBuildPushImage( Map args )`

pull previous docker image, build new image, tag built image and push it to ecr registry

* **Parameters:** `args` — `Map` see structure below

  * check pullBuildImage method args doc
  * check tagPushImage method args doc

### `void pullBuildImage( Map args )`

try to docker pull image from tags arg and then build it

* **Parameters:** `args` — `Map` see structure below

  * check pullImage method args doc
  * check buildImage method args doc

### `void promoteTag( Map args )`

pull docker image with current commitSha, tag it with envType and push it to ecr registry when there is no changeset

* **Parameters:** `args` — `Map` see structure below

  * check pullImage method args doc
  * check buildImage method args doc
  * check tagImage method args doc

### `void tagPushImage( Map args )`

tag locally built docker image with given tags and push it to ecr registry

* **Parameters:** `args` — `Map` see structure below

  * check tagImage method args doc
  * check pushImage method args doc

### `String pullImage( Map args )`

try to docker pull image from pullTags arg best practice: provide pullTags ['shortSha', 'branchName']

* **Parameters:**
  * `args` — `Map` see structure below
  * `args.registryImageUrl` — `String` eg:123.dkr.ecr.example.amazonaws.com/project
  * `args.pullTags` — `String[]` list of tags, to pull image
     image will be pulled from pullTags array
* **Deprecated:** args.tagRemotePrefix `String` = '' eventual prefix to add before the calculated pullTag

* **Returns:** String the tag that has been successfully pulled, null if none
* **Exceptions:** `` — Exception on invalid argument

### `void buildImage( Map args )`

try to docker build image from eventual tagged image cache best practice: provide tags ['shortSha', 'branchName']

* **Parameters:**
  * `args` — `Map` see structure below
  * `args.buildDirectory` — `String` base directory from where dockefile will take hits resources
  * `args.tagPrefix` — `String` = '' eg: 'project-spa:' the prefix should include the delimiter
    if you set `args.tagPrefix = 'project-spa:'` and `args.tags = ['shortSha', 'branchName']`
     resulting tags will be 'project-spa:shortSha' and 'project-spa:branchName'
  * `args.tags` — `String` list of tags, the first one will be used as local tag
  * `args.buildArgs` — `String` = '' additional arguments to add to the docker build command
  * `args.dockerFilePath` — `String` = '' full path to the Dockerfile
    optional as by default the Dockerfile is taken from args.buildDirectory
     but can be necessary when the Dockerfile is in another place or has another name
  * `args.cacheFromTags` — `String[]` = [] list of images to use as cache
* **Exceptions:** `Exception` — if build fails

### `void tagImage( Map args )`

Image built is tagged with tags provided best practice: provide tags ['shortSha', 'branchName'] so image will be tagged with

* tagPrefix_shortSha
* tagPrefix_branchName

* **Parameters:**
  * `args` — `Map` see structure below
  * `args.registryImageUrl` — `String` eg:123.dkr.ecr.example.amazonaws.com/project
  * `args.tagPrefix` — `String` = '' eg: 'project-spa:' the prefix should include the delimiter
  * `args.localTagName` — `String` docker image tag which used in build stage
    if you set `args.tagPrefix = 'project-spa:'` and `args.tags = ['shortSha', 'branchName']`
     resulting tags will be 'project-spa:shortSha' and 'project-spa:branchName'
  * `args.tags` — `String` tags that used to push into ecr registry
* **Deprecated:** args.tagRemotePrefix `String` = '' eventual prefix to add before the calculated tag

* **Exceptions:** `Exception` — if local image is not tagged properly

### `void pushImage( Map args )`

push tagged docker image into ecr repository

* **Parameters:**
  * `args` — `Map` see structure below
  * `args.registryImageUrl` — `String` ecr repository name
  * `args.tags` — `String` tags that used to push into ecr registry
* **Exceptions:** `Exception` — if unable to push one of the tags

### `Short checkImage(String repositoryName, String imageTag)`

check docker image already exists in ecr repository

* **Parameters:**
  * `repositoryName` — `String` ecr repository name
  * `imageTag` — `String` docker image tag
* **Returns:**  `Short` script execution status
