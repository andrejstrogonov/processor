package org.kiuru.processor

import org.yaml.snakeyaml.Yaml
import java.io.{File, FileInputStream}
import scala.beans.BeanProperty

case class MemoryConfig(
                         @BeanProperty name: String,
                         @BeanProperty depth: Int,
                         @BeanProperty width: Int,
                         @BeanProperty init_values: java.util.List[Integer]
                       )

case class AttributeRule(
                          @BeanProperty pattern: String,
                          @BeanProperty `type`: String,
                          @BeanProperty ram_style: Option[String] = None,
                          @BeanProperty value: Option[String] = None
                        )

case class DeviceConfig(
                         @BeanProperty family: String,
                         @BeanProperty part: String,
                         @BeanProperty top_module: String,
                         @BeanProperty clock_freq_mhz: Double,
                         @BeanProperty clock_signal: String
                       )

case class ProjectConfig(
                          @BeanProperty input_sv: String,
                          @BeanProperty output_sv: String,
                          @BeanProperty output_dir: String
                        )

case class AppConfig(
                      @BeanProperty device: DeviceConfig,
                      @BeanProperty attributes: Seq[AttributeRule],
                      @BeanProperty project: ProjectConfig,
                      @BeanProperty memories: Seq[MemoryConfig]
                    )

object ConfigLoader {
  def load(path: String): AppConfig = {
    val yaml = new Yaml()
    val input = new FileInputStream(new File(path))
    val reader = new java.io.InputStreamReader(input, "UTF-8")
    try {
      yaml.load(reader).asInstanceOf[AppConfig]
    } finally {
      reader.close()
      input.close()
    }
  }
}
