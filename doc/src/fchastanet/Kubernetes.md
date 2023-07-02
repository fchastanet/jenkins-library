# Documentation

## `class Kubernetes implements Serializable`

Provides methods related to Kubernetes

### `void deployHelmChart( Map args )`

deploy helm chart into selected environment 



 * **Parameters:**
   * `args` — `Map` see structure below
   * `args.helmDirectory` — `String` base directory for helm chart
   * `args.chartName` — `String` helm chart name
   * `args.nameSpace` — `String` deploying namespace
   * `args.imageTag` — `String` docker image tag
   * `args.helmValueFilePath` — `String` full path to the helm value file
 * **Exceptions:** `Exception` — if helm chart doesn't deploy correctly

### `String getIngressUrl(String helmPath)`

get ingress url value 



 * **Parameters:** `helmPath` — `String` full path to the helm value file
 * **Returns:** ingressUrl `String` ingress url value
     


 * **Exceptions:** `Exception` — if helm value file path is not correct
