package org.kiuru.processor

import chisel3._
import circt.stage.ChiselStage

class DualBRegister(width: Int) extends Module {
  val io = IO(new Bundle {
    val inA = Input(UInt(width.W))  // Input for the first register
    val inB = Input(UInt(width.W))  // Input for the second register
    val loadA = Input(Bool())       // Control signal to load the first register
    val loadB = Input(Bool())       // Control signal to load the second register
    val outA = Output(UInt(width.W)) // Output from the first register
    val outB = Output(UInt(width.W)) // Output from the second register
  })

  // Define two registers
  val regA = RegInit(0.U(width.W))
  val regB = RegInit(0.U(width.W))

  // Load the registers based on control signals
  when(io.loadA) {
    regA := io.inA
  }

  when(io.loadB) {
    regB := io.inB
  }

  // Connect the outputs
  io.outA := regA
  io.outB := regB
}

// Generate the Verilog code
object DualBRegisterMain extends App {
  println(ChiselStage.emitSystemVerilog(new DualBRegister(18))) // Example with 32-bit wide registers
}

