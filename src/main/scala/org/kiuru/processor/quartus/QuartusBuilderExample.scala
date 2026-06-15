package org.kiuru.processor.quartus

object QuartusBuilderExample extends App {
  println("\n╔════════════════════════════════════════════════════════════╗")
  println("║     Quartus II 13.1 Configuration Builder                ║")
  println("║     Powered by Quartus Config System for Scala           ║")
  println("╚════════════════════════════════════════════════════════════╝\n")

  // Example 1: Build from YAML string
  println("[1] Building from Cyclone IV E configuration...\n")
  
  val cyclone4Config = """
device:
  family: "Cyclone IV E"
  device: "EP4CE115F29C7"
  partNumber: "10M50DAF484C7G"
  clockFreqMHz: 50
  timingAnalysis: true
  hardwareVerification: false

pins:
  - name: "clk"
    pin: "AF14"
    ioStandard: "3.3-V LVCMOS"
    currentStrength: "8MA"
  - name: "reset"
    pin: "J2"
    ioStandard: "3.3-V LVCMOS"
    pullUp: true
  - name: "data_in"
    pin: "K1"
    ioStandard: "3.3-V LVCMOS"
  - name: "data_out"
    pin: "L1"
    ioStandard: "3.3-V LVCMOS"
    currentStrength: "12MA"
  - name: "enable"
    pin: "M1"
    ioStandard: "3.3-V LVCMOS"
  - name: "status_led"
    pin: "N1"
    ioStandard: "3.3-V LVCMOS"
    currentStrength: "12MA"

optimizationTechnique: "BALANCED"
powerEstimation: false
topModule: "processor_top"
verilogFile: "processor.v"
"""

  try {
    // Build project
    val project = QuartusProjectBuilder.buildFromYaml(
      cyclone4Config,
      "processor",
      "quartus_build"
    )

    // Print summary
    project.printSummary()

    // Example 2: Access generated files
    println("\n[2] Generated Files Summary:\n")
    println(s"QSF file size:  ${project.qsfContent.length} bytes")
    println(s"SDC file size:  ${project.sdcContent.length} bytes")
    println(s"TCL file size:  ${project.tclContent.length} bytes")
    println(s"MIF file size:  ${project.mifContent.length} bytes")
    println(s"Total:          ${project.getTotalSize} bytes\n")

    // Example 3: Show first few lines of each file
    println("[3] File Content Samples:\n")

    println("QSF (first 200 chars):")
    println("─" * 65)
    println(project.qsfContent.take(200).replace("\n", "\n"))
    println("─" * 65)
    println()

    println("SDC (first 200 chars):")
    println("─" * 65)
    println(project.sdcContent.take(200).replace("\n", "\n"))
    println("─" * 65)
    println()

    println("TCL (first 200 chars):")
    println("─" * 65)
    println(project.tclContent.take(200).replace("\n", "\n"))
    println("─" * 65)
    println()

    // Example 4: Save files
    println("\n[4] Saving files to disk...\n")
    try {
      val outputDir = project.save()
      println(s"✓ Files saved to: $outputDir")
      
      import java.io.File
      val files = new File(outputDir).listFiles().filter(_.isFile)
      files.foreach { f =>
        println(s"  • ${f.getName} (${f.length} bytes)")
      }
    } catch {
      case e: Exception =>
        println(s"⚠ Could not save files: ${e.getMessage}")
        println("  (This may be expected in restricted environments)")
    }

    println("\n[5] Configuration Details:\n")
    println(s"Device Family:        ${project.config.device.family}")
    println(s"Device Model:         ${project.config.device.device}")
    println(s"Part Number:          ${project.config.device.partNumber.getOrElse("N/A")}")
    println(s"Clock Frequency:      ${project.config.device.clockFreqMHz} MHz")
    println(s"Timing Analysis:      ${project.config.device.timingAnalysis}")
    println(s"Hardware Verification: ${project.config.device.hardwareVerification}")
    println(s"Optimization:         ${project.config.optimizationTechnique}")
    println(s"Power Estimation:     ${project.config.powerEstimation}")
    println(s"Total Pins:           ${project.config.pins.length}")

    println("\n[6] Project ready!\n")
    println("To build in Quartus II 13.1:")
    println("  1. quartus_sh -t quartus_build/build.tcl")
    println("  2. Open the generated .pof file in Quartus Programmer")
    println()

  } catch {
    case e: Exception =>
      println(s"Error: ${e.getMessage}")
      e.printStackTrace()
  }
}
