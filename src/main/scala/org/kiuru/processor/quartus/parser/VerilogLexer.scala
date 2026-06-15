package org.kiuru.processor.quartus.parser

/**
 * Токены Verilog языка для лексического анализа.
 */
sealed trait Token {
  def value: String
  def lineNumber: Int
  def columnNumber: Int
}

case class KeywordToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class IdentifierToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class NumberToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class StringToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class OperatorToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class PunctuationToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class BracketToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class WhitespaceToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class CommentToken(value: String, lineNumber: Int, columnNumber: Int) extends Token
case class EOFToken(lineNumber: Int, columnNumber: Int) extends Token {
  override def value: String = "EOF"
}

object TokenType {
  final val KEYWORD = "KEYWORD"
  final val IDENTIFIER = "IDENTIFIER"
  final val NUMBER = "NUMBER"
  final val STRING = "STRING"
  final val OPERATOR = "OPERATOR"
  final val PUNCTUATION = "PUNCTUATION"
  final val BRACKET = "BRACKET"
  final val WHITESPACE = "WHITESPACE"
  final val COMMENT = "COMMENT"
  final val EOF = "EOF"
}

/**
 * Verilog Лексер - преобразует исходный код в последовательность токенов.
 * Использует regex для простоты и скорости.
 */
class VerilogLexer(input: String) {
  private val lines = input.split("\n")
  private var position = 0
  private var line = 1
  private var column = 1
  private val tokens = scala.collection.mutable.ListBuffer[Token]()

  // Verilog ключевые слова
  private val keywords = Set(
    "module", "endmodule", "input", "output", "inout",
    "wire", "reg", "integer", "real", "parameter", "localparam",
    "assign", "always", "always_comb", "always_ff", "always_latch",
    "initial", "begin", "end",
    "if", "else", "case", "default", "for", "while", "do",
    "function", "endfunction", "task", "endtask",
    "posedge", "negedge", "edge",
    "signed", "unsigned",
    "and", "or", "xor", "not", "nand", "nor", "xnor",
    "buf", "bufif0", "bufif1", "notif0", "notif1",
    "primitive", "endprimitive",
    "table", "endtable",
    "specify", "endspecify",
    "generate", "endgenerate",
    "genvar", "timescale", "define", "ifdef", "endif",
    "include", "interface", "endinterface",
    "modport", "clocking", "endclocking"
  )

