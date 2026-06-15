package org.kiuru.processor.quartus

import org.kiuru.processor.quartus.config.{ConfigParser, FPGAConfig}
import org.kiuru.processor.quartus.generator.{QsfGenerator, SdcGenerator, ShellScriptGen, MifGenerator}
import java.io.File

/**
 * Unified Quartus configuration and generation system.
 * Provides a simple API for parsing FPGA configurations and generating Quartus II 13.1 project files.
 */
object QuartusProjectBuilder {

  /**
   * Build a complete Quartus project from a configuration.
   * 
   * @param configFile Path to YAML configuration file
   * @param projectName Name for the Quartus project
   * @param outputDir Output directory for generated files
   * @return Tuple of (qsf, sdc, tcl, config) content and configuration object
   */
  def buildFromFile(
    configFile: String,
    projectName: String,
    outputDir: String = "quartus_build"
  ): QuartusProject = {
    val config = ConfigParser.parseYaml(configFile)
    build(config, projectName, outputDir)
  }

  /**
   * Build a complete Quartus project from YAML string.
   * 
   * @param yamlContent YAML configuration content
   * @param projectName Name for the Quartus project
   * @param outputDir Output directory for generated files
   * @return QuartusProject with all generated files
   */
  def buildFromYaml(
    yamlContent: String,
    projectName: String,
    outputDir: String = "quartus_build"
  ): QuartusProject = {
    val config = ConfigParser.parseYamlString(yamlContent)
    build(config, projectName, outputDir)
  }

  /**
   * Build from parsed configuration object.
   * 
   * @param config FPGAConfig object
   * @param projectName Name for the Quartus project
   * @param outputDir Output directory for generated files
   * @return QuartusProject with all generated files
   */
  def build(
    config: FPGAConfig,
    projectName: String,
    outputDir: String = "quartus_build"
  ): QuartusProject = {
    val qsf = QsfGenerator.generate(config, projectName, config.verilogFile)
    val sdc = SdcGenerator.generate(config, Seq("clk"))
    val tcl = ShellScriptGen.generate(projectName, config.verilogFile)
    val mif = MifGenerator.generate(1024, 32, Seq())

    val project = QuartusProject(
      projectName = projectName,
      config = config,
      qsfContent = qsf,
      sdcContent = sdc,
      tclContent = tcl,
      mifContent = mif,
      outputDirectory = outputDir
    )

    project
  }

  /**
   * Save all project files to disk.
   * 
   * @param project QuartusProject to save
   * @return Path to output directory
   */
  def saveProject(project: QuartusProject): String = {
    new File(project.outputDirectory).mkdirs()

    val qsfPath = s"${project.outputDirectory}/${project.projectName}.qsf"
    val sdcPath = s"${project.outputDirectory}/${project.projectName}.sdc"
    val tclPath = s"${project.outputDirectory}/build.tcl"
    val mifPath = s"${project.outputDirectory}/memory.mif"

    QsfGenerator.saveToFile(project.qsfContent, qsfPath)
    SdcGenerator.saveToFile(project.sdcContent, sdcPath)
    ShellScriptGen.saveToFile(project.tclContent, tclPath)
    MifGenerator.saveToFile(project.mifContent, mifPath)

    project.outputDirectory
  }

  /**
   * Print project summary to console.
   * 
   * @param project QuartusProject to summarize
   */
  def printSummary(project: QuartusProject): Unit = {
    println("╔" + "═" * 60 + "╗")
    println("║ Quartus II 13.1 Project Configuration".padTo(61, ' ') + "║")
    println("╚" + "═" * 60 + "╝")
    println()
    println(s"Project Name:     ${project.projectName}")
    println(s"Device:           ${project.config.device.device} (${project.config.device.family})")
    println(s"Clock Frequency:  ${project.config.device.clockFreqMHz} MHz")
    println(s"Pins Configured:  ${project.config.pins.length}")
    println(s"Optimization:     ${project.config.optimizationTechnique}")
    println(s"Power Estimation: ${project.config.powerEstimation}")
    println(s"Output Directory: ${project.outputDirectory}")
    println()
    println("Pin Assignments:")
    println("─" * 65)
    project.config.pins.foreach { pin =>
      val io = pin.ioStandard.getOrElse("default").padTo(18, ' ')
      val strength = pin.currentStrength.map(s => f", $s").getOrElse("").padTo(6, ' ')
      val pullup = if (pin.pullUp.getOrElse(false)) ", PU" else ""
      println(f"  ${pin.name}%-12s -> ${pin.pin}%-6s [$io$strength$pullup]")
    }
    println("─" * 65)
    println()
    println("Generated Files:")
    println(s"  • ${project.projectName}.qsf  (${project.qsfContent.length} bytes)")
    println(s"  • ${project.projectName}.sdc  (${project.sdcContent.length} bytes)")
    println(s"  • build.tcl         (${project.tclContent.length} bytes)")
    println(s"  • memory.mif        (${project.mifContent.length} bytes)")
    println()
  }
}

/**
 * Represents a complete Quartus project with all generated content.
 */
case class QuartusProject(
  projectName: String,
  config: FPGAConfig,
  qsfContent: String,
  sdcContent: String,
  tclContent: String,
  mifContent: String,
  outputDirectory: String = "quartus_build"
) {
  
  /**
   * Save all project files to disk.
   * 
   * @return Path to output directory
   */
  def save(): String = QuartusProjectBuilder.saveProject(this)

  /**
   * Print project summary to console.
   */
  def printSummary(): Unit = QuartusProjectBuilder.printSummary(this)

  /**
   * Get QSF file content.
   */
  def getQsfFile: String = qsfContent

  /**
   * Get SDC file content.
   */
  def getSdcFile: String = sdcContent

  /**
   * Get TCL script content.
   */
  def getTclFile: String = tclContent

  /**
   * Get MIF file content.
   */
  def getMifFile: String = mifContent

  /**
   * Get total generated file size in bytes.
   */
  def getTotalSize: Long = 
    qsfContent.length + sdcContent.length + tclContent.length + mifContent.length
}
