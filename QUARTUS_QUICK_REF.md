#!/usr/bin/env scala
/**
 * Quartus II 13.1 Transform Quick Reference
 * 
 * Быстрая справка по использованию трансформаций
 */

// ============ QUICK START ============

// 1. Импорты
import org.kiuru.processor.quartus.transform._
import org.kiuru.processor.quartus.ast._

// 2. Базовое использование

// Вариант A: Трансформировать Verilog текст
val verilogCode = """
  module 1 (input clk, output wire signed [31:0] count);
    wire signed result;
    always @(posedge clk) result <= count + 1;
  endmodule
"""
val transformed = VerilogToQuartus.transform(verilogCode)

// Вариант B: Трансформировать AST напрямую
val module = Module("1", Seq(), Seq())
val transformer = new QuartusTransformer()
val result = transformer.transformModule(module)

// ============ TRANSFORMATIONS ============

/*
1. ModuleDupFix - удалить дубликаты модулей
   Проблема: module m (...); endmodule; module m (...); endmodule;
   Решение: удалить второе определение
   
2. HierarchyFix - исправить имена модулей
   Проблема:  module 1 (...); endmodule;        // Invalid!
   Решение:   module top_module (...); endmodule;
   
   Проблема:  module 2xyz (...); endmodule;     // Invalid!
   Решение:   module m_2xyz (...); endmodule;
   
   Проблема:  module "" (...); endmodule;       // Invalid!
   Решение:   module unnamed_0 (...); endmodule;

3. SignedTypeFix - удалить signed флаг
   Проблема:  wire signed [31:0] data;
   Решение:   wire [31:0] data; ## QUARTUS FIX: SIGNED
   
4. WireRegFix - wire → reg в always блоках
   Проблема:  wire result;
              always @(posedge clk) result <= a + b;  // ERROR!
   Решение:   reg result;
              always @(posedge clk) result <= a + b;  // OK
*/

// ============ TRANSFORM OPTIONS ============

val options = TransformOptions(
  verilogVersion = "2005",           // Версия Verilog
  targetFamily = "Cyclone",          // Семейство FPGA
  preserveComments = true,           // Сохранять комментарии
  addFixMarkers = true               // Добавлять ## QUARTUS FIX
)

// ============ EXAMPLES ============

// Пример 1: Исправить все ошибки
val allModules = Seq(
  Module("1", Seq(), Seq()),
  Module("1", Seq(), Seq()),  // Duplicate!
  Module("counter", Seq(), Seq(
    Wire("count", Some((7, 0)), isSigned = true),
    AlwaysBlock("@(posedge clk)", Seq("count <= 0;"))
  ))
)
val fixed = new QuartusTransformer(options).transformModules(allModules)

// Пример 2: Применить только один Fix
val hierarchyOnly = new HierarchyFix(options).apply(allModules)

// Пример 3: Получить информацию о трансформациях
val transformer = new QuartusTransformer(options)
transformer.transformModules(allModules)
println(transformer.getTransformSummary)
println(transformer.getTransformLog)

// ============ COMMON ERRORS & FIXES ============

/*
Error 10170: Invalid module name
  Решение: Используйте HierarchyFix
  
Error 10137: wire is not procedural
  Решение: Используйте WireRegFix
  
Multiple module definitions
  Решение: Используйте ModuleDupFix
  
signed type issues
  Решение: Используйте SignedTypeFix
*/

// ============ INTEGRATION WITH EXISTING CODE ============

// Парсить Verilog
val parser = new org.kiuru.processor.quartus.parser.VerilogParser(verilogCode)
val module = parser.parseModule()

// Трансформировать
val quartusFix = new QuartusTransformer(options)
val fixed = quartusFix.transformModule(module)

// Конвертировать обратно в текст
val outputVerilog = VerilogToQuartus.transform(verilogCode, options)

// ============ RUNNING TESTS ============

// Запустить все тесты
// sbt test

// Запустить тесты конкретного пакета
// sbt testOnly org.kiuru.processor.quartus.transform.*

// ============ API REFERENCE ============

/*
Главный класс: QuartusTransformer
  Methods:
    - transformModule(module: Module): Module
    - transformModules(modules: Seq[Module]): Seq[Module]
    - getTransformSummary(): String
    - getTransformLog(): Seq[String]

Утилита: VerilogToQuartus
  Methods:
    - transform(code: String, options: TransformOptions): String
    - transformModules(modules: Seq[Module], options: TransformOptions): Seq[Module]
    - getTransformSummary(options: TransformOptions): String

Fix классы:
    - ModuleDupFix
    - HierarchyFix
    - SignedTypeFix
    - WireRegFix
    
    Каждый имеет:
      - apply(modules: Seq[Module]): Seq[Module]
      - description: String
*/

// ============ TIPS & TRICKS ============

/*
1. Всегда применяй все трансформации в правильном порядке:
   ModuleDupFix -> HierarchyFix -> SignedTypeFix -> WireRegFix
   (QuartusTransformer делает это автоматически)

2. Используй addFixMarkers = true чтобы отследить изменения

3. Проверяй getTransformLog для отладки

4. Используй getWarnings() на каждом Fix'е для получения деталей

5. Тестируй трансформации перед применением в production коде
*/

// ============ ADVANCED USAGE ============

// Кастомный порядок трансформаций
class CustomTransformer(options: TransformOptions) {
  def transformCustom(modules: Seq[Module]): Seq[Module] = {
    var result = modules
    result = new HierarchyFix(options).apply(result)
    result = new ModuleDupFix(options).apply(result)
    result = new SignedTypeFix(options).apply(result)
    result = new WireRegFix(options).apply(result)
    result
  }
}

// Получить информацию о каждом Fix'е
val dupFix = new ModuleDupFix(options)
val hierarchyFix = new HierarchyFix(options)
val signedFix = new SignedTypeFix(options)
val wireFix = new WireRegFix(options)

val fixes = Seq(dupFix, hierarchyFix, signedFix, wireFix)
for (fix <- fixes) {
  println(s"${fix.getClass.getSimpleName}: ${fix.description}")
}

// ============ TROUBLESHOOTING ============

/*
Q: Как я узнаю, какие изменения были сделаны?
A: Используйте getTransformLog() и ищите комментарии ## QUARTUS FIX

Q: Могу ли я применить трансформации частично?
A: Да, используйте отдельные Fix классы

Q: Что если парсер не понимает мой Verilog?
A: Парсер использует регулярные выражения, поддерживает базовый Verilog.
   Для сложных конструкций может потребоваться ручное исправление.

Q: Безопасны ли трансформации?
A: Да, все трансформации preserve функциональность и обновляют ссылки
*/
