# Quartus Configuration System - Quick Start Guide

## 5-Minute Setup

### 1. Create your YAML configuration (cyclone4.yaml)

```yaml
device:
  family: "Cyclone IV E"
  device: "EP4CE115F29C7"
  partNumber: "10M50DAF484C7G"
  clockFreqMHz: 50
  timingAnalysis: true

pins:
  - name: "clk"
    pin: "AF14"
    ioStandard: "3.3-V LVCMOS"
  - name: "reset"
    pin: "J2"
    ioStandard: "3.3-V LVCMOS"
    pullUp: true

optimizationTechnique: "BALANCED"
powerEstimation: false
topModule: "top_module"
verilogFile: "design.v"
```

### 2. Scala Code - Simplest Way

```scala
import org.kiuru.processor.quartus.QuartusProjectBuilder

val project = QuartusProjectBuilder.buildFromFile("cyclone4.yaml", "myproject")
project.printSummary()
project.save()
```

### 3. Access Generated Files

```scala
val qsf = project.getQsfFile()
val sdc = project.getSdcFile()
val tcl = project.getTclFile()
```

## Three Ways to Build

### Way 1: From YAML File
```scala
val project = QuartusProjectBuilder.buildFromFile(
  "config.yaml",
  "processor",
  "output_dir"
)
```

### Way 2: From YAML String
```scala
val yaml = """..."""
val project = QuartusProjectBuilder.buildFromYaml(yaml, "processor")
```

### Way 3: From Parsed Config
```scala
import org.kiuru.processor.quartus.config.ConfigParser

val config = ConfigParser.parseYaml("config.yaml")
val project = QuartusProjectBuilder.build(config, "processor")
```

## Generated Files

After `project.save()`, you get:

1. **processor.qsf** - Quartus project settings
2. **processor.sdc** - Timing constraints
3. **build.tcl** - Build automation script
4. **memory.mif** - Memory initialization (optional)

## Using in Build Script

```bash
# Compile Scala code that generates files
sbt "run"

# Build FPGA design
cd quartus_build
quartus_sh -t build.tcl

# Results: processor.sof (SRAM programming file)
#         processor.pof (Flash programming file)
```

## API Reference

### QuartusProjectBuilder

```scala
// Build from configuration
QuartusProjectBuilder.buildFromFile(configPath, projectName, outputDir)
QuartusProjectBuilder.buildFromYaml(yamlContent, projectName, outputDir)
QuartusProjectBuilder.build(config, projectName, outputDir)

// Save to disk
QuartusProjectBuilder.saveProject(project)

// Print info
QuartusProjectBuilder.printSummary(project)
```

### QuartusProject

```scala
// Access content
project.getQsfFile()
project.getSdcFile()
project.getTclFile()
project.getMifFile()

// Save to disk
project.save()

// Print summary
project.printSummary()

// Get total size
project.getTotalSize()
```

## Supported Devices

- **Cyclone IV E** - 50-100 MHz, 3.3V LVCMOS
- **Cyclone II** - 50-150 MHz, LVTTL
- **MAX V** - 100-200 MHz, 3.3V LVCMOS

## Optimization Techniques

- `BALANCED` - Balance between speed and area (default)
- `AREA` - Minimize chip area
- `SPEED` - Maximize clock frequency

## I/O Standards

- `3.3-V LVCMOS` - Standard 3.3V CMOS
- `LVTTL` - Low voltage TTL (older FPGA)
- `LVCMOS` - Generic CMOS
- `PCI` - PCI interface
- `SSTL_15` - Server I/O standard

## Common Pins

Cyclone IV E (EP4CE115F29C7):
- Clock: AF14, D9, K16, R5, etc.
- Reset: J2, K17, etc.
- User LEDs: W15, W16, AA24, AA25, etc.

Cyclone II (EP2C70F672C6):
- Clock: E13, J21
- Reset: R2

MAX V (5M2210Z):
- Clock: E1
- Reset: D2

## Testing

```bash
# Run all tests
sbt test

# Run specific test class
sbt "testOnly org.kiuru.processor.quartus.config.ConfigParserTest"

# Run integration tests
sbt "testOnly org.kiuru.processor.quartus.config.QuartusIntegrationTest"
```

## File Format Examples

### QSF (Quartus Settings)
```
set_global_assignment -name FAMILY "Cyclone IV E"
set_global_assignment -name DEVICE EP4CE115F29C7
set_instance_assignment -name IO_STANDARD "3.3-V LVCMOS" -to clk
```

### SDC (Timing Constraints)
```
create_clock -name clk -period 20.0 [get_ports clk]
set_input_delay -clock clk -max 3.0 [get_ports reset]
```

### TCL (Build Script)
```tcl
load_package flow
project_new -name processor
execute_module -tool map
execute_module -tool fit
project_close
```

## Tips & Tricks

1. **Multiple Clocks**: Pass multiple clock names to SdcGenerator
   ```scala
   val sdc = SdcGenerator.generate(config, Seq("clk", "pll_clk", "ref_clk"))
   ```

2. **Custom Memory**: Generate MIF files programmatically
   ```scala
   val values = (0 until 1024).map(_ * 2).toSeq
   val mif = MifGenerator.generate(1024, 32, values)
   ```

3. **Batch Generation**: Generate for multiple configurations
   ```scala
   val configs = Seq("cyclone4.yaml", "cyclone2.yaml", "maxv.yaml")
   configs.foreach { config =>
     val project = QuartusProjectBuilder.buildFromFile(config, "batch")
     project.save()
   }
   ```

## Troubleshooting

**Problem**: "Resource not found: cyclone4.yaml"
- **Solution**: Place YAML files in `src/main/resources/`

**Problem**: "Cannot find quartus_sh"
- **Solution**: Add Quartus II bin directory to PATH environment variable

**Problem**: "Invalid YAML format"
- **Solution**: Check YAML syntax at https://www.yamllint.com/

## Complete Example

```scala
import org.kiuru.processor.quartus.QuartusProjectBuilder
import org.kiuru.processor.quartus.config.ConfigParser
import org.kiuru.processor.quartus.generator._

object CompleteExample extends App {
  // Step 1: Parse configuration
  val config = ConfigParser.parseYaml("cyclone4.yaml")
  
  // Step 2: Build project
  val project = QuartusProjectBuilder.build(
    config, 
    "mydesign",
    "quartus_output"
  )
  
  // Step 3: Print info
  project.printSummary()
  
  // Step 4: Save files
  val outputDir = project.save()
  println(s"Project saved to: $outputDir")
  
  // Step 5: Can now run
  // quartus_sh -t quartus_output/build.tcl
}
```

## More Information

- Full API: See QUARTUS_CONFIG_API.md
- Examples: See src/main/scala/org/kiuru/processor/quartus/config/QuartusConfigExample.scala
- Tests: See src/test/scala/org/kiuru/processor/quartus/config/
