package fchastanet

/**
 * Provides method to build docker images
 */
class Docker implements Serializable {

  private static final long serialVersionUID = 1L

  private static final short STATUS_OK = 0

  public static final String ARG_TAGS = 'tags'

  public static final String INVALID_BUILD_DIRECTORY_ARG = 'Invalid or missing buildDirectory arg'
  public static final String INVALID_REGISTRY_IMAGE_URL_ARG = 'Invalid or missing registryImageUrl arg'
  public static final String INVALID_TAGS_ARG = 'Invalid or missing tags arg'
  public static final String INVALID_PULL_TAGS_ARG = 'Invalid or missing pullTags arg'

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor

  /**
   * @param jenkinsExecutor `Executor`
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Docker(jenkinsExecutor) {
    this.jenkinsExecutor = jenkinsExecutor
  }

  /**
   * @return String a string compatible with docker tag format
   */
  String getTagCompatibleFromBranch(String branchName, String separator = '-') {
    String tag = branchName.toLowerCase()
    tag = tag.replaceAll('^origin/', '')
    return tag.replaceAll('/', separator)
  }

  /**
   * pull previous docker image, build new image, tag built image and push it
   * to ecr registry
   *
   * @param args `Map` see structure below
   *  - check pullBuildImage method args doc
   *  - check tagPushImage method args doc
   */
  void pullBuildPushImage(
    Map args
  ) {
    this.pullBuildImage(args)
    this.tagPushImage(args)
  }

  /**
   * try to docker pull image from tags arg and then build it
   *
   * @param args `Map` see structure below
   *  - check pullImage method args doc
   *  - check buildImage method args doc
   */
  void pullBuildImage(
    Map args
  ) {
    String tagPulled = this.pullImage(args)
    if (tagPulled != null) {
      args.cacheFromTags = [tagPulled]
    }
    this.jenkinsExecutor.println("This image tag ${tagPulled} will be used as cache")
    this.buildImage(args)
  }

  /**
   * pull docker image with current commitSha, tag it with envType and push it to
   * ecr registry when there is no changeset
   *
   * @param args `Map` see structure below
   *  - check pullImage method args doc
   *  - check buildImage method args doc
   *  - check tagImage method args doc
   */
  void promoteTag(
    Map args
  ) {
    this.pullImage(args)
    this.tagImage(args)
    this.pushImage(args)
  }

  /**
   * tag locally built docker image with given tags and push it to ecr registry
   *
   * @param args `Map` see structure below
   *  - check tagImage method args doc
   *  - check pushImage method args doc
   */
  void tagPushImage(
    Map args
  ) {
    this.tagImage(args)
    this.pushImage(args)
  }

  /**
   * try to docker pull image from pullTags arg
   * best practice: provide pullTags ['shortSha', 'branchName']
   *
   * @param args `Map` see structure below
   * @param args.registryImageUrl `String` eg:123.dkr.ecr.example.amazonaws.com/project
   * @param args.pullTags `String[]` list of tags, to pull image
   *    image will be pulled from pullTags array
   * @deprecated args.tagRemotePrefix `String` = '' eventual prefix to add before the calculated pullTag
   *
   * @return String the tag that has been successfully pulled, null if none
   * @throws Exception on invalid argument
   */
  String pullImage(
    Map args
  ) {
    if (!args?.registryImageUrl?.trim()) {
      throw new Exception(Docker.INVALID_REGISTRY_IMAGE_URL_ARG)
    }
    if (args.containsKey('pullTags')) {
      if (!Utils.isCollectionOrArray(args.pullTags)) {
        throw new Exception(Docker.INVALID_PULL_TAGS_ARG)
      }
    }

    String[] pullTags = args.pullTags
    String tagRemotePrefix = args.tagRemotePrefix?.trim() ?: ''
    String registryImageUrl = args.registryImageUrl
    short status = Docker.STATUS_OK

    // try to pull one of the image tag
    String cacheTag = ''
    for (tag in pullTags) {
      cacheTag = "${registryImageUrl}:${tagRemotePrefix}${tag}"
      status = this.jenkinsExecutor.sh(script: "docker pull ${cacheTag}", returnStatus: true)
      if (status == Docker.STATUS_OK) {
        this.jenkinsExecutor.println('Image pulled successfully')
        break
      }
    }
    return cacheTag
  }

