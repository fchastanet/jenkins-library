import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

/**
 * execute body or skip the stage if condition fails
 *
 * ```Jenkinsfile
 * stage('Zero') {
 *   whenOrSkip(BRANCH_NAME == 'master') {
 *     echo 'Performing steps if branch is master'
 *   }
 * }
 * ```
 */
void call(boolean condition, Closure body=null) {
  Map config = [:]
  body.resolveStrategy = Closure.OWNER_FIRST
  body.delegate = config

  if (condition) {
    body()
    } else {
    Utils.markStageSkippedForConditional(STAGE_NAME)
  }
}
