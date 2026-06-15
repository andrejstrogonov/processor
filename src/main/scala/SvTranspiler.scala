package org.kiuru.processor

import java.nio.file.{Files, Paths}
import scala.io.Source

object SvTranspiler {
  def transpile(inputPath: String, outputPath: String, rules: Seq[AttributeRule]): Unit = {
    val lines = Source.fromFile(inputPath).getLines().toList
    val result = lines.map { line =>
      rules.find(r => line.contains(r.pattern)) match {
        case Some(rule) =>
          rule.`type` match {
            case "ram" =>
              val style = rule.ram_style.getOrElse("M9K")
              s"(* ramstyle = \"$style\" *) $line"
            case "keep" =>
              s"(* keep = \"true\" *) $line"
            case _ => line
          }
        case None => line
      }
    }
    Files.write(Paths.get(outputPath), result.mkString("\n").getBytes)
  }
}

