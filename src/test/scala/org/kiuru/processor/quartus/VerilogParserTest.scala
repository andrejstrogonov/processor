package org.kiuru.processor.quartus.parser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.kiuru.processor.quartus.ast._

/**
 * Тесты для VerilogParser
 * Проверяют:
 * - Парсинг модуля с портами
 * - Парсинг wire/reg декараций с signed флагом
 * - Парсинг combinational и sequential логики
 * - Парсинг module instances
 * - Экстрацию процедурных присваиваний
 */
class VerilogParserTest extends AnyFunSuite with Matchers {

  test("Parse simple module with ports") {
    val verilog = """
      |module adder(
      |  input [31:0] a,
      |  input [31:0] b,
      |  output [32:0] sum
      |);
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()

    module.name should equal("adder")
    module.ports should have length 3
    module.ports(0).name should equal("a")
    module.ports(0).direction should equal("input")
    module.ports(0).width should equal(Some((31, 0)))
  }

  test("Parse wire declarations with signed flag") {
    val verilog = """
      |module test;
      |  wire [15:0] unsigned_wire;
      |  wire signed [15:0] signed_wire;
      |  wire [7:0] another_wire;
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val (signedWires, _) = parser.extractSignedSignals()

    signedWires should have length 1
    signedWires(0).name should equal("signed_wire")
    signedWires(0).isSigned should be(true)
    signedWires(0).width should equal(Some((15, 0)))
  }

  test("Parse reg declarations with signed flag") {
    val verilog = """
      |module test;
      |  reg [15:0] unsigned_reg;
      |  reg signed [15:0] signed_reg;
      |  reg [7:0] counter;
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val (_, signedRegs) = parser.extractSignedSignals()

    signedRegs should have length 1
    signedRegs(0).name should equal("signed_reg")
    signedRegs(0).isSigned should be(true)
  }

  test("Parse assign statements") {
    val verilog = """
      |module logic_module;
      |  wire [7:0] result;
      |  wire [7:0] a, b;
      |  assign result = a & b;
      |  assign c = a | b;
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val assigns = parser.extractCombinationalLogic()

    assigns should have length 2
    assigns(0)._1.trim should equal("result")
    assigns(0)._2.trim should equal("a & b")
    assigns(1)._1.trim should equal("c")
  }

  test("Parse always blocks - sequential logic") {
    val verilog = """
      |module counter;
      |  reg [31:0] count;
      |  always @(posedge clk) begin
      |    count <= count + 1;
      |  end
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val seqLogic = parser.extractSequentialLogic()

    seqLogic should have length 1
    seqLogic(0).trigger should include("posedge")
    seqLogic(0).statements should have length 1
  }

  test("Parse always blocks - combinational logic") {
    val verilog = """
      |module mux;
      |  wire [7:0] sel;
      |  wire [7:0] out;
      |  always @(*) begin
      |    case(sel)
      |      2'b00: out = 8'h00;
      |      2'b01: out = 8'hFF;
      |      default: out = 8'hXX;
      |    endcase
      |  end
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()
    val alwaysBlocks = module.alwaysBlocks

    alwaysBlocks should have length 1
    alwaysBlocks(0).trigger should equal("@(*)")
  }

  test("Parse module instances") {
    val verilog = """
      |module top;
      |  wire [7:0] a, b, sum;
      |  adder add1(
      |    .a(a),
      |    .b(b),
      |    .sum(sum)
      |  );
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val instances = parser.extractModuleInstances()

    instances should have length 1
    instances(0).moduleName should equal("adder")
    instances(0).instanceName should equal("add1")
    instances(0).connections should have size 3
    instances(0).connections("a") should equal("a")
    instances(0).connections("b") should equal("b")
    instances(0).connections("sum") should equal("sum")
  }

  test("Parse procedural assignments") {
    val verilog = """
      |module counter;
      |  reg [31:0] count, next_count;
      |  always @(posedge clk) begin
      |    count <= next_count;
      |    next_count <= count + 1;
      |  end
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val procAssigns = parser.extractProceduralAssignments()

    procAssigns should have length 2
    procAssigns(0)._1 should equal("count")
    procAssigns(0)._2 should equal("next_count")
    procAssigns(1)._1 should equal("next_count")
    procAssigns(1)._2.trim should equal("count + 1")
  }

