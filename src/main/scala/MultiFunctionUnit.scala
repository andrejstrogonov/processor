package org.kiuru.processor

import chisel3._
import chisel3.util._

class MultiFunctionUnit(width: Int) extends Module {
  val io = IO(new Bundle {
    val in1 = Input(UInt(width.W))
    val in2 = Input(UInt(width.W))
    val acc = Input(UInt(width.W))
    val shiftAmount = Input(UInt(log2Ceil(width).W))
    val operation = Input(UInt(3.W)) // 3-bit operation selector
    val result = Output(UInt(width.W))
    val comparison = Output(Bool())
  })

  // Define operations
  val multiply = io.in1 * io.in2
  val mac = (io.in1 * io.in2) + io.acc
  val addMul = io.in1 + (io.in2 * io.acc)
  val circularShift = (io.in1 << io.shiftAmount) | (io.in1 >> (width.U - io.shiftAmount))
  val compare = io.in1 === io.in2

  // Select operation based on the operation selector
  io.result := MuxLookup(io.operation, 0.U, Seq(
    0.U -> multiply,
    1.U -> mac,
    2.U -> addMul,
    3.U -> circularShift
  ))

  // Output comparison result
  io.comparison := compare
}

// Generate the Verilog code
object MultiFunctionUnitMain extends App {
  println(chisel3.stage.ChiselStage.emitSystemVerilog(new MultiFunctionUnit(16))) // Example with 32-bit wide inputs
}

