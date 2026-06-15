package org.kiuru.processor

import java.nio.file.{Files, Paths}
import java.io.{File, PrintWriter}
import scala.jdk.CollectionConverters._

object QuartusProjectGenerator {

  def generateQsf(cfg: AppConfig): Unit = {
    val qsf = new StringBuilder
    qsf ++= s"set_global_assignment -name FAMILY \"${cfg.device.family}\"\n"
    qsf ++= s"set_global_assignment -name DEVICE ${cfg.device.part}\n"
    qsf ++= s"set_global_assignment -name TOP_LEVEL_ENTITY ${cfg.device.top_module}\n"
    qsf ++= s"set_global_assignment -name VERILOG_FILE \"${cfg.project.output_sv}\"\n"

    // Добавляем атрибуты памяти через QSF (это надежнее, чем в SV)
    cfg.memories.foreach { m =>
      qsf ++= s"set_global_assignment -name RAM_STYLE \"M9K\" -to ${m.name}\n"
      qsf ++= s"set_instance_assignment -name INIT_FILE \"${m.name}.mif\" -to ${m.name}\n"
    }

    Files.write(Paths.get(cfg.project.output_dir, s"${cfg.device.top_module}.qsf"), qsf.toString.getBytes)
  }

  def generateSdc(cfg: AppConfig): Unit = {
    val periodNs = 1000.0 / cfg.device.clock_freq_mhz
    val sdc = s"""create_clock -period ${periodNs} [get_clocks {${cfg.device.clock_signal}}]
                 |set_clock_uncertainty 0.5 [get_clocks [get_clocks]]
                 |""".stripMargin
    Files.write(Paths.get(cfg.project.output_dir, s"${cfg.device.top_module}.sdc"), sdc.getBytes)
  }

  def generateMif(cfg: AppConfig): Unit = {
    cfg.memories.foreach { m =>
      val path = Paths.get(cfg.project.output_dir, s"${m.name}.mif").toString
      val pw = new PrintWriter(new File(path))
      try {
        pw.println(s"DEPTH = ${m.depth};")
        pw.println(s"WIDTH = ${m.width};")
        pw.println("ADDRESS_RADIX = HEX;")
        pw.println("DATA_RADIX = HEX;")
        pw.println("CONTENT BEGIN")
        m.init_values.asScala.zipWithIndex.foreach { case (v, i) =>
          val intValue = v.intValue()
          val mask = (1 << m.width) - 1
          val hexWidth = (m.width + 3) / 4
          val hexValue = intValue & mask
          val hexStr = String.format("%0" + hexWidth + "X", hexValue)
          pw.println(s"  $i : $hexStr;")
        }
        pw.println("END;")
      } finally pw.close()
    }
  }
}

