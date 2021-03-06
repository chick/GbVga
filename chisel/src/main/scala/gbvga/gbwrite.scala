package gbvga

import chisel3._
import chisel3.util._
import chisel3.formal._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

import GbConst._

class GbWrite (val datawidth: Int = 2,
               val debug_simu: Boolean = true,
               val aformal: Boolean = false) extends Module {//with Formal {
  val io = IO(new Bundle {
    /* GameBoy input */
    val gb = Input(new Gb())
    /* Memory write */
    val Maddr  = Output(UInt((log2Ceil(GBWIDTH*GBHEIGHT)).W))
    val Mdata  = Output(UInt(datawidth.W))
    val Mwrite = Output(Bool())
    /* debug */
    val countcol = if(debug_simu) Some(Output(UInt(32.W))) else None
  })

  val shsync = if(debug_simu) ShiftRegister(io.gb.hsync,2) else io.gb.hsync
  val svsync = if(debug_simu) ShiftRegister(io.gb.vsync,2) else io.gb.vsync
  val sclk   = if(debug_simu) ShiftRegister(io.gb.clk  ,2) else io.gb.clk
  val sdata  = if(debug_simu) ShiftRegister(io.gb.data ,2) else io.gb.data


  val lineCount = RegInit(0.U(log2Ceil(GBHEIGHT).W))
  val pixelCount = RegInit((GBHEIGHT*GBWIDTH).U)

  val countreg = RegInit(0.U(12.W))
//  if(debug_simu) {
//    io.countcol := pixelCount
//  }

  val pixel = RegInit(0.U(datawidth.W))

  /* Reset lines an column on vsync */
  when(risingedge(svsync)) {
    lineCount := 0.U
    countreg := 0.U
    pixelCount := 0.U
  }

  /* change lines on gb.hsync */
  when(fallingedge(shsync)) {
    lineCount := lineCount + 1.U
    countreg := 0.U
  }

  /* read pixel on sclk fall */
  io.Mwrite := false.B
  when(fallingedge(sclk)) {
    pixel := sdata
    pixelCount := pixelCount + 1.U
    io.Mwrite := true.B
  }

//  if(aformal){
//      /* Mwrite should be 1 cycle wide */
//      past(io.Mwrite, 1) (pMwrite => {
//        when(io.Mwrite === true.B) {
//          assert(pMwrite === false.B)
//        }
//      })
//      cover(countreg === 10.U)
//  }

  io.Maddr := pixelCount
  io.Mdata := pixel
}

object GbWriteDriver extends App {
  println("")
  println("> generate verilog")
  (new ChiselStage).execute(args,
      Seq(ChiselGeneratorAnnotation(() => new GbWrite(8, debug_simu=true))))
  println("")
  println("> generate systemVerilog for formal")
  (new ChiselStage).execute(Array("-X", "sverilog"),
      Seq(ChiselGeneratorAnnotation(() => new GbWrite(8, aformal=true))))
}
