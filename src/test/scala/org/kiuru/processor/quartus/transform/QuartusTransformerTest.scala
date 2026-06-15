package org.kiuru.processor.quartus.transform

import org.kiuru.processor.quartus.ast._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Тесты для Quartus II 13.1 трансформаций.
 *
 * Включает:
 * - Тесты каждого Fix'а отдельно
 * - Интеграционный тест всех трансформаций вместе
 * - Проверка корректности выходного Verilog
 */
class QuartusTransformerTest extends AnyFlatSpec with Matchers {

  val options = TransformOptions(verilogVersion = "2005", addFixMarkers = true)

  // ============ Tests for SignedTypeFix ============

  "SignedTypeFix" should "remove signed flag from wire" in {
    val wire = Wire("data", Some((7, 0)), isSigned = true)
    val module = Module("test", Seq(), Seq(wire))

    val fix = new SignedTypeFix(options)
    val result = fix.apply(Seq(module)).head

    val transformedWire = result.items.head.asInstanceOf[Wire]
    transformedWire.isSigned shouldBe false
    transformedWire.name shouldBe "data"
  }

  "SignedTypeFix" should "remove signed flag from reg" in {
    val reg = Reg("count", Some((31, 0)), isSigned = true)
    val module = Module("test", Seq(), Seq(reg))

    val fix = new SignedTypeFix(options)
    val result = fix.apply(Seq(module)).head

    val transformedReg = result.items.head.asInstanceOf[Reg]
    transformedReg.isSigned shouldBe false
    transformedReg.name shouldBe "count"
  }

  "SignedTypeFix" should "preserve unsigned wires" in {
    val wire = Wire("data", Some((7, 0)), isSigned = false)
    val module = Module("test", Seq(), Seq(wire))

    val fix = new SignedTypeFix(options)
    val result = fix.apply(Seq(module)).head

    val transformedWire = result.items.head.asInstanceOf[Wire]
    transformedWire.isSigned shouldBe false
  }

  // ============ Tests for ModuleDupFix ============

  "ModuleDupFix" should "remove duplicate module definitions" in {
    val module1 = Module("counter", Seq(), Seq())
    val module2 = Module("adder", Seq(), Seq())
    val module1Dup = Module("counter", Seq(), Seq())

    val fix = new ModuleDupFix(options)
    val result = fix.apply(Seq(module1, module2, module1Dup))

    result.length shouldBe 2
    result.map(_.name) shouldBe Seq("counter", "adder")
    fix.getWarnings.length shouldBe 1
    fix.getWarnings.head should include("duplicate module")
  }

  "ModuleDupFix" should "preserve module order" in {
    val modules = Seq(
      Module("first", Seq(), Seq()),
      Module("second", Seq(), Seq()),
      Module("third", Seq(), Seq()),
      Module("second", Seq(), Seq())
    )

    val fix = new ModuleDupFix(options)
    val result = fix.apply(modules)

    result.map(_.name) shouldBe Seq("first", "second", "third")
  }

  "ModuleDupFix" should "handle empty module list" in {
    val fix = new ModuleDupFix(options)
    val result = fix.apply(Seq())

    result shouldBe empty
  }

  // ============ Tests for HierarchyFix ============

  "HierarchyFix" should "rename numeric-only module names" in {
    val modules = Seq(
      Module("1", Seq(), Seq()),
      Module("2", Seq(), Seq())
    )

    val fix = new HierarchyFix(options)
    val result = fix.apply(modules)

    result(0).name shouldBe "top_module"
    result(1).name shouldBe "top_module_1"
  }

  "HierarchyFix" should "rename modules starting with digits" in {
    val modules = Seq(
      Module("2xyz", Seq(), Seq()),
      Module("3abc", Seq(), Seq())
    )

    val fix = new HierarchyFix(options)
    val result = fix.apply(modules)

    result(0).name shouldBe "m_2xyz"
    result(1).name shouldBe "m_3abc"
  }

