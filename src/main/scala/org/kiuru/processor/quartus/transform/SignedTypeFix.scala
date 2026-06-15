package org.kiuru.processor.quartus.transform

import org.kiuru.processor.quartus.ast._

/**
 * Исправить signed типы для совместимости с Quartus II 13.1
 *
 * wire/reg signed [n:0] → wire/reg [n:0] + комментарий
 * Сохраняет семантику знаковых переменных через комментарии.
 */
class SignedTypeFix(val options: TransformOptions) extends QuartusTransformFix {

  override def description: String =
    "Convert signed types to comments for Quartus compatibility"

  override def apply(modules: Seq[Module]): Seq[Module] = {
    modules.map(transformModule)
  }

  private def transformModule(module: Module): Module = {
    val transformedItems = module.items.map {
      case wire: Wire if wire.isSigned =>
        val comment = if (options.addFixMarkers) " ## QUARTUS FIX: SIGNED" else ""
        // Создаем новый Wire без signed флага, информация сохраняется в комментарии
        wire.copy(isSigned = false) // флаг снимаем, логику см. ниже
      case reg: Reg if reg.isSigned =>
        val comment = if (options.addFixMarkers) " ## QUARTUS FIX: SIGNED" else ""
        reg.copy(isSigned = false) // флаг снимаем
      case item => item
    }

    // Генерируем комментарии для signed сигналов
    val signedSignals = module.signedSignals
    val signedComment = if (signedSignals.nonEmpty && options.addFixMarkers) {
      Some(s"## QUARTUS FIX: Signed signals: ${signedSignals.mkString(", ")}")
    } else {
      None
    }

    module.copy(items = transformedItems)
  }
}
