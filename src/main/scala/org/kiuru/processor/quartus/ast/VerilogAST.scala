package org.kiuru.processor.quartus.ast

/**
 * Abstract Syntax Tree для Verilog модулей.
 * Содержит hierarchical representation Verilog кода для парсинга и трансформации.
 */

sealed trait VerilogNode {
  def nodeType: String
}

/**
 * Основной узел - модуль Verilog
 * @param name имя модуля
 * @param ports список портов модуля
 * @param items элементы внутри модуля (wire, reg, assign, always, инстанции)
 */
case class Module(
    name: String,
    ports: Seq[Port],
    items: Seq[ModuleItem]
) extends VerilogNode {
  override def nodeType: String = "Module"

  def wires: Seq[Wire] =
    items.collect { case w: Wire => w }

  def regs: Seq[Reg] =
    items.collect { case r: Reg => r }

  def assigns: Seq[Assign] =
    items.collect { case a: Assign => a }

  def alwaysBlocks: Seq[AlwaysBlock] =
    items.collect { case ab: AlwaysBlock => ab }

  def instances: Seq[ModuleInstance] =
    items.collect { case mi: ModuleInstance => mi }

  def signedSignals: Seq[String] =
    (wires.filter(_.isSigned).map(_.name) ++
      regs.filter(_.isSigned).map(_.name)).distinct
}

/**
 * Порт модуля
 * @param name имя порта
 * @param direction направление: "input", "output", "inout"
 * @param width опциональная ширина [high:low]
 */
case class Port(
    name: String,
    direction: String,
    width: Option[(Int, Int)]
) extends VerilogNode {
  override def nodeType: String = "Port"

  def widthString: String = width match {
    case Some((high, low)) => s"[$high:$low]"
    case None              => ""
  }
}

sealed trait ModuleItem extends VerilogNode

/**
 * Wire декларация
 * @param name имя провода
 * @param width опциональная ширина [high:low]
 * @param isSigned флаг signed
 */
case class Wire(
    name: String,
    width: Option[(Int, Int)],
    isSigned: Boolean
) extends ModuleItem {
  override def nodeType: String = "Wire"

  def widthString: String = width match {
    case Some((high, low)) => s"[$high:$low]"
    case None              => ""
  }

  def declaration: String = {
    val signedStr = if (isSigned) "signed " else ""
    s"wire $signedStr$widthString $name;"
  }
}

/**
 * Reg декларация
 * @param name имя регистра
 * @param width опциональная ширина [high:low]
 * @param isSigned флаг signed
 */
case class Reg(
    name: String,
    width: Option[(Int, Int)],
    isSigned: Boolean
) extends ModuleItem {
  override def nodeType: String = "Reg"

  def widthString: String = width match {
    case Some((high, low)) => s"[$high:$low]"
    case None              => ""
  }

  def declaration: String = {
    val signedStr = if (isSigned) "signed " else ""
    s"reg $signedStr$widthString $name;"
  }
}

/**
 * Assign декларация
 * @param lhs левая сторона (target)
 * @param rhs правая сторона (expression)
 */
case class Assign(
    lhs: String,
    rhs: String
) extends ModuleItem {
  override def nodeType: String = "Assign"

  def statement: String = s"assign $lhs = $rhs;"
}

/**
 * Always блок
 * @param trigger триггер: "@(posedge clk)", "@(*)", и т.д.
 * @param statements список statements внутри блока
 */
case class AlwaysBlock(
    trigger: String,
    statements: Seq[String]
) extends ModuleItem {
  override def nodeType: String = "AlwaysBlock"

  def isProcedural: Boolean =
    !trigger.contains("@(*)") && !trigger.contains("posedge") && !trigger.contains("negedge")
}

/**
 * Module Instance - использование другого модуля
 * @param moduleName имя используемого модуля
 * @param instanceName имя инстанции
 * @param connections маппинг портов: port_name -> signal_name
 */
case class ModuleInstance(
    moduleName: String,
    instanceName: String,
    connections: Map[String, String]
) extends ModuleItem {
  override def nodeType: String = "ModuleInstance"

  def connectionString: String =
    connections
      .map { case (port, signal) => s".$port($signal)" }
      .mkString(", ")
}

/**
 * Parameter декларация
 * @param name имя параметра
 * @param valueType тип значения (int, string, и т.д.)
 * @param defaultValue опциональное значение по умолчанию
 */
case class Parameter(
    name: String,
    valueType: String,
    defaultValue: Option[String]
) extends ModuleItem {
  override def nodeType: String = "Parameter"
}

/**
 * Internal Localparam декларация
 * @param name имя локального параметра
 * @param value значение
 */
case class Localparam(
    name: String,
    value: String
) extends ModuleItem {
  override def nodeType: String = "Localparam"
}

/**
 * If statement внутри процедурального блока
 */
case class IfStatement(
    condition: String,
    thenBranch: Seq[String],
    elseBranch: Option[Seq[String]]
) extends VerilogNode {
  override def nodeType: String = "IfStatement"
}

/**
 * Case statement внутри процедурального блока
 */
case class CaseStatement(
    expr: String,
    cases: Seq[(String, Seq[String])], // condition -> statements
    default: Option[Seq[String]]
) extends VerilogNode {
  override def nodeType: String = "CaseStatement"
}
