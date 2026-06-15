# Quartus Configuration Parser & Generator System

Полнофункциональная система парсирования конфигурации и генерирования файлов проекта для Quartus II 13.1 на Scala.

## Архитектура

```
org.kiuru.processor.quartus
├── config/
│   ├── FPGAConfig.scala          # Case classes для конфигурации
│   └── ConfigParser.scala        # YAML парсер через SnakeYAML
└── generator/
    ├── QsfGenerator.scala        # Генератор .qsf файлов
    ├── SdcGenerator.scala        # Генератор .sdc файлов (Synopsys constraints)
    ├── ShellScriptGen.scala      # Генератор TCL скриптов для quartus_sh
    └── MifGenerator.scala        # Генератор .mif файлов (Memory Initialization)
```

## Основные компоненты

### 1. FPGAConfig - Структура конфигурации

```scala
case class PinAssignment(
  name: String,                    // Имя сигнала (clk, reset, data_in и т.д.)
  pin: String,                     // Номер пина на FPGA (AF14, J2 и т.д.)
  ioStandard: Option[String],      // I/O стандарт (3.3-V LVCMOS, LVTTL и т.д.)
  currentStrength: Option[String], // Выходной ток (4MA, 8MA, 12MA и т.д.)
  pullUp: Option[Boolean]          // Подтягивающий резистор
)

case class FPGADevice(
  family: String,                  // Семейство чипа (Cyclone IV E, Cyclone II, MAX V)
  device: String,                  // Модель чипа (EP4CE115F29C7, EP2C70F672C6 и т.д.)
  partNumber: Option[String],      // Номер детали производителя
  clockFreqMHz: Int,               // Максимальная частота в MHz
  timingAnalysis: Boolean,         // Включить временной анализ
  hardwareVerification: Boolean    // Включить аппаратную верификацию
)

case class FPGAConfig(
  device: FPGADevice,              // Конфигурация устройства
  pins: Seq[PinAssignment],        // Назначения выводов
  optimizationTechnique: String,   // Техника оптимизации (BALANCED, AREA, SPEED)
  powerEstimation: Boolean,        // Включить оценку энергопотребления
  miscFiles: Seq[String],          // Дополнительные файлы проекта
  topModule: String,               // Имя топ-модуля Verilog (по умолчанию: top_module)
  verilogFile: String              // Имя основного Verilog файла (по умолчанию: project.v)
)
```

### 2. ConfigParser - Парсирование YAML конфигурации

```scala
object ConfigParser {
  // Парсировать конфигурацию из файла
  def parseYaml(filePath: String): FPGAConfig
  
  // Парсировать конфигурацию из строки
  def parseYamlString(content: String): FPGAConfig
}
```

**Пример использования:**

```scala
// Из файла
val config = ConfigParser.parseYaml("cyclone4.yaml")

// Из строки
val config = ConfigParser.parseYamlString(yamlContent)
```

### 3. QsfGenerator - Генератор Quartus Project Settings

Генерирует `.qsf` файлы (Quartus Project Settings) для Quartus II 13.1.

```scala
object QsfGenerator {
  // Сгенерировать QSF контент
  def generate(config: FPGAConfig, projectName: String, verilogFile: String): String
  
  // Сохранить в файл
  def saveToFile(qsfContent: String, filePath: String): Unit
}
```

**Генерируемые директивы:**
- `set_global_assignment` - Глобальные параметры проекта (семейство, устройство, оптимизация)
- `set_instance_assignment` - Параметры для конкретных выводов (I/O стандарт, ток, слоу-рейт)

**Пример использования:**

```scala
val config = ConfigParser.parseYaml("cyclone4.yaml")
val qsf = QsfGenerator.generate(config, "my_project", "design.v")
QsfGenerator.saveToFile(qsf, "my_project.qsf")
```

### 4. SdcGenerator - Генератор Synopsys Design Constraints

Генерирует `.sdc` файлы для задания временных ограничений.

```scala
object SdcGenerator {
  // Сгенерировать SDC контент
  def generate(config: FPGAConfig, clockNames: Seq[String] = Seq("clk")): String
  
  // Сохранить в файл
  def saveToFile(sdcContent: String, filePath: String): Unit
}
```

**Генерируемые констрейнты:**
- `create_clock` - Определение тактовых сигналов с периодом из конфигурации
- `set_input_delay` - Задержки входных сигналов
- `set_output_delay` - Задержки выходных сигналов

**Пример использования:**

```scala
val sdc = SdcGenerator.generate(config, Seq("clk", "pll_clk"))
SdcGenerator.saveToFile(sdc, "constraints.sdc")
```

### 5. ShellScriptGen - Генератор TCL скриптов

Генерирует TCL скрипты для автоматизации сборки в Quartus.

```scala
object ShellScriptGen {
  // Сгенерировать TCL скрипт
  def generate(projectName: String, verilogFile: String): String
  
  // Сохранить в файл
  def saveToFile(tclContent: String, filePath: String): Unit
}
```

**Выполняемые этапы:**
1. Создание проекта (`project_new`)
2. Синтез (`execute_module -tool map`)
3. Размещение и маршрутизация (`execute_module -tool fit`)
4. Временной анализ (`execute_module -tool sta`)
5. Генерация программирующего файла (`execute_module -tool asm`)

**Пример использования:**

```scala
val tcl = ShellScriptGen.generate("processor", "processor.v")
ShellScriptGen.saveToFile(tcl, "build.tcl")

// Запустить из командной строки:
// quartus_sh -t build.tcl
```

### 6. MifGenerator - Генератор Memory Initialization Files

Генерирует `.mif` файлы для инициализации памяти.

