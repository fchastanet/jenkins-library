package fchastanet

import fchastanet.transformers.Lighthouse2Issues
import fchastanet.transformers.ITransformer
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import io.jenkins.plugins.analysis.core.steps.AnnotatedReport

/**
 * provide methods for linting files
 */
class Lint implements Serializable {

  private static final long serialVersionUID = 1L
  public  static final String RESULT_SUCCESS = 'SUCCESS'
  public  static final String RESULT_UNSTABLE = 'UNSTABLE'
  public  static final String RESULT_FAILURE = 'FAILURE'

  private boolean converterImageBuildNeeded = true

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor

  private String buildResult
  private String stageResult

  private List<AnnotatedReport> allIssues = []

  /**
   * @param jenkinsExecutor `Executor`
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Lint(jenkinsExecutor) {
    this.jenkinsExecutor = jenkinsExecutor
    // buildResult is the result of the linter
    // if there is an error during publishing of the the result
    // the stage will be marked as failure but not the build,
    // so it will not prevent the other stages to run
    this.buildResult = Lint.RESULT_SUCCESS
    this.stageResult = Lint.RESULT_SUCCESS
  }

  /**
   * @deprecated please use catchError and when clauses instead
   */
  String getBuildResult() {
    return this.buildResult
  }

  /**
   * @deprecated please use catchError and when clauses instead
   */
  Lint setBuildResult(String buildResult) {
    this.buildResult = buildResult
    return this
  }

  /**
   * @deprecated please use catchError and when clauses instead
   */
  String getStageResult() {
    return this.stageResult
  }

  /**
   * @deprecated please use catchError and when clauses instead
   */
  Lint setStageResult(String stageResult) {
    this.stageResult = stageResult
    return this
  }

  /**
   * docker lint
   * @param projectDir `String`
   * @param dockerfileRelativePath `String` path relative to project dir
   * @param logRelativePath `String` path relative to project dir
   * @param hadoLintImage `String` docker image to run
   * @return String one the constant Lint.RESULT_*
   */
  String dockerLint(
    String projectDir,
    String dockerfileRelativePath,
    String logRelativePath,
    String hadoLintImage = 'hadolint/hadolint'
  ) {
    try {
      this.jenkinsExecutor.sh """
        cd '${projectDir}'
        docker pull '${hadoLintImage}'
        # ensure that logs file folder exists
        mkdir -p "\$(dirname '${logRelativePath}')"
        # run hadolint
        docker run \
          --rm \
          -i \
          '${hadoLintImage}' \
          hadolint -f json - 2>&1 < '${dockerfileRelativePath}' > '${logRelativePath}' || true
        if grep -E '^hadolint:' '${logRelativePath}'; then
          # unexpected error has occurred, we ignore hadolint warning
          exit 1
        fi
        # make logs with relative path
        sed -i -E 's#^/build/#./#g' '${logRelativePath}'
      """
    } catch (err) {
      this.jenkinsExecutor.println "hadolint ${dockerfileRelativePath} has failed : ${err}"
      this.stageResult = Lint.RESULT_FAILURE
      return Lint.RESULT_FAILURE
    }

    return Lint.RESULT_SUCCESS
  }

