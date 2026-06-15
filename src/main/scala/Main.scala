package org.kiuru.processor

import javafx.application.Application
import javafx.scene.{Scene, control}
import javafx.scene.control.{Button, Label, TextArea}
import javafx.scene.layout.VBox
import javafx.stage.Stage

object Main extends Application {
  override def start(primaryStage: Stage): Unit = {
    val logArea = new TextArea()
    logArea.setEditable(false)
    logArea.setPrefHeight(300)

    val btn = new Button("Подготовить проект для Quartus II 13.1")

    btn.setOnAction(_ => {
      logArea.appendText("Загрузка конфигурации...\n")
      try {
        val config = ConfigLoader.load("src/main/resources/config.yaml")

        logArea.appendText(s"Чтение SV: ${config.project.input_sv}\n")
        SvTranspiler.transpile(config.project.input_sv, config.project.output_sv, config.attributes)
        logArea.appendText("SV код адаптирован (добавлены атрибуты).\n")

        logArea.appendText("Генерация файлов проекта Quartus...\n")
        QuartusProjectGenerator.generateQsf(config)
        QuartusProjectGenerator.generateSdc(config)
        QuartusProjectGenerator.generateMif(config)

        logArea.appendText("Готово!\n")
        logArea.appendText(s"Запустите в терминале:\nquartus_sh --flow compile ${config.project.output_dir}/${config.device.top_module}\n")
      } catch {
        case e: Exception => logArea.appendText(s"Ошибка: ${e.getMessage}\n")
      }
    })

    val layout = new VBox(10, new Label("Инструмент подготовки проекта Quartus (Chisel Edition)"), btn, logArea)
    primaryStage.setScene(new Scene(layout, 600, 450))
    primaryStage.setTitle("Quartus Chisel Tool v1.0")
    primaryStage.show()
  }
}
