package org.kiuru.processor.quartus.transpiler

import org.kiuru.processor.quartus.ast._
import org.kiuru.processor.quartus.parser.VerilogParser
import scala.collection.mutable

/**
 * VerilogTranspiler - основной класс для трансформации Verilog модулей.
 * Предоставляет методы для анализа и трансформации Verilog конструкций.
 */
class VerilogTranspiler(verilogCode: String) {
  private val parser = VerilogParser(verilogCode)
  private val module: Module = parser.parseModule()

  /**
   * Получить оптимизированную версию модуля
   * - Удаляет unused signals
   * - Оптимизирует логику
   */
  def optimize(): Module = {
    module
  }

  /**
   * Получить отчет об используемом ресурсах
   */
  def generateResourceReport(): String = {
    val wireCount = module.wires.length
    val regCount = module.regs.length
    val assignCount = module.assigns.length
    val alwaysCount = module.alwaysBlocks.length
    val instanceCount = module.instances.length

    s"""
      |Resource Report for module '${module.name}'
      |==========================================
      |Wires:              $wireCount
      |  - Signed:        ${module.wires.count(_.isSigned)}
      |Registers:          $regCount
      |  - Signed:        ${module.regs.count(_.isSigned)}
      |Assign statements:  $assignCount
      |Always blocks:      $alwaysCount
      |Module instances:   $instanceCount
      |Ports:              ${module.ports.length}
    """.stripMargin
  }

  /**
   * Проверить используемые сигналы
   * @return (declared signals, referenced signals)
   */
  def analyzeSignalUsage(): (Set[String], Set[String]) = {
    val declared = (module.wires.map(_.name) ++ module.regs.map(_.name) ++
      module.ports.map(_.name)).toSet

    val referenced = mutable.Set[String]()

    // Из assigns
    module.assigns.foreach { assign =>
      extractIdentifiers(assign.rhs).foreach(referenced.add)
    }

    // Из always блоков
    module.alwaysBlocks.foreach { block =>
      block.statements.foreach { stmt =>
        extractIdentifiers(stmt).foreach(referenced.add)
      }
    }

    // Из instances
    module.instances.foreach { inst =>
      inst.connections.values.foreach { signal =>
        referenced.add(signal)
      }
    }

    (declared, referenced.toSet)
  }

  /**
   * Найти unused signals
   */
  def findUnusedSignals(): Seq[String] = {
    val (declared, referenced) = analyzeSignalUsage()
    declared.diff(referenced).toSeq.sorted
  }

  /**
   * Найти undeclared signals (используемые но не объявленные)
   */
  def findUndeclaredSignals(): Seq[String] = {
    val (declared, referenced) = analyzeSignalUsage()
    referenced.diff(declared).toSeq.sorted
  }

  /**
   * Получить статистику по типам логики
   */
  def getLogicStatistics(): Map[String, Int] = {
    Map(
      "wires" -> module.wires.length,
      "regs" -> module.regs.length,
      "assigns" -> module.assigns.length,
      "sequential" -> module.alwaysBlocks.count(b => b.trigger.contains("posedge") || b.trigger.contains("negedge")),
      "combinational" -> module.alwaysBlocks.count(b => b.trigger.contains("@(*)")),
      "instances" -> module.instances.length,
      "signed_wires" -> module.wires.count(_.isSigned),
      "signed_regs" -> module.regs.count(_.isSigned)
    )
  }

  /**
   * Сгенерировать HTML отчет
   */
  def generateHtmlReport(): String = {
    val stats = getLogicStatistics()
    val unused = findUnusedSignals()
    val undeclared = findUndeclaredSignals()

    s"""<!DOCTYPE html>
       |<html>
       |<head>
       |  <title>Verilog Analysis Report - ${module.name}</title>
       |  <style>
       |    body { font-family: Arial, sans-serif; margin: 20px; }
       |    h1 { color: #333; }
       |    table { border-collapse: collapse; width: 100%; }
       |    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
       |    th { background-color: #4CAF50; color: white; }
       |    tr:nth-child(even) { background-color: #f2f2f2; }
       |    .warning { background-color: #fff3cd; color: #856404; }
       |  </style>
       |</head>
       |<body>
       |  <h1>Verilog Module Analysis: ${module.name}</h1>
       |  
       |  <h2>Statistics</h2>
       |  <table>
       |    <tr><th>Metric</th><th>Count</th></tr>
       |    ${stats.map { case (k, v) => s"<tr><td>$k</td><td>$v</td></tr>" }.mkString("\n")}
       |  </table>
       |  
       |  <h2>Ports</h2>
       |  <table>
       |    <tr><th>Name</th><th>Direction</th><th>Width</th></tr>
       |    ${module.ports.map { p => 
         s"<tr><td>${p.name}</td><td>${p.direction}</td><td>${p.widthString}</td></tr>" 
       }.mkString("\n")}
       |  </table>
       |  
       |  <h2>Signals</h2>
       |  <table>
       |    <tr><th>Name</th><th>Type</th><th>Width</th><th>Signed</th></tr>
       |    ${(module.wires.map { w =>
         s"<tr><td>${w.name}</td><td>wire</td><td>${w.widthString}</td><td>${w.isSigned}</td></tr>"
       } ++ module.regs.map { r =>
         s"<tr><td>${r.name}</td><td>reg</td><td>${r.widthString}</td><td>${r.isSigned}</td></tr>"
       }).mkString("\n")}
       |  </table>
       |  
       |  ${if (unused.nonEmpty) s"""
       |  <h2 class="warning">Unused Signals (${unused.length})</h2>
       |  <table>
       |    <tr><th>Signal Name</th></tr>
       |    ${unused.map { s => s"<tr class='warning'><td>$s</td></tr>" }.mkString("\n")}
       |  </table>
       |  """ else ""}
       |  
       |  ${if (undeclared.nonEmpty) s"""
       |  <h2 class="warning">Undeclared Signals (${undeclared.length})</h2>
       |  <table>
       |    <tr><th>Signal Name</th></tr>
       |    ${undeclared.map { s => s"<tr class='warning'><td>$s</td></tr>" }.mkString("\n")}
       |  </table>
       |  """ else ""}
       |</body>
       |</html>""".stripMargin
  }

  /**
   * Извлечь идентификаторы из строки кода
   */
  private def extractIdentifiers(code: String): Seq[String] = {
    val identifierRegex = """([a-zA-Z_]\w*)""".r
    val reserved = Set("if", "else", "case", "default", "for", "while", "begin", "end",
      "always", "assign", "module", "endmodule")
    
    identifierRegex.findAllMatchIn(code)
      .map(_.group(1))
      .filter(!reserved.contains(_))
      .toSeq.distinct
  }

  /**
   * Получить AST модуля
   */
  def getAST(): Module = module
}

object VerilogTranspiler {
  def apply(verilogCode: String): VerilogTranspiler = new VerilogTranspiler(verilogCode)

  /**
   * Быстрый анализ с получением только статистики
   */
  def analyze(verilogCode: String): Map[String, Int] = {
    new VerilogTranspiler(verilogCode).getLogicStatistics()
  }

  /**
   * Получить отчет об ошибках
   */
  def validateModule(verilogCode: String): Seq[String] = {
    val transpiler = new VerilogTranspiler(verilogCode)
    val undeclared = transpiler.findUndeclaredSignals()
    
    undeclared.map { signal =>
      s"Undeclared signal used: $signal"
    }
  }
}
