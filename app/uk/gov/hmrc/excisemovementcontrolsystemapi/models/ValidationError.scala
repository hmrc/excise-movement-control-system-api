package uk.gov.hmrc.excisemovementcontrolsystemapi.models

sealed trait TransformationError

case class FormatValidationError(error: String) extends TransformationError
case class SchemaValidationError(error:String) extends TransformationError
case object MessageDoesNotExistError extends TransformationError

case class RewriteNamespaceError(error: String) extends TransformationError
case class ImportSadConversionError(error:String) extends  TransformationError
case class ExportDeclaractionTransformError(error: String) extends TransformationError
