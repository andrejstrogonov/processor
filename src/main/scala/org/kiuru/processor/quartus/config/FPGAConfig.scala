package org.kiuru.processor.quartus.config

case class PinAssignment(
  name: String,
  pin: String,
  ioStandard: Option[String] = None,
  currentStrength: Option[String] = None,
  pullUp: Option[Boolean] = None
)

case class FPGADevice(
  family: String,
  device: String,
  partNumber: Option[String],
  clockFreqMHz: Int,
  timingAnalysis: Boolean = true,
  hardwareVerification: Boolean = false
)

case class FPGAConfig(
  device: FPGADevice,
  pins: Seq[PinAssignment],
  optimizationTechnique: String = "BALANCED",
  powerEstimation: Boolean = false,
  miscFiles: Seq[String] = Seq(),
  topModule: String = "top_module",
  verilogFile: String = "project.v"
)
