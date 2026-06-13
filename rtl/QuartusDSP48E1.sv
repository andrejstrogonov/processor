// Simplified DSP48E1-like slice
// Functional subset: multiply (signed/unsigned) and add/sub with C, single pipeline register
// Compatible with Quartus II 13.1 (Verilog-2001)

module DSP48E1 #(
  parameter AWidth = 30,
  parameter BWidth = 18,
  parameter CWidth = 48,
  parameter PWidth = 48
) (
  input clk,
  input rst, // synchronous reset
  input ce,
  input [AWidth-1:0] A,
  input [BWidth-1:0] B,
  input [CWidth-1:0] C,
  input [6:0] OPMODE,
  input [3:0] ALUMODE,
  output [PWidth-1:0] P
);

  parameter MULT_WIDTH = AWidth + BWidth;
  parameter ALU_WIDTH = (MULT_WIDTH > CWidth ? MULT_WIDTH : CWidth) + 1;

  // multiply (signed or unsigned) based on OPMODE[0]
  wire [MULT_WIDTH-1:0] mult_signed;
  wire [MULT_WIDTH-1:0] mult_unsigned;
  wire [ALU_WIDTH-1:0] mult_ext;
  wire [ALU_WIDTH-1:0] c_ext;
  wire [ALU_WIDTH-1:0] alu_res;
  reg [PWidth-1:0] pReg;

  // Use Verilog-style assign for compatibility
  assign mult_signed = $signed(A) * $signed(B);
  assign mult_unsigned = A * B;

  // ALU operation
  always @(*) begin
    if (OPMODE[0]) begin
      // signed multiply
      mult_ext = $signed(mult_signed);
    end else begin
      // unsigned multiply cast to signed (positive)
      mult_ext = $signed(mult_unsigned);
    end
  end

  // extend C to ALU_WIDTH
  assign c_ext = $signed(C);

  // ALU operation
  always @(*) begin
    if (ALUMODE[0]) begin
      alu_res = mult_ext - c_ext;
    end else begin
      alu_res = mult_ext + c_ext;
    end
  end

  // pipeline register for P (synchronous reset, clock enable)
  always @(posedge clk) begin
    if (rst) begin
      pReg <= 0;
    end else if (ce) begin
      // truncate/resize to PWidth
      pReg <= alu_res[PWidth-1:0];
    end
  end

  assign P = pReg;

endmodule
