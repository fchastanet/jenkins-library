# Documentation

## `class Json2Issues implements Serializable, ITransformer`

Transforms json file to ng warnings issues format using specified jq transformer

### `boolean transform(String srcReportFile, String targetReportFile, Map args = [:])`

This method is used by Lint.transformReport method Transform a json report in Warnings NG issues format using jqfile provided in constructor 



 * **Parameters:**
   * `srcReportFile` — `String` source file to transform
   * `targetReportFile` — `String` target file
   * `args` — `Map` not used
