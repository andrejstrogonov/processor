package org.kiuru.processor.quartus.transform

import org.kiuru.processor.quartus.ast._

/**
 * Примеры использования QuartusTransformer
 *
 * Этот объект содержит примеры того, как использовать различные трансформации
 * для исправления Verilog кода для совместимости с Quartus II 13.1.
 */
object TransformExamples {

  /**
   * Пример 1: Исправление invalid module name (Error 10170)
   *
   * Quartus Error: ERROR 10170: "1" is not a valid module name
   */
  def example1_InvalidModuleName(): Unit = {
    val module = Module(
      name = "1",  // Invalid: только цифры!
      ports = Seq(Port("clk", "input", None)),
      items = Seq(Reg("counter", Some((31, 0)), isSigned = false))
    )

    val transformer = new QuartusTransformer()
    val transformed = transformer.transformModule(module)

    println("=== Example 1: Invalid Module Name ===")
    println(s"Before:  module ${module.name}")
    println(s"After:   module ${transformed.name}")
    println()
  }

  /**
   * Пример 2: Исправление wire assignment в always блоке (Error 10137)
   *
   * Quartus Error: ERROR 10137: wire is not procedural
   */
  def example2_WireInAlways(): Unit = {
    val wire = Wire("result", Some((31, 0)), isSigned = false)
    val alwaysBlock = AlwaysBlock(
      trigger = "@(posedge clk)",
      statements = Seq("result <= input1 + input2;")
    )

    val module = Module("adder", Seq(), Seq(wire, alwaysBlock))

    val transformer = new QuartusTransformer()
    val transformed = transformer.transformModule(module)

    println("=== Example 2: Wire in Always Block ===")
    println(s"Before: ${module.items(0).asInstanceOf[Wire].nodeType}")
    println(s"After:  ${transformed.items(0).asInstanceOf[Reg].nodeType}")
    println()
  }

  /**
   * Пример 3: Исправление signed типов
   *
   * Quartus может требовать переформатирования signed объявлений
   */
  def example3_SignedTypes(): Unit = {
    val signedWire = Wire("data", Some((31, 0)), isSigned = true)
    val signedReg = Reg("counter", Some((31, 0)), isSigned = true)

    val module = Module("signed_module", Seq(), Seq(signedWire, signedReg))

    val transformer = new QuartusTransformer()
    val transformed = transformer.transformModule(module)

    println("=== Example 3: Signed Types ===")
    val tWire = transformed.items(0).asInstanceOf[Wire]
    val tReg = transformed.items(1).asInstanceOf[Reg]

    println(s"Before: wire signed [31:0] data")
    println(s"After:  wire ${tWire.widthString} ${tWire.name}")
    println()
    println(s"Before: reg signed [31:0] counter")
    println(s"After:  reg ${tReg.widthString} ${tReg.name}")
    println()
  }

  /**
   * Пример 4: Удаление дубликатов модулей
   *
   * Quartus не позволяет иметь несколько определений одного модуля
   */
  def example4_RemoveDuplicates(): Unit = {
    val modules = Seq(
      Module("counter", Seq(), Seq(Reg("count", Some((7, 0)), false))),
      Module("adder", Seq(), Seq(Wire("sum", Some((15, 0)), false))),
      Module("counter", Seq(), Seq(Reg("count", Some((7, 0)), false)))  // Duplicate!
    )

    val transformer = new QuartusTransformer()
    val transformed = transformer.transformModules(modules)

    println("=== Example 4: Remove Duplicates ===")
    println(s"Before: ${modules.length} modules")
    println(s"After:  ${transformed.length} modules")
    println(s"Modules: ${transformed.map(_.name).mkString(", ")}")
    println()
  }

  /**
   * Пример 5: Исправление имен иерархии
   *
   * Quartus требует валидные идентификаторы для всех имен модулей
   */
  def example5_HierarchyNames(): Unit = {
    val modules = Seq(
      Module("1", Seq(), Seq()),           // Только цифры
      Module("2xyz", Seq(), Seq()),        // Начинается с цифры
      Module("", Seq(), Seq()),            // Пустое имя
      Module("valid_name", Seq(), Seq())   // Уже валидное
    )

    val transformer = new QuartusTransformer()
    val transformed = transformer.transformModules(modules)

    println("=== Example 5: Hierarchy Names ===")
    println("Transformations:")
    for ((before, after) <- modules.zip(transformed)) {
      println(s"  '${before.name}' -> '${after.name}'")
    }
    println()
  }

