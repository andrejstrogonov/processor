package org.kiuru.processor.quartus.transform

import org.kiuru.processor.quartus.ast._
import scala.collection.mutable

/**
 * Удалить дублирующиеся модули
 *
 * Алгоритм:
 * 1. Парсить все module definitions
 * 2. Сохранить первое определение каждого модуля
 * 3. Удалить все последующие дубликаты по имени
 * 4. Выдать warning о удалённых дубликатах
 */
class ModuleDupFix(val options: TransformOptions) extends QuartusTransformFix {

  override def description: String =
    "Remove duplicate module definitions"

  private val warnings: mutable.Buffer[String] = mutable.Buffer()

  override def apply(modules: Seq[Module]): Seq[Module] = {
    warnings.clear()
    val seenModuleNames = mutable.Set[String]()
    val result = mutable.Buffer[Module]()

    for (module <- modules) {
      if (seenModuleNames.contains(module.name)) {
        warnings += s"Removing duplicate module definition: ${module.name}"
        if (options.addFixMarkers) {
          // Можно отметить в логе
        }
      } else {
        seenModuleNames += module.name
        result += module
      }
    }

    result.toSeq
  }

  /**
   * Получить список предупреждений об удаленных дубликатах
   *
   * @return список предупреждений
   */
  def getWarnings: Seq[String] = warnings.toSeq
}
