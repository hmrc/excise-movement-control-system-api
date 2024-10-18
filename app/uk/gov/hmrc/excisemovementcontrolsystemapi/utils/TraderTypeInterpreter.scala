package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

object TraderTypeInterpreter {
  def fromExciseId(exciseId: String) = {
    val prefix   = exciseId.take(2)
    val extended = exciseId.substring(2, 4)
    val suffix   = exciseId.takeRight(2)

    (prefix, extended, suffix) match {
      case ("GB" || "XI", "WK", "WK") => "1" //Warehouse Keeper
      case ("XI", "00", "RT")         => "4" //Registered Consignee
      case ("GB" || "XI", "00", _)    => "2" //Tax Warehouse
      case ("GB" || "XI", "RC", "RC") => "3" //Registered Consignor
      case ("XI", "TC", "TC")         => "5" //Temporary Registered Consignee
      //TODO: Confirm - Temporary Registered Authorisation - rules unclear/unknown => Some("6")
      //Default
      case _                          => "7" //Other
    }
  }
}

// Temporary Registered Authorisation probably started with an XI, but the extension/suffix are unknown.
// The term may well be wrong.
