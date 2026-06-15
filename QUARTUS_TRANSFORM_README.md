# Quartus II 13.1 Compatibility Transformations

Полный набор трансформаций для преобразования Verilog кода в формат, совместимый с Quartus II 13.1.

## Обзор

Quartus II 13.1 имеет ряд ограничений и правил относительно валидного Verilog кода. Этот модуль предоставляет набор автоматических трансформаций, которые преобразуют произвольный Verilog код в совместимый формат, сохраняя при этом функциональность и семантику кода.

## Архитектура

```
QuartusTransformer (главная фасада)
├── ModuleDupFix (удалить дубликаты)
├── HierarchyFix (исправить имена иерархии)
├── SignedTypeFix (исправить signed типы)
└── WireRegFix (wire → reg для процедурных блоков)
```

### Порядок применения

Трансформации применяются в следующем порядке:

1. **ModuleDupFix** - сначала удалить дубликаты модулей
2. **HierarchyFix** - затем исправить invalid имена модулей и обновить ссылки
3. **SignedTypeFix** - затем исправить signed типы
4. **WireRegFix** - наконец преобразовать wire в reg где необходимо

## Трансформации

### 1. ModuleDupFix - Удаление дубликатов

**Проблема:** Quartus II 13.1 не позволяет иметь несколько определений одного и того же модуля.

**Решение:**
- Сканируем все модули
- Сохраняем первое определение каждого модуля по имени
- Удаляем все последующие дубликаты
- Выводим warning о удаленных дубликатах

**Пример:**
```verilog
// До
module counter (...); endmodule
module counter (...); endmodule  // Дубликат!

// После
module counter (...); endmodule
```

### 2. HierarchyFix - Исправление имен иерархии

**Проблема:** Quartus II 13.1 требует валидные идентификаторы для имен модулей:
- Имена не могут быть только цифрами
- Имена не могут начинаться с цифры
- Имена не могут быть пустыми

**Решение:**
- Numeric-only имена → `top_module`, `top_module_1`, ...
- Имена начинающиеся с цифр → `m_2xyz` (добавить префикс `m_`)
- Пустые имена → `unnamed_0`, `unnamed_1`, ...
- Обновить все ссылки на переименованные модули в инстанциях

**Примеры:**
```verilog
// До
module 1 (...); endmodule          // Invalid!
module 2xyz (...); endmodule       // Invalid!
module "" (...); endmodule         // Invalid!

// После
module top_module (...); endmodule
module m_2xyz (...); endmodule
module unnamed_0 (...); endmodule
```

### 3. SignedTypeFix - Исправление signed типов

**Проблема:** Quartus II 13.1 может иметь ограничения на использование `signed` ключевого слова в определенных контекстах.

**Решение:**
- Удалить `signed` флаг из wire/reg декларации
- Сохранить информацию о знаковости в комментариях `## QUARTUS FIX: SIGNED`
- Семантика знаковости остается понятна при необходимости ручного разбора

**Пример:**
```verilog
// До
wire signed [31:0] result;
reg signed count;

// После
wire [31:0] result;  ## QUARTUS FIX: SIGNED
reg count;           ## QUARTUS FIX: SIGNED
```

### 4. WireRegFix - Преобразование wire в reg

**Проблема:** В Quartus II 13.1 wire не может быть присвоен значение внутри `always` блока.

**Решение:**
- Сканировать все `always` блоки
- Найти wire, которые присваиваются в этих блоках (оператор `<=` или `=`)
- Преобразовать такие wire в reg
- Логика остается той же, просто меняется объявление

**Пример:**
```verilog
// До
wire result;
always @(posedge clk) begin
  result <= input1 + input2;  // Error: wire не может быть присвоен!
end

// После
reg result;
always @(posedge clk) begin
  result <= input1 + input2;  // OK: reg может быть присвоен
end
```

## Использование

### Базовое использование

```scala
import org.kiuru.processor.quartus.transform._
import org.kiuru.processor.quartus.ast._

// Опция 1: Трансформировать с помощью VerilogToQuartus утилиты
val verilogCode = """
  module 1 (
    input clk,
    output wire signed [31:0] count
  );
    wire signed result;
    always @(posedge clk) begin
      result <= count + 1;
    end
  endmodule
"""

val quartusVerilog = VerilogToQuartus.transform(verilogCode)
println(quartusVerilog)
```

### Advanced использование

```scala
// Создать трансформер с параметрами
val options = TransformOptions(
  verilogVersion = "2005",
  targetFamily = "Cyclone",
  addFixMarkers = true,
  preserveComments = true
)

val transformer = new QuartusTransformer(options)

// Трансформировать AST напрямую
val module = Module("counter", Seq(), Seq(...))
val transformed = transformer.transformModule(module)

// Получить summary
println(transformer.getTransformSummary)
```

### Применение отдельных Fix'ов