  test("Extract signal declarations") {
    val verilog = """
      |module test;
      |  wire [15:0] data;
      |  wire signed [7:0] signed_value;
      |  reg [31:0] counter;
      |  reg [15:0] temp;
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val signals = parser.extractSignalDeclarations()

    signals should have length 4
    val wireSignals = signals.filter(_._2 == "wire")
    wireSignals should have length 2
    wireSignals.filter(_._4) should have length 1 // 1 signed wire

    val regSignals = signals.filter(_._2 == "reg")
    regSignals should have length 2
  }

  test("Parse parameters") {
    val verilog = """
      |module parameterized #(
      |  parameter WIDTH = 8,
      |  parameter DEPTH = 256
      |);
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()

    // Параметры должны быть в items
    val params = module.items.collect { case p: Parameter => p }
    params should have length(2)
  }

  test("Parse complex module with mixed declarations") {
    val verilog = """
      |module alu(
      |  input [31:0] a,
      |  input [31:0] b,
      |  input [2:0] op,
      |  output reg [31:0] result
      |);
      |  wire [31:0] and_result;
      |  wire signed [31:0] signed_result;
      |  
      |  assign and_result = a & b;
      |  
      |  always @(*) begin
      |    case(op)
      |      3'b000: result = a + b;
      |      3'b001: result = a - b;
      |      3'b010: result = and_result;
      |      default: result = 32'b0;
      |    endcase
      |  end
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()

    module.name should equal("alu")
    module.ports should have length 4
    module.wires should have length 2
    module.assigns should have length 1
    module.alwaysBlocks should have length 1
    
    val signedWires = module.signedSignals
    signedWires should have length 1
    signedWires(0) should equal("signed_result")
  }

  test("Parse with comments and whitespace") {
    val verilog = """
      |// This is a comment
      |module test(
      |  input clk,         // clock input
      |  output [7:0] data  // 8-bit output
      |);
      |  /* Multi-line comment
      |     describing the module
      |  */
      |  wire [15:0] temp;
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()

    module.name should equal("test")
    module.ports should have length 2
    module.wires should have length 1
  }

  test("Handle empty module") {
    val verilog = """
      |module empty();
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()

    module.name should equal("empty")
    module.ports should have length 0
    module.items should have length 0
  }

  test("Parse multiple module instances with different styles") {
    val verilog = """
      |module top;
      |  adder u1(.a(x), .b(y), .sum(z));
      |  adder u2(x, y, z);
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val instances = parser.extractModuleInstances()

    instances should have length 2
    instances(0).moduleName should equal("adder")
    instances(1).moduleName should equal("adder")
  }

  test("Extract sequential vs combinational logic") {
    val verilog = """
      |module mixed_logic;
      |  wire comb_out;
      |  reg seq_out;
      |  
      |  assign comb_out = a & b;
      |  
      |  always @(posedge clk)
      |    seq_out <= comb_out;
      |    
      |  always @(*)
      |    seq_out = comb_out;
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val seqLogic = parser.extractSequentialLogic()
    val combLogic = parser.extractCombinationalLogic()

    seqLogic should have length 1 // только posedge
    combLogic should have length 1
  }

  test("Parse port widths correctly") {
    val verilog = """
      |module wide_bus(
      |  input [127:0] wide_in,
      |  input [0:31] reversed,
      |  output [15:0] narrow_out
      |);
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()

    module.ports(0).width should equal(Some((127, 0)))
    module.ports(1).width should equal(Some((0, 31)))
    module.ports(2).width should equal(Some((15, 0)))
  }

  test("LexerTokenization works correctly") {
    val verilog = """
      |wire [7:0] test;
      |reg signed value;
    """.stripMargin

    val lexer = VerilogLexer(verilog)
    val tokens = lexer.tokenizeFiltered()

    tokens should not be empty
    tokens(0) match {
      case KeywordToken(value, _, _) => value should equal("wire")
      case _                          => fail("Expected KeywordToken")
    }
  }

  test("Module with localparams") {
    val verilog = """
      |module test;
      |  localparam WIDTH = 8;
      |  localparam DEPTH = 256;
      |  wire [WIDTH-1:0] data;
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val module = parser.parseModule()

    val localparams = module.items.collect { case lp: Localparam => lp }
    localparams should have length 2
    localparams(0).name should equal("WIDTH")
  }

  test("Parse nested/complex expressions in assign") {
    val verilog = """
      |module expr_test;
      |  wire [7:0] result;
      |  wire [3:0] a, b;
      |  assign result = (a + b) * 2 + (a >> 1);
      |endmodule
    """.stripMargin

    val parser = VerilogParser(verilog)
    val assigns = parser.extractCombinationalLogic()

    assigns should have length 1
    assigns(0)._2 should include("(a + b)")
  }
}
