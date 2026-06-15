package org.kiuru.processor.quartus.examples

import org.kiuru.processor.quartus.parser.VerilogParser
import org.kiuru.processor.quartus.transpiler.VerilogTranspiler

/**
 * Примеры использования Verilog Parser и Transpiler
 */
object VerilogParserExamples {

  /**
   * Пример 1: Парсинг простого модуля сумматора
   */
  def example1_SimpleAdder(): Unit = {
    val verilogCode = """
      module adder(
        input [31:0] a,
        input [31:0] b,
        output [32:0] sum
      );
        assign sum = a + b;
      endmodule
    """

    val parser = VerilogParser(verilogCode)
    val module = parser.parseModule()

    println("=== Example 1: Simple Adder ===")
    println(s"Module name: ${module.name}")
    println(s"Number of ports: ${module.ports.length}")
    println(s"Ports:")
    module.ports.foreach { port =>
      println(s"  ${port.direction} ${port.widthString} ${port.name}")
    }
    println(s"Combinational logic:")
    module.assigns.foreach { assign =>
      println(s"  assign ${assign.lhs} = ${assign.rhs};")
    }
    println()
  }

  /**
   * Пример 2: Анализ sequential logic (счетчик)
   */
  def example2_Counter(): Unit = {
    val verilogCode = """
      module counter #(parameter WIDTH = 8)(
        input clk,
        input reset,
        output reg [WIDTH-1:0] count
      );
        always @(posedge clk) begin
          if (reset)
            count <= 0;
          else
            count <= count + 1;
        end
      endmodule
    """

    val transpiler = VerilogTranspiler(verilogCode)
    val stats = transpiler.getLogicStatistics()

    println("=== Example 2: Counter Analysis ===")
    println(s"Sequential logic blocks: ${stats("sequential")}")
    println(s"Registers: ${stats("regs")}")
    println(s"Resource Report:")
    println(transpiler.generateResourceReport())
    println()
  }

  /**
   * Пример 3: Анализ сигналов со знаком
   */
  def example3_SignedSignals(): Unit = {
    val verilogCode = """
      module dsp_unit(
        input signed [15:0] a,
        input signed [15:0] b,
        output [31:0] product
      );
        wire signed [31:0] signed_product;
        wire [31:0] unsigned_temp;
        
        assign signed_product = a * b;
        assign product = signed_product;
      endmodule
    """

    val (signedWires, signedRegs) = VerilogParser.parseSignedSignals(verilogCode)

    println("=== Example 3: Signed Signals ===")
    println("Signed wires:")
    signedWires.foreach { wire =>
      println(s"  ${wire.name}${wire.widthString}")
    }
    println(s"Signed regs: ${signedRegs.length}")
    println()
  }

  /**
   * Пример 4: Парсинг module instances
   */
  def example4_ModuleInstances(): Unit = {
    val verilogCode = """
      module top(
        input [7:0] data_in,
        output [7:0] data_out
      );
        wire [7:0] intermediate;
        
        buffer_8bit buf1(.in(data_in), .out(intermediate));
        inverter_8bit inv1(.in(intermediate), .out(data_out));
      endmodule
    """

    val instances = VerilogParser.parseInstances(verilogCode)

    println("=== Example 4: Module Instances ===")
    instances.foreach { inst =>
      println(s"Module: ${inst.moduleName}, Instance: ${inst.instanceName}")
      println(s"Connections:")
      inst.connections.foreach { case (port, signal) =>
        println(s"  .$port($signal)")
      }
    }
    println()
  }

  /**
   * Пример 5: Поиск проблем (unused и undeclared signals)
   */
  def example5_SignalAnalysis(): Unit = {
    val verilogCode = """
      module analysis_test;
        input [7:0] used_input;
        input [7:0] unused_input;
        wire [7:0] temp1;
        wire [7:0] unused_wire;
        
        assign temp1 = used_input & 8'hFF;
        assign output_signal = temp1 | external_signal;
      endmodule
    """

    val transpiler = VerilogTranspiler(verilogCode)
    val unused = transpiler.findUnusedSignals()
    val undeclared = transpiler.findUndeclaredSignals()

    println("=== Example 5: Signal Analysis ===")
    println(s"Unused signals: ${unused.mkString(", ")}")
    println(s"Undeclared signals: ${undeclared.mkString(", ")}")
    println()
  }

