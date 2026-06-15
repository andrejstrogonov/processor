# Quartus II 13.1 Configuration System

Система для парсирования YAML конфигураций FPGA и генерирования необходимых файлов проекта Quartus II 13.1.

## Быстрый старт

### 1. Подготовить конфигурацию (YAML)

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
  - name: "data_in"
    pin: "K1"
  - name: "data_out"
    pin: "L1"
    currentStrength: "12MA"

optimizationTechnique: "BALANCED"
powerEstimation: false
```

### 2. Загрузить и сгенерировать файлы

```scala
import org.kiuru.processor.quartus.config.ConfigParser
import org.kiuru.processor.quartus.generator._

// Загрузить конфигурацию
val config = ConfigParser.parseYaml("cyclone4.yaml")

// Сгенерировать файлы
val qsf = QsfGenerator.generate(config, "processor", "processor.v")
val sdc = SdcGenerator.generate(config, Seq("clk"))
val tcl = ShellScriptGen.generate("processor", "processor.v")

// Сохранить на диск
QsfGenerator.saveToFile(qsf, "processor.qsf")
SdcGenerator.saveToFile(sdc, "processor.sdc")
ShellScriptGen.saveToFile(tcl, "build.tcl")
```

### 3. Запустить сборку

```bash
quartus_sh -t build.tcl
```

## Файлы структуры

```
src/main/scala/org/kiuru/processor/quartus/
├── config/
│   ├── FPGAConfig.scala           # Case classes
│   ├── ConfigParser.scala         # YAML парсер
│   └── QuartusConfigExample.scala # Примеры использования
│
└── generator/
    ├── QsfGenerator.scala         # Генератор .qsf
    ├── SdcGenerator.scala         # Генератор .sdc
    ├── ShellScriptGen.scala       # Генератор .tcl
    └── MifGenerator.scala         # Генератор .mif

src/main/resources/
├── cyclone4.yaml                  # Cyclone IV E конфигурация
├── cyclone2.yaml                  # Cyclone II конфигурация
└── maxv.yaml                      # MAX V конфигурация

src/test/scala/org/kiuru/processor/quartus/config/
├── ConfigParserTest.scala         # Тесты парсера
└── QuartusGeneratorsTest.scala    # Тесты генераторов
```

## API Справка

### ConfigParser

**Метод:** `parseYaml(filePath: String): FPGAConfig`
- Загрузить конфигурацию из YAML файла
- Параметр: Путь к файлу (абсолютный или относительный)
- Возвращает: FPGAConfig объект

**Метод:** `parseYamlString(content: String): FPGAConfig`
- Загрузить конфигурацию из YAML строки
- Параметр: Содержимое YAML
- Возвращает: FPGAConfig объект

### QsfGenerator

**Метод:** `generate(config: FPGAConfig, projectName: String, verilogFile: String): String`
- Сгенерировать QSF (Quartus Project Settings) файл
- Параметры:
  - `config` - FPGAConfig объект
  - `projectName` - Имя проекта (для комментариев)
  - `verilogFile` - Основной Verilog файл
- Возвращает: QSF содержимое как строка

**Метод:** `saveToFile(qsfContent: String, filePath: String): Unit`
- Сохранить QSF содержимое в файл
- Создаст необходимые директории автоматически

### SdcGenerator

**Метод:** `generate(config: FPGAConfig, clockNames: Seq[String] = Seq("clk")): String`
- Сгенерировать SDC (Synopsys Design Constraints) файл
- Автоматически считает период из clockFreqMHz
- Параметры:
  - `config` - FPGAConfig объект
  - `clockNames` - Список имен тактовых сигналов
- Возвращает: SDC содержимое как строка

**Метод:** `saveToFile(sdcContent: String, filePath: String): Unit`
- Сохранить SDC содержимое в файл

### ShellScriptGen

**Метод:** `generate(projectName: String, verilogFile: String): String`
- Сгенерировать TCL скрипт для quartus_sh
- Параметры:
  - `projectName` - Имя проекта в Quartus
  - `verilogFile` - Основной Verilog файл
- Возвращает: TCL скрипт как строка

**Метод:** `saveToFile(tclContent: String, filePath: String): Unit`
- Сохранить TCL скрипт в файл

### MifGenerator

**Метод:** `generate(depth: Int, width: Int, values: Seq[Int]): String`
- Сгенерировать MIF (Memory Initialization File)
- Параметры:
  - `depth` - Количество адресов
  - `width` - Ширина слова в битах (8, 16, 32 и т.д.)
  - `values` - Инициальные значения
- Возвращает: MIF содержимое как строка

**Метод:** `saveToFile(mifContent: String, filePath: String): Unit`
- Сохранить MIF содержимое в файл

## Примеры

### Пример 1: Базовое использование

```scala
import org.kiuru.processor.quartus.config.ConfigParser
import org.kiuru.processor.quartus.generator.{QsfGenerator, SdcGenerator}

val config = ConfigParser.parseYaml("resources/cyclone4.yaml")
val qsf = QsfGenerator.generate(config, "my_project", "design.v")
println(qsf)
```

### Пример 2: Сохранение всех файлов

```scala
val config = ConfigParser.parseYaml("cyclone4.yaml")

