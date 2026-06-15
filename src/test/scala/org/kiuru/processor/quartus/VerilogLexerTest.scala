package org.kiuru.processor.quartus.parser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Тесты для VerilogLexer
 * Проверяют корректность tokenization
 */
class VerilogLexerTest extends AnyFunSuite with Matchers {

  test("Tokenize simple wire declaration") {
    val code = "wire [7:0] test;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    tokens.length should be > 0
    tokens.head match {
      case KeywordToken(value, _, _) => value should equal("wire")
      case _                          => fail("Expected KeywordToken for 'wire'")
    }
  }

  test("Tokenize with operators") {
    val code = "a <= b + c;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val operatorTokens = tokens.collect { case o: OperatorToken => o.value }
    operatorTokens should contain("<=")
    operatorTokens should contain("+")
    operatorTokens should contain(";")
  }

  test("Tokenize with brackets") {
    val code = "wire [15:0] data;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val bracketTokens = tokens.collect { case b: BracketToken => b.value }
    bracketTokens should contain("[")
    bracketTokens should contain("]")
    bracketTokens should contain(":")
  }

  test("Tokenize complex expression") {
    val code = "(a & b) | (c ^ d)"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    tokens.length should be > 0
    val operators = tokens.collect { case o: OperatorToken => o.value }
    operators should contain("&")
    operators should contain("|")
    operators should contain("^")
  }

  test("Handle numbers - decimal") {
    val code = "reg [31:0] count; count = 1234;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val numbers = tokens.collect { case n: NumberToken => n.value }
    numbers should contain("31")
    numbers should contain("0")
    numbers should contain("1234")
  }

  test("Handle numbers - hex") {
    val code = "reg [7:0] value = 8'hFF;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val numbers = tokens.collect { case n: NumberToken => n.value }
    numbers should contain("8")
    numbers.filter(_.contains("h")) should have length 1
  }

  test("Handle numbers - binary") {
    val code = "wire [3:0] sel = 4'b1010;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val numbers = tokens.collect { case n: NumberToken => n.value }
    numbers should contain("4")
    numbers.filter(_.contains("b")) should have length 1
  }

  test("Tokenize keywords") {
    val code = "module test (input clk, output result); endmodule"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val keywords = tokens.collect { case k: KeywordToken => k.value }
    keywords should contain("module")
    keywords should contain("input")
    keywords should contain("output")
    keywords should contain("endmodule")
  }

  test("Tokenize identifiers") {
    val code = "wire my_signal_123;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val identifiers = tokens.collect { case i: IdentifierToken => i.value }
    identifiers should contain("my_signal_123")
  }

  test("Tokenize strings") {
    val code = """msg = "Hello, World!";"""
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val strings = tokens.collect { case s: StringToken => s.value }
    strings should have length 1
    strings(0) should include("Hello")
  }

  test("Multiple operators") {
    val code = "sum = (a + b) * (c - d);"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val operators = tokens.collect { case o: OperatorToken => o.value }
    operators should contain("+")
    operators should contain("-")
    operators should contain("*")
  }

  test("Comparison operators") {
    val code = "if (a == b && c != d || e < f && g > h)"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val operators = tokens.collect { case o: OperatorToken => o.value }
    operators should contain("==")
    operators should contain("!=")
    operators should contain("&&")
    operators should contain("||")
    operators should contain("<")
    operators should contain(">")
  }

  test("Bitwise operators") {
    val code = "result = (a & b) | (c ^ d) | ~e;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val operators = tokens.collect { case o: OperatorToken => o.value }
    operators should contain("&")
    operators should contain("|")
    operators should contain("^")
    operators should contain("~")
  }

  test("Shift operators") {
    val code = "shifted = value << 2; shifted2 = value >> 3;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val operators = tokens.collect { case o: OperatorToken => o.value }
    operators should contain("<<")
    operators should contain(">>")
  }

  test("Special operators") {
    val code = "@(posedge clk) or @(negedge reset) -> out"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val operators = tokens.collect { case o: OperatorToken => o.value }
    operators should contain("@")
  }

  test("Mixed token types") {
    val code = "always @(posedge clk) counter <= counter + 1'b1;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val hasKeywords = tokens.exists { case k: KeywordToken => k.value == "always"; case _ => false }
    val hasOperators = tokens.exists { case o: OperatorToken => o.value == "<="; case _ => false }
    val hasNumbers = tokens.exists { case n: NumberToken => n.value == "1"; case _ => false }
    val hasIdentifiers = tokens.exists { case i: IdentifierToken => i.value == "counter"; case _ => false }

    hasKeywords should be(true)
    hasOperators should be(true)
    hasNumbers should be(true)
    hasIdentifiers should be(true)
  }

  test("EOF token at end") {
    val code = "wire test;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    tokens.last match {
      case _: EOFToken => // OK
      case _           => fail("Expected EOFToken at end")
    }
  }

  test("Tokenize word boundaries correctly") {
    val code = "reg_value and and_mask"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val identifiers = tokens.collect { case i: IdentifierToken => i.value }
    identifiers should contain("reg_value")
    identifiers should contain("and_mask")
  }

  test("Handle underscore in identifiers") {
    val code = "wire _private_signal; wire __double_underscore;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val identifiers = tokens.collect { case i: IdentifierToken => i.value }
    identifiers should contain("_private_signal")
    identifiers should contain("__double_underscore")
  }

  test("Handle dollar sign in identifiers") {
    val code = "wire $generate_signal; wire test$name;"
    val tokens = VerilogLexer.tokenizeFiltered(code)

    val identifiers = tokens.collect { case i: IdentifierToken => i.value }
    identifiers.exists(_.contains("$")) should be(true)
  }
}
