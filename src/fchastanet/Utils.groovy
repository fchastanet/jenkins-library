package fchastanet

/**
 * provide delete files and aws utilities
 */
class Utils implements Serializable {

  private static final long serialVersionUID = 1L

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor

  /**
   * @param jenkinsExecutor `Executor`
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Utils(jenkinsExecutor) {
    this.jenkinsExecutor = jenkinsExecutor
  }

  /**
   * merge newMap inside original
   * @param original `Map`
   * @param newMap `Map`
   */
  static Map deepMerge(Map original, Map newMap) {
    for (Object key : newMap.keySet()) {
        /* groovylint-disable-next-line Instanceof */
      if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
        Map originalChild = (Map) original.get(key)
        Map newChild = (Map) newMap.get(key)
        original.put(key, deepMerge(originalChild, newChild))
      }
        /* groovylint-disable-next-line Instanceof */
        else if (newMap.get(key) instanceof List && original.get(key) instanceof List) {
        List originalChild = (List) original.get(key)
        List newChild = (List) newMap.get(key)
            /* groovylint-disable-next-line NestedForLoop */
        for (Object each : newChild) {
          if (!originalChild.contains(each)) {
            originalChild.add(each)
          }
        }
        } else {
        original.put(key, newMap.get(key))
        }
    }
    return original
  }

  /**
   * @param object the object to test
   * @return {boolean} true if object is an array or a Collection
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  static boolean isCollectionOrArray(object) {
    /* groovylint-disable-next-line Instanceof */
    return object instanceof Object[] || object instanceof Collection
  }

  /**
   * **!!!!!!!! Use with caution !!!!!**
   * ability to remove directory using root rights
   * @param baseDir `String` could ./ for workspace dir
   * @param relativeDirToRemove `String` the directory to remove relative to baseDir
   */
  void deleteDirAsRoot(String baseDir, String relativeDirToRemove) {
    this.jenkinsExecutor.sh """#!/bin/bash
        set -x
        if [[ -d "${baseDir}/${relativeDirToRemove}" ]]; then
            echo "deleting "\$(cd '${baseDir}' && pwd)/${relativeDirToRemove}""
            docker run \
              -v "\$(cd '${baseDir}' && pwd):/data" \
              --user root \
              ubuntu:18.04 \
              /bin/bash -c 'rm -Rf "/data/${relativeDirToRemove}"'
        fi
    """
  }

  /**
   * Initialize AWS environment variables for given role arn:
   * - AWS_ACCESS_KEY_ID
   * - AWS_SECRET_ACCESS_KEY
   * - AWS_SESSION_TOKEN
   * @param prodRoleArn `String` the aws role to use (eg: 'arn:aws:iam::786687:role/JenkinsSlave')
   */
  void initAws(String prodRoleArn, String roleSessionName) {
    String prodRole = this.jenkinsExecutor.sh(
      script: """aws sts assume-role --role-arn "${prodRoleArn}" --role-session-name '${roleSessionName}'""",
      returnStdout: true
    )
    // jq does not work as quotes are missing in prodRole
    // Another alternative would have been groovy.json.JsonOutput
    this.jenkinsExecutor.env.AWS_ACCESS_KEY_ID = this.jenkinsExecutor.sh(
      script: "set +x; echo \"${prodRole}\" | grep AccessKeyId | cut -d ':' -f2 | sed -e 's/,\\s*\$//'",
      returnStdout: true
    ).trim()

    this.jenkinsExecutor.env.AWS_SECRET_ACCESS_KEY = this.jenkinsExecutor.sh(
      script: """set +x; echo \"${prodRole}\" | grep SecretAccessKey | cut -d ':' -f2 | sed -e 's/,\\s*\$//'""",
      returnStdout: true
    ).trim()

    this.jenkinsExecutor.env.AWS_SESSION_TOKEN = this.jenkinsExecutor.sh(
      script: """set +x; echo \"${prodRole}\" | grep SessionToken | cut -d ':' -f2 | sed -e 's/,\\s*\$//'""",
      returnStdout: true
    ).trim()
  }

}