  /**
   * set current build and stage result deduced from
   * stageResult and buildResult properties
   * @deprecated please use catchError and when clauses instead
   */
  void markBuildAndStage() {
    this.jenkinsExecutor.currentBuild.result = this.buildResult
    // mark stage as unstable/failure if necessary
    this.jenkinsExecutor.catchError(stageResult: this.stageResult) {
        if (this.stageResult != Lint.RESULT_SUCCESS) {
        sh 'exit 1'
        }
    }
  }

/**
 * report lint logs using Warnings Next Generation Plugin
 * @see https://jenkins.io/doc/pipeline/steps/warnings-ng/
 * @see https://github.com/jenkinsci/warnings-ng-plugin/blob/master/doc/Documentation.md
 * @see https://github.com/jenkinsci/warnings-ng-plugin/blob/master/SUPPORTED-FORMATS.md
 * mark build as failed if error found
 *
 * @param referenceJob `String` field use in order to calculate the reference build that
 * that will be used to compare the results with the current build's results
 * for more information check
 * [Configure the selection of the reference build (baseline)](https://tinyurl.com/yw4xthc8)
 *
 * @param lintTools `List` list of parameters to pass to the ng warnings `scanForIssues`
 * and `publishIssues` methods. for more information about some parameters check
 * [Warnings NG API doc](https://www.jenkins.io/doc/pipeline/steps/warnings-ng/)
 * @param lintTools[].tool `String` log parser to use (check Warnings NG API doc)
 * @param lintTools[].pattern `String` pattern parameter to pass to the tool, indicates
 * path and files pattern to match log files to parse
 * @param lintTools[].id `String` The results of the selected tool are published using a unique ID
 * @param lintTools[].name `String` the title of the result page
 * @param lintTools[].qualityGates `String` Defines a quality gate based on a specific threshold of
 * issues (total, new, delta) in the current build. After a build has been finished, a set of
 * quality gates will be evaluated and the overall quality gate status will be reported in Jenkins UI.
 * (check Warnings NG API doc for more details)
 * @param lintTools[].ignoreQualityGate `String` (default false) (check Warnings NG API doc for
 * more details)
 * @param lintTools[].trendChartType `String` (default TOOLS_AGGREGATION) (check Warnings NG API
 * doc for more details)
 *
 * @example report all hadolint, addons and phpstorm lint logs
 * **Note**: Lint.transformReport - Note that some logs need to be converted to ng format
 * before being able to report them
 * ```groovy
   report([
    [
      tool: 'hadoLint',
      pattern: 'logs/hadolint-*.log',
      id: "hadoLint",
      name: 'lint docker',
      qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
      ignoreQualityGate: false
    ],
    [
      tool: 'issues',
      pattern: 'logs/ng-addons-lint.json',
      id: "addonsLint",
      name: 'lint addons',
      qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
      ignoreQualityGate: true
    ],
    [
      tool: 'ideaInspection',
      pattern: 'logs/idea_inspections/*.xml',
      id: "idea",
      name: 'phpstorm inspections',
      qualityGates: [[threshold: 1, type: 'NEW', unstable: true]],
      ignoreQualityGate: false
    ]
   ])
 * ```
 */
  void report(String referenceJob, List lintTools) {
    this.jenkinsExecutor.discoverReferenceBuild referenceJob: referenceJob
    List<AnnotatedReport> allIssues = []
    lintTools.each { log ->
      try {
        AnnotatedReport lintIssues = this.jenkinsExecutor.scanForIssues \
          tool: this.jenkinsExecutor."${log.tool}"(pattern: log.pattern)
        allIssues += lintIssues
        this.allIssues += lintIssues

        this.jenkinsExecutor.publishIssues \
          id: log.id, \
          name: log.name, \
          issues:[lintIssues], \
          qualityGates: log.qualityGates, \
          ignoreQualityGate: log.ignoreQualityGate, \
          trendChartType: log.trendChartType ?: 'TOOLS_AGGREGATION'

      } catch (err) {
        this.jenkinsExecutor.println("scan/publish results has failed : ${err}")
        throw err
      }
    }
  }

