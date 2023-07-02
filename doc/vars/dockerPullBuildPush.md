# Documentation

## `void call(Map args, Closure body=null)`

usage dockerBuild( buildDirectory:   '.docker/lint', registryImageUrl: "${REGISTRY_URL}/${ECR_CK_LINT}", tags:             [LINT_TAG], tagSuffix:        deploymentBranchTagCompatible, localTagName:     "${ECR_CK_LINT}_${LINT_TAG}:latest" ) { script { .... script containing code to test built image before push tag } } 



 * **Parameters:**
   * `args.buildDirectory` — `String`
   * `args.registryImageUrl` — `String` eg:134.dkr.ecr.abc.amazonaws.com/project
   * `args.tags` — `String`
   * `args.buildArgs` — `String` = ''
   * `args.dockerFilePath` — `String` = ''