  /**
   * Пример 6: Complex hierarchy with module references
   *
   * Все ссылки обновляются при переименовании модулей
   */
  def example6_HierarchyReferences(): Unit = {
    val instance1 = ModuleInstance("1", "inst1", Map("clk" -> "clk"))
    val instance2 = ModuleInstance("2xyz", "inst2", Map("data_in" -> "data"))

    val modules = Seq(
      Module("1", Seq(), Seq(instance1)),
      Module("2xyz", Seq(), Seq()),
      Module("top", Seq(), Seq(instance2))
    )

    val transformer = new QuartusTransformer()
    val transformed = transformer.transformModules(modules)

    println("=== Example 6: Hierarchy References ===")
    val topModule = transformed.find(_.name == "top").get
    val inst = topModule.items.head.asInstanceOf[ModuleInstance]
    println(s"Module 'top' now references: ${inst.moduleName}")
    println()
  }

  /**
   * Пример 7: Интеграционный тест - все трансформации вместе
   *
   * Все Fix'ы применяются в правильном порядке
   */
  def example7_IntegrationTest(): Unit = {
    val modules = Seq(
      Module(
        "1",
        Seq(),
        Seq(
          Wire("data_out", Some((31, 0)), isSigned = true),
          AlwaysBlock("@(posedge clk)", Seq("data_out <= input1 + input2;"))
        )
      ),
      Module("1", Seq(), Seq()),  // Duplicate!
      Module(
        "processing",
        Seq(),
        Seq(
          ModuleInstance("1", "proc_inst", Map("data" -> "proc_data"))
        )
      )
    )

    val transformer = new QuartusTransformer()
    val transformed = transformer.transformModules(modules)

    println("=== Example 7: Integration Test ===")
    println(s"Input modules: ${modules.length}")
    println(f"Output modules: ${transformed.length}")
    println()
    println("Summary:")
    for (module <- transformed) {
      println(f"  Module: ${module.name}")
      println(f"    Items: ${module.items.length}")
      for (item <- module.items) {
        println(f"      - ${item.nodeType}")
      }
    }
    println()
    println("Transformation log:")
    for (log <- transformer.getTransformLog) {
      println(f"  $log")
    }
    println()
  }

  /**
   * Пример 8: Использование VerilogToQuartus утилиты
   */
  def example8_VerilogToQuartusTool(): Unit = {
    val options = TransformOptions(
      verilogVersion = "2005",
      targetFamily = "Cyclone",
      addFixMarkers = true
    )

    println("=== Example 8: VerilogToQuartus Tool ===")
    println("Transform Summary:")
    println(VerilogToQuartus.getTransformSummary(options))
    println()
  }

  /**
   * Пример 9: Применение отдельных Fix'ов
   */
  def example9_IndividualFixes(): Unit = {
    val module = Module("1", Seq(), Seq(Wire("data", Some((7, 0)), isSigned = true)))

    // Применить только HierarchyFix
    val hierarchyFix = new HierarchyFix()
    val step1 = hierarchyFix.apply(Seq(module))

    // Применить только SignedTypeFix
    val signedFix = new SignedTypeFix()
    val step2 = signedFix.apply(step1)

    println("=== Example 9: Individual Fixes ===")
    println(s"Original:        module ${module.name}")
    println(s"After Hierarchy: module ${step1.head.name}")
    println(s"After Signed:    module ${step2.head.name}")
    println()
  }

  /**
   * Запустить все примеры
   */
  def runAll(): Unit = {
    println("╔════════════════════════════════════════════════════════╗")
    println("║  Quartus II 13.1 Transform Examples                    ║")
    println("╚════════════════════════════════════════════════════════╝")
    println()

    example1_InvalidModuleName()
    example2_WireInAlways()
    example3_SignedTypes()
    example4_RemoveDuplicates()
    example5_HierarchyNames()
    example6_HierarchyReferences()
    example7_IntegrationTest()
    example8_VerilogToQuartusTool()
    example9_IndividualFixes()

    println("╔════════════════════════════════════════════════════════╗")
    println("║  All examples completed successfully!                  ║")
    println("╚════════════════════════════════════════════════════════╝")
  }
}
