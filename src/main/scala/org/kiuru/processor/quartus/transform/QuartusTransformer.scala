package org.kiuru.processor.quartus.transform

import org.kiuru.processor.quartus.ast._
import scala.collection.mutable

/**
 * Главная фасада для Quartus II 13.1 трансформаций.
 * Применяет все Fix'ы в правильном порядке для обеспечения совместимости.
 *
 * Порядок применения:
 * 1. ModuleDupFix - удалить дублирующиеся модули
 * 2. HierarchyFix - исправить имена иерархии
 * 3. SignedTypeFix - исправить signed типы
 * 4. WireRegFix - исправить wire в процедурных блоках
 */
class QuartusTransformer(options: TransformOptions = TransformOptions()) {

  private val fixes: Seq[QuartusTransformFix] = Seq(
    new ModuleDupFix(options),
    new HierarchyFix(options),
    new SignedTypeFix(options),
    new WireRegFix(options)
  )

  private val transformLog: mutable.Buffer[String] = mutable.Buffer()

  /**
   * Применить все трансформации к модулям
   *
   * @param modules список модулей для трансформации
   * @return трансформированные модули
   */
  def transformModules(modules: Seq[Module]): Seq[Module] = {
    transformLog.clear()
    var result = modules

    for (fix <- fixes) {
      val fixName = fix.getClass.getSimpleName
      transformLog += s"Applying $fixName..."
      result = fix.apply(result)
      transformLog += s"$fixName completed"
    }

    result
  }

  /**
   * Применить все трансформации к одному модулю
   *
   * @param module модуль для трансформации
   * @return трансформированный модуль
   */
  def transformModule(module: Module): Module = {
    transformModules(Seq(module)).head
  }

  /**
   * Получить лог трансформаций
   *
   * @return строки лога
   */
  def getTransformLog: Seq[String] = transformLog.toSeq

  /**
   * Получить описание выполненных трансформаций
   *
   * @return многострочный текст с описанием
   */
  def getTransformSummary: String = {
    val summary = mutable.Buffer[String]()
    summary += "=== Quartus II 13.1 Transformation Summary ==="
    summary += s"Version: ${options.verilogVersion}"
    summary += s"Target Family: ${options.targetFamily}"
    summary += s"Add Fix Markers: ${options.addFixMarkers}"
    summary += ""
    summary += "Fixes Applied:"
    fixes.foreach { fix =>
      summary += s"  - ${fix.getClass.getSimpleName}: ${fix.description}"
    }
    summary.mkString("\n")
  }
}

/**
 * Базовый trait для всех трансформаций
 */
sealed trait QuartusTransformFix {
  def options: TransformOptions

  def description: String

  def apply(modules: Seq[Module]): Seq[Module]
}
