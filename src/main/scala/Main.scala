package org.kiuru.configurationalLogicBlock

import logger._
import sv2chisel.{Driver, Project, TranslationOptions}

object Main extends App {
  Logger.setLevel(LogLevel.Info)

  val basePath = "src/main/resources/project/hdl"
  val files = Seq(
    "project/hdl/sha.sv"
  )
  val project = Project("project", basePath, files)

  Driver.emitChisel(project, TranslationOptions(), "chisel_gen")


}