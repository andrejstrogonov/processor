package org.kiuru.processor.quartus.parser

import org.kiuru.processor.quartus.ast._
import scala.util.matching.Regex
import scala.collection.mutable

/**
 * Verilog Parser - преобразует исходный код в AST.
 * Использует регулярные выражения для быстрого и простого парсинга.
 * Поддерживает основные конструкции: module, wire, reg, assign, always, инстанции.
 */
class VerilogParser(verilogCode: String) {

  private val normalizedCode = verilogCode.replaceAll("//.*?$", "").replaceAll("/\\*[\\s\\S]*?\\*/", "")

  /**
   * Парсить полный модуль из Verilog кода
   * @return Module AST
   */
  def parseModule(): Module = {
    val moduleNameRegex = """module\s+(\w+)\s*(\([^)]*\))?\s*;""".r
    val moduleEndRegex = """endmodule""".r

    val moduleMatch = moduleNameRegex.findFirstMatchIn(normalizedCode)
    val moduleName = moduleMatch.map(_.group(1)).getOrElse("UnnamedModule")

    val moduleStart = moduleMatch.map(_.end).getOrElse(0)
    val moduleEnd = moduleEndRegex.findFirstMatchIn(normalizedCode).map(_.start).getOrElse(normalizedCode.length)

    val moduleContent = normalizedCode.substring(moduleStart, moduleEnd)

    val ports = parsePorts()
    val items = parseModuleItems(moduleContent)

    Module(moduleName, ports, items)
  }

  /**
   * Парсить порты модуля
   * @return Seq[Port]
   */
  def parsePorts(): Seq[Port] = {
    val portsRegex = """module\s+\w+\s*\(\s*([^)]+)\s*\)""".r
    val portListRegex = """(input|output|inout)\s+(signed\s+)?\[?(\d+):(\d+)\]?\s*(\w+)""".r

    val portMatch = portsRegex.findFirstMatchIn(normalizedCode)
    if (portMatch.isEmpty) return Seq()

    val portString = portMatch.get.group(1)
    val ports = mutable.ListBuffer[Port]()

    portListRegex.findAllMatchIn(portString).foreach { m =>
      val direction = m.group(1)
      val isSigned = Option(m.group(2)).isDefined
      val high = Option(m.group(3)).map(_.toInt)
      val low = Option(m.group(4)).map(_.toInt)
      val name = m.group(5)

      val width = (high, low) match {
        case (Some(h), Some(l)) => Some((h, l))
        case _                  => None
      }

      ports += Port(name, direction, width)
    }

    // Fallback: если нет скобок, парсить простой список портов
    if (ports.isEmpty) {
      val simplePortRegex = """(input|output|inout)\s+(\w+)""".r
      simplePortRegex.findAllMatchIn(portString).foreach { m =>
        val direction = m.group(1)
        val name = m.group(2)
        ports += Port(name, direction, None)
      }
    }

    ports.toSeq
  }

  /**
   * Парсить все элементы внутри модуля
   * @param moduleContent содержимое модуля
   * @return Seq[ModuleItem]
   */
  def parseModuleItems(moduleContent: String): Seq[ModuleItem] = {
    val items = mutable.ListBuffer[ModuleItem]()

    // Wire декларации
    parseWires(moduleContent).foreach(items += _)

    // Reg декларации
    parseRegs(moduleContent).foreach(items += _)

    // Parameters
    parseParameters(moduleContent).foreach(items += _)

    // Localparams
    parseLocalparams(moduleContent).foreach(items += _)

    // Assign statements
    parseAssigns(moduleContent).foreach(items += _)

    // Always блоки
    parseAlwaysBlocks(moduleContent).foreach(items += _)

    // Module instances
    parseModuleInstances(moduleContent).foreach(items += _)

    items.toSeq
  }

