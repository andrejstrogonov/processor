# Verilog Parser Quick Reference

## Installation & Setup

```scala
// No additional dependencies needed - ScalaTest already in build.sbt

// Import statements
import org.kiuru.processor.quartus.parser.{VerilogParser, VerilogLexer}
import org.kiuru.processor.quartus.transpiler.VerilogTranspiler
import org.kiuru.processor.quartus.ast._
```

## VerilogParser - Main API

### Parse Module
```scala
val code = """
module adder(input [31:0] a, b, output [32:0] sum);
  assign sum = a + b;
endmodule
"""

// Method 1: Full parsing
val parser = VerilogParser(code)
val module = parser.parseModule()

// Method 2: Quick parse
val module = VerilogParser.parse(code)
```

### Extract Data

```scala
// Get module info
module.name              // String: "adder"
module.ports            // Seq[Port]
module.wires            // Seq[Wire]
module.regs             // Seq[Reg]
module.assigns          // Seq[Assign]
module.alwaysBlocks     // Seq[AlwaysBlock]
module.instances        // Seq[ModuleInstance]
module.signedSignals    // Seq[String]: names of signed signals

// Working with ports
module.ports.foreach { port =>
  println(s"${port.direction} ${port.widthString} ${port.name}")
}

// Working with wires
module.wires.foreach { wire =>
  println(s"wire ${wire.widthString} ${wire.name} (signed: ${wire.isSigned})")
}
```

### Specific Queries

```scala
// Signed signals
val (signedWires, signedRegs) = VerilogParser.parseSignedSignals(code)

// Combinational logic
val assigns = VerilogParser.parseAssignments(code)
assigns.foreach { case (lhs, rhs) => println(s"$lhs = $rhs") }

// Procedural assignments (always blocks)
val procAssigns = VerilogParser.parseProceduralAssignments(code)
procAssigns.foreach { case (signal, expr) => println(s"$signal <= $expr") }

// Module instances
val instances = VerilogParser.parseInstances(code)
instances.foreach { inst =>
  println(s"${inst.moduleName} ${inst.instanceName}")
  inst.connections.foreach { case (port, signal) => 
    println(s"  .$port($signal)") 
  }
}

// Signal declarations
val signals = VerilogParser.parseSignals(code)
signals.foreach { case (name, type_, width, isSigned) =>
  println(s"$type_ $name${width.map(w => s"[${w._1}:${w._2}]").getOrElse("")} (signed=$isSigned)")
}
```

## VerilogLexer - Tokenization

```scala
val code = "wire [7:0] test;"

// Get all tokens
val tokens = VerilogLexer.tokenizeFiltered(code)

// Process tokens
tokens.foreach {
  case KeywordToken(value, line, col)     => println(s"KEYWORD: $value at $line:$col")
  case IdentifierToken(value, _, _)       => println(s"ID: $value")
  case NumberToken(value, _, _)           => println(s"NUMBER: $value")
  case StringToken(value, _, _)           => println(s"STRING: $value")
  case OperatorToken(value, _, _)         => println(s"OPERATOR: $value")
  case BracketToken(value, _, _)          => println(s"BRACKET: $value")
  case EOFToken(_, _)                     => println("EOF")
  case _                                  => ()
}

// Token types available:
// KeywordToken, IdentifierToken, NumberToken, StringToken
// OperatorToken, BracketToken, WhitespaceToken, CommentToken, EOFToken
```

## VerilogTranspiler - Analysis & Reports

```scala
val transpiler = VerilogTranspiler(verilogCode)

// Statistics
val stats = transpiler.getLogicStatistics()
println(s"Wires: ${stats("wires")}")
println(s"Regs: ${stats("regs")}")
println(s"Assigns: ${stats("assigns")}")
println(s"Sequential: ${stats("sequential")}")
println(s"Combinational: ${stats("combinational")}")
println(s"Signed wires: ${stats("signed_wires")}")
println(s"Signed regs: ${stats("signed_regs")}")
println(s"Module instances: ${stats("instances")}")

// Text Report
println(transpiler.generateResourceReport())

// HTML Report
val html = transpiler.generateHtmlReport()
// Save to file or display

// Signal Analysis
val (declared, referenced) = transpiler.analyzeSignalUsage()
println(s"Declared: ${declared.mkString(", ")}")
println(s"Referenced: ${referenced.mkString(", ")}")

// Find Issues
val unused = transpiler.findUnusedSignals()
val undeclared = transpiler.findUndeclaredSignals()
println(s"Unused: ${unused.mkString(", ")}")
println(s"Undeclared: ${undeclared.mkString(", ")}")

// Validation
val errors = VerilogTranspiler.validateModule(verilogCode)
if (errors.nonEmpty) {
  println("Validation errors:")
  errors.foreach(println)
}

// Get AST
val ast = transpiler.getAST()
println(s"Module: ${ast.name}")
```

## AST Nodes

### Module
```scala
case class Module(
  name: String,
  ports: Seq[Port],
  items: Seq[ModuleItem]
)
```

### Port
```scala
case class Port(
  name: String,
  direction: String,           // "input", "output", "inout"
  width: Option[(Int, Int)]    // (high, low)
)
port.widthString              // "[high:low]" or ""
```

### Wire & Reg
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

