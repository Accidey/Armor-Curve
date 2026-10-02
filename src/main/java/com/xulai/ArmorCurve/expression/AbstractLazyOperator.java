/*
 * Copyright 2018 Udo Klimaschewski
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
public abstract class AbstractLazyOperator implements LazyOperator {
  protected String oper;
  protected int precedence;
  protected boolean leftAssoc;
  protected boolean booleanOperator = false;
  protected boolean unaryOperator;
  protected AbstractLazyOperator(String oper, int precedence, boolean leftAssoc,
      boolean booleanOperator, boolean unaryOperator) {
    this.oper = oper;
    this.precedence = precedence;
    this.leftAssoc = leftAssoc;
    this.booleanOperator = booleanOperator;
    this.unaryOperator = unaryOperator;
  }
  protected AbstractLazyOperator(String oper, int precedence, boolean leftAssoc,
      boolean booleanOperator) {
    this.oper = oper;
    this.precedence = precedence;
    this.leftAssoc = leftAssoc;
    this.booleanOperator = booleanOperator;
    this.unaryOperator = false;
  }
  protected AbstractLazyOperator(String oper, int precedence, boolean leftAssoc) {
    this.oper = oper;
    this.precedence = precedence;
    this.leftAssoc = leftAssoc;
    this.unaryOperator = false;
  }
  public String getOper() {
    return oper;
  }
  public int getPrecedence() {
    return precedence;
  }
  public boolean isLeftAssoc() {
    return leftAssoc;
  }
  public boolean isBooleanOperator() {
    return booleanOperator;
  }
  public boolean isUnaryOperator() {
    return unaryOperator;
  }
}