  /**
   * Парсить wire декларации
   * @return Seq[Wire]
   */
  def parseWires(code: String): Seq[Wire] = {
    val wireRegex = """wire\s+(signed\s+)?\[?(\d+)?:(\d+)?\]?\s*(\w+)\s*;""".r
    val wires = mutable.ListBuffer[Wire]()

    wireRegex.findAllMatchIn(code).foreach { m =>
      val isSigned = Option(m.group(1)).isDefined
      val high = Option(m.group(2)).map(_.toInt)
      val low = Option(m.group(3)).map(_.toInt)
      val name = m.group(4)

      val width = (high, low) match {
        case (Some(h), Some(l)) => Some((h, l))
        case _                  => None
      }

      wires += Wire(name, width, isSigned)
    }

    wires.toSeq
  }

  /**
   * Парсить reg декларации
   * @return Seq[Reg]
   */
  def parseRegs(code: String): Seq[Reg] = {
    val regRegex = """reg\s+(signed\s+)?\[?(\d+)?:(\d+)?\]?\s*(\w+)\s*;""".r
    val regs = mutable.ListBuffer[Reg]()

    regRegex.findAllMatchIn(code).foreach { m =>
      val isSigned = Option(m.group(1)).isDefined
      val high = Option(m.group(2)).map(_.toInt)
      val low = Option(m.group(3)).map(_.toInt)
      val name = m.group(4)

      val width = (high, low) match {
        case (Some(h), Some(l)) => Some((h, l))
        case _                  => None
      }

      regs += Reg(name, width, isSigned)
    }

    regs.toSeq
  }

  /**
   * Парсить parameter декларации
   * @return Seq[Parameter]
   */
  def parseParameters(code: String): Seq[Parameter] = {
    val paramRegex = """parameter\s+(\w+)\s*=\s*(.+?)\s*[,;]""".r
    val parameters = mutable.ListBuffer[Parameter]()

    paramRegex.findAllMatchIn(code).foreach { m =>
      val name = m.group(1)
      val value = m.group(2).trim
      parameters += Parameter(name, "int", Some(value))
    }

    parameters.toSeq
  }

  /**
   * Парсить localparam декларации
   * @return Seq[Localparam]
   */
  def parseLocalparams(code: String): Seq[Localparam] = {
    val localparamRegex = """localparam\s+(\w+)\s*=\s*(.+?)\s*[;]""".r
    val localparams = mutable.ListBuffer[Localparam]()

    localparamRegex.findAllMatchIn(code).foreach { m =>
      val name = m.group(1)
      val value = m.group(2).trim
      localparams += Localparam(name, value)
    }

    localparams.toSeq
  }

  /**
   * Парсить assign statements
   * @return Seq[Assign]
   */
  def parseAssigns(code: String): Seq[Assign] = {
    val assignRegex = """assign\s+([^=]+?)\s*=\s*(.+?)\s*;""".r
    val assigns = mutable.ListBuffer[Assign]()

    assignRegex.findAllMatchIn(code).foreach { m =>
      val lhs = m.group(1).trim
      val rhs = m.group(2).trim
      assigns += Assign(lhs, rhs)
    }

    assigns.toSeq
  }

  /**
   * Парсить always блоки
   * @return Seq[AlwaysBlock]
   */
  def parseAlwaysBlocks(code: String): Seq[AlwaysBlock] = {
    val alwaysRegex = """always\s*(@\([^)]+\)|@\(\*\))?\s*begin(.+?)end""".r
    val alwaysBlocks = mutable.ListBuffer[AlwaysBlock]()

    alwaysRegex.findAllMatchIn(code).foreach { m =>
      val trigger = Option(m.group(1)).getOrElse("@(*)")
      val content = m.group(2).trim
      val statements = content.split(";").map(_.trim).filter(_.nonEmpty).toSeq

      alwaysBlocks += AlwaysBlock(trigger, statements)
    }

    alwaysBlocks.toSeq
  }

  /**
   * Парсить module instances
   * @return Seq[ModuleInstance]
   */
  def parseModuleInstances(code: String): Seq[ModuleInstance] = {
    // Ищем pattern: ModuleName instanceName ( connections );
    val instanceRegex = """(\w+)\s+(\w+)\s*\((.*?)\)\s*;""".r
    val instances = mutable.ListBuffer[ModuleInstance]()

    instanceRegex.findAllMatchIn(code).foreach { m =>
      val moduleName = m.group(1)
      val instanceName = m.group(2)
      val connectionString = m.group(3)

      // Парсить подключения: .port(signal) или port(signal)
      val connectionRegex = """\.?(\w+)\s*\(\s*(\w+)\s*\)""".r
      val connections = mutable.Map[String, String]()

      connectionRegex.findAllMatchIn(connectionString).foreach { connMatch =>
        val portName = connMatch.group(1)
        val signalName = connMatch.group(2)
        connections(portName) = signalName
      }

      instances += ModuleInstance(moduleName, instanceName, connections.toMap)
    }

    instances.toSeq
  }

