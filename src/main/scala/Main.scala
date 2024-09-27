package org.kiuru.processor

import chisel3.{Bundle, Reg, UInt, Vec, VecInit, fromIntToLiteral, fromIntToWidth}

class LPDCDecoder extends Bundle{
  val observation = Reg(Vec(14,UInt(32.W)))
  val states = Reg(Vec(14,UInt(32.W)))
  val observationY = Reg(Vec(14,UInt(32.W)))
  val switchingMatrix = VecInit.fill(14,14)(32.W)
  val emissionMatrix = VecInit.fill(14,14)(32.W)
  val randomVector = 

  val multiply =
}



object Main extends App {

}