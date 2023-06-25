# Documentation

## `class Lint implements Serializable`

provide methods for linting files

### `String getBuildResult()`

* **Deprecated:** please use catchError and when clauses instead

### `Lint setBuildResult(String buildResult)`

* **Deprecated:** please use catchError and when clauses instead

### `String getStageResult()`

* **Deprecated:** please use catchError and when clauses instead

### `Lint setStageResult(String stageResult)`

* **Deprecated:** please use catchError and when clauses instead

### `String dockerLint( String projectDir, String dockerfileRelativePath, String logRelativePath, String hadoLintImage = 'hadolint`

docker lint

* **Parameters:**
  * `projectDir` — `String`
  * `dockerfileRelativePath` — `String` path relative to project dir
  * `logRelativePath` — `String` path relative to project dir
  * `hadoLintImage` — `String` docker image to run
* **Returns:**  String one the constant Lint.RESULT_*

### `void markBuildAndStage()`

set current build and stage result deduced from stageResult and buildResult properties

* **Deprecated:** please use catchError and when clauses instead

### `void report(String referenceJob, List lintTools)`

report lint logs using Warnings Next Generation Plugin

* **See also:**
  * <https://jenkins.io/doc/pipeline/steps/warnings-ng/>
  * <https://github.com/jenkinsci/warnings-ng-plugin/blob/master/doc/Documentation.md>
  * <https://github.com/jenkinsci/warnings-ng-plugin/blob/master/SUPPORTED-FORMATS.md>
    mark build as failed if error found

* **Parameters:**
  * `referenceJob` — `String` field use in order to calculate the reference build that
    that will be used to compare the results with the current build's results
    for more information check
    [Configure the selection of the reference build (baseline)](https://tinyurl.com/yw4xthc8)
  * `lintTools` — `List` list of parameters to pass to the ng warnings `scanForIssues`
    and `publishIssues` methods. for more information about some parameters check
    [Warnings NG API doc](https://www.jenkins.io/doc/pipeline/steps/warnings-ng/)
  * `lintTools[].tool` — `String` log parser to use (check Warnings NG API doc)
  * `lintTools[].pattern` — `String` pattern parameter to pass to the tool, indicates
     path and files pattern to match log files to parse
  * `lintTools[].id` — `String` The results of the selected tool are published using a unique ID
  * `lintTools[].name` — `String` the title of the result page
  * `lintTools[].qualityGates` — `String` Defines a quality gate based on a specific threshold of
    issues (total, new, delta) in the current build. After a build has been finished, a set of
    quality gates will be evaluated and the overall quality gate status will be reported in Jenkins UI.
     (check Warnings NG API doc for more details)
  * `lintTools[].ignoreQualityGate` — `String` (default false) (check Warnings NG API doc for
     more details)
  * `lintTools[].trendChartType` — `String` (default TOOLS_AGGREGATION) (check Warnings NG API
    doc for more details)
* **Example:** report all hadolint, addons and phpstorm lint logs
    **Note**: Lint.transformReport - Note that some logs need to be converted to ng format
    before being able to report them

```groovy
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
```

### `void transformReport( String libraryResourcePath, String srcReportFile, String targetReportFile, ITransformer transformer, Map transformerArgs = [:], String registryImageUrl, String imageName = 'lint-converters' )`

allows to transform a linting report in a NG warning compatible report

* **Parameters:**
  * `libraryResourcePath` — `String` the path where transformer resources can be found
    for security reason, we cannot detect the path where this library is installed so
    resources used for docker have to be external, you can use the following code in your
    Jenkinsfile in order to checkout the resources repository and pass the folder in this parameter

```groovy
     libGit.branchCheckout(
      "${env.WORKSPACE_TMP}/jenkins_library_resources",
      credentialsId,
      'git@github.com:fchastanet/jenkins_library_resources.git',
      'main'
     )
```

* `srcReportFile` — `String` the name of original log file
* `targetReportFile` — `String` the name of converted log file
* `transformer` — `ITransformer` transformer instance object of class defined
    in fchastanet.transformers packages
    check [Lighthouse2Issues transformer](transformers/Lighthouse2Issues.md)
     or [Json2Issues transformer](transformers/Json2Issues.md)
* `transformerArgs` — `Map` = (default value: [:]) arguments to pass to the transformer instance
     check transformer documentation
* `registryImageUrl` — `String`
     the registry where the docker image used to transform logs can be built, pushed, or pulled
* `imageName` — `String` (default value: 'lint-converters') docker image name used to transform logs
* **Example:** convert stylelint log file to Warnings NG issues format

```groovy
     lint.transformReport(
        "${env.WORKSPACE_TMP}/jenkins_library_resources",
        "${env.WORKSPACE}/logs/stylelint.json",
        "${env.WORKSPACE}/logs/ng-stylelint.json",
        lib.fchastanet.transformers.Json2Issues.new(this, 'stylelint-v1')
     )
```

* **Exceptions:** `Exception` — if one of the command fails

### `void transformLightHouseReports( String libraryResourcePath, String dir, String reportFilePattern, String lighthouseThresholdConfigPath, String registryImageUrl, String imageName = 'lint-converters', String jqFile = 'lighthouse-v0.8' )`

Transform all lighthouse reports in Warnings NG issues format

* **See also:**
  * [LightHouse understanding-results.md](https://tinyurl.com/75uzfub4)
  * [Warnings NG issues examples](https://tinyurl.com/n4jv44yx)

* **Parameters:**
  * `libraryResourcePath` — `String` directory containing the git project
    you can checkout this repository using

```groovy
     libGit.branchCheckout(
       "${env.WORKSPACE_TMP}/jenkins_library_resources",
       credentialsId,
       'git@github.com:fchastanet/jenkins_library_resources.git',
       'main'
     )
```

* `dir` — `String` base dir from which report files are searched
* `reportFilePattern` — `String` the report files glob pattern
    eg: 'lhr-*.json'
* `lighthouseThresholdConfigPath` — `String` path to the config defining threshold configuration
* `registryImageUrl` — `String`
    the docker image registry from where lint-converter image will be pulled
* `imageName` — `String` = 'lint-converters'
    the image name of lint-converter image
* `jqFile` — `String` = 'lighthouse-v0.8'
    the converter to use

### `void publishAllIssues(String title = 'All Issues', String trendChartType = 'AGGREGATION_TOOLS')`

publish all the reports scanned using this class use this method in post action of your jenkins file

* **Parameters:**
  * `title` — `String`
  * `trendChartType` — `String`
