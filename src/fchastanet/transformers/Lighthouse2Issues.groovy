/* groovylint-disable LineLength */
package fchastanet.transformers

/**
 * Transforms lighthouse json file to ng warnings issues format
 */
class Lighthouse2Issues implements Serializable, ITransformer {

  private static final long serialVersionUID = 1L

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor
  private final String jqFile

  /**
   * @param jenkinsExecutor `Executor`
   * @param jqFile `String` the name of jq file to use (name without extension)
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Lighthouse2Issues(jenkinsExecutor, String jqFile = 'lighthouse-v0.8') {
    this.jenkinsExecutor = jenkinsExecutor
    this.jqFile = jqFile
  }

 /**
   * This method is used by Lint.transformLightHouseReports method
   * Transform a lighthouse report in Warnings NG issues format
   * and apply thresholds on cetegories
   * @see [LightHouse understanding-results.md](https://tinyurl.com/75uzfub4)
   * @see [Warnings NG issues examples](https://tinyurl.com/n4jv44yx)
   *
   * @param srcReportFile `String` lighthouse json report file (traditionaly the representative run report)
   * @param targetReportFile `String` result file in warnings NG issues format
   * @param args `Map`
   * @param args.lighthouseThresholdConfigPath `String` thresholds configuration path
   *
   * @see [Threshold configuration defaults with documentation integrated](https://github.com/fchastanet/jenkins-library-resources/blob/master/warnings-ng/lintLogsSamples/conf/lighthouse-v0.8-thresholds-default.js)
   */
  boolean transform(String srcReportFile, String targetReportFile, Map args = [:]) {
    return this.jenkinsExecutor.sh("""#!/bin/bash
      set -o errexit
      set -x
      docker run --rm \
        -v "${srcReportFile}":/tmp/srcReportFile \
        -v "${args.lighthouseThresholdConfigPath}":/tmp/thresholdConfigFile \
        lint-converters \
        /usr/app/converters/convertLightouse.sh \
          "${this.jqFile}" \
          '/tmp/thresholdConfigFile' \
      > "${targetReportFile}"
    """)
  }

}
