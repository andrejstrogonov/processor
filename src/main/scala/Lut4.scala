package org.kiuru.configurationalLogicBlock

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

class Lut4 extends Module {
  val io = IO(new Bundle {
    val inputs = Input(UInt(4.W))  // 4 inputs to the LUT
    val output = Output(UInt(1.W))  // 1 output from the LUT
  })

  // Define the LUT contents as a Vec of UInts
  val lut = VecInit(Seq(
    0.U,  // LUT[0] = 0
    1.U,  // LUT[1] = 1
    1.U,  // LUT[2] = 1
    0.U,  // LUT[3] = 0
    1.U,  // LUT[4] = 1
    0.U,  // LUT[5] = 0
    0.U,  // LUT[6] = 0
    1.U   // LUT[7] = 1
  ))

  // Use the inputs to index into the LUT
  io.output := lut(io.inputs)
}

object Lut4 extends App {
  println(ChiselStage.emitSystemVerilog(new Lut4))
}
