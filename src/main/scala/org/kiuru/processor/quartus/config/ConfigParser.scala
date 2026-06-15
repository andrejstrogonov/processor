package org.kiuru.processor.quartus.config

import org.yaml.snakeyaml.Yaml
import scala.jdk.CollectionConverters._
import scala.io.Source

object ConfigParser {

  def parseYaml(filePath: String): FPGAConfig = {
    val source = Source.fromFile(filePath, "UTF-8")
    try {
      val content = source.mkString
      parseYamlString(content)
    } finally {
      source.close()
    }
  }

  def parseYamlString(content: String): FPGAConfig = {
    val yaml = new Yaml()
    val data = yaml.load(content).asInstanceOf[java.util.Map[String, Any]]

    val deviceMap = data.get("device").asInstanceOf[java.util.Map[String, Any]]
    val device = parseDevice(deviceMap)

    val pinsList = data.get("pins").asInstanceOf[java.util.List[Any]]
    val pins = parsePins(pinsList)

    val optimizationTechnique = 
      Option(data.get("optimizationTechnique"))
        .map(_.toString)
        .getOrElse("BALANCED")

    val powerEstimation = 
      Option(data.get("powerEstimation"))
        .map(_.asInstanceOf[Boolean])
        .getOrElse(false)

    val miscFiles = 
      Option(data.get("miscFiles"))
        .map(_.asInstanceOf[java.util.List[String]].asScala.toSeq)
        .getOrElse(Seq())

    val topModule = 
      Option(data.get("topModule"))
        .map(_.toString)
        .getOrElse("top_module")

    val verilogFile = 
      Option(data.get("verilogFile"))
        .map(_.toString)
        .getOrElse("project.v")

    FPGAConfig(
      device = device,
      pins = pins,
      optimizationTechnique = optimizationTechnique,
      powerEstimation = powerEstimation,
      miscFiles = miscFiles,
      topModule = topModule,
      verilogFile = verilogFile
    )
  }

  private def parseDevice(deviceMap: java.util.Map[String, Any]): FPGADevice = {
    val family = deviceMap.get("family").asInstanceOf[String]
    val device = deviceMap.get("device").asInstanceOf[String]
    val partNumber = 
      Option(deviceMap.get("partNumber")).map(_.asInstanceOf[String])
    val clockFreqMHz = deviceMap.get("clockFreqMHz").asInstanceOf[Int]
    val timingAnalysis = 
      Option(deviceMap.get("timingAnalysis"))
        .map(_.asInstanceOf[Boolean])
        .getOrElse(true)
    val hardwareVerification = 
      Option(deviceMap.get("hardwareVerification"))
        .map(_.asInstanceOf[Boolean])
        .getOrElse(false)

    FPGADevice(
      family = family,
      device = device,
      partNumber = partNumber,
      clockFreqMHz = clockFreqMHz,
      timingAnalysis = timingAnalysis,
      hardwareVerification = hardwareVerification
    )
  }

  private def parsePins(pinsList: java.util.List[Any]): Seq[PinAssignment] = {
    pinsList.asScala.map { pinObj =>
      val pinMap = pinObj.asInstanceOf[java.util.Map[String, Any]]
      val name = pinMap.get("name").asInstanceOf[String]
      val pin = pinMap.get("pin").asInstanceOf[String]
      val ioStandard = Option(pinMap.get("ioStandard")).map(_.asInstanceOf[String])
      val currentStrength = Option(pinMap.get("currentStrength")).map(_.asInstanceOf[String])
      val pullUp = Option(pinMap.get("pullUp")).map(_.asInstanceOf[Boolean])

      PinAssignment(
        name = name,
        pin = pin,
        ioStandard = ioStandard,
        currentStrength = currentStrength,
        pullUp = pullUp
      )
    }.toSeq
  }
}