  "HierarchyFix" should "rename empty module names" in {
    val modules = Seq(
      Module("", Seq(), Seq()),
      Module("valid_name", Seq(), Seq())
    )

    val fix = new HierarchyFix(options)
    val result = fix.apply(modules)

    result(0).name shouldBe "unnamed_0"
    result(1).name shouldBe "valid_name"
  }

  "HierarchyFix" should "update instance references when renaming modules" in {
    val instance = ModuleInstance("1", "inst1", Map("clk" -> "clk"))
    val modules = Seq(
      Module("1", Seq(), Seq(instance)),
      Module("counter", Seq(), Seq())
    )

    val fix = new HierarchyFix(options)
    val result = fix.apply(modules)

    val transformedInstance = result(0).items.head.asInstanceOf[ModuleInstance]
    transformedInstance.moduleName shouldBe "top_module"
  }

  "HierarchyFix" should "preserve valid module names" in {
    val modules = Seq(
      Module("valid_module", Seq(), Seq()),
      Module("Counter_32bit", Seq(), Seq())
    )

    val fix = new HierarchyFix(options)
    val result = fix.apply(modules)

    result(0).name shouldBe "valid_module"
    result(1).name shouldBe "Counter_32bit"
  }

  // ============ Tests for WireRegFix ============

  "WireRegFix" should "convert procedurally assigned wire to reg" in {
    val wire = Wire("result", Some((31, 0)), isSigned = false)
    val alwaysBlock = AlwaysBlock(
      "@(posedge clk)",
      Seq("result <= input1 + input2;")
    )
    val module = Module("adder", Seq(), Seq(wire, alwaysBlock))

    val fix = new WireRegFix(options)
    val result = fix.apply(Seq(module)).head

    val transformedWire = result.items.head.asInstanceOf[Reg]
    transformedWire.name shouldBe "result"
  }

  "WireRegFix" should "preserve wires not assigned in always blocks" in {
    val wire1 = Wire("data_in", Some((7, 0)), isSigned = false)
    val wire2 = Wire("data_out", Some((7, 0)), isSigned = false)
    val alwaysBlock = AlwaysBlock(
      "@(posedge clk)",
      Seq("data_out <= data_in;")
    )
    val module = Module("pipeline", Seq(), Seq(wire1, wire2, alwaysBlock))

    val fix = new WireRegFix(options)
    val result = fix.apply(Seq(module)).head

    val items = result.items
    items(0).asInstanceOf[Wire].name shouldBe "data_in"
    items(1).asInstanceOf[Reg].name shouldBe "data_out"
  }

  "WireRegFix" should "convert wire with combinatorial assignment" in {
    val wire = Wire("temp", Some((15, 0)), isSigned = false)
    val alwaysBlock = AlwaysBlock(
      "@(*)",
      Seq("temp = a & b;")
    )
    val module = Module("logic", Seq(), Seq(wire, alwaysBlock))

    val fix = new WireRegFix(options)
    val result = fix.apply(Seq(module)).head

    val transformedWire = result.items.head.asInstanceOf[Reg]
    transformedWire.name shouldBe "temp"
  }

  "WireRegFix" should "handle multiple assignments in single always block" in {
    val wire1 = Wire("a", Some((7, 0)), isSigned = false)
    val wire2 = Wire("b", Some((7, 0)), isSigned = false)
    val alwaysBlock = AlwaysBlock(
      "@(posedge clk)",
      Seq(
        "a <= in1;",
        "b <= in2;"
      )
    )
    val module = Module("test", Seq(), Seq(wire1, wire2, alwaysBlock))

    val fix = new WireRegFix(options)
    val result = fix.apply(Seq(module)).head

    val items = result.items
    items(0).asInstanceOf[Reg].name shouldBe "a"
    items(1).asInstanceOf[Reg].name shouldBe "b"
  }

  // ============ Integration Tests ============

