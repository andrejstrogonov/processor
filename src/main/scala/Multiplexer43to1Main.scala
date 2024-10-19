package org.kiuru.configurationalLogicBlock

import chisel3._
import circt.stage.ChiselStage

class Multiplexer43to1 extends Module {
  val io = IO(new Bundle {
    val in = Input(Vec(43, UInt(1.W))) // 43 input lines, each 1 bit wide
    val sel = Input(UInt(6.W))         // Selector signal, 6 bits to select one of 43 inputs
    val out = Output(UInt(1.W))        // 1-bit output
  })

  // Use the selector to choose one of the inputs
  io.out := io.in(io.sel)
}

// Generate the Verilog code
object Multiplexer43to1Main extends App {
  println(ChiselStage.emitSystemVerilog(new Multiplexer43to1))
}