```scala
object MifGenerator {
  // Сгенерировать MIF контент
  def generate(depth: Int, width: Int, values: Seq[Int]): String
  
  // Сохранить в файл
  def saveToFile(mifContent: String, filePath: String): Unit
}
```

**Параметры:**
- `depth` - Количество адресов памяти
- `width` - Ширина данных в битах
- `values` - Последовательность инициальных значений

**Пример использования:**

```scala
val mif = MifGenerator.generate(1024, 32, Seq(0xDEADBEEF, 0xCAFEBABE, 0x12345678))
MifGenerator.saveToFile(mif, "memory_init.mif")
```

## Примеры конфигураций

### Cyclone IV E (cyclone4.yaml)

```yaml
device:
  family: "Cyclone IV E"
  device: "EP4CE115F29C7"
  partNumber: "10M50DAF484C7G"
  clockFreqMHz: 50
  timingAnalysis: true
  hardwareVerification: false

pins:
  - name: "clk"
    pin: "AF14"
    ioStandard: "3.3-V LVCMOS"
    currentStrength: "8MA"
  - name: "reset"
    pin: "J2"
    ioStandard: "3.3-V LVCMOS"
    pullUp: true
  - name: "data_in"
    pin: "K1"
    ioStandard: "3.3-V LVCMOS"
  - name: "data_out"
    pin: "L1"
    ioStandard: "3.3-V LVCMOS"
    currentStrength: "12MA"

optimizationTechnique: "BALANCED"
powerEstimation: false
topModule: "top_module"
verilogFile: "project.v"
```

### Cyclone II (cyclone2.yaml)

Для более старых FPGA с LVTTL I/O и меньшим количеством ресурсов.

### MAX V (maxv.yaml)

Для компактных приложений с ограниченным энергопотреблением.

## Полный рабочий пример

```scala
import org.kiuru.processor.quartus.config.{ConfigParser, FPGAConfig}
import org.kiuru.processor.quartus.generator.{
  QsfGenerator, SdcGenerator, ShellScriptGen, MifGenerator
}

object QuartusProjectBuilder {
  def main(args: Array[String]): Unit = {
    // 1. Загрузить конфигурацию
    val config = ConfigParser.parseYaml("quartus/cyclone4.yaml")
    
    // 2. Сгенерировать файлы проекта
    val qsf = QsfGenerator.generate(config, "processor", "processor.v")
    val sdc = SdcGenerator.generate(config, Seq("clk"))
    val tcl = ShellScriptGen.generate("processor", "processor.v")
    val mif = MifGenerator.generate(4096, 32, Seq.fill(256)(0x00000000))
    
    // 3. Сохранить файлы
    QsfGenerator.saveToFile(qsf, "quartus/processor.qsf")
    SdcGenerator.saveToFile(sdc, "quartus/processor.sdc")
    ShellScriptGen.saveToFile(tcl, "quartus/build.tcl")
    MifGenerator.saveToFile(mif, "quartus/memory.mif")
    
    // 4. Запустить сборку (Linux/Mac)
    // Runtime.getRuntime.exec(Array("quartus_sh", "-t", "quartus/build.tcl"))
  }
}
```

## Тестирование

Для запуска тестов:

```bash
sbt test
```

### Покрытые тесты:

1. **ConfigParserTest**
   - Парсирование YAML конфигураций (Cyclone4, Cyclone2, MAX V)
   - Обработка необязательных полей (ioStandard, currentStrength, pullUp)
   - Значения по умолчанию
   - Загрузка конфигурации из файла

2. **QuartusGeneratorsTest**
   - Генерирование валидного QSF с правильными директивами
   - Включение всех назначений выводов
   - Корректный расчет периода тактового сигнала в SDC
   - Поддержка нескольких тактовых сигналов
   - Сохранение файлов на диск
   - Полный workflow генерирования всех типов файлов

## Совместимость

- **Quartus II версия:** 13.1.0 build 162
- **Платформы:** Windows, Linux, macOS
- **Семейства FPGA:** Cyclone II, Cyclone IV E, MAX V
- **Scala версия:** 2.13.12
- **YAML парсер:** SnakeYAML 2.2

## Особенности

✓ Полная поддержка кроссплатформенных путей (Windows + Unix)
✓ Генерация Quartus-совместимых файлов
✓ Автоматический расчет периода тактового сигнала
✓ Гибкая конфигурация параметров FPGA
✓ Типобезопасность через Scala case classes
✓ Комментарии в генерируемых файлах с указанием источника

## Структура генерируемых файлов

### .qsf (Quartus Project Settings)
```
set_global_assignment -name FAMILY "Cyclone IV E"
set_global_assignment -name DEVICE EP4CE115F29C7
set_global_assignment -name TOP_LEVEL_ENTITY top_module
set_instance_assignment -name IO_STANDARD "3.3-V LVCMOS" -to clk
```

### .sdc (Synopsys Design Constraints)
```
create_clock -name clk -period 20.0 -waveform {0.0 10.0} [get_ports clk]
set_input_delay -clock clk -max 3.0 [get_ports {reset}]
set_output_delay -clock clk -max 3.0 [get_ports {*}]
```

### build.tcl (Quartus TCL Script)
```tcl
load_package flow
project_new -name processor -revision processor -overwrite
execute_module -tool map
execute_module -tool fit
execute_module -tool sta
project_close
```

### .mif (Memory Initialization)
```
WIDTH = 32;
DEPTH = 1024;
ADDRESS_RADIX = HEX;
DATA_RADIX = HEX;
CONTENT BEGIN
  00000000 : DEADBEEF;
  00000001 : CAFEBABE;
END;
```
