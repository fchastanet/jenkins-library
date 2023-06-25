package fchastanet

import groovy.json.JsonOutput

/**
 * Provides methods allowing cloudflare interaction
 */
class Cloudflare implements Serializable {

  private static final long serialVersionUID = 1L

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor

  /**
   * @param jenkinsExecutor `Executor`
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Cloudflare(jenkinsExecutor) {
    this.jenkinsExecutor = jenkinsExecutor
  }

  /**
   * The file url to indicate to cloudflare must be the source file and not asset file
   * so in our case aws file
   * @param data `Map` cloudflare api data
   * see https://api.cloudflare.com/#zone-purge-files-by-url
   * eg: ["files":["https://project.s3.amazonaws.com/project/${instance}/index.html"]]
   */
  void zonePurge(
    String cloudflareZoneId,
    Map data,
    String credentialsId = 'cloudflare-workers-deploy',
    String usernameVariable = 'CLOUDFLARE_ACCOUNT_ID',
    String passwordVariable = 'CLOUDFLARE_API_TOKEN'
  ) {
    String dataJson = JsonOutput.toJson(data)
    this.jenkinsExecutor.withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: credentialsId,
      usernameVariable: usernameVariable,
      passwordVariable: passwordVariable]]
    ) {
      // we need this.jenkinsExecutor.CLOUDFLARE_API_TOKEN otherwise $CLOUDFLARE_API_TOKEN does not exist on master node
      this.jenkinsExecutor.sh """#!/bin/bash
          set -x
          curl -X POST 'https://api.cloudflare.com/client/v4/zones/${cloudflareZoneId}/purge_cache' \
          -H 'Authorization: Bearer ${this.jenkinsExecutor.CLOUDFLARE_API_TOKEN}' \
          -H 'Content-Type: application/json' \
          --data '${dataJson}'
        """
    }
  }

}