  "QuartusTransformer" should "apply all fixes in correct order" in {
    val wire = Wire("data", Some((7, 0)), isSigned = true)
    val alwaysBlock = AlwaysBlock(
      "@(posedge clk)",
      Seq("data <= input1;")
    )
    val module = Module("1", Seq(), Seq(wire, alwaysBlock))

    val transformer = new QuartusTransformer(options)
    val result = transformer.transformModules(Seq(module))

    result.length shouldBe 1
    val transformed = result.head

    // Модуль переименован
    transformed.name shouldBe "top_module"

    // Wire превращен в Reg
    val regItem = transformed.items.head.asInstanceOf[Reg]
    regItem.name shouldBe "data"

    // Signed флаг снят
    regItem.isSigned shouldBe false
  }

  "QuartusTransformer" should "handle complex hierarchy with multiple issues" in {
    val modules = Seq(
      Module(
        "1",
        Seq(),
        Seq(
          Wire("counter", Some((31, 0)), isSigned = true),
          AlwaysBlock("@(posedge clk)", Seq("counter <= counter + 1;"))
        )
      ),
      Module("1", Seq(), Seq()),
      Module(
        "adder",
        Seq(),
        Seq(
          ModuleInstance("1", "cnt_inst", Map("clk" -> "clk"))
        )
      )
    )

    val transformer = new QuartusTransformer(options)
    val result = transformer.transformModules(modules)

    result.length shouldBe 2 // Дубликат удален

    val topModule = result.find(_.name == "top_module").get
    val counterReg = topModule.items(0).asInstanceOf[Reg]
    counterReg.isSigned shouldBe false

    val adderModule = result.find(_.name == "adder").get
    val instance = adderModule.items.head.asInstanceOf[ModuleInstance]
    instance.moduleName shouldBe "top_module"
  }

  "QuartusTransformer" should "generate transformation summary" in {
    val transformer = new QuartusTransformer(options)
    val module = Module("test", Seq(), Seq())
    transformer.transformModules(Seq(module))

    val summary = transformer.getTransformSummary
    summary should include("Quartus II 13.1")
    summary should include("2005")
    summary should include("ModuleDupFix")
    summary should include("HierarchyFix")
    summary should include("SignedTypeFix")
    summary should include("WireRegFix")
  }

  "QuartusTransformer" should "preserve valid modules without changes" in {
    val module = Module(
      "valid_module",
      Seq(Port("clk", "input", None)),
      Seq(
        Wire("temp", Some((7, 0)), isSigned = false),
        Assign("temp", "1'b0")
      )
    )

    val transformer = new QuartusTransformer(options)
    val result = transformer.transformModule(module)

    result.name shouldBe "valid_module"
    result.ports.length shouldBe 1
    result.items.length shouldBe 2
    result.wires.head.isSigned shouldBe false
  }

  // ============ Error Case Tests ============

  "QuartusTransformer" should "handle empty module list" in {
    val transformer = new QuartusTransformer(options)
    val result = transformer.transformModules(Seq())

    result shouldBe empty
  }

  "QuartusTransformer" should "handle module with no items" in {
    val module = Module("empty_module", Seq(), Seq())

    val transformer = new QuartusTransformer(options)
    val result = transformer.transformModule(module)

    result.name shouldBe "empty_module"
    result.items shouldBe empty
  }

  "QuartusTransformer" should "handle circular module references" in {
    val inst1 = ModuleInstance("mod2", "inst2", Map())
    val inst2 = ModuleInstance("1", "inst1", Map())

    val modules = Seq(
      Module("mod1", Seq(), Seq(inst1)),
      Module("1", Seq(), Seq(inst2))
    )

    val transformer = new QuartusTransformer(options)
    val result = transformer.transformModules(modules)

    result.length shouldBe 2
    val mod1 = result.find(_.name == "mod1").get
    val inst = mod1.items.head.asInstanceOf[ModuleInstance]
    inst.moduleName shouldBe "mod2"
  }
}
