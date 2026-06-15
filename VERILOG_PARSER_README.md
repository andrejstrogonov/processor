# Verilog Lexer & Parser для Quartus Транспилера

## Описание

Полнофункциональный Verilog лексер и парсер на Scala для транспиляции Verilog кода. Предоставляет:

- **VerilogLexer** - токенизация Verilog кода (KEYWORD, IDENTIFIER, OPERATOR, NUMBER, и т.д.)
- **VerilogParser** - парсинг в AST с поддержкой wire, reg, assign, always блоков, instances
- **VerilogTranspiler** - анализ и трансформация модулей
- **VerilogAST** - полная иерархия AST узлов

## Структура проекта

```
src/main/scala/org/kiuru/processor/quartus/
├── ast/
│   └── VerilogAST.scala          # AST классы и traits
├── parser/
│   ├── VerilogLexer.scala        # Лексический анализатор
│   └── VerilogParser.scala       # Синтаксический анализатор
└── transpiler/
    └── VerilogTranspiler.scala   # Трансформирование и анализ

src/test/scala/org/kiuru/processor/quartus/
├── VerilogLexerTest.scala        # Тесты лексера
├── VerilogParserTest.scala       # Тесты парсера
└── VerilogTranspilerTest.scala   # Тесты транспилера
```

## Использование

### Базовый парсинг

```scala
import org.kiuru.processor.quartus.parser.VerilogParser
import org.kiuru.processor.quartus.ast._

val verilogCode = """
module adder(
  input [31:0] a,
  input [31:0] b,
  output [32:0] sum
);
  assign sum = a + b;
endmodule
"""

// Парсить модуль
val module = VerilogParser.parse(verilogCode)
println(s"Module: ${module.name}")
println(s"Ports: ${module.ports.length}")

// Извлечь специфические элементы
val wires = module.wires
val regs = module.regs
val assigns = module.assigns
val alwaysBlocks = module.alwaysBlocks
```

### Работа с лексером

```scala
import org.kiuru.processor.quartus.parser._

val verilog = "wire [7:0] data;"

// Получить все токены
val lexer = VerilogLexer(verilog)
val tokens = lexer.tokenizeFiltered()

// Или быстрый вызов
val tokens = VerilogLexer.tokenizeFiltered(verilog)

tokens.foreach {
  case KeywordToken(value, line, col) => println(s"Keyword: $value at $line:$col")
  case IdentifierToken(value, _, _)   => println(s"Identifier: $value")
  case NumberToken(value, _, _)       => println(s"Number: $value")
  case OperatorToken(value, _, _)     => println(s"Operator: $value")
  case _                              => ()
}
```

### Парсинг сигналов со знаком

```scala
val verilog = """
wire [15:0] unsigned_data;
wire signed [15:0] signed_data;
"""

// Получить signed сигналы
val (signedWires, signedRegs) = VerilogParser.parseSignedSignals(verilog)
signedWires.foreach(w => println(s"Signed wire: ${w.name} [${w.width}]"))
```

### Анализ логики

```scala
// Combinational logic (assign statements)
val assigns = VerilogParser.parseAssignments(verilog)
assigns.foreach { case (lhs, rhs) =>
  println(s"$lhs = $rhs")
}

// Procedural assignments (always блоки)
val procAssigns = VerilogParser.parseProceduralAssignments(verilog)
procAssigns.foreach { case (signal, expr) =>
  println(s"$signal <= $expr")
}

// Module instances
val instances = VerilogParser.parseInstances(verilog)
instances.foreach { inst =>
  println(s"${inst.moduleName} ${inst.instanceName}")
  inst.connections.foreach { case (port, signal) =>
    println(s"  .$port($signal)")
  }
}
```

### Анализ и отчеты

```scala
import org.kiuru.processor.quartus.transpiler.VerilogTranspiler

val transpiler = VerilogTranspiler(verilogCode)

// Получить статистику
val stats = transpiler.getLogicStatistics()
println(s"Wires: ${stats("wires")}")
println(s"Regs: ${stats("regs")}")
println(s"Assigns: ${stats("assigns")}")
println(s"Sequential: ${stats("sequential")}")
println(s"Combinational: ${stats("combinational")}")
println(s"Signed wires: ${stats("signed_wires")}")
println(s"Signed regs: ${stats("signed_regs")}")

// Текстовый отчет
println(transpiler.generateResourceReport())

// HTML отчет
val html = transpiler.generateHtmlReport()
// Сохранить в файл...

// Найти проблемы
val unused = transpiler.findUnusedSignals()
println(s"Unused signals: ${unused.mkString(", ")}")

val undeclared = transpiler.findUndeclaredSignals()
println(s"Undeclared signals: ${undeclared.mkString(", ")}")

// Валидация
val errors = VerilogTranspiler.validateModule(verilogCode)
errors.foreach(println)
```

### AST Nodes

#### Module (корневой узел)

```scala
case class Module(
    name: String,
    ports: Seq[Port],
    items: Seq[ModuleItem]
)

// Удобные методы
module.wires         // Seq[Wire]
module.regs          // Seq[Reg]
module.assigns       // Seq[Assign]
module.alwaysBlocks  // Seq[AlwaysBlock]
module.instances     // Seq[ModuleInstance]
module.signedSignals // Seq[String]
```

#### Port (портовая декларация)

```scala
case class Port(
    name: String,
    direction: String,        // "input", "output", "inout"
    width: Option[(Int, Int)]  // (high, low)
)
```

#### Wire & Reg (сигналы)

