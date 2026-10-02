/*
 * Copyright 2012-2020 Udo Klimaschewski
 *
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.xulai.ArmorCurve.expression;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
public class Expression {
  public static final int OPERATOR_PRECEDENCE_UNARY = 60;
  public static final int OPERATOR_PRECEDENCE_EQUALITY = 7;
  public static final int OPERATOR_PRECEDENCE_COMPARISON = 10;
  public static final int OPERATOR_PRECEDENCE_OR = 2;
  public static final int OPERATOR_PRECEDENCE_AND = 4;
  public static final int OPERATOR_PRECEDENCE_POWER = 40;
  public static final int OPERATOR_PRECEDENCE_POWER_HIGHER = 80;
  public static final int OPERATOR_PRECEDENCE_MULTIPLICATIVE = 30;
  public static final int OPERATOR_PRECEDENCE_ADDITIVE = 20;
  public static final BigDecimal PI = new BigDecimal(
      "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");
  public static final BigDecimal e = new BigDecimal(
      "2.71828182845904523536028747135266249775724709369995957496696762772407663");
  public static final String MISSING_PARAMETERS_FOR_OPERATOR = "Missing parameter(s) for operator ";
  private MathContext mc;
  private int powerOperatorPrecedence = OPERATOR_PRECEDENCE_POWER;
  private String firstVarChars = "_";
  private String varChars = "_";
  private final String originalExpression;
  private String expressionString = null;
  private List<Token> rpn = null;
  protected Map<String, LazyOperator> operators = new TreeMap<String, LazyOperator>(
      String.CASE_INSENSITIVE_ORDER);
  protected Map<String, com.xulai.ArmorCurve.expression.LazyFunction> functions = new TreeMap<String, com.xulai.ArmorCurve.expression.LazyFunction>(
      String.CASE_INSENSITIVE_ORDER);
  protected Map<String, LazyNumber> variables = new TreeMap<String, LazyNumber>(
      String.CASE_INSENSITIVE_ORDER);
  private static final char DECIMAL_SEPARATOR = '.';
  private static final char MINUS_SIGN = '-';
  private static final LazyNumber PARAMS_START = new LazyNumber() {
    public BigDecimal eval() {
      return null;
    }
    public String getString() {
      return null;
    }
  };
  public static class ExpressionException extends RuntimeException {
    private static final long serialVersionUID = 1118142866870779047L;
    public ExpressionException(String message) {
      super(message);
    }
    public ExpressionException(String message, int characterPosition) {
      super(message + " at character position " + characterPosition);
    }
  }
  public interface LazyNumber {
    BigDecimal eval();
    String getString();
  }
  protected LazyNumber createLazyNumber(final BigDecimal bigDecimal) {
    return new LazyNumber() {
      @Override
      public String getString() {
        return bigDecimal.toPlainString();
      }
      @Override
      public BigDecimal eval() {
        return bigDecimal;
      }
    };
  }
  public abstract class LazyFunction extends AbstractLazyFunction {
    public LazyFunction(String name, int numParams, boolean booleanFunction) {
      super(name, numParams, booleanFunction);
    }
    public LazyFunction(String name, int numParams) {
      super(name, numParams);
    }
  }
  public abstract class Function extends AbstractFunction {
    public Function(String name, int numParams) {
      super(name, numParams);
    }
    public Function(String name, int numParams, boolean booleanFunction) {
      super(name, numParams, booleanFunction);
    }
  }
  public abstract class Operator extends AbstractOperator {
    public Operator(String oper, int precedence, boolean leftAssoc, boolean booleanOperator,
        boolean unaryOperator) {
      super(oper, precedence, leftAssoc, booleanOperator, unaryOperator);
    }
    public Operator(String oper, int precedence, boolean leftAssoc, boolean booleanOperator) {
      super(oper, precedence, leftAssoc, booleanOperator);
    }
    public Operator(String oper, int precedence, boolean leftAssoc) {
      super(oper, precedence, leftAssoc);
    }
  }
  public abstract class UnaryOperator extends AbstractUnaryOperator {
    public UnaryOperator(String oper, int precedence, boolean leftAssoc) {
      super(oper, precedence, leftAssoc);
    }
  }
  enum TokenType {
    VARIABLE, FUNCTION, LITERAL, OPERATOR, UNARY_OPERATOR, OPEN_PAREN, COMMA, CLOSE_PAREN, HEX_LITERAL, STRINGPARAM
  }
  public class Token {
    public String surface = "";
    public TokenType type;
    public int pos;
    public void append(char c) {
      surface += c;
    }
    public void append(String s) {
      surface += s;
    }
    public char charAt(int pos) {
      return surface.charAt(pos);
    }
    public int length() {
      return surface.length();
    }
    @Override
    public String toString() {
      return surface;
    }
  }
  private class Tokenizer implements Iterator<Token> {
    private int pos = 0;
    private String input;
    private Token previousToken;
    private Token nextToken = new Token();
    public Tokenizer(String input) {
      this.input = input.trim();
    }
    @Override
    public boolean hasNext() {
      return (pos < input.length());
    }
    private char peekNextChar() {
      if (pos < (input.length() - 1)) {
        return input.charAt(pos + 1);
      } else {
        return 0;
      }
    }
    private boolean isHexDigit(char ch) {
      return ch == 'x' || ch == 'X' || (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')
          || (ch >= 'A' && ch <= 'F');
    }
    @Override
    public Token next() {
      Token token = new Token();
      if (pos >= input.length()) {
        previousToken = null;
        return null;
      }
      char ch = input.charAt(pos);
      while (Character.isWhitespace(ch) && pos < input.length()) {
        ch = input.charAt(++pos);
      }
      token.pos = pos;
      if (pos < input.length() - 1) {
        nextToken.pos = pos + 1;
      } else {
        nextToken = null;
      }
      boolean isHex = false;
      if (Character.isDigit(ch) || (ch == DECIMAL_SEPARATOR && Character.isDigit(peekNextChar()))) {
        if (ch == '0' && (peekNextChar() == 'x' || peekNextChar() == 'X')) {
          isHex = true;
        }
        while ((isHex
            && isHexDigit(
            ch))
            || (Character.isDigit(ch) || ch == DECIMAL_SEPARATOR || ch == 'e' || ch == 'E'
            || (ch == MINUS_SIGN && token.length() > 0
            && ('e' == token.charAt(token.length() - 1)
            || 'E' == token.charAt(token.length() - 1)))
            || (ch == '+' && token.length() > 0
            && ('e' == token.charAt(token.length() - 1)
            || 'E' == token.charAt(token.length() - 1))))
            && (pos < input.length())) {
          token.append(input.charAt(pos++));
          ch = pos == input.length() ? 0 : input.charAt(pos);
        }
        token.type = isHex ? TokenType.HEX_LITERAL : TokenType.LITERAL;
      } else if (ch == '"') {
        pos++;
        if (previousToken.type != TokenType.STRINGPARAM) {
          ch = input.charAt(pos);
          while (ch != '"') {
            token.append(input.charAt(pos++));
            ch = pos == input.length() ? 0 : input.charAt(pos);
          }
          token.type = TokenType.STRINGPARAM;
        } else {
          return next();
        }
      } else if (Character.isLetter(ch) || firstVarChars.indexOf(ch) >= 0) {
        while ((Character.isLetter(ch) || Character.isDigit(ch) || varChars.indexOf(ch) >= 0
            || token.length() == 0 && firstVarChars.indexOf(ch) >= 0) && (pos < input.length())) {
          token.append(input.charAt(pos++));
          ch = pos == input.length() ? 0 : input.charAt(pos);
        }
        if (Character.isWhitespace(ch)) {
          while (Character.isWhitespace(ch) && pos < input.length()) {
            ch = input.charAt(pos++);
          }
          pos--;
        }
        if (operators.containsKey(token.surface)) {
          token.type = TokenType.OPERATOR;
        } else if (ch == '(') {
          token.type = TokenType.FUNCTION;
        } else {
          token.type = TokenType.VARIABLE;
        }
      } else if (ch == '(' || ch == ')' || ch == ',') {
        if (ch == '(') {
          token.type = TokenType.OPEN_PAREN;
        } else if (ch == ')') {
          token.type = TokenType.CLOSE_PAREN;
        } else {
          token.type = TokenType.COMMA;
        }
        token.append(ch);
        pos++;
      } else {
        String greedyMatch = "";
        int initialPos = pos;
        ch = input.charAt(pos);
        int validOperatorSeenUntil = -1;
        while (!Character.isLetter(ch) && !Character.isDigit(ch) && firstVarChars.indexOf(ch) < 0
            && !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != ','
            && (pos < input.length())) {
          greedyMatch += ch;
          pos++;
          if (operators.containsKey(greedyMatch)) {
            validOperatorSeenUntil = pos;
          }
          ch = pos == input.length() ? 0 : input.charAt(pos);
        }
        if (validOperatorSeenUntil != -1) {
          token.append(input.substring(initialPos, validOperatorSeenUntil));
          pos = validOperatorSeenUntil;
        } else {
          token.append(greedyMatch);
        }
        if (previousToken == null || (previousToken.type == TokenType.OPERATOR && !operators
            .get(previousToken.surface).isUnaryOperator())
            || previousToken.type == TokenType.OPEN_PAREN || previousToken.type == TokenType.COMMA
            || previousToken.type == TokenType.UNARY_OPERATOR) {
          token.surface += "u";
          token.type = TokenType.UNARY_OPERATOR;
        } else {
          token.type = TokenType.OPERATOR;
        }
      }
      previousToken = token;
      return token;
    }
    @Override
    public void remove() {
      throw new ExpressionException("remove() not supported");
    }
  }
  public Expression(String expression) {
    this(expression, MathContext.DECIMAL32);
  }
  public Expression(String expression, MathContext defaultMathContext) {
    this(expression, ExpressionSettings
        .builder()
        .mathContext(defaultMathContext)
        .build());
  }
  public Expression(String expression,
      ExpressionSettings expressionSettings) {
    this.mc = expressionSettings.getMathContext();
    this.powerOperatorPrecedence = expressionSettings.getPowerOperatorPrecedence();
    this.expressionString = expression;
    this.originalExpression = expression;
    addOperator(new Operator("+", OPERATOR_PRECEDENCE_ADDITIVE, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.add(v2, mc);
      }
    });
    addOperator(new Operator("-", OPERATOR_PRECEDENCE_ADDITIVE, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.subtract(v2, mc);
      }
    });
    addOperator(new Operator("*", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.multiply(v2, mc);
      }
    });
    addOperator(new Operator("/", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.divide(v2, mc);
      }
    });
    addOperator(new Operator("%", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.remainder(v2, mc);
      }
    });
    addOperator(new Operator("^", powerOperatorPrecedence, false) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        int signOf2 = v2.signum();
        double dn1 = v1.doubleValue();
        v2 = v2.multiply(new BigDecimal(signOf2));
        BigDecimal remainderOf2 = v2.remainder(BigDecimal.ONE);
        BigDecimal n2IntPart = v2.subtract(remainderOf2);
        BigDecimal intPow = v1.pow(n2IntPart.intValueExact(), mc);
        BigDecimal doublePow = BigDecimal.valueOf(Math.pow(dn1, remainderOf2.doubleValue()));
        BigDecimal result = intPow.multiply(doublePow, mc);
        if (signOf2 == -1) {
          result = BigDecimal.ONE.divide(result, mc.getPrecision(), RoundingMode.HALF_UP);
        }
        return result;
      }
    });
    addOperator(new Operator("&&", OPERATOR_PRECEDENCE_AND, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        boolean b1 = v1.compareTo(BigDecimal.ZERO) != 0;
        if (!b1) {
          return BigDecimal.ZERO;
        }
        boolean b2 = v2.compareTo(BigDecimal.ZERO) != 0;
        return b2 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator("||", OPERATOR_PRECEDENCE_OR, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        boolean b1 = v1.compareTo(BigDecimal.ZERO) != 0;
        if (b1) {
          return BigDecimal.ONE;
        }
        boolean b2 = v2.compareTo(BigDecimal.ZERO) != 0;
        return b2 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator(">", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.compareTo(v2) == 1 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator(">=", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.compareTo(v2) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator("<", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.compareTo(v2) == -1 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator("<=", OPERATOR_PRECEDENCE_COMPARISON, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return v1.compareTo(v2) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator("=", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        if (v1 == v2) {
          return BigDecimal.ONE;
        }
        if (v1 == null || v2 == null) {
          return BigDecimal.ZERO;
        }
        return v1.compareTo(v2) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator("==", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        return ((Operator) operators.get("=")).eval(v1, v2);
      }
    });
    addOperator(new Operator("!=", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        if (v1 == v2) {
          return BigDecimal.ZERO;
        }
        if (v1 == null || v2 == null) {
          return BigDecimal.ONE;
        }
        return v1.compareTo(v2) != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addOperator(new Operator("<>", OPERATOR_PRECEDENCE_EQUALITY, false, true) {
      @Override
      public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
        assertNotNull(v1, v2);
        return ((Operator) operators.get("!=")).eval(v1, v2);
      }
    });
    addOperator(new UnaryOperator("-", OPERATOR_PRECEDENCE_UNARY, false) {
      @Override
      public BigDecimal evalUnary(BigDecimal v1) {
        return v1.multiply(new BigDecimal(-1));
      }
    });
    addOperator(new UnaryOperator("+", OPERATOR_PRECEDENCE_UNARY, false) {
      @Override
      public BigDecimal evalUnary(BigDecimal v1) {
        return v1.multiply(BigDecimal.ONE);
      }
    });
    addFunction(new Function("FACT", 1, false) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        int number = parameters.get(0).intValue();
        BigDecimal factorial = BigDecimal.ONE;
        for (int i = 1; i <= number; i++) {
          factorial = factorial.multiply(new BigDecimal(i));
        }
        return factorial;
      }
    });
    addFunction(new Function("NOT", 1, true) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        boolean zero = parameters.get(0).compareTo(BigDecimal.ZERO) == 0;
        return zero ? BigDecimal.ONE : BigDecimal.ZERO;
      }
    });
    addLazyFunction(new LazyFunction("IF", 3) {
      @Override
      public LazyNumber lazyEval(List<LazyNumber> lazyParams) {
        return new LazyIfNumber(lazyParams);
      }
    });
    addFunction(new Function("RANDOM", 0) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        double d = Math.random();
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("SINR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.sin(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("COSR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.cos(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("TANR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.tan(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("COTR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.tan(parameters.get(0).doubleValue());
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("SECR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.cos(parameters.get(0).doubleValue());
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("CSCR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.sin(parameters.get(0).doubleValue());
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("SIN", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.sin(Math.toRadians(parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("COS", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.cos(Math.toRadians(parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("TAN", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.tan(Math.toRadians(parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("COT", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.tan(Math.toRadians(parameters.get(0).doubleValue()));
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("SEC", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.cos(Math.toRadians(parameters.get(0).doubleValue()));
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("CSC", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.sin(Math.toRadians(parameters.get(0).doubleValue()));
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("ASINR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.asin(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ACOSR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.acos(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ATANR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.atan(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ACOTR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        if (parameters.get(0).doubleValue() == 0) {
          throw new ExpressionException("Number must not be 0");
        }
        double d = Math.atan(1 / parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ATAN2R", 2) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0), parameters.get(1));
        double d = Math.atan2(parameters.get(0).doubleValue(), parameters.get(1).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ASIN", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.toDegrees(Math.asin(parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ACOS", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.toDegrees(Math.acos(parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ATAN", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.toDegrees(Math.atan(parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ACOT", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        if (parameters.get(0).doubleValue() == 0) {
          throw new ExpressionException("Number must not be 0");
        }
        double d = Math.toDegrees(Math.atan(1 / parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ATAN2", 2) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0), parameters.get(1));
        double d = Math.toDegrees(
            Math.atan2(parameters.get(0).doubleValue(), parameters.get(1).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("SINH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.sinh(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("COSH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.cosh(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("TANH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.tanh(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("SECH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.cosh(parameters.get(0).doubleValue());
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("CSCH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.sinh(parameters.get(0).doubleValue());
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("COTH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double one = 1;
        double d = Math.tanh(parameters.get(0).doubleValue());
        return new BigDecimal((one / d), mc);
      }
    });
    addFunction(new Function("ASINH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.log(parameters.get(0).doubleValue()
            + (Math.sqrt(Math.pow(parameters.get(0).doubleValue(), 2) + 1)));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ACOSH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        if (Double.compare(parameters.get(0).doubleValue(), 1) < 0) {
          throw new ExpressionException("Number must be x >= 1");
        }
        double d = Math.log(parameters.get(0).doubleValue()
            + (Math.sqrt(Math.pow(parameters.get(0).doubleValue(), 2) - 1)));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ATANH", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        if (Math.abs(parameters.get(0).doubleValue()) > 1
            || Math.abs(parameters.get(0).doubleValue()) == 1) {
          throw new ExpressionException("Number must be |x| < 1");
        }
        double d = 0.5
            * Math
            .log((1 + parameters.get(0).doubleValue()) / (1 - parameters.get(0).doubleValue()));
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("RAD", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.toRadians(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("DEG", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.toDegrees(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("MAX", -1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        if (parameters.isEmpty()) {
          throw new ExpressionException("MAX requires at least one parameter");
        }
        BigDecimal max = null;
        for (BigDecimal parameter : parameters) {
          assertNotNull(parameter);
          if (max == null || parameter.compareTo(max) > 0) {
            max = parameter;
          }
        }
        return max;
      }
    });
    addFunction(new Function("MIN", -1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        if (parameters.isEmpty()) {
          throw new ExpressionException("MIN requires at least one parameter");
        }
        BigDecimal min = null;
        for (BigDecimal parameter : parameters) {
          assertNotNull(parameter);
          if (min == null || parameter.compareTo(min) < 0) {
            min = parameter;
          }
        }
        return min;
      }
    });
    addFunction(new Function("ABS", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        return parameters.get(0).abs(mc);
      }
    });
    addFunction(new Function("LOG", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.log(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("LOG10", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        double d = Math.log10(parameters.get(0).doubleValue());
        return new BigDecimal(d, mc);
      }
    });
    addFunction(new Function("ROUND", 2) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0), parameters.get(1));
        BigDecimal toRound = parameters.get(0);
        int precision = parameters.get(1).intValue();
        return toRound.setScale(precision, mc.getRoundingMode());
      }
    });
    addFunction(new Function("FLOOR", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        BigDecimal toRound = parameters.get(0);
        return toRound.setScale(0, RoundingMode.FLOOR);
      }
    });
    addFunction(new Function("CEILING", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        BigDecimal toRound = parameters.get(0);
        return toRound.setScale(0, RoundingMode.CEILING);
      }
    });
    addFunction(new Function("SQRT", 1) {
      @Override
      public BigDecimal eval(List<BigDecimal> parameters) {
        assertNotNull(parameters.get(0));
        BigDecimal x = parameters.get(0);
        if (x.compareTo(BigDecimal.ZERO) == 0) {
          return new BigDecimal(0);
        }
        if (x.signum() < 0) {
          throw new ExpressionException("Argument to SQRT() function must not be negative");
        }
        BigInteger n = x.movePointRight(mc.getPrecision() << 1).toBigInteger();
        int bits = (n.bitLength() + 1) >> 1;
        BigInteger ix = n.shiftRight(bits);
        BigInteger ixPrev;
        BigInteger test;
        do {
          ixPrev = ix;
          ix = ix.add(n.divide(ix)).shiftRight(1);
          Thread.yield();
          test = ix.subtract(ixPrev).abs();
        } while (test.compareTo(BigInteger.ZERO) != 0 && test.compareTo(BigInteger.ONE) != 0);
        return new BigDecimal(ix, mc.getPrecision());
      }
    });
    variables.put("e", createLazyNumber(e));
    variables.put("PI", createLazyNumber(PI));
    variables.put("NULL", null);
    variables.put("TRUE", createLazyNumber(BigDecimal.ONE));
    variables.put("FALSE", createLazyNumber(BigDecimal.ZERO));
  }
  public static void assertNotNull(BigDecimal v1) {
    if (v1 == null) {
      throw new ArithmeticException("Operand may not be null");
    }
  }
  public static void assertNotNull(BigDecimal v1, BigDecimal v2) {
    if (v1 == null) {
      throw new ArithmeticException("First operand may not be null");
    }
    if (v2 == null) {
      throw new ArithmeticException("Second operand may not be null");
    }
  }
  protected boolean isNumber(String st) {
    if (st.charAt(0) == MINUS_SIGN && st.length() == 1) {
      return false;
    }
    if (st.charAt(0) == '+' && st.length() == 1) {
      return false;
    }
    if (st.charAt(0) == DECIMAL_SEPARATOR && (st.length() == 1 || !Character
        .isDigit(st.charAt(1)))) {
      return false;
    }
    if (st.charAt(0) == 'e' || st.charAt(0) == 'E') {
      return false;
    }
    for (char ch : st.toCharArray()) {
      if (!Character.isDigit(ch) && ch != MINUS_SIGN && ch != DECIMAL_SEPARATOR && ch != 'e'
          && ch != 'E'
          && ch != '+') {
        return false;
      }
    }
    return true;
  }
  @SuppressWarnings("incomplete-switch")
  private List<Token> shuntingYard(String expression) {
    List<Token> outputQueue = new ArrayList<Token>();
    Stack<Token> stack = new Stack<Token>();
    Tokenizer tokenizer = new Tokenizer(expression);
    Token lastFunction = null;
    Token previousToken = null;
    while (tokenizer.hasNext()) {
      Token token = tokenizer.next();
      switch (token.type) {
        case STRINGPARAM:
          stack.push(token);
          break;
        case LITERAL:
        case HEX_LITERAL:
          if (previousToken != null && (previousToken.type == TokenType.LITERAL
              || previousToken.type == TokenType.HEX_LITERAL)) {
            throw new ExpressionException("Missing operator", token.pos);
          }
          outputQueue.add(token);
          break;
        case VARIABLE:
          outputQueue.add(token);
          break;
        case FUNCTION:
          stack.push(token);
          lastFunction = token;
          break;
        case COMMA:
          if (previousToken != null && previousToken.type == TokenType.OPERATOR) {
            throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + previousToken,
                previousToken.pos);
          }
          while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN) {
            outputQueue.add(stack.pop());
          }
          if (stack.isEmpty()) {
            if (lastFunction == null) {
              throw new ExpressionException("Unexpected comma", token.pos);
            } else {
              throw new ExpressionException(
                  "Parse error for function " + lastFunction, token.pos);
            }
          }
          break;
        case OPERATOR: {
          if (previousToken != null && operators.containsKey(token.surface)
              && (previousToken.type == TokenType.COMMA
              || previousToken.type == TokenType.OPEN_PAREN)) {
            if (!operators.get(token.surface).isUnaryOperator()) {
              throw new ExpressionException(
                  MISSING_PARAMETERS_FOR_OPERATOR + token, token.pos);
            }
          }
          LazyOperator o1 = operators.get(token.surface);
          if (o1 == null) {
            throw new ExpressionException("Unknown operator " + token, token.pos + 1);
          }
          shuntOperators(outputQueue, stack, o1);
          stack.push(token);
          break;
        }
        case UNARY_OPERATOR: {
          if (previousToken != null && previousToken.type != TokenType.OPERATOR
              && previousToken.type != TokenType.COMMA && previousToken.type != TokenType.OPEN_PAREN
              && previousToken.type != TokenType.UNARY_OPERATOR) {
            throw new ExpressionException(
                "Invalid position for unary operator " + token, token.pos);
          }
          LazyOperator o1 = operators.get(token.surface);
          if (o1 == null) {
            throw new ExpressionException(
                "Unknown unary operator " + token.surface.substring(0, token.surface.length() - 1)
                , token.pos + 1);
          }
          shuntOperators(outputQueue, stack, o1);
          stack.push(token);
          break;
        }
        case OPEN_PAREN:
          if (previousToken != null) {
            if (previousToken.type == TokenType.LITERAL
                || previousToken.type == TokenType.CLOSE_PAREN
                || previousToken.type == TokenType.VARIABLE
                || previousToken.type == TokenType.HEX_LITERAL) {
              Token multiplication = new Token();
              multiplication.append("*");
              multiplication.type = TokenType.OPERATOR;
              stack.push(multiplication);
            }
            if (previousToken.type == TokenType.FUNCTION) {
              outputQueue.add(token);
            }
          }
          stack.push(token);
          break;
        case CLOSE_PAREN:
          if (previousToken != null && previousToken.type == TokenType.OPERATOR && !operators
              .get(previousToken.surface).isUnaryOperator()) {
            throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + previousToken,
                previousToken.pos);
          }
          while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN) {
            outputQueue.add(stack.pop());
          }
          if (stack.isEmpty()) {
            throw new ExpressionException("Mismatched parentheses");
          }
          stack.pop();
          if (!stack.isEmpty() && stack.peek().type == TokenType.FUNCTION) {
            outputQueue.add(stack.pop());
          }
      }
      previousToken = token;
    }
    while (!stack.isEmpty()) {
      Token element = stack.pop();
      if (element.type == TokenType.OPEN_PAREN || element.type == TokenType.CLOSE_PAREN) {
        throw new ExpressionException("Mismatched parentheses");
      }
      outputQueue.add(element);
    }
    return outputQueue;
  }
  private void shuntOperators(List<Token> outputQueue, Stack<Token> stack,
      LazyOperator o1) {
    Expression.Token nextToken = stack.isEmpty() ? null : stack.peek();
    while (nextToken != null
        && (nextToken.type == Expression.TokenType.OPERATOR
        || nextToken.type == Expression.TokenType.UNARY_OPERATOR)
        && (
        (o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(nextToken.surface).getPrecedence())
            || (o1.getPrecedence() < operators.get(nextToken.surface).getPrecedence()))) {
      outputQueue.add(stack.pop());
      nextToken = stack.isEmpty() ? null : stack.peek();
    }
  }
  public BigDecimal eval() {
    return eval(true);
  }
  public BigDecimal eval(boolean stripTrailingZeros) {
    Deque<LazyNumber> stack = new ArrayDeque<LazyNumber>();
    for (final Token token : getRPN()) {
      switch (token.type) {
        case UNARY_OPERATOR: {
          final LazyNumber value = stack.pop();
          LazyNumber result = new LazyNumber() {
            public BigDecimal eval() {
              return operators.get(token.surface).eval(value, null).eval();
            }
            @Override
            public String getString() {
              return String.valueOf(operators.get(token.surface).eval(value, null).eval());
            }
          };
          stack.push(result);
          break;
        }
        case OPERATOR:
          if (operators.get(token.surface).isUnaryOperator()) {
            final LazyNumber value = stack.pop();
            LazyNumber result = new LazyNumber() {
              public BigDecimal eval() {
                return operators.get(token.surface).eval(value, null).eval();
              }
              @Override
              public String getString() {
                return String.valueOf(operators.get(token.surface).eval(value, null).eval());
              }
            };
            stack.push(result);
            break;
          } else {
            final LazyNumber v1 = stack.pop();
            final LazyNumber v2 = stack.pop();
            LazyNumber result = new LazyNumber() {
              public BigDecimal eval() {
                return operators.get(token.surface).eval(v2, v1).eval();
              }
              public String getString() {
                return String.valueOf(operators.get(token.surface).eval(v2, v1).eval());
              }
            };
            stack.push(result);
            break;
          }
        case VARIABLE:
          if (!variables.containsKey(token.surface)) {
            throw new ExpressionException("Unknown operator or function: " + token);
          }
          stack.push(new LazyNumber() {
            public BigDecimal eval() {
              LazyNumber lazyVariable = variables.get(token.surface);
              BigDecimal value = lazyVariable == null ? null : lazyVariable.eval();
              return value == null ? null : value.round(mc);
            }
            public String getString() {
              LazyNumber lazyVariable = variables.get(token.surface);
              return lazyVariable.getString();
            }
          });
          break;
        case FUNCTION:
          com.xulai.ArmorCurve.expression.LazyFunction f = functions.get(token.surface.toUpperCase(Locale.ROOT));
          ArrayList<LazyNumber> p = new ArrayList<LazyNumber>(
              !f.numParamsVaries() ? f.getNumParams() : 0);
          while (!stack.isEmpty() && stack.peek() != PARAMS_START) {
            p.add(0, stack.pop());
          }
          if (stack.peek() == PARAMS_START) {
            stack.pop();
          }
          LazyNumber fResult = f.lazyEval(p);
          stack.push(fResult);
          break;
        case OPEN_PAREN:
          stack.push(PARAMS_START);
          break;
        case LITERAL:
          stack.push(new LazyNumber() {
            public BigDecimal eval() {
              if (token.surface.equalsIgnoreCase("NULL")) {
                return null;
              }
              return new BigDecimal(token.surface, mc);
            }
            public String getString() {
              return String.valueOf(new BigDecimal(token.surface, mc));
            }
          });
          break;
        case STRINGPARAM:
          stack.push(new LazyNumber() {
            public BigDecimal eval() {
              return null;
            }
            public String getString() {
              return token.surface;
            }
          });
          break;
        case HEX_LITERAL:
          stack.push(new LazyNumber() {
            public BigDecimal eval() {
              return new BigDecimal(new BigInteger(token.surface.substring(2), 16), mc);
            }
            public String getString() {
              return new BigInteger(token.surface.substring(2), 16).toString();
            }
          });
          break;
        default:
          throw new ExpressionException(
              "Unexpected token " + token.surface, token.pos);
      }
    }
    BigDecimal result = stack.pop().eval();
    if (result == null) {
      return null;
    }
    if (stripTrailingZeros) {
      result = result.stripTrailingZeros();
    }
    return result;
  }
  public Expression setPrecision(int precision) {
    this.mc = new MathContext(precision);
    return this;
  }
  public Expression setRoundingMode(RoundingMode roundingMode) {
    this.mc = new MathContext(mc.getPrecision(), roundingMode);
    return this;
  }
  public Expression setFirstVariableCharacters(String chars) {
    this.firstVarChars = chars;
    return this;
  }
  public Expression setVariableCharacters(String chars) {
    this.varChars = chars;
    return this;
  }
  @SuppressWarnings("unchecked")
  public <OPERATOR extends LazyOperator> OPERATOR addOperator(OPERATOR operator) {
    String key = operator.getOper();
    if (operator instanceof AbstractUnaryOperator) {
      key += "u";
    }
    return (OPERATOR) operators.put(key, operator);
  }
  public com.xulai.ArmorCurve.expression.Function addFunction(com.xulai.ArmorCurve.expression.Function function) {
    return (com.xulai.ArmorCurve.expression.Function) functions.put(function.getName(), function);
  }
  public com.xulai.ArmorCurve.expression.LazyFunction addLazyFunction(com.xulai.ArmorCurve.expression.LazyFunction function) {
    return functions.put(function.getName(), function);
  }
  public Expression setVariable(String variable, BigDecimal value) {
    return setVariable(variable, createLazyNumber(value));
  }
  public Expression setVariable(String variable, LazyNumber value) {
    variables.put(variable, value);
    return this;
  }
  public Expression setVariable(String variable, String value) {
    if (isNumber(value)) {
      variables.put(variable, createLazyNumber(new BigDecimal(value, mc)));
    } else if (value.equalsIgnoreCase("null")) {
      variables.put(variable, null);
    } else {
      final String expStr = value;
      variables.put(variable, new LazyNumber() {
        private final Map<String, LazyNumber> outerVariables = variables;
        private final Map<String, com.xulai.ArmorCurve.expression.LazyFunction> outerFunctions = functions;
        private final Map<String, LazyOperator> outerOperators = operators;
        private final String innerExpressionString = expStr;
        private final MathContext inneMc = mc;
        @Override
        public String getString() {
          return innerExpressionString;
        }
        @Override
        public BigDecimal eval() {
          Expression innerE = new Expression(innerExpressionString, inneMc);
          innerE.variables = outerVariables;
          innerE.functions = outerFunctions;
          innerE.operators = outerOperators;
          return innerE.eval();
        }
      });
      rpn = null;
    }
    return this;
  }
  private Expression createEmbeddedExpression(final String expression) {
    final Map<String, LazyNumber> outerVariables = variables;
    final Map<String, com.xulai.ArmorCurve.expression.LazyFunction> outerFunctions = functions;
    final Map<String, LazyOperator> outerOperators = operators;
    final MathContext inneMc = mc;
    Expression exp = new Expression(expression, inneMc);
    exp.variables = outerVariables;
    exp.functions = outerFunctions;
    exp.operators = outerOperators;
    return exp;
  }
  public Expression with(String variable, BigDecimal value) {
    return setVariable(variable, value);
  }
  public Expression with(String variable, LazyNumber value) {
    return setVariable(variable, value);
  }
  public Expression and(String variable, String value) {
    return setVariable(variable, value);
  }
  public Expression and(String variable, BigDecimal value) {
    return setVariable(variable, value);
  }
  public Expression and(String variable, LazyNumber value) {
    return setVariable(variable, value);
  }
  public Expression with(String variable, String value) {
    return setVariable(variable, value);
  }
  public Iterator<Token> getExpressionTokenizer() {
    final String expression = this.expressionString;
    return new Tokenizer(expression);
  }
  private List<Token> getRPN() {
    if (rpn == null) {
      rpn = shuntingYard(this.expressionString);
      validate(rpn);
    }
    return rpn;
  }
  private void validate(List<Token> rpn) {
    Stack<Integer> stack = new Stack<Integer>();
    stack.push(0);
    for (final Token token : rpn) {
      switch (token.type) {
        case UNARY_OPERATOR:
          if (stack.peek() < 1) {
            throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + token);
          }
          break;
        case OPERATOR:
          LazyOperator op = operators.get(token.surface);
          int numberOperands = 2;
          if (op.isUnaryOperator()) {
            numberOperands = 1;
          }
          if (stack.peek() < numberOperands) {
            throw new ExpressionException(MISSING_PARAMETERS_FOR_OPERATOR + token);
          }
          if (numberOperands > 1) {
            stack.set(stack.size() - 1, stack.peek() - numberOperands + 1);
          }
          break;
        case FUNCTION:
          com.xulai.ArmorCurve.expression.LazyFunction f = functions.get(token.surface.toUpperCase(Locale.ROOT));
          if (f == null) {
            throw new ExpressionException("Unknown function " + token, token.pos + 1);
          }
          int numParams = stack.pop();
          if (!f.numParamsVaries() && numParams != f.getNumParams()) {
            throw new ExpressionException(
                "Function " + token + " expected " + f.getNumParams() + " parameters, got "
                    + numParams);
          }
          if (stack.isEmpty()) {
            throw new ExpressionException("Too many function calls, maximum scope exceeded");
          }
          stack.set(stack.size() - 1, stack.peek() + 1);
          break;
        case OPEN_PAREN:
          stack.push(0);
          break;
        default:
          stack.set(stack.size() - 1, stack.peek() + 1);
      }
    }
    if (stack.size() > 1) {
      throw new ExpressionException("Too many unhandled function parameter lists");
    } else if (stack.peek() > 1) {
      throw new ExpressionException("Too many numbers or variables");
    } else if (stack.peek() < 1) {
      throw new ExpressionException("Empty expression");
    }
  }
  public String toRPN() {
    StringBuilder result = new StringBuilder();
    for (Token t : getRPN()) {
      if (result.length() != 0) {
        result.append(" ");
      }
      if (t.type == TokenType.VARIABLE && variables.containsKey(t.surface)) {
        LazyNumber innerVariable = variables.get(t.surface);
        String innerExp = innerVariable.getString();
        if (isNumber(innerExp)) {
          result.append(t.toString());
        } else {
          Expression exp = createEmbeddedExpression(innerExp);
          String nestedExpRpn = exp.toRPN();
          result.append(nestedExpRpn);
        }
      } else {
        result.append(t.toString());
      }
    }
    return result.toString();
  }
  public Set<String> getDeclaredVariables() {
    return Collections.unmodifiableSet(variables.keySet());
  }
  public Set<String> getDeclaredOperators() {
    return Collections.unmodifiableSet(operators.keySet());
  }
  public Set<String> getDeclaredFunctions() {
    return Collections.unmodifiableSet(functions.keySet());
  }
  public String getExpression() {
    return expressionString;
  }
  public List<String> getUsedVariables() {
    List<String> result = new ArrayList<String>();
    Tokenizer tokenizer = new Tokenizer(expressionString);
    while (tokenizer.hasNext()) {
      Token nextToken = tokenizer.next();
      String token = nextToken.toString();
      if (nextToken.type != TokenType.VARIABLE || token.equals("PI") || token.equals("e") || token
          .equals("TRUE")
          || token.equals("FALSE")) {
        continue;
      }
      result.add(token);
    }
    return result;
  }
  public String getOriginalExpression() {
    return this.originalExpression;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Expression that = (Expression) o;
    if (this.expressionString == null) {
      return that.expressionString == null;
    } else {
      return this.expressionString.equals(that.expressionString);
    }
  }
  @Override
  public int hashCode() {
    return this.expressionString == null ? 0 : this.expressionString.hashCode();
  }
  @Override
  public String toString() {
    return this.expressionString;
  }
  public boolean isBoolean() {
    List<Token> rpnList = getRPN();
    if (!rpnList.isEmpty()) {
      for (int i = rpnList.size() - 1; i >= 0; i--) {
        Token t = rpnList.get(i);
        if (t.surface.equals("IF")) {
          continue;
        }
        if (t.type == TokenType.FUNCTION) {
          return functions.get(t.surface).isBooleanFunction();
        } else if (t.type == TokenType.OPERATOR) {
          return operators.get(t.surface).isBooleanOperator();
        }
      }
    }
    return false;
  }
  public List<String> infixNotation() {
    final List<String> infix = new ArrayList<String>();
    Tokenizer tokenizer = new Tokenizer(expressionString);
    while (tokenizer.hasNext()) {
      Token token = tokenizer.next();
      String infixNotation = "{" + token.type + ":" + token.surface + "}";
      infix.add(infixNotation);
    }
    return infix;
  }
}