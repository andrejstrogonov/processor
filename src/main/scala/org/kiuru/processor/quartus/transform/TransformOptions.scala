package org.kiuru.processor.quartus.transform

/**
 * Параметры трансформации для Quartus совместимости
 *
 * @param verilogVersion версия Verilog (2001, 2005)
 * @param targetFamily целевое семейство FPGA (Cyclone, Stratix, и т.д.)
 * @param preserveComments сохранять ли исходные комментарии
 * @param addFixMarkers добавлять ли комментарии ## QUARTUS FIX
 */
case class TransformOptions(
    verilogVersion: String = "2005",
    targetFamily: String = "Cyclone",
    preserveComments: Boolean = true,
    addFixMarkers: Boolean = true
)