```scala
case class Wire(
    name: String,
    width: Option[(Int, Int)],
    isSigned: Boolean
)

case class Reg(
    name: String,
    width: Option[(Int, Int)],
    isSigned: Boolean
)
```

#### Assign (комбинационная логика)

```scala
case class Assign(
    lhs: String,  // target
    rhs: String   // expression
)
```

#### AlwaysBlock (процедурная логика)

```scala
case class AlwaysBlock(
    trigger: String,      // "@(posedge clk)", "@(*)", и т.д.
    statements: Seq[String]
)
```

#### ModuleInstance

```scala
case class ModuleInstance(
    moduleName: String,
    instanceName: String,
    connections: Map[String, String]  // port -> signal
)
```

## Поддерживаемые конструкции

### Декларации

✅ Модули (module...endmodule)
✅ Порты (input, output, inout)
✅ Wire (включая signed)
✅ Reg (включая signed)
✅ Parameter
✅ Localparam

### Логика

✅ Assign statements
✅ Always блоки (@(posedge clk), @(negedge clk), @(*))
✅ If/else statements
✅ Case statements
✅ Процедурные присваивания (<=)
✅ Комбинационные присваивания (=)

### Инстанции

✅ Module instances с подключениями
✅ Поддержка как .port(signal), так и port(signal)

### Операторы

✅ Арифметические: +, -, *, /, %
✅ Логические: &&, ||, !
✅ Побитовые: &, |, ^, ~, <<, >>
✅ Сравнения: ==, !=, <, >, <=, >=
✅ Тернарный: ?:

## Тестирование

```bash
# Запустить все тесты
sbt test

# Запустить специфичные тесты
sbt "testOnly org.kiuru.processor.quartus.parser.VerilogParserTest"
sbt "testOnly org.kiuru.processor.quartus.parser.VerilogLexerTest"
sbt "testOnly org.kiuru.processor.quartus.transpiler.VerilogTranspilerTest"
```

### Примеры тестов

- ✅ Парсинг простого модуля с портами
- ✅ Экстрация signed флага для wire/reg
- ✅ Парсинг assign statements
- ✅ Парсинг always блоков (sequential и combinational)
- ✅ Парсинг module instances
- ✅ Экстрация процедурных присваиваний
- ✅ Поиск unused signals
- ✅ Поиск undeclared signals
- ✅ Генерация отчетов (текстовые и HTML)

## API Reference

### VerilogLexer

```scala
class VerilogLexer(input: String)
  def tokenize(): Seq[Token]
  def tokenizeFiltered(): Seq[Token]  // без whitespace и комментариев

object VerilogLexer
  def apply(input: String): VerilogLexer
  def tokenize(input: String): Seq[Token]
  def tokenizeFiltered(input: String): Seq[Token]
```

### VerilogParser

```scala
class VerilogParser(verilogCode: String)
  def parseModule(): Module
  def parsePorts(): Seq[Port]
  def parseModuleItems(moduleContent: String): Seq[ModuleItem]
  def parseWires(code: String): Seq[Wire]
  def parseRegs(code: String): Seq[Reg]
  def parseAssigns(code: String): Seq[Assign]
  def parseAlwaysBlocks(code: String): Seq[AlwaysBlock]
  def parseModuleInstances(code: String): Seq[ModuleInstance]
  def extractSignedSignals(): (Seq[Wire], Seq[Reg])
  def extractProceduralAssignments(): Seq[(String, String)]
  def extractCombinationalLogic(): Seq[(String, String)]
  def extractSequentialLogic(): Seq[AlwaysBlock]
  def extractModuleInstances(): Seq[ModuleInstance]
  def extractSignalDeclarations(): Seq[(String, String, Option[(Int, Int)], Boolean)]

object VerilogParser
  def apply(verilogCode: String): VerilogParser
  def parse(verilogCode: String): Module
  def parseSignals(verilogCode: String): Seq[...]
  def parseAssignments(verilogCode: String): Seq[(String, String)]
  def parseProceduralAssignments(verilogCode: String): Seq[(String, String)]
  def parseInstances(verilogCode: String): Seq[ModuleInstance]
  def parseSignedSignals(verilogCode: String): (Seq[Wire], Seq[Reg])
```

### VerilogTranspiler

```scala
class VerilogTranspiler(verilogCode: String)
  def optimize(): Module
  def generateResourceReport(): String
  def analyzeSignalUsage(): (Set[String], Set[String])
  def findUnusedSignals(): Seq[String]
  def findUndeclaredSignals(): Seq[String]
  def getLogicStatistics(): Map[String, Int]
  def generateHtmlReport(): String
  def getAST(): Module

object VerilogTranspiler
  def apply(verilogCode: String): VerilogTranspiler
  def analyze(verilogCode: String): Map[String, Int]
  def validateModule(verilogCode: String): Seq[String]
```

## Ограничения и примечания

1. **Комментарии** - полностью удаляются перед парсингом
2. **Макросы** - `define` обрабатываются как простые идентификаторы
3. **Иерархия** - парсится только один module за раз
4. **Сложные выражения** - хранятся как строки (не полностью парсятся)
5. **Кодировка** - предполагается UTF-8

## Производительность

- Лексический анализ: ~1-5ms для типичного модуля
- Синтаксический анализ: ~2-10ms для типичного модуля
- HTML отчет: ~5-20ms для модуля с 100+ сигналами

## Лицензия

MIT

## Контрибьюция

Улучшения приветствуются! Откройте issue или pull request.
