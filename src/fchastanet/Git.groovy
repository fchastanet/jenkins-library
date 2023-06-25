package fchastanet

/**
 * provides methods to apply to the git repository
 */
class Git implements Serializable {

  private static final long serialVersionUID = 1L

  private static final String GIT_TOOL = 'Default'
  private static final String SCM_CLASS = 'GitSCM'

  private static final String GIT_STATUS_FAILURE = 'FAILURE'
  private static final String GIT_STATUS_SUCCESS = 'SUCCESS'

  private static final String GITHUB_STATUS_FAILURE = 'FAILURE'
  private static final String GITHUB_STATUS_SUCCESS = 'SUCCESS'

  private static final String GITHUB_BUILD_CONDITIONAL_RESULT_SOURCE = 'ConditionalStatusResultSource'
  private static final String GITHUB_BUILD_RESULT_BETTER_THAN_OR_EQUAL = 'BetterThanOrEqualBuildResult'
  private static final String GITHUB_BUILD_RESULT_ANY = 'AnyBuildResult'

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor

  /**
   * @param jenkinsExecutor `Executor`
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Git(jenkinsExecutor) {
    this.jenkinsExecutor = jenkinsExecutor
  }

  /**
   * @return String url of the current dir or provided repository
   */
  String getRepoURL(String gitDir = '') {
    return this.jenkinsExecutor.dir(gitDir) {
      return this.jenkinsExecutor.sh(returnStdout: true, script: 'git config --get remote.origin.url').trim()
    }
  }

  /**
   * @param gitDir `String` source directory containing .git directory
   * @return String current commit sha
   */
  String getCommitSha(String gitDir = '') {
    return this.jenkinsExecutor.dir(gitDir) {
      return this.jenkinsExecutor.sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    }
  }

  /**
   * @param gitDir `String` source directory containing .git directory
   * @return String current short commit sha
   */
  String getShortCommitSha(String gitDir = '') {
    return this.jenkinsExecutor.dir(gitDir) {
      return this.jenkinsExecutor.sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
    }
  }

  /**
   * @param gitDir `String` source directory containing .git directory
   * @return String the email of the last commit user
   */
  String getLastPusherEmail(String gitDir = '') {
    return this.jenkinsExecutor.dir(gitDir) {
      return this.jenkinsExecutor.sh(returnStdout: true, script: 'git log -n1 --pretty=format:"%ae"').trim()
    }
  }

  /**
   * do a shallow clone of given depth for given repository
   * @param gitDir `String` source directory containing .git directory
   * @param credentialsId `String`
   * @param remoteUrl `String`
   * @param branch `String`
   * @param depth `short`
  */
  void lightCheckout(String gitDir, String credentialsId, String remoteUrl, String branch = 'master', short depth = 1) {
    this.jenkinsExecutor.dir(gitDir) {
      this.jenkinsExecutor.checkout(
        changelog: false,
        poll: false,
        scm: [
            $class: Git.SCM_CLASS,
            branches: [[name: branch]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[$class: 'CloneOption', depth: depth, noTags: true, reference: '', shallow: true]],
            gitTool: Git.GIT_TOOL,
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: credentialsId, url: remoteUrl]]
        ]
      )
    }
  }

  /**
   * do a clone for given repository and given branch
   * @param gitDir `String` source directory containing .git directory
   * @param credentialsId `String`
   * @param remoteUrl `String`
   * @param branch `String`
   * @param depth `short`
  */
  void branchCheckout(String gitDir = '', String credentialsId, String remoteUrl, String branch = 'master') {
    this.jenkinsExecutor.dir(gitDir) {
      this.jenkinsExecutor.checkout(
        changelog: false,
        poll: false,
        scm: [
            $class: Git.SCM_CLASS,
            branches: [[name: branch]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[
              $class: 'SubmoduleOption',
              disableSubmodules: false,
              parentCredentials: true,
              recursiveSubmodules: true,
              reference: '',
              trackingSubmodules: false
            ]],
            gitTool: Git.GIT_TOOL,
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: credentialsId, url: remoteUrl]]
        ]
      )
    }
  }

  void updateGithubCommitStatusGeneric(String gitDir = '', Map args = [:]) {
    // workaround https://issues.jenkins-ci.org/browse/JENKINS-38674
    String repoUrl = this.getRepoURL(gitDir)
    String commitSha = this.getCommitSha(gitDir)
    Map newArgs = Utils.deepMerge([
      $class: 'GitHubCommitStatusSetter',
      reposSource: [$class: 'ManuallyEnteredRepositorySource', url: repoUrl],
      commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: commitSha],
      errorHandlers: [[$class: 'ShallowAnyErrorHandler']],
    ], args)

    this.jenkinsExecutor.step(newArgs)
  }

  /**
   * Step updating git commit status
   * @deprecated please use updateConditionalGithubCommitStatus instead
   */
  void updateGithubCommitStatus(String status, String gitDir = '') {
    this.updateGithubCommitStatusGeneric(
      gitDir,
      [
        statusResultSource: [
          $class: Git.GITHUB_BUILD_CONDITIONAL_RESULT_SOURCE,
          results: [
            [
              $class: Git.GITHUB_BUILD_RESULT_ANY,
              state: status
            ]
          ]
        ]
      ]
    )
  }

  /**
   * Step updating git commit status depending on build result
   *
   * this method allows to only put this instruction at the end of the pipeline
   * post {
   *   always{
   *     libGit.updateConditionalGithubCommitStatus()
   *   }
   * }
   * @param gitDir `String`
   */
  void updateConditionalGithubCommitStatus(String gitDir = '') {
    this.updateGithubCommitStatusGeneric(
      gitDir,
      [
        statusResultSource: [
          $class: Git.GITHUB_BUILD_CONDITIONAL_RESULT_SOURCE,
          results: [
            [
              $class:  Git.GITHUB_BUILD_RESULT_BETTER_THAN_OR_EQUAL,
              result:  Git.GITHUB_STATUS_SUCCESS,
              state:   Git.GIT_STATUS_SUCCESS,
              message: this.jenkinsExecutor.currentBuild.description
            ],
            [
              $class:  Git.GITHUB_BUILD_RESULT_BETTER_THAN_OR_EQUAL,
              result:  Git.GITHUB_STATUS_FAILURE,
              state:   Git.GIT_STATUS_FAILURE,
              message: this.jenkinsExecutor.currentBuild.description
            ],
            [
              $class:  Git.GITHUB_BUILD_RESULT_ANY,
              state:   Git.GIT_STATUS_FAILURE,
              message: 'Loophole'
            ]
          ]
        ]
      ]
    )
  }

}
