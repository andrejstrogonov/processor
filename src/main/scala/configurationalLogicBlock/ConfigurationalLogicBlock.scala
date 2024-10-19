package org.kiuru.configurationalLogicBlock
package configurationalLogicBlock
package org.kiuru.configurationalLogicBlock

import _root_.circt.stage.ChiselStage
import chisel3._

class CLB extends Module {
  val io = IO(new Bundle {
    val inputs = Input(UInt(4.W))  // 4 inputs to the LUT
    val select = Input(Bool())     // Select signal for the MUX
    val regEnable = Input(Bool())  // Enable signal for the register
    val muxInput = Input(UInt(1.W)) // Additional input for the MUX
    val output = Output(UInt(1.W)) // Output from the CLB
  })

  // LUT definition
  val lut = VecInit(Seq(
    0.U, 1.U, 1.U, 0.U,
    1.U, 0.U, 0.U, 1.U
  ))

  // LUT output
  val lutOutput = lut(io.inputs)

  // MUX: Select between LUT output and another input
  val muxOutput = Mux(io.select, lutOutput, io.muxInput)

  // Register: Store the MUX output
  val reg = RegInit(0.U(1.W))
  when(io.regEnable) {
    reg := muxOutput
  }

  // Output from the CLB
  io.output := reg
}

object  ConfigurationalLogicBlock extends App {
  println(ChiselStage.emitSystemVerilogFile(new CLB, firtoolOpts = Array("-disable-all-randomization","-strip-debug-info")))
}
