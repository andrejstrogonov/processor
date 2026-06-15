# Quartus II 13.1 Configuration System - Component Index

## 📋 Table of Contents

1. [System Overview](#overview)
2. [File Structure](#file-structure)
3. [Component Reference](#component-reference)
4. [Quick Links](#quick-links)
5. [Getting Started](#getting-started)

## Overview

Complete Scala-based configuration parser and code generator for Quartus II 13.1 FPGA projects.

**Capabilities:**
- Parse FPGA configurations from YAML files
- Generate Quartus Project Settings (.qsf)
- Generate Synopsys Design Constraints (.sdc)
- Generate Quartus build automation scripts (.tcl)
- Generate Memory Initialization Files (.mif)
- Support for 3 FPGA device families
- Cross-platform compatibility
- 33+ comprehensive test cases

## File Structure

### Configuration Layer
```
src/main/scala/org/kiuru/processor/quartus/config/
├── FPGAConfig.scala                # Core data classes
│   ├── case class PinAssignment    # Signal-to-pin mapping
│   ├── case class FPGADevice       # Device configuration
│   └── case class FPGAConfig       # Full project configuration
│
├── ConfigParser.scala              # YAML parsing
│   ├── object ConfigParser
│   ├── def parseYaml()             # Parse from file
│   └── def parseYamlString()       # Parse from string
│
└── QuartusConfigExample.scala      # Usage examples
    └── def main()                  # Demonstrates basic usage
```

### Generation Layer
```
src/main/scala/org/kiuru/processor/quartus/generator/
├── QsfGenerator.scala              # Quartus Project Settings
│   ├── def generate()              # Generate .qsf content
│   └── def saveToFile()            # Save to disk
│
├── SdcGenerator.scala              # Timing Constraints
│   ├── def generate()              # Generate .sdc content
│   └── def saveToFile()            # Save to disk
│
├── ShellScriptGen.scala            # Build Automation
│   ├── def generate()              # Generate .tcl content
│   └── def saveToFile()            # Save to disk
│
└── MifGenerator.scala              # Memory Initialization
    ├── def generate()              # Generate .mif content
    └── def saveToFile()            # Save to disk
```

### High-Level API
```
src/main/scala/org/kiuru/processor/quartus/
├── QuartusProjectBuilder.scala     # Unified API
│   ├── object QuartusProjectBuilder
│   ├── def buildFromFile()         # Build from YAML file
│   ├── def buildFromYaml()         # Build from YAML string
│   ├── def build()                 # Build from FPGAConfig
│   ├── def saveProject()           # Save all files
│   └── def printSummary()          # Display summary
│
├── case class QuartusProject       # Result container
│   ├── def save()                  # Save files to disk
│   ├── def printSummary()          # Print summary
│   ├── def getQsfFile()            # Get .qsf content
│   ├── def getSdcFile()            # Get .sdc content
│   ├── def getTclFile()            # Get .tcl content
│   └── def getTotalSize()          # Get total size
│
└── QuartusBuilderExample.scala     # Complete example
    └── object extends App          # Runnable example
```

### Configuration Examples
```
src/main/resources/
├── cyclone4.yaml                   # Cyclone IV E (50 MHz)
├── cyclone2.yaml                   # Cyclone II (100 MHz)
└── maxv.yaml                       # MAX V (200 MHz)
```

### Test Suite
```
src/test/scala/org/kiuru/processor/quartus/config/
├── ConfigParserTest.scala          # Parser validation (8 tests)
│   ├── parseYaml()
│   ├── parseYamlString()
│   ├── Handle optional fields
│   └── Load from file
│
├── QuartusGeneratorsTest.scala     # Generator validation (15 tests)
│   ├── QSF generation
│   ├── SDC generation
│   ├── TCL generation
│   ├── MIF generation
│   └── File I/O
│
└── QuartusIntegrationTest.scala    # Workflow tests (10 tests)
    ├── Complete workflow
    ├── Multi-device support
    ├── Clock period accuracy
    ├── Syntax validation
    └── Cross-platform handling
```

### Documentation
```
Root directory (documentation/)
├── QUARTUS_CONFIG_README.md        # User guide (11 KB)
│   ├── Quick start
│   ├── API reference
│   ├── Examples
│   └── Troubleshooting
│
├── QUARTUS_CONFIG_QUICKSTART.md    # Quick reference (6 KB)
│   ├── 5-minute setup
│   ├── Three ways to build
│   ├── Common tasks
│   └── Tips & tricks
│
├── QUARTUS_CONFIG_API.md           # API documentation (12 KB)
│   ├── Component details
│   ├── Method signatures
│   ├── Usage examples
│   └── File format reference
│
├── QUARTUS_CONFIG_SUMMARY.md       # Architecture (12 KB)
│   ├── System overview
│   ├── Component details
│   ├── Usage workflow
│   └── Advanced usage
│
└── QUARTUS_CONFIG_COMPLETE.md      # Build summary
    └── What was created
```

## Component Reference

### ConfigParser

**Purpose:** Parse YAML configurations into FPGAConfig objects

**Location:** `src/main/scala/org/kiuru/processor/quartus/config/ConfigParser.scala`

**API:**
```scala
object ConfigParser {
  def parseYaml(filePath: String): FPGAConfig
  def parseYamlString(content: String): FPGAConfig
}
```

**Handles:**
- YAML deserialization via SnakeYAML
- Type conversion and validation
- Optional fields with sensible defaults
- Error reporting

### QsfGenerator

**Purpose:** Generate Quartus Project Settings (.qsf) files

**Location:** `src/main/scala/org/kiuru/processor/quartus/generator/QsfGenerator.scala`

**Generates:**
- Device and family assignments
- I/O standard settings
- Pin current strength
- Optimization parameters
- Timing analysis settings

**API:**
```scala
object QsfGenerator {
  def generate(config: FPGAConfig, projectName: String, verilogFile: String): String
  def saveToFile(qsfContent: String, filePath: String): Unit
}
```

### SdcGenerator

**Purpose:** Generate Synopsys Design Constraints (.sdc) files

**Location:** `src/main/scala/org/kiuru/processor/quartus/generator/SdcGenerator.scala`

**Generates:**
- Clock definitions with correct periods
- Input delay constraints
- Output delay constraints
- Multicycle path templates

**Features:**
- Automatic period calculation from clock frequency
- Support for multiple clocks
- Quartus II 13.1 compatible syntax

**API:**
```scala
object SdcGenerator {
  def generate(config: FPGAConfig, clockNames: Seq[String] = Seq("clk")): String
  def saveToFile(sdcContent: String, filePath: String): Unit
}
```

### ShellScriptGen

**Purpose:** Generate TCL automation scripts for quartus_sh

**Location:** `src/main/scala/org/kiuru/processor/quartus/generator/ShellScriptGen.scala`

**Includes:**
- Project creation
- Synthesis (map phase)
- Place and route (fit phase)
- Static timing analysis (sta)
- Programming file generation (asm)

**API:**
```scala
object ShellScriptGen {
  def generate(projectName: String, verilogFile: String): String
  def saveToFile(tclContent: String, filePath: String): Unit
}
```

### MifGenerator

**Purpose:** Generate Memory Initialization Files (.mif)

**Location:** `src/main/scala/org/kiuru/processor/quartus/generator/MifGenerator.scala`

**Supports:**
- Configurable memory depth and width
- Hex address and data format
- Optional initial values
- Quartus-compatible format

**API:**
```scala
object MifGenerator {
  def generate(depth: Int, width: Int, values: Seq[Int]): String
  def saveToFile(mifContent: String, filePath: String): Unit
}
```

### QuartusProjectBuilder

**Purpose:** High-level API for building complete projects

**Location:** `src/main/scala/org/kiuru/processor/quartus/QuartusProjectBuilder.scala`

**Features:**
- Three input methods (file, string, object)
- Unified project generation
- Convenient file saving
- Summary printing

**API:**
```scala
object QuartusProjectBuilder {
  def buildFromFile(configFile: String, projectName: String, outputDir: String = "quartus_build"): QuartusProject
  def buildFromYaml(yamlContent: String, projectName: String, outputDir: String = "quartus_build"): QuartusProject
  def build(config: FPGAConfig, projectName: String, outputDir: String = "quartus_build"): QuartusProject
  def saveProject(project: QuartusProject): String
  def printSummary(project: QuartusProject): Unit
}

case class QuartusProject(
  projectName: String,
  config: FPGAConfig,
  qsfContent: String,
  sdcContent: String,
  tclContent: String,
  mifContent: String,
  outputDirectory: String = "quartus_build"
) {
  def save(): String
  def printSummary(): Unit
  def getQsfFile: String
  def getSdcFile: String
  def getTclFile: String
  def getMifFile: String
  def getTotalSize: Long
}
```

## Quick Links

**Documentation:**
- 📖 [Complete User Guide](QUARTUS_CONFIG_README.md)
- 🚀 [Quick Start (5 min)](QUARTUS_CONFIG_QUICKSTART.md)
- 📚 [Detailed API Reference](QUARTUS_CONFIG_API.md)
- 🏗️ [Architecture Overview](QUARTUS_CONFIG_SUMMARY.md)

**Code Examples:**
- 💡 [Basic Example](src/main/scala/org/kiuru/processor/quartus/config/QuartusConfigExample.scala)
- 🎯 [Complete Example](src/main/scala/org/kiuru/processor/quartus/QuartusBuilderExample.scala)

**Configuration Files:**
- ⚙️ [Cyclone IV E Config](src/main/resources/cyclone4.yaml)
- ⚙️ [Cyclone II Config](src/main/resources/cyclone2.yaml)
- ⚙️ [MAX V Config](src/main/resources/maxv.yaml)

**Tests:**
- 🧪 [Parser Tests](src/test/scala/org/kiuru/processor/quartus/config/ConfigParserTest.scala)
- 🧪 [Generator Tests](src/test/scala/org/kiuru/processor/quartus/config/QuartusGeneratorsTest.scala)
- 🧪 [Integration Tests](src/test/scala/org/kiuru/processor/quartus/config/QuartusIntegrationTest.scala)

## Getting Started

### 1. Basic Usage (30 seconds)
```scala
val project = QuartusProjectBuilder.buildFromFile("cyclone4.yaml", "processor")
project.save()
```

### 2. See Available Configurations
- `src/main/resources/cyclone4.yaml` - Example for Cyclone IV E
- `src/main/resources/cyclone2.yaml` - Example for Cyclone II
- `src/main/resources/maxv.yaml` - Example for MAX V

### 3. Run Tests
```bash
sbt test
```

### 4. Run Example
```bash
sbt "runMain org.kiuru.processor.quartus.QuartusBuilderExample"
```

### 5. Read Documentation
- Start with [Quick Start Guide](QUARTUS_CONFIG_QUICKSTART.md)
- Then read [Complete User Guide](QUARTUS_CONFIG_README.md)
- Reference [API Documentation](QUARTUS_CONFIG_API.md) as needed

## System Statistics

| Metric | Count |
|--------|-------|
| Scala Files | 15 |
| Test Cases | 33+ |
| Example Configs | 3 |
| Documentation Files | 5 |
| Lines of Code | ~1,500 |
| Test Coverage | Comprehensive |
| Supported Devices | 3 families |

## Compatibility Matrix

| Component | Quartus II 13.1 | Windows | Linux | macOS |
|-----------|-----------------|---------|-------|-------|
| QSF Gen | ✓ | ✓ | ✓ | ✓ |
| SDC Gen | ✓ | ✓ | ✓ | ✓ |
| TCL Gen | ✓ | ✓ | ✓ | ✓ |
| MIF Gen | ✓ | ✓ | ✓ | ✓ |

## Supported Devices

- ✓ Cyclone II (EP2C70F672C6)
- ✓ Cyclone IV E (EP4CE115F29C7)
- ✓ MAX V (5M2210Z)

---

**Package:** `org.kiuru.processor.quartus`
**Status:** ✅ Production Ready
**Version:** 1.0.0
**License:** Project License