```scala
// Применить только один Fix
val fix = new HierarchyFix(options)
val modules = Seq(Module("1", Seq(), Seq()), ...)
val result = fix.apply(modules)

// Получить информацию о трансформации
if (fix.isInstanceOf[ModuleDupFix]) {
  val dupFix = fix.asInstanceOf[ModuleDupFix]
  println(dupFix.getWarnings)
}
```

## TransformOptions параметры

```scala
case class TransformOptions(
  verilogVersion: String = "2005",      // Целевая версия Verilog
  targetFamily: String = "Cyclone",     // FPGA семейство
  preserveComments: Boolean = true,     // Сохранять ли исходные комментарии
  addFixMarkers: Boolean = true         // Добавлять ли ## QUARTUS FIX комментарии
)
```

## Output комментарии

Когда `addFixMarkers = true`, трансформер добавляет комментарии для отслеживания внесенных изменений:

- `## QUARTUS FIX: SIGNED` - означает, что была удалена информация о знаковости
- `## QUARTUS FIX: wire → reg` - означает, что wire был преобразован в reg
- `Removing duplicate module definition: X` - выводится при удалении дубликата
- `Renaming module 'X' to 'Y'` - выводится при переименовании модуля

## Тестирование

Проект включает comprehensive тесты для всех трансформаций:

```bash
sbt test
```

Тесты включают:
- Unit тесты для каждого Fix'а
- Интеграционные тесты всего QuartusTransformer
- Edge case тесты (пустые модули, circular ссылки, и т.д.)
- Error case тесты (некорректный ввод, и т.д.)

Примеры из тестов:

```scala
"HierarchyFix" should "rename numeric-only module names" in {
  val modules = Seq(Module("1", Seq(), Seq()))
  val fix = new HierarchyFix(options)
  val result = fix.apply(modules)
  
  result.head.name shouldBe "top_module"
}
```

## Примеры использования

### Пример 1: Исправление invalid module name (Error 10170)

```verilog
// Quartus Error: ERROR 10170: "1" is not a valid module name

// Before
module 1 (input clk); endmodule

// After
module top_module (input clk); endmodule
```

### Пример 2: Исправление wire assignment в always (Error 10137)

```verilog
// Quartus Error: ERROR 10137: wire is not procedural

// Before
module counter (input clk);
  wire [31:0] result;
  always @(posedge clk)
    result <= 0;  // Error!
endmodule

// After
module counter (input clk);
  reg [31:0] result;
  always @(posedge clk)
    result <= 0;  // OK
endmodule
```

### Пример 3: Исправление signed типов

```verilog
// Before
module adder (
  input [31:0] a,
  input signed [31:0] b,
  output wire signed [31:0] sum
);
endmodule

// After
module adder (
  input [31:0] a,
  input [31:0] b,           ## QUARTUS FIX: SIGNED
  output wire [31:0] sum     ## QUARTUS FIX: SIGNED
);
endmodule
```

### Пример 4: Удаление дубликатов

```verilog
// Before
module counter (input clk, output [7:0] count); endmodule
module other_module (...); endmodule
module counter (input clk, output [7:0] count); endmodule  // Duplicate!

// After
module counter (input clk, output [7:0] count); endmodule
module other_module (...); endmodule
```

## Безопасность трансформаций

Все трансформации гарантируют:

✅ **Функциональность не изменяется** - логика остается той же
✅ **Порядок портов сохраняется** - последовательность портов модуля не меняется
✅ **Порядок объявлений сохраняется** - wire, reg, assign, always остаются в оригинальном порядке
✅ **Ссылки обновляются** - все инстанции обновляются при переименовании модулей
✅ **Информация сохраняется** - signed информация сохраняется в комментариях

## Ограничения

- Парсер работает с регулярными выражениями (поддерживает базовый Verilog)
- Не поддерживает очень сложные конструкции (generate блоки, вложенные always, и т.д.)
- Комментарии в коде могут быть обработаны лучше
- Параметры и localparam имеют базовую поддержку

## Будущие улучшения

- [ ] Поддержка generate блоков
- [ ] Лучшая обработка комментариев
- [ ] Поддержка System Verilog конструкций
- [ ] Оптимизация скорости парсинга
- [ ] Вывод в различные форматы (JSON, XML, и т.д.)

## Файлы проекта

```
src/main/scala/org/kiuru/processor/quartus/transform/
├── TransformOptions.scala          # Параметры трансформации
├── QuartusTransformer.scala        # Главная фасада
├── SignedTypeFix.scala             # Fix для signed типов
├── WireRegFix.scala                # Fix для wire → reg
├── ModuleDupFix.scala              # Fix для дубликатов
├── HierarchyFix.scala              # Fix для имен иерархии
└── VerilogToQuartus.scala          # Утилита для конвертации

src/test/scala/org/kiuru/processor/quartus/transform/
└── QuartusTransformerTest.scala    # Comprehensive тесты
```

## Лицензия

Этот код является частью проекта `processor` и подчиняется его лицензии.

## Контакт и поддержка

Для вопросов и отчетов об ошибках, обратитесь к основному проекту.
