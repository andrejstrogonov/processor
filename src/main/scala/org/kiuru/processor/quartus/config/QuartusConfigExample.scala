package org.kiuru.processor.quartus.config

import java.io.File
import org.kiuru.processor.quartus.generator.{QsfGenerator, SdcGenerator, ShellScriptGen}

object QuartusConfigExample {
  def main(args: Array[String]): Unit = {
    println("=== Quartus Configuration Parser & Generator Example ===\n")

    // Example 1: Parse Cyclone IV E configuration
    println("1. Parsing Cyclone IV E configuration...")
    val cyclone4Yaml = """
device:
  family: "Cyclone IV E"
  device: "EP4CE115F29C7"
  partNumber: "10M50DAF484C7G"
  clockFreqMHz: 50
  timingAnalysis: true

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

optimizationTechnique: "BALANCED"
powerEstimation: false
topModule: "top_module"
verilogFile: "processor.v"
"""

    val config = ConfigParser.parseYamlString(cyclone4Yaml)
    
    println(s"   Device: ${config.device.device}")
    println(s"   Family: ${config.device.family}")
    println(s"   Clock: ${config.device.clockFreqMHz} MHz")
    println(s"   Pins configured: ${config.pins.length}\n")

    // Example 2: Generate QSF file
    println("2. Generating Quartus Settings File (.qsf)...")
    val qsfContent = QsfGenerator.generate(config, "processor", "processor.v")
    println("   First 300 characters of QSF:")
    println("   " + qsfContent.take(300).replace("\n", "\n   ") + "\n")

    // Example 3: Generate SDC file
    println("3. Generating Synopsys Design Constraints (.sdc)...")
    val sdcContent = SdcGenerator.generate(config, Seq("clk"))
    println("   First 300 characters of SDC:")
    println("   " + sdcContent.take(300).replace("\n", "\n   ") + "\n")

    // Example 4: Generate TCL script
    println("4. Generating Quartus Shell TCL script...")
    val tclContent = ShellScriptGen.generate("processor", "processor.v")
    println("   First 300 characters of TCL:")
    println("   " + tclContent.take(300).replace("\n", "\n   ") + "\n")

    // Example 5: Generate files to disk
    println("5. Saving configuration files to disk...")
    val outputDir = "quartus_output"
    new File(outputDir).mkdirs()
    
    QsfGenerator.saveToFile(qsfContent, s"$outputDir/processor.qsf")
    println(s"   ✓ Saved to $outputDir/processor.qsf")
    
    SdcGenerator.saveToFile(sdcContent, s"$outputDir/processor.sdc")
    println(s"   ✓ Saved to $outputDir/processor.sdc")
    
    ShellScriptGen.saveToFile(tclContent, s"$outputDir/build.tcl")
    println(s"   ✓ Saved to $outputDir/build.tcl")

    // Example 6: Display pin configuration
    println("\n6. Pin Configuration Summary:")
    config.pins.foreach { pin =>
      val io = pin.ioStandard.getOrElse("default")
      val strength = pin.currentStrength.map(s => s", $s").getOrElse("")
      val pullup = if (pin.pullUp.getOrElse(false)) ", PULL-UP" else ""
      println(f"   ${pin.name}%-12s -> Pin ${pin.pin}%-5s [$io$strength$pullup]")
    }

    println("\n=== Configuration Generation Complete ===")
  }
}
