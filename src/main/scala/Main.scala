package org.kiuru.configurationalLogicBlock

import chisel3.{Bundle, Input, Module, Output, fromIntToLiteral}

class MyModule extends Module{
  val io = IO(new Bundle {
    val inPTS = Input(1.U)
    val inPTC = Input(1.U)
    val inPTR = Input(1.U)
    val inPTOE = Input(1.U)
    val inGSK1 = Input(1.U)
    val inGSK2 = Input(1.U)
    val inGSK3 = Input(1.U)
    val inGSR1 = Input(1.U)
    val inGSR2 = Input(1.U)
    val inGSR3 = Input(1.U)
    val inUpper = Input(1.U)
    val inAbove = Input(1.U)
    val inData = Input(1.U)

    val outPM = Output(1.U)
    val outPTOE = Output(1.U)
    val outUpper = Output(1.U)
    val outAbove = Output(1.U)
    
  })


}




object Main extends App {

}