// Simplified SystemVerilog model of a DSP48E1-like slice
// Functional subset: multiply (signed/unsigned) and add/sub with C, single pipeline register

module DSP48E1 #(
  parameter int AWidth = 30,
  parameter int BWidth = 18,
  parameter int CWidth = 48,
  parameter int PWidth = 48
) (
  input  logic clk,
  input  logic rst, // synchronous reset
  input  logic ce,
  input  logic [AWidth-1:0] A,
  input  logic [BWidth-1:0] B,
  input  logic [CWidth-1:0] C,
  input  logic [6:0] OPMODE,
  input  logic [3:0] ALUMODE,
  output logic [PWidth-1:0] P
);

  localparam int MULT_WIDTH = AWidth + BWidth;
  localparam int ALU_WIDTH = (MULT_WIDTH > CWidth ? MULT_WIDTH : CWidth) + 1;

  // multiply (signed or unsigned) based on OPMODE[0]
  logic signed [MULT_WIDTH-1:0] mult_signed;
  logic [MULT_WIDTH-1:0] mult_unsigned;
  assign mult_signed = $signed(A) * $signed(B);
  assign mult_unsigned = A * B;

  logic signed [ALU_WIDTH-1:0] mult_ext;
  always_comb begin
    if (OPMODE[0]) begin
      // signed multiply
      mult_ext = $signed(mult_signed);
    end else begin
      // unsigned multiply cast to signed (positive)
      mult_ext = $signed(mult_unsigned);
    end
  end

  // extend C to ALU_WIDTH and perform add/sub
  logic signed [ALU_WIDTH-1:0] c_ext;
  assign c_ext = $signed(C);

  logic signed [ALU_WIDTH-1:0] alu_res;
  always_comb begin
    if (ALUMODE[0]) begin
      alu_res = mult_ext - c_ext;
    end else begin
      alu_res = mult_ext + c_ext;
    end
  end

  // pipeline register for P (synchronous reset, clock enable)
  logic signed [PWidth-1:0] pReg;
  always_ff @(posedge clk) begin
    if (rst) begin
      pReg <= '0;
    end else if (ce) begin
      // truncate/resize to PWidth
      pReg <= alu_res[PWidth-1:0];
    end
  end

  assign P = pReg;

endmodule

