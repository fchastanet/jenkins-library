package fchastanet.transformers

/**
 * a Transformer class needs to implement this interface
 */
interface ITransformer {

  boolean transform(String srcReportFile, String targetReportFile, Map args)

}
