/* groovylint-disable UnusedMethodParameter */
package fchastanet.transformers

/**
 * Transforms json file to ng warnings issues format
 * using specified jq transformer
 */
class Json2Issues implements Serializable, ITransformer {

  private static final long serialVersionUID = 1L

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor
  private final String jqFile

  /**
   * @param jenkinsExecutor `Executor`
   * @param jqFile `String` the name of jq file to use (name without extension)
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Json2Issues(jenkinsExecutor, String jqFile) {
    this.jenkinsExecutor = jenkinsExecutor
    this.jqFile = jqFile
  }

  /**
   * This method is used by Lint.transformReport method
   * Transform a json report in Warnings NG issues format using jqfile
   * provided in constructor
   *
   * @param srcReportFile `String` source file to transform
   * @param targetReportFile `String` target file
   * @param args `Map` not used
   */
  boolean transform(String srcReportFile, String targetReportFile, Map args = [:]) {
    return this.jenkinsExecutor.sh("""
      docker run --rm \
        -v ${srcReportFile}:/tmp/srcReportFile \
        -w /tmp \
        lint-converters \
       jq -f "/usr/app/converters/${this.jqFile}.jq" srcReportFile > "${targetReportFile}"
    """)
  }

}
