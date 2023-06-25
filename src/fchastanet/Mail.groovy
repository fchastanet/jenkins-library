package fchastanet

import hudson.model.Result

/**
 * provide methods to send generic emails
 */
class Mail implements Serializable {

  private static final long serialVersionUID = 1L

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor

  private final Git git

  /**
   * @param jenkinsExecutor `Executor`
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Mail(jenkinsExecutor) {
    this.jenkinsExecutor = jenkinsExecutor
    this.git = new Git(jenkinsExecutor)
  }

  /**
   * sends a generic email to following recipients:
   *  - jenkins build requestor
   *  - last commiter email (if different than build requestor)
   *  - email specified in "to" parameter (if specified)
   * @param subject `String`
   * @param body `String`
   * @param to `String` additional email, can be null
   * @param from `String` from email address
   * @param gitDir `String` source directory containing .git directory (default: current directory)
   */
  /* groovylint-disable-next-line UnusedMethodParameter */
  void sendGenericEmail(
    String subject,
    String body,
    String to,
    String from,
    String gitDir = ''
  ) {
    this.jenkinsExecutor.emailext(
      recipientProviders: [
        // [$class: 'CulpritsRecipientProvider'],
        [$class: 'RequesterRecipientProvider']
      ],
      from: from,
      to: to,
      subject: subject,
      body: body
    )
  }

  void sendFailureEmail(
    String to = null,
    String from,
    String gitDir = ''
  ) {
    Object env = this.jenkinsExecutor.env
    this.sendGenericEmail(
      "[Jenkins] ${env.JOB_NAME} build failed !",
      this.getGenericBody('failed'),
      to,
      from,
      gitDir
    )
  }

  void sendUnstableEmail(
    String to = null,
    String from,
    String gitDir = ''
  ) {
    Object env = this.jenkinsExecutor.env
    this.sendGenericEmail(
      "[Jenkins] ${env.JOB_NAME} build unstable !",
      this.getGenericBody('unstable'),
      to,
      from,
      gitDir
    )
  }

  void sendAbortedEmail(
    String to = null,
    String from,
    String gitDir = ''
  ) {
    Object env = this.jenkinsExecutor.env
    this.sendGenericEmail(
      "[Jenkins] ${env.JOB_NAME} build aborted !",
      this.getGenericBody('aborted'),
      to,
      from,
      gitDir
    )
  }

  void sendSuccessfulEmail(
    String to = null,
    String from,
    String gitDir = ''
  ) {
    Object env = this.jenkinsExecutor.env
    this.sendGenericEmail(
      "[Jenkins] ${env.JOB_NAME} build successful !",
      this.getGenericBody('successful'),
      to,
      from,
      gitDir
    )
  }

  void sendTeamsNotification(
    String to = null,
    String from,
    String gitDir = ''
  ) {
    Object env = this.jenkinsExecutor.env
    this.sendGenericEmail(
      "[Jenkins] ${env.JOB_NAME} deployed in production !",
      """
          <p>build has been successfully deployed</p>
          <p>Get more information here: <a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>
      """.trim(),
      to,
      from,
      gitDir
    )
  }

  /**
   * Send an email following the result of the build
   * emails sent can be SUCCESS, FAILURE, ABORTED or UNSTABLE
   * Usage:
   * post {
   *   always{
   *     libMail.sendConditionalEmail(to, from)
   *   }
   * }
   */
  void sendConditionalEmail(
    String to = null,
    String from,
    String gitDir = ''
  ) {
    switch (this.jenkinsExecutor.currentBuild.result) {
      case Result.SUCCESS.toString():
        this.sendSuccessfulEmail(to, from, gitDir)
        break
      case Result.FAILURE.toString():
        this.sendFailureEmail(to, from, gitDir)
        break
      case Result.ABORTED.toString():
        this.sendAbortedEmail(to, from, gitDir)
        break
      case Result.UNSTABLE.toString():
        this.sendUnstableEmail(to, from, gitDir)
        break
    }
  }

  /**
   * Generates a generic body for the email
   * displays the following data:
   *  - build user email
   *  - status of the build
   *  - build parameters
   *  - build context: build url, job name, build number
   * @param status `String` status of the build
   * @private
   */
  private String getGenericBody(String status) {
    String paramsList = ''
    Object env = this.jenkinsExecutor.env
    if (this.jenkinsExecutor.params) {
      this.jenkinsExecutor.params.each { it -> paramsList += "<li>${it.key}: ${it.value}</li>\n" }
    }
    String buildUserEmail = this.jenkinsExecutor.requestor()
    return """
      <p>Build initiated by ${buildUserEmail} has been ${status}:
            <ul>
              <li>Branch: '${env.GIT_BRANCH}'</li>
              <li>Commit: ${env.GIT_COMMIT}</li>
              ${paramsList}
          </ul>
      </p>
      <p>Jenkins build: <a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>
    """.trim()
  }

}