  /**
   * Пример 6: Парсинг процедурных присваиваний
   */
  def example6_ProceduralLogic(): Unit = {
    val verilogCode = """
      module procedural_test;
        reg [7:0] state, next_state;
        
        always @(posedge clk) begin
          state <= next_state;
          counter <= counter + 1;
        end
        
        always @(*) begin
          case(state)
            8'h00: next_state = 8'h01;
            8'h01: next_state = 8'h02;
            default: next_state = 8'h00;
          endcase
        end
      endmodule
    """

    val procAssigns = VerilogParser.parseProceduralAssignments(verilogCode)

    println("=== Example 6: Procedural Logic ===")
    println("Procedural assignments:")
    procAssigns.foreach { case (signal, expr) =>
      println(s"  $signal <= $expr")
    }
    println()
  }

  /**
   * Пример 7: Полный анализ ALU (Arithmetic Logic Unit)
   */
  def example7_CompleteALU(): Unit = {
    val verilogCode = """
      module alu(
        input [31:0] a,
        input [31:0] b,
        input [2:0] op,
        output reg [31:0] result
      );
        wire [31:0] add_result, sub_result, and_result;
        
        assign add_result = a + b;
        assign sub_result = a - b;
        assign and_result = a & b;
        
        always @(*) begin
          case(op)
            3'b000: result = add_result;
            3'b001: result = sub_result;
            3'b010: result = and_result;
            3'b011: result = a | b;
            3'b100: result = a ^ b;
            default: result = 32'b0;
          endcase
        end
      endmodule
    """

    val transpiler = VerilogTranspiler(verilogCode)
    val parser = VerilogParser(verilogCode)
    
    val module = parser.parseModule()
    val stats = transpiler.getLogicStatistics()

    println("=== Example 7: Complete ALU Analysis ===")
    println(s"Module: ${module.name}")
    println(s"Statistics:")
    stats.foreach { case (key, value) =>
      println(s"  $key: $value")
    }
    println(s"\nSignals:")
    module.wires.foreach { w =>
      println(s"  wire ${w.widthString} ${w.name}")
    }
    module.regs.foreach { r =>
      println(s"  reg ${r.widthString} ${r.name}")
    }
    println()
  }

  /**
   * Пример 8: Генерация HTML отчета
   */
  def example8_HTMLReport(): Unit = {
    val verilogCode = """
      module report_test(
        input [15:0] data_in,
        output [15:0] data_out
      );
        wire [15:0] temp1, temp2;
        reg [15:0] result;
        
        assign temp1 = data_in & 16'hFFFF;
        assign temp2 = temp1 >> 2;
        
        always @(posedge clk)
          result <= temp2;
        
        assign data_out = result;
      endmodule
    """

    val transpiler = VerilogTranspiler(verilogCode)
    val html = transpiler.generateHtmlReport()

    println("=== Example 8: HTML Report ===")
    println("Generated HTML report (first 500 chars):")
    println(html.take(500))
    println("...")
    println("[Full HTML saved to file in production]")
    println()
  }

  /**
   * Пример 9: Валидация модуля
   */
  def example9_Validation(): Unit = {
    val verilogCode = """
      module invalid_module;
        wire [7:0] a;
        
        assign b = a & unknown_signal;
        assign c = unconnected;
      endmodule
    """

    val errors = VerilogTranspiler.validateModule(verilogCode)

    println("=== Example 9: Module Validation ===")
    if (errors.isEmpty) {
      println("Module is valid!")
    } else {
      println(s"Found ${errors.length} issues:")
      errors.foreach(println)
    }
    println()
  }

  /**
   * Пример 10: Лексический анализ
   */
  def example10_Lexer(): Unit = {
    import org.kiuru.processor.quartus.parser._

    val verilog = "wire signed [15:0] test_value;"
    val tokens = VerilogLexer.tokenizeFiltered(verilog)

    println("=== Example 10: Lexer Analysis ===")
    println(s"Code: $verilog")
    println("Tokens:")
    tokens.foreach {
      case k: KeywordToken     => println(s"  KEYWORD: ${k.value}")
      case i: IdentifierToken  => println(s"  IDENTIFIER: ${i.value}")
      case n: NumberToken      => println(s"  NUMBER: ${n.value}")
      case o: OperatorToken    => println(s"  OPERATOR: ${o.value}")
      case b: BracketToken     => println(s"  BRACKET: ${b.value}")
      case _: EOFToken         => println(s"  EOF")
      case _                   => ()
    }
    println()
  }

  def main(args: Array[String]): Unit = {
    println("\n" + "="*60)
    println("Verilog Parser & Transpiler Examples")
    println("="*60 + "\n")

    example1_SimpleAdder()
    example2_Counter()
    example3_SignedSignals()
    example4_ModuleInstances()
    example5_SignalAnalysis()
    example6_ProceduralLogic()
    example7_CompleteALU()
    example8_HTMLReport()
    example9_Validation()
    example10_Lexer()

    println("="*60)
    println("All examples completed!")
    println("="*60)
  }
}
