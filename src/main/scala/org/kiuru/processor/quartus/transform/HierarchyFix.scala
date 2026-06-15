package org.kiuru.processor.quartus.transform

import org.kiuru.processor.quartus.ast._
import scala.collection.mutable

/**
 * Исправить имена в иерархии модулей для совместимости с Quartus II 13.1
 *
 * Правила переименования:
 * 1. Модули с numeric-only именами: "1" → "top_module", "2" → "top_module_2"
 * 2. Имена начинающиеся с цифр: "2xyz" → "m_2xyz"
 * 3. Пустые имена: "" → "unnamed_0"
 * 4. Обновить все ссылки на переименованные модули в инстанциях
 */
class HierarchyFix(val options: TransformOptions) extends QuartusTransformFix {

  override def description: String =
    "Fix invalid module names and update hierarchy references"

  private val nameMapping: mutable.Map[String, String] = mutable.Map()
  private val warnings: mutable.Buffer[String] = mutable.Buffer()

  override def apply(modules: Seq[Module]): Seq[Module] = {
    nameMapping.clear()
    warnings.clear()

    // Фаза 1: Определить новые имена для invalid модулей
    val moduleNameMapping = mutable.Map[String, String]()
    var unnamedCounter = 0

    for (module <- modules) {
      val newName = sanitizeModuleName(module.name, unnamedCounter)
      if (newName != module.name) {
        moduleNameMapping(module.name) = newName
        warnings += s"Renaming module '$${module.name}' to '$newName'"
        unnamedCounter += 1
      }
    }

    // Фаза 2: Трансформировать модули с новыми именами и обновить ссылки
    val transformedModules = modules.map { module =>
      val newModuleName = moduleNameMapping.getOrElse(module.name, module.name)

      // Обновить имена в инстанциях (если они ссылаются на переименованные модули)
      val transformedItems = module.items.map {
        case instance: ModuleInstance =>
          val newModuleRefName = moduleNameMapping.getOrElse(instance.moduleName, instance.moduleName)
          instance.copy(moduleName = newModuleRefName)

        case item => item
      }

      module.copy(name = newModuleName, items = transformedItems)
    }

    transformedModules
  }

  /**
   * Санитизировать имя модуля для совместимости с Quartus
   *
   * @param name исходное имя
   * @param counter счетчик для unique имен
   * @return новое имя
   */
  private def sanitizeModuleName(name: String, counter: Int): String = {
    if (name.isEmpty) {
      s"unnamed_$counter"
    } else if (name.matches("^\\d+$")) {
      // Только цифры - это не валидное имя в Quartus
      if (counter == 0) "top_module" else s"top_module_$counter"
    } else if (name.matches("^\\d.*")) {
      // Начинается с цифры - добавить префикс
      s"m_$name"
    } else if (!isValidIdentifier(name)) {
      // Содержит недопустимые символы - сделать валидным
      val sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_")
      s"m_$sanitized"
    } else {
      name
    }
  }

  /**
   * Проверить, является ли строка валидным идентификатором для Verilog
   *
   * @param name имя для проверки
   * @return true если валидно
   */
  private def isValidIdentifier(name: String): Boolean = {
    name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")
  }

  /**
   * Получить список всех выполненных переименований
   *
   * @return список предупреждений
   */
  def getWarnings: Seq[String] = warnings.toSeq
}