  /**
   * try to docker build image from eventual tagged image cache
   * best practice: provide tags ['shortSha', 'branchName']
   *
   * @param args `Map` see structure below
   * @param args.buildDirectory `String` base directory from where dockefile will take hits resources
   * @param args.tagPrefix `String` = '' eg: 'project-spa:' the prefix should include the delimiter
   *   if you set `args.tagPrefix = 'project-spa:'` and `args.tags = ['shortSha', 'branchName']`
   *   resulting tags will be 'project-spa:shortSha' and 'project-spa:branchName'
   * @param args.tags `String` list of tags, the first one will be used as local tag
   * @param args.buildArgs `String` = '' additional arguments to add to the docker build command
   * @param args.dockerFilePath `String` = '' full path to the Dockerfile
   *  optional as by default the Dockerfile is taken from args.buildDirectory
   *  but can be necessary when the Dockerfile is in another place or has another name
   * @param args.cacheFromTags `String[]` = [] list of images to use as cache
   *
   * @throws Exception if build fails
   */
  void buildImage(
    Map args
  ) {
    if (!args?.buildDirectory?.trim()) {
      throw new Exception(Docker.INVALID_BUILD_DIRECTORY_ARG)
    }
    if (!args.containsKey(Docker.ARG_TAGS) && !Utils.isCollectionOrArray(args.tags) && args.tags.length < 1) {
      throw new Exception(Docker.INVALID_TAGS_ARG)
    }

    String[] tags = args.tags
    String[] cacheFrom = args?.cacheFromTags ?: []
    String tagPrefix = args.tagPrefix?.trim() ?: ''
    String buildDirectory = args.buildDirectory
    String buildArgs = args?.buildArgs ?: ''
    String dockerFilePath = args?.dockerFilePath ?: ''
    if (!dockerFilePath?.trim()) {
      dockerFilePath = "${args.buildDirectory}/Dockerfile"
    }
    String localTagName = tagPrefix + tags[0]
    short status = Docker.STATUS_OK

    // compute cache from
    String cacheFromArgs = ''
    for (cacheFromTag in cacheFrom) {
      cacheFromArgs += "--cache-from='${cacheFromTag}' "
    }

    // added BUILDKIT_INLINE_CACHE in order to use the resulting image as a cache source
    // @see https://docs.docker.com/engine/reference/commandline/build/#specifying-external-cache-sources
    status = this.jenkinsExecutor.sh(script: """
      DOCKER_BUILDKIT=0 docker build \
        ${buildArgs} \
        --build-arg BUILDKIT_INLINE_CACHE=0 \
        -f "${dockerFilePath}" \
        ${cacheFromArgs} \
        -t "${localTagName}" \
        -t "${tagPrefix}latest" \
        "${buildDirectory}"
    """, returnStatus: true)
    if (status != Docker.STATUS_OK) {
      throw new Exception("unable to build ${dockerFilePath}")
    }
  }

  /**
   * Image built is tagged with tags provided
   * best practice: provide tags ['shortSha', 'branchName']
   * so image will be tagged with
   * - tagPrefix_shortSha
   * - tagPrefix_branchName
   *
   * @param args `Map` see structure below
   * @param args.registryImageUrl `String` eg:123.dkr.ecr.example.amazonaws.com/project
   * @param args.tagPrefix `String` = '' eg: 'project-spa:' the prefix should include the delimiter
   * @param args.localTagName `String` docker image tag which used in build stage
   *   if you set `args.tagPrefix = 'project-spa:'` and `args.tags = ['shortSha', 'branchName']`
   *   resulting tags will be 'project-spa:shortSha' and 'project-spa:branchName'
   * @param args.tags `String` tags that used to push into ecr registry
   * @deprecated args.tagRemotePrefix `String` = '' eventual prefix to add before the calculated tag
   *
   * @throws Exception if local image is not tagged properly
   */
  void tagImage(
    Map args
  ) {
    if (!args?.registryImageUrl?.trim()) {
      throw new Exception(Docker.INVALID_REGISTRY_IMAGE_URL_ARG)
    }
    if (!args.containsKey(Docker.ARG_TAGS) && !Utils.isCollectionOrArray(args.tags) && args.tags.length < 1) {
      throw new Exception(Docker.INVALID_TAGS_ARG)
    }

    String[] tags = args.tags
    String registryImageUrl = args.registryImageUrl
    String tagPrefix = args?.tagPrefix?.trim() ?: ''
    String tagRemotePrefix = args.tagRemotePrefix?.trim() ?: ''
    String localTagName = tagPrefix + args.localTagName

    for (tag in tags) {
      String finalTag = "${tagRemotePrefix}${tag}"
      short status = this.jenkinsExecutor.sh(
        script: "docker tag '${localTagName}' '${registryImageUrl}:${finalTag}'",
        returnStatus: true
      )
      if (status != Docker.STATUS_OK) {
        throw new Exception("unable to tag local image ${localTagName} with tag ${registryImageUrl}:${finalTag}")
      }
    }
  }

  /**
   * push tagged docker image into ecr repository
   *
   * @param args `Map` see structure below
   * @param args.registryImageUrl `String` ecr repository name
   * @param args.tags `String` tags that used to push into ecr registry
   *
   * @throws Exception if unable to push one of the tags
   */
  void pushImage(
    Map args
  ) {
    if (!args?.registryImageUrl?.trim()) {
      throw new Exception(Docker.INVALID_REGISTRY_IMAGE_URL_ARG)
    }
    if (!args.containsKey(Docker.ARG_TAGS) && !Utils.isCollectionOrArray(args.tags) && args.tags.length < 1) {
      throw new Exception(Docker.INVALID_TAGS_ARG)
    }
    String[] tags = args.tags
    String registryImageUrl = args.registryImageUrl
    String tagRemotePrefix = args.tagRemotePrefix?.trim() ?: ''

    for (tag in tags) {
      String finalTag = "${tagRemotePrefix}${tag}"
      short status = this.jenkinsExecutor.sh(
        script: "docker push '${registryImageUrl}:${finalTag}'",
        returnStatus: true
      )
      if (status != Docker.STATUS_OK) {
        throw new Exception("unable to push tag ${registryImageUrl}:${finalTag}")
      }
    }
  }

  /**
   * check docker image already exists in ecr repository
   *
   * @param repositoryName `String` ecr repository name
   * @param imageTag `String` docker image tag
   * @return `Short` script execution status
   */
  Short checkImage(String repositoryName, String imageTag) {
    short status = this.jenkinsExecutor.sh(
      script: "aws ecr describe-images --repository-name='${repositoryName}' --image-ids=imageTag='${imageTag}'",
      returnStatus: true
    )
    if (status != Docker.STATUS_OK) {
      this.jenkinsExecutor.println(
        "Unable to find image tag ${imageTag} in ${repositoryName} respository. Proceeding to Build stage.."
      )
    }
    else {
      this.jenkinsExecutor.println(
        "Image tag ${imageTag} found in ${repositoryName} respository. Proceeding to Promote stage.."
      )
    }
    return status
  }

}