  /**
   * Лексическом анализ входного кода, возвращает список токенов
   */
  def tokenize(): Seq[Token] = {
    var input = this.input
    var currentLine = 1
    var currentColumn = 1

    // Удалить комментарии и трейлинг пробелы
    input = input.replaceAll("//.*?$", "").replaceAll("/\\*.*?\\*/", "")

    var pos = 0
    while (pos < input.length) {
      val char = input(pos)
      val char2 = if (pos + 1 < input.length) input(pos + 1) else '\u0000'

      char match {
        // Whitespace
        case ' ' | '\t' =>
          pos += 1
          currentColumn += 1

        case '\n' =>
          currentLine += 1
          currentColumn = 1
          pos += 1

        // Operators and punctuation
        case '{' | '}' | '[' | ']' | '(' | ')' | ',' | ';' | ':' =>
          tokens += BracketToken(char.toString, currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '.' =>
          // Может быть оператор или точка
          if (char2.isLetterOrDigit) {
            tokens += OperatorToken(".", currentLine, currentColumn)
            pos += 1
            currentColumn += 1
          } else {
            pos += 1
            currentColumn += 1
          }

        case '=' =>
          if (char2 == '=') {
            tokens += OperatorToken("==", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else {
            tokens += OperatorToken("=", currentLine, currentColumn)
            pos += 1
            currentColumn += 1
          }

        case '!' =>
          if (char2 == '=') {
            tokens += OperatorToken("!=", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else {
            tokens += OperatorToken("!", currentLine, currentColumn)
            pos += 1
            currentColumn += 1
          }

        case '<' =>
          if (char2 == '=') {
            tokens += OperatorToken("<=", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else if (char2 == '<') {
            tokens += OperatorToken("<<", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else {
            tokens += OperatorToken("<", currentLine, currentColumn)
            pos += 1
            currentColumn += 1
          }

        case '>' =>
          if (char2 == '=') {
            tokens += OperatorToken(">=", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else if (char2 == '>') {
            tokens += OperatorToken(">>", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else {
            tokens += OperatorToken(">", currentLine, currentColumn)
            pos += 1
            currentColumn += 1
          }

        case '&' =>
          if (char2 == '&') {
            tokens += OperatorToken("&&", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else {
            tokens += OperatorToken("&", currentLine, currentColumn)
            pos += 1
            currentColumn += 1
          }

        case '|' =>
          if (char2 == '|') {
            tokens += OperatorToken("||", currentLine, currentColumn)
            pos += 2
            currentColumn += 2
          } else {
            tokens += OperatorToken("|", currentLine, currentColumn)
            pos += 1
            currentColumn += 1
          }

        case '^' =>
          tokens += OperatorToken("^", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '+' =>
          tokens += OperatorToken("+", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '-' =>
          tokens += OperatorToken("-", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '*' =>
          tokens += OperatorToken("*", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '/' =>
          tokens += OperatorToken("/", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '%' =>
          tokens += OperatorToken("%", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '~' =>
          tokens += OperatorToken("~", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '?' =>
          tokens += OperatorToken("?", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '#' =>
          tokens += OperatorToken("#", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        case '@' =>
          tokens += OperatorToken("@", currentLine, currentColumn)
          pos += 1
          currentColumn += 1

        // String literals
        case '"' =>
          val endPos = input.indexOf('"', pos + 1)
          if (endPos != -1) {
            val stringValue = input.substring(pos, endPos + 1)
            tokens += StringToken(stringValue, currentLine, currentColumn)
            pos = endPos + 1
            currentColumn += stringValue.length
          } else {
            pos += 1
            currentColumn += 1
          }

        // Numbers (decimal, hex, binary, octal)
        case c if c.isDigit =>
          val startPos = pos
          val startCol = currentColumn
          while (pos < input.length && (input(pos).isDigit || input(pos) == '_' ||
            input(pos) == 'x' || input(pos) == 'X' ||
            input(pos) == 'b' || input(pos) == 'B' ||
            input(pos) == 'o' || input(pos) == 'O' ||
            input(pos) == 'd' || input(pos) == 'D' ||
            (input(pos) >= 'a' && input(pos) <= 'f') ||
            (input(pos) >= 'A' && input(pos) <= 'F'))) {
            pos += 1
            currentColumn += 1
          }
          val numberValue = input.substring(startPos, pos)
          tokens += NumberToken(numberValue, currentLine, startCol)

        // Identifiers and keywords
        case c if c.isLetter || c == '_' =>
          val startPos = pos
          val startCol = currentColumn
          while (pos < input.length && (input(pos).isLetterOrDigit || input(pos) == '_' || input(pos) == '$')) {
            pos += 1
            currentColumn += 1
          }
          val word = input.substring(startPos, pos)
          if (keywords.contains(word.toLowerCase)) {
            tokens += KeywordToken(word, currentLine, startCol)
          } else {
            tokens += IdentifierToken(word, currentLine, startCol)
          }

        case _ =>
          pos += 1
          currentColumn += 1
      }
    }

    tokens += EOFToken(currentLine, currentColumn)
    tokens.toSeq
  }

  /**
   * Получить токены без whitespace и комментариев
   */
  def tokenizeFiltered(): Seq[Token] = {
    tokenize().filter {
      case _: WhitespaceToken => false
      case _: CommentToken    => false
      case _                  => true
    }
  }
}

object VerilogLexer {
  def apply(input: String): VerilogLexer = new VerilogLexer(input)

  def tokenize(input: String): Seq[Token] = {
    new VerilogLexer(input).tokenize()
  }

  def tokenizeFiltered(input: String): Seq[Token] = {
    new VerilogLexer(input).tokenizeFiltered()
  }
}
