package org.kiuru.processor

import chisel3._
import circt.stage.ChiselStage

class Microcell extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))      // 8-bit input
    val enable = Input(Bool())      // Enable signal
    val out = Output(UInt(8.W))     // 8-bit output
  })

  // Internal register to hold the value
  val reg = RegInit(0.U(8.W))

  // Combinational logic
  when(io.enable) {
    reg := io.in   // Load the input into the register when enabled
  }

  // Output the value of the register
  io.out := reg
}

// Generate the Verilog code
object MicrocellMain extends App {
  println(ChiselStage.emitSystemVerilog(new Microcell))
}
