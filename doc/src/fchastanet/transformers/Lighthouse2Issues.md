# Documentation

## `class Lighthouse2Issues implements Serializable, ITransformer`

Transforms lighthouse json file to ng warnings issues format

### `boolean transform(String srcReportFile, String targetReportFile, Map args = [:])`

This method is used by Lint.transformLightHouseReports method Transform a lighthouse report in Warnings NG issues format and apply thresholds on cetegories

 * **See also:**
   * [LightHouse understanding-results.md](https://tinyurl.com/75uzfub4)
   * [Warnings NG issues examples](https://tinyurl.com/n4jv44yx)
     


   * [Threshold configuration defaults with documentation integrated](https://github.com/fchastanet/jenkins-library-resources/blob/master/warnings-ng/lintLogsSamples/conf/lighthouse-v0.8-thresholds-default.js)
 * **Parameters:**
   * `srcReportFile` — `String` lighthouse json report file (traditionaly the representative run report)
   * `targetReportFile` — `String` result file in warnings NG issues format
   * `args` — `Map`
   * `args.lighthouseThresholdConfigPath` — `String` thresholds configuration path
