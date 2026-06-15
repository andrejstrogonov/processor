package org.kiuru.processor.quartus.transform

import org.kiuru.processor.quartus.ast._
import scala.collection.mutable

/**
 * Исправить wire в процедурных блоках (always)
 *
 * Выявить wire, которые используются в always блоках с non-blocking assign (<=).
 * Правило: если wire присваивается в always с <=, то это должен быть reg.
 *
 * Алгоритм:
 * 1. Найти все wire, которые присваиваются в always блоках
 * 2. Проверить наличие non-blocking assign (<=)
 * 3. Преобразовать такие wire в reg
 */
class WireRegFix(val options: TransformOptions) extends QuartusTransformFix {

  override def description: String =
    "Convert procedurally assigned wires to regs"

  override def apply(modules: Seq[Module]): Seq[Module] = {
    modules.map(transformModule)
  }

  private def transformModule(module: Module): Module = {
    // Найти все сигналы, которые присваиваются в always блоках
    val assignedInAlways = findProcedurallyAssignedSignals(module)

    // Трансформировать wire в reg если они присваиваются в always
    val transformedItems = module.items.map {
      case wire: Wire if assignedInAlways.contains(wire.name) =>
        // Преобразуем wire в reg
        val reg = Reg(wire.name, wire.width, wire.isSigned)
        reg

      case item => item
    }

    module.copy(items = transformedItems)
  }

  /**
   * Найти все сигналы, которые присваиваются в always блоках с non-blocking assign
   *
   * @param module модуль для анализа
   * @return набор имен сигналов, присваиваемых в always блоках
   */
  private def findProcedurallyAssignedSignals(module: Module): Set[String] = {
    val assigned = mutable.Set[String]()

    for (alwaysBlock <- module.alwaysBlocks) {
      for (statement <- alwaysBlock.statements) {
        // Найти non-blocking assign (<= или =)
        // non-blocking: signal <= value
        val nbPattern = """(\w+)\s*<=\s*""".r
        val bPattern = """(\w+)\s*=\s*""".r

        nbPattern.findAllMatchIn(statement).foreach { m =>
          assigned += m.group(1)
        }

        // Также проверяем blocking assign в комбинаторных always блоках
        if (alwaysBlock.trigger.contains("@(*)")) {
          bPattern.findAllMatchIn(statement).foreach { m =>
            assigned += m.group(1)
          }
        }
      }
    }

    assigned.toSet
  }
}