  /**
   * Получить все signed сигналы из модуля
   * @return Seq[Wire] и Seq[Reg], которые помечены как signed
   */
  def extractSignedSignals(): (Seq[Wire], Seq[Reg]) = {
    val module = parseModule()
    val signedWires = module.wires.filter(_.isSigned)
    val signedRegs = module.regs.filter(_.isSigned)
    (signedWires, signedRegs)
  }

  /**
   * Получить все процедурные присваивания (из always блоков)
   * @return Seq[(signalName, expression)]
   */
  def extractProceduralAssignments(): Seq[(String, String)] = {
    val module = parseModule()
    val assignments = mutable.ListBuffer[(String, String)]()

    module.alwaysBlocks.foreach { block =>
      block.statements.foreach { stmt =>
        // Ищем pattern: signal <= expression
        val assignRegex = """(\w+)\s*<=\s*(.+)""".r
        assignRegex.findFirstMatchIn(stmt).foreach { m =>
          val signal = m.group(1)
          val expr = m.group(2)
          assignments += ((signal, expr))
        }
      }
    }

    assignments.toSeq
  }

  /**
   * Получить combinational logic (assign statements)
   * @return Seq[(lhs, rhs)]
   */
  def extractCombinationalLogic(): Seq[(String, String)] = {
    val module = parseModule()
    module.assigns.map(a => (a.lhs, a.rhs))
  }

  /**
   * Получить sequential logic (always blocks с posedge/negedge)
   * @return Seq[AlwaysBlock]
   */
  def extractSequentialLogic(): Seq[AlwaysBlock] = {
    val module = parseModule()
    module.alwaysBlocks.filter { block =>
      block.trigger.contains("posedge") || block.trigger.contains("negedge")
    }
  }

  /**
   * Получить все module instances
   * @return Seq[ModuleInstance]
   */
  def extractModuleInstances(): Seq[ModuleInstance] = {
    parseModule().instances
  }

  /**
   * Получить все декларации сигналов (wire + reg)
   * @return Seq[(name, type, width, isSigned)]
   */
  def extractSignalDeclarations(): Seq[(String, String, Option[(Int, Int)], Boolean)] = {
    val module = parseModule()
    val signals = mutable.ListBuffer[(String, String, Option[(Int, Int)], Boolean)]()

    module.wires.foreach { w =>
      signals += ((w.name, "wire", w.width, w.isSigned))
    }

    module.regs.foreach { r =>
      signals += ((r.name, "reg", r.width, r.isSigned))
    }

    signals.toSeq
  }
}

object VerilogParser {
  def apply(verilogCode: String): VerilogParser = new VerilogParser(verilogCode)

  /**
   * Быстрый парс с получением только модуля
   */
  def parse(verilogCode: String): Module = {
    new VerilogParser(verilogCode).parseModule()
  }

  /**
   * Парс с экстрацией specific элементов
   */
  def parseSignals(verilogCode: String): Seq[(String, String, Option[(Int, Int)], Boolean)] = {
    new VerilogParser(verilogCode).extractSignalDeclarations()
  }

  def parseAssignments(verilogCode: String): Seq[(String, String)] = {
    new VerilogParser(verilogCode).extractCombinationalLogic()
  }

  def parseProceduralAssignments(verilogCode: String): Seq[(String, String)] = {
    new VerilogParser(verilogCode).extractProceduralAssignments()
  }

  def parseInstances(verilogCode: String): Seq[ModuleInstance] = {
    new VerilogParser(verilogCode).extractModuleInstances()
  }

  def parseSignedSignals(verilogCode: String): (Seq[Wire], Seq[Reg]) = {
    new VerilogParser(verilogCode).extractSignedSignals()
  }
}