val qsf = QsfGenerator.generate(config, "processor", "processor.v")
val sdc = SdcGenerator.generate(config, Seq("clk"))
val tcl = ShellScriptGen.generate("processor", "processor.v")

QsfGenerator.saveToFile(qsf, "output/processor.qsf")
SdcGenerator.saveToFile(sdc, "output/processor.sdc")
ShellScriptGen.saveToFile(tcl, "output/build.tcl")

println("Files generated successfully!")
```

### Пример 3: Множественные тактовые сигналы

```scala
val config = ConfigParser.parseYaml("cyclone4.yaml")
val sdc = SdcGenerator.generate(config, Seq("clk", "sys_clk", "pll_clk"))
```

### Пример 4: Инициализация памяти

```scala
val mif = MifGenerator.generate(
  depth = 1024,
  width = 32,
  values = Seq(0x12345678, 0xABCDEF00, 0xDEADBEEF)
)
MifGenerator.saveToFile(mif, "memory.mif")
```

### Пример 5: Полный workflow

```scala
object BuildQuartusProject {
  def main(args: Array[String]): Unit = {
    val configFile = args.getOrElse(0, "cyclone4.yaml")
    val projectName = args.getOrElse(1, "processor")
    
    // Загрузить конфигурацию
    val config = ConfigParser.parseYaml(configFile)
    println(s"Loaded config for ${config.device.device}")
    println(s"Configured ${config.pins.length} pins")
    
    // Сгенерировать файлы
    val qsf = QsfGenerator.generate(config, projectName, config.verilogFile)
    val sdc = SdcGenerator.generate(config, Seq("clk"))
    val tcl = ShellScriptGen.generate(projectName, config.verilogFile)
    
    // Сохранить
    val outputDir = "quartus_build"
    new java.io.File(outputDir).mkdirs()
    
    QsfGenerator.saveToFile(qsf, s"$outputDir/$projectName.qsf")
    SdcGenerator.saveToFile(sdc, s"$outputDir/$projectName.sdc")
    ShellScriptGen.saveToFile(tcl, s"$outputDir/build.tcl")
    
    println(s"Generated files in $outputDir/")
  }
}
```

## YAML Конфигурация

### Полная структура

```yaml
# Параметры устройства FPGA
device:
  family: String              # Семейство (Cyclone II, Cyclone IV E, MAX V)
  device: String              # Модель чипа (EP4CE115F29C7, etc)
  partNumber: String | null   # Номер детали производителя
  clockFreqMHz: Int           # Тактовая частота в MHz
  timingAnalysis: Boolean     # Включить TimeQuest анализ
  hardwareVerification: Boolean # Включить аппаратную верификацию

# Назначение выводов FPGA
pins:
  - name: String              # Имя сигнала в Verilog
    pin: String               # Номер пина на кристалле (AF14, J2, etc)
    ioStandard: String?       # I/O стандарт (3.3-V LVCMOS, LVTTL, etc)
    currentStrength: String?  # Выходной ток (4MA, 8MA, 12MA, 16MA, 24MA)
    pullUp: Boolean?          # Подтягивающий резистор (true/false)

# Параметры проекта
optimizationTechnique: String # BALANCED, AREA, SPEED
powerEstimation: Boolean      # Включить оценку мощности
topModule: String             # Имя верхнего модуля
verilogFile: String           # Основной Verilog файл
miscFiles: String[]?          # Дополнительные файлы
```

### Поддерживаемые I/O стандарты

- `3.3-V LVCMOS` - 3.3V CMOS (Cyclone IV, MAX V)
- `LVTTL` - Low Voltage TTL (Cyclone II)
- `LVCMOS` - CMOS (старые FPGA)
- `PCI` - PCI стандарт
- `SSTL_15` - Signaled Single Terminated Logic 1.5V

### Поддерживаемые техники оптимизации

- `BALANCED` - Баланс между площадью и скоростью
- `AREA` - Минимизация площади (для Cyclone II)
- `SPEED` - Максимальная скорость

## Тестирование

```bash
# Запустить все тесты
sbt test

# Запустить только конфиг тесты
sbt "testOnly org.kiuru.processor.quartus.config.*"

# Запустить конкретный тест класс
sbt "testOnly org.kiuru.processor.quartus.config.ConfigParserTest"
```

## Примеры конфигураций

Система включает три предконфигурированных примера в `src/main/resources/`:

1. **cyclone4.yaml** - Для Cyclone IV E (50 MHz)
2. **cyclone2.yaml** - Для Cyclone II (100 MHz)
3. **maxv.yaml** - Для MAX V (200 MHz)

Используйте эти примеры как шаблоны для своих конфигураций.

## Совместимость

- **Quartus II:** версия 13.1.0 build 162
- **Scala:** 2.13.12
- **SnakeYAML:** 2.2
- **Платформы:** Windows, Linux, macOS

## Замечания

- Все пути обрабатываются кроссплатформенно (Windows/Unix)
- Генерируемые файлы полностью совместимы с Quartus II 13.1
- Комментарии в файлах указывают на автоматическую генерацию
- Необязательные поля в YAML имеют разумные значения по умолчанию