  /**
   * allows to transform a linting report in a NG warning compatible report
   * @param libraryResourcePath `String` the path where transformer resources can be found
   * for security reason, we cannot detect the path where this library is installed so
   * resources used for docker have to be external, you can use the following code in your
   * Jenkinsfile in order to checkout the resources repository and pass the folder in this parameter
   * ```groovy
     libGit.branchCheckout(
      "${env.WORKSPACE_TMP}/jenkins-library-resources",
      credentialsId,
      'git@github.com:fchastanet/jenkins-library-resources.git',
      'main'
     )
   * ```
   * @param srcReportFile `String` the name of original log file
   * @param targetReportFile `String` the name of converted log file
   * @param transformer `ITransformer` transformer instance object of class defined
   * in fchastanet.transformers packages
   * check [Lighthouse2Issues transformer](transformers/Lighthouse2Issues.md)
   * or [Json2Issues transformer](transformers/Json2Issues.md)
   * @param transformerArgs `Map` = (default value: [:]) arguments to pass to the transformer instance
   * check transformer documentation
   * @param registryImageUrl `String`
   * the registry where the docker image used to transform logs can be built, pushed, or pulled
   * @param imageName `String` (default value: 'lint-converters') docker image name used to transform logs
   *
   * @example convert stylelint log file to Warnings NG issues format
   * ```groovy
     lint.transformReport(
        "${env.WORKSPACE_TMP}/jenkins-library-resources",
        "${env.WORKSPACE}/logs/stylelint.json",
        "${env.WORKSPACE}/logs/ng-stylelint.json",
        lib.fchastanet.transformers.Json2Issues.new(this, 'stylelint-v1')
     )
   * ```
   *
   * @throws Exception if one of the command fails
   */
  void transformReport(
    String libraryResourcePath,
    String srcReportFile,
    String targetReportFile,
    ITransformer transformer,
    Map transformerArgs = [:],
    String registryImageUrl,
    String imageName = 'lint-converters'
  ) {
    if (this.converterImageBuildNeeded) {
      Docker docker = new Docker(this.jenkinsExecutor)
      Git git = new Git(this.jenkinsExecutor)

      String dockerPath = libraryResourcePath + '/warnings-ng'
      docker.pullBuildPushImage(
          buildDirectory:   dockerPath,
          registryImageUrl: "${registryImageUrl}/${imageName}",
          tagPrefix:        "${imageName}:",
          tags: [
            git.getCommitSha(libraryResourcePath)
          ]
      )
      this.converterImageBuildNeeded = false
    }

    transformer.transform(srcReportFile, targetReportFile, transformerArgs)
  }

  /**
   * Transform all lighthouse reports in Warnings NG issues format
   * @see [LightHouse understanding-results.md](https://tinyurl.com/75uzfub4)
   * @see [Warnings NG issues examples](https://tinyurl.com/n4jv44yx)
   *
   * @param libraryResourcePath `String` directory containing the git project
   * you can checkout this repository using
   * ```groovy
     libGit.branchCheckout(
       "${env.WORKSPACE_TMP}/jenkins-library-resources",
       credentialsId,
       'git@github.com:fchastanet/jenkins-library-resources.git',
       'main'
     )
   * ```
   *
   * @param dir `String` base dir from which report files are searched
   *
   * @param reportFilePattern `String` the report files glob pattern
   * eg: 'lhr-*.json'
   *
   * @param lighthouseThresholdConfigPath `String` path to the config defining threshold configuration
   *
   * @param registryImageUrl `String`
   *    the docker image registry from where lint-converter image will be pulled
   *
   * @param imageName `String` = 'lint-converters'
   *    the image name of lint-converter image
   *
   * @param jqFile `String` = 'lighthouse-v0.8'
   *    the converter to use
   */
  void transformLightHouseReports(
    String libraryResourcePath,
    String dir,
    String reportFilePattern,
    String lighthouseThresholdConfigPath,
    String registryImageUrl,
    String imageName = 'lint-converters',
    String jqFile = 'lighthouse-v0.8'
  ) {
    FileWrapper[] reports
    this.jenkinsExecutor.dir(dir) {
      reports = this.jenkinsExecutor.findFiles(glob: reportFilePattern)
    }
    for (report in reports) {
      String transformedReportFilename = "${dir}/ng-${report.name}"
      this.transformReport(
        libraryResourcePath,
        "${dir}/${report.name}",
        transformedReportFilename,
        new Lighthouse2Issues(this.jenkinsExecutor, jqFile),
        [lighthouseThresholdConfigPath: lighthouseThresholdConfigPath],
        registryImageUrl,
        imageName
      )
    }
  }

  /**
   * publish all the reports scanned using this class
   * use this method in post action of your jenkins file
   * @param title `String`
   * @param trendChartType `String`
   */
  void publishAllIssues(String title = 'All Issues', String trendChartType = 'AGGREGATION_TOOLS') {
    if (!this.allIssues.isEmpty()) {
      this.jenkinsExecutor.publishIssues \
        id: 'allIssues', \
        name: title, \
        issues: this.allIssues, \
        trendChartType: trendChartType
      // cleaning
      this.allIssues = []
    }
  }

}