wire.declaration              // "wire [15:0] name;"
wire.widthString              // "[15:0]" or ""
```

### Assign
```scala
case class Assign(
  lhs: String,                // "result"
  rhs: String                 // "a & b"
)
assign.statement              // "assign result = a & b;"
```

### AlwaysBlock
```scala
case class AlwaysBlock(
  trigger: String,            // "@(posedge clk)", "@(*)"
  statements: Seq[String]
)
block.isProcedural            // !trigger.contains("@(*)")
```

### ModuleInstance
```scala
case class ModuleInstance(
  moduleName: String,         // "adder"
  instanceName: String,       // "add1"
  connections: Map[String, String]  // "a" -> "x"
)
inst.connectionString         // ".a(x), .b(y), .sum(z)"
```

## Common Patterns

### Extract all sequential logic
```scala
val parser = VerilogParser(code)
val seqLogic = parser.extractSequentialLogic()
seqLogic.foreach { block =>
  println(s"${block.trigger}: ${block.statements.mkString("; ")}")
}
```

### Find all signals used in expressions
```scala
def extractSignals(code: String): Set[String] = {
  val identifierRegex = """([a-zA-Z_]\w*)""".r
  identifierRegex.findAllMatchIn(code)
    .map(_.group(1))
    .filter(!Set("if", "else", "case", "begin", "end").contains(_))
    .toSet
}
```

### Generate report and save to file
```scala
val transpiler = VerilogTranspiler(code)
val html = transpiler.generateHtmlReport()

val pw = new java.io.PrintWriter(new java.io.File("report.html"))
try {
  pw.println(html)
} finally {
  pw.close()
}
```

### Process all module items
```scala
module.items.foreach {
  case w: Wire => println(s"Wire: ${w.name}")
  case r: Reg => println(s"Reg: ${r.name}")
  case a: Assign => println(s"Assign: ${a.lhs} = ${a.rhs}")
  case b: AlwaysBlock => println(s"Always: ${b.trigger}")
  case m: ModuleInstance => println(s"Instance: ${m.moduleName}")
  case p: Parameter => println(s"Parameter: ${p.name}")
  case lp: Localparam => println(s"Localparam: ${lp.name}")
  case _ => ()
}
```

### Check if module is valid
```scala
val errors = VerilogTranspiler.validateModule(code)
val isValid = errors.isEmpty
println(if (isValid) "Valid module" else s"Invalid: ${errors.length} errors")
```

## Performance Tips

1. **Cache parsing result** - Don't re-parse same code
```scala
val parser = VerilogParser(code)
val module = parser.parseModule()
val stats = parser.parseModule().wires.length  // Re-parses! Avoid.
```

2. **Use filtered tokens** - Remove whitespace/comments
```scala
val tokens = VerilogLexer.tokenizeFiltered(code)  // Better
val tokens = VerilogLexer.tokenize(code)          // Includes whitespace
```

3. **Batch operations** - Process results once
```scala
val transpiler = VerilogTranspiler(code)
val stats = transpiler.getLogicStatistics()
val unused = transpiler.findUnusedSignals()
// Reuse transpiler object
```

## Error Handling

```scala
try {
  val module = VerilogParser.parse(code)
  println(s"Parsed module: ${module.name}")
} catch {
  case e: Exception =>
    println(s"Parse error: ${e.getMessage}")
    e.printStackTrace()
}

// Validation
val errors = VerilogTranspiler.validateModule(code)
if (errors.nonEmpty) {
  errors.foreach { error =>
    println(s"Error: $error")
  }
}
```

## Supported Verilog Constructs

✅ Modules: `module name(...); ... endmodule`
✅ Ports: `input/output/inout [width] name`
✅ Types: `wire`, `reg` (with optional `signed`)
✅ Declarations: `parameter`, `localparam`
✅ Logic: `assign`, `always @(...)`, `always @(*)`
✅ Instances: `ModuleName name(...)`
✅ Numbers: decimal, hex (0xFF), binary (0b1010), octal (0o77)
✅ Operators: All standard Verilog operators
✅ Comments: `//` and `/* */`

## Debug Tips

```scala
// Show module structure
val parser = VerilogParser(code)
val module = parser.parseModule()
println(s"Module: ${module.name}")
println(s"  Ports: ${module.ports.map(_.name).mkString(", ")}")
println(s"  Wires: ${module.wires.map(_.name).mkString(", ")}")
println(s"  Regs: ${module.regs.map(_.name).mkString(", ")}")
println(s"  Assigns: ${module.assigns.length}")
println(s"  Always: ${module.alwaysBlocks.length}")
println(s"  Instances: ${module.instances.length}")

// Show token stream
val tokens = VerilogLexer.tokenizeFiltered(code)
tokens.zipWithIndex.foreach { case (token, i) =>
  println(s"$i: ${token.getClass.getSimpleName} = ${token.value}")
}

// Show statistics
val transpiler = VerilogTranspiler(code)
transpiler.getLogicStatistics().foreach { case (k, v) =>
  println(s"$k: $v")
}
```

## Files Location

- **AST**: `src/main/scala/org/kiuru/processor/quartus/ast/VerilogAST.scala`
- **Lexer**: `src/main/scala/org/kiuru/processor/quartus/parser/VerilogLexer.scala`
- **Parser**: `src/main/scala/org/kiuru/processor/quartus/parser/VerilogParser.scala`
- **Transpiler**: `src/main/scala/org/kiuru/processor/quartus/transpiler/VerilogTranspiler.scala`
- **Examples**: `src/main/scala/org/kiuru/processor/quartus/examples/VerilogParserExamples.scala`
- **Tests**: `src/test/scala/org/kiuru/processor/quartus/`

---

For more details, see `VERILOG_PARSER_README.md`
