package fchastanet

/**
 * Provides methods related to Kubernetes
 */
class Kubernetes implements Serializable {

  private static final long serialVersionUID = 1L

  private static final short STATUS_OK = 0

  public static final String INVALID_HELM_DIRECTORY_ARG = 'Invalid or missing helmDirectory arg'
  public static final String INVALID_HELM_CHART_NAME_ARG = 'Invalid or missing chartName arg'
  public static final String INVALID_NAMESPACE_ARG = 'Invalid or missing nameSpace arg'
  public static final String INVALID_IMAGE_TAG_ARG = 'Invalid or missing imageTag arg'
  public static final String INVALID_HELM_PATH = 'Invalid or missing helm path'

  /* groovylint-disable-next-line FieldTypeRequired, NoDef */
  private final jenkinsExecutor

  /**
   * @param jenkinsExecutor `Executor`
   */
  /* groovylint-disable-next-line MethodParameterTypeRequired, NoDef */
  Kubernetes(jenkinsExecutor) {
    this.jenkinsExecutor = jenkinsExecutor
  }

  /**
   * deploy helm chart into selected environment
   *
   * @param args `Map` see structure below
   * @param args.helmDirectory `String` base directory for helm chart
   * @param args.chartName `String` helm chart name
   * @param args.nameSpace `String` deploying namespace
   * @param args.imageTag `String` docker image tag
   * @param args.helmValueFilePath `String` full path to the helm value file
   *
   * @throws Exception if helm chart doesn't deploy correctly
   */
  void deployHelmChart(
    Map args
  ) {
    if (!args?.helmDirectory?.trim()) {
      throw new Exception(Kubernetes.INVALID_HELM_DIRECTORY_ARG)
    }
    if (!args?.chartName?.trim()) {
      throw new Exception(Kubernetes.INVALID_HELM_CHART_NAME_ARG)
    }
    if (!args?.nameSpace?.trim()) {
      throw new Exception(Kubernetes.INVALID_NAMESPACE_ARG)
    }
    if (!args?.imageTag?.trim()) {
      throw new Exception(Kubernetes.INVALID_IMAGE_TAG_ARG)
    }
    String chartName = args.chartName
    String helmDirectory = args.helmDirectory
    String nameSpace = args.nameSpace
    String imageTag = args.imageTag
    String helmValueFilePath = args?.helmValueFilePath ?: ''
    if (!helmValueFilePath.trim()) {
      helmValueFilePath = "${args.helmDirectory}/value_files/values-default.yaml"
    }
    short status = Kubernetes.STATUS_OK

    status = this.jenkinsExecutor.sh(script: """
      helm3 upgrade ${chartName} -i \
      --namespace ${nameSpace} \
      -f ${helmValueFilePath} \
      --set image.tag=${imageTag} \
      --create-namespace \
      --cleanup-on-fail \
      --atomic \
      ${helmDirectory}
    """, returnStatus: true)
    if (status != Kubernetes.STATUS_OK) {
      throw new Exception("Unable to deploy ${chartName} helm chart")
    }
  }

  void deployHelmChartParameterized(
    Map args
  ) {
    String helmParameters = args.helmParameters
    String chartName = args.chartName
    short status = Kubernetes.STATUS_OK

    status = this.jenkinsExecutor.sh(script: """
      ${helmParameters}
    """, returnStatus: true)
    if (status != Kubernetes.STATUS_OK) {
      throw new Exception("Unable to deploy ${chartName} helm chart")
    }
  }
  /**
   * get ingress url value
   *
   * @param helmPath `String` full path to the helm value file
   * @return ingressUrl `String` ingress url value
   *
   * @throws Exception if helm value file path is not correct
   */
  String getIngressUrl(String helmPath) {
    if (!helmPath.trim()) {
      throw new Exception(Kubernetes.INVALID_HELM_PATH)
    }
    String ingressUrl = this.jenkinsExecutor.sh(
      script: "cat ${helmPath} | grep host | cut -d ':' -f2 ",
      returnStdout: true
    ).trim()
    return ingressUrl
  }

}
