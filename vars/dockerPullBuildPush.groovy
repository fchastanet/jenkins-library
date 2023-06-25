import fchastanet.Docker

/**
 * usage
 * dockerBuild(
 *   buildDirectory:   '.docker/lint',
 *   registryImageUrl: "${REGISTRY_URL}/${ECR_CK_LINT}",
 *   tags:             [LINT_TAG],
 *   tagSuffix:        deploymentBranchTagCompatible,
 *   localTagName:     "${ECR_CK_LINT}_${LINT_TAG}:latest"
 * ) {
 *   script {
 *     .... script containing code to test built image before push tag
 *   }
 * }
 *
 * @param args.buildDirectory `String`
 * @param args.registryImageUrl `String` eg:134.dkr.ecr.abc.amazonaws.com/project
 * @param args.tags `String`
 * @param args.buildArgs `String` = ''
 * @param args.dockerFilePath `String` = ''
 */
void call(Map args, Closure body=null) {
  Map arguments = args ?: [:]

  Docker docker = new Docker(this)
  docker.pullBuildImage(arguments)
  if (body) { body() }
  docker.pushImage(arguments)
}
