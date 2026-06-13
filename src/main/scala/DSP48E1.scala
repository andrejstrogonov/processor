package org.kiuru.processor

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

/**
 * Simplified, compile-friendly Chisel model of a DSP48E1-like slice.
 * Functional subset: multiply (signed/unsigned) and add/sub with C, single pipeline register.
 */
class DSP48E1(
               val AWidth: Int = 30,
               val BWidth: Int = 18,
               val CWidth: Int = 48,
               val PWidth: Int = 48
             ) extends Module {
  val io = IO(new Bundle {
    val A = Input(UInt(AWidth.W))
    val B = Input(UInt(BWidth.W))
    val C = Input(UInt(CWidth.W))
    val OPMODE = Input(UInt(7.W))
    val ALUMODE = Input(UInt(4.W))
    val CE = Input(Bool())
    val RST = Input(Bool())
    val P = Output(UInt(PWidth.W))
  })

  // control bits (convention for this model)
  val signedMul = io.OPMODE(0)
  val doSub = io.ALUMODE(0)

  // compute multiply: support signed or unsigned multiply
  val multSigned = (io.A.asSInt * io.B.asSInt).asSInt
  val multUnsigned = (io.A * io.B).asUInt

  // unify to SInt of sufficient width
  val multWidth = AWidth + BWidth
  val mult_s = Wire(SInt((multWidth).W))
  when (signedMul.asBool) {
    mult_s := multSigned
  } .otherwise {
    // cast unsigned mult to SInt (positive)
    mult_s := multUnsigned.asSInt
  }

  // extend C to same width and perform add/sub
  val c_ext = io.C.asSInt
  val aluWidth = math.max(mult_s.getWidth, CWidth) + 1
  val mult_ext = Wire(SInt(aluWidth.W))
  val c_ext2 = Wire(SInt(aluWidth.W))
  mult_ext := mult_s.asSInt.asTypeOf(SInt(aluWidth.W))
  c_ext2 := c_ext.asTypeOf(SInt(aluWidth.W))

  val alu_res = Wire(SInt(aluWidth.W))
  when (doSub.asBool) {
    alu_res := mult_ext - c_ext2
  } .otherwise {
    alu_res := mult_ext + c_ext2
  }

  // pipeline register (single stage) with CE and synchronous reset
  val pReg = RegInit(0.S(PWidth.W))
  when (io.RST) {
    pReg := 0.S
  } .elsewhen (io.CE) {
    pReg := alu_res.asSInt
  }

  io.P := pReg.asUInt
}

object GenerateDSP48E1Verilog {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).emitVerilog(new DSP48E1(), Array("-o", "generated"))
  }
}