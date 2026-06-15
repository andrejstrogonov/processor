package org.kiuru.processor.quartus.transpiler

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Тесты для VerilogTranspiler
 * Проверяют:
 * - Анализ используемых сигналов
 * - Поиск unused signals
 * - Поиск undeclared signals
 * - Генерацию отчетов
 */
class VerilogTranspilerTest extends AnyFunSuite with Matchers {

  test("Generate resource report") {
    val verilog = """
      |module test(
      |  input [7:0] a,
      |  input [7:0] b,
      |  output [7:0] result
      |);
      |  wire [7:0] temp1;
      |  wire signed [7:0] temp2;
      |  reg [7:0] count;
      |  
      |  assign temp1 = a & b;
      |  
      |  always @(posedge clk)
      |    count <= count + 1;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val report = transpiler.generateResourceReport()

    report should include("test")
    report should include("Wires:")
    report should include("Registers:")
    report should include("Assign statements:")
    report should include("Always blocks:")
  }

  test("Get logic statistics") {
    val verilog = """
      |module calc(
      |  input [15:0] x,
      |  input [15:0] y,
      |  output [15:0] result
      |);
      |  wire [15:0] sum;
      |  wire signed [15:0] diff;
      |  reg [15:0] product;
      |  
      |  assign sum = x + y;
      |  
      |  always @(*) begin
      |    if (x > y)
      |      product = x * y;
      |    else
      |      product = 0;
      |  end
      |  
      |  always @(posedge clk)
      |    result <= product;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val stats = transpiler.getLogicStatistics()

    stats("wires") should equal(2)
    stats("regs") should equal(1)
    stats("assigns") should equal(1)
    stats("signed_wires") should equal(1)
  }

  test("Find unused signals") {
    val verilog = """
      |module unused_test;
      |  wire [7:0] used_signal;
      |  wire [7:0] unused_signal;
      |  reg [7:0] used_reg;
      |  
      |  assign used_signal = 8'h00;
      |  
      |  always @(posedge clk)
      |    used_reg <= used_signal;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val unused = transpiler.findUnusedSignals()

    unused should contain("unused_signal")
  }

  test("Find undeclared signals") {
    val verilog = """
      |module undeclared_test;
      |  wire [7:0] declared_wire;
      |  
      |  assign declared_wire = undeclared_signal;
      |  
      |  always @(posedge clk)
      |    result <= declared_wire + unknown_value;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val undeclared = transpiler.findUndeclaredSignals()

    undeclared should contain("undeclared_signal")
    undeclared should contain("result")
  }

  test("Analyze signal usage - declared signals") {
    val verilog = """
      |module usage_test;
      |  input [7:0] in_a;
      |  input [7:0] in_b;
      |  output [7:0] out_result;
      |  wire [7:0] internal;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val (declared, _) = transpiler.analyzeSignalUsage()

    declared should contain("in_a")
    declared should contain("in_b")
    declared should contain("out_result")
    declared should contain("internal")
  }

  test("Analyze signal usage - referenced signals") {
    val verilog = """
      |module reference_test;
      |  wire [7:0] a;
      |  wire [7:0] b;
      |  wire [7:0] result;
      |  
      |  assign result = a & b;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val (_, referenced) = transpiler.analyzeSignalUsage()

    referenced should contain("a")
    referenced should contain("b")
  }

  test("Generate HTML report") {
    val verilog = """
      |module html_test(
      |  input clk,
      |  output [7:0] data
      |);
      |  wire [7:0] temp;
      |  assign temp = 8'hAA;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val html = transpiler.generateHtmlReport()

    html should include("<!DOCTYPE html>")
    html should include("html_test")
    html should include("table")
    html should include("Statistics")
  }

  test("Validate module - no errors") {
    val verilog = """
      |module valid_test;
      |  wire [7:0] a;
      |  wire [7:0] b;
      |  wire [7:0] result;
      |  
      |  assign result = a + b;
      |endmodule
    """.stripMargin

    val errors = VerilogTranspiler.validateModule(verilog)
    errors should have length 0
  }

  test("Validate module - with errors") {
    val verilog = """
      |module invalid_test;
      |  wire [7:0] a;
      |  
      |  assign result = a + unknown;
      |endmodule
    """.stripMargin

    val errors = VerilogTranspiler.validateModule(verilog)
    errors.length should be > 0
    errors(0) should include("Undeclared signal")
  }

  test("Get AST") {
    val verilog = """
      |module ast_test(
      |  input [7:0] x
      |);
      |  wire [7:0] y;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val ast = transpiler.getAST()

    ast.name should equal("ast_test")
    ast.ports should have length 1
    ast.wires should have length 1
  }

  test("Complex module analysis") {
    val verilog = """
      |module complex_dsp(
      |  input clk,
      |  input reset,
      |  input [31:0] a,
      |  input [31:0] b,
      |  output reg [63:0] product
      |);
      |  wire [63:0] mult_out;
      |  wire signed [31:0] signed_a;
      |  reg signed [31:0] signed_b;
      |  
      |  assign mult_out = a * b;
      |  
      |  dsp_mult mult_inst(
      |    .a(a),
      |    .b(b),
      |    .out(product)
      |  );
      |  
      |  always @(posedge clk)
      |    if (reset)
      |      product <= 64'b0;
      |    else
      |      product <= mult_out;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val stats = transpiler.getLogicStatistics()

    stats("wires") should equal(2)
    stats("regs") should equal(2)
    stats("signed_wires") should equal(1)
    stats("signed_regs") should equal(1)
    stats("instances") should equal(1)
    stats("sequential") should equal(1)
  }

  test("Identify procedural vs combinational logic") {
    val verilog = """
      |module logic_types;
      |  wire [7:0] comb_in;
      |  reg [7:0] seq_out, comb_out;
      |  
      |  assign comb_in = 8'h00;
      |  
      |  always @(posedge clk)
      |    seq_out <= comb_in;
      |    
      |  always @(*)
      |    comb_out = comb_in;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val stats = transpiler.getLogicStatistics()

    stats("sequential") should equal(1)
    stats("combinational") should equal(1)
  }

  test("Analyze module instances connections") {
    val verilog = """
      |module top;
      |  wire [7:0] data_in, data_out;
      |  wire valid;
      |  
      |  fifo_8bit fifo_inst(
      |    .din(data_in),
      |    .dout(data_out),
      |    .valid(valid)
      |  );
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val ast = transpiler.getAST()
    
    ast.instances should have length 1
    val inst = ast.instances(0)
    inst.moduleName should equal("fifo_8bit")
    inst.instanceName should equal("fifo_inst")
    inst.connections should have size 3
  }

  test("Handle empty module") {
    val verilog = """
      |module empty;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val stats = transpiler.getLogicStatistics()

    stats.values.sum should equal(0)
  }

  test("Report with multiple signals") {
    val verilog = """
      |module multi_signal;
      |  wire [31:0] data0, data1, data2;
      |  reg [15:0] counter, accumulator;
      |  wire signed [7:0] s1, s2;
      |endmodule
    """.stripMargin

    val transpiler = VerilogTranspiler(verilog)
    val report = transpiler.generateResourceReport()

    report should include("Wires:")
    report should include("Registers:")
    report should include("5") // data0, data1, data2, s1, s2
  }
}
