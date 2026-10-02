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
import com.xulai.ArmorCurve.expression.Expression.LazyNumber;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
public abstract class AbstractFunction extends AbstractLazyFunction implements Function {
  protected AbstractFunction(String name, int numParams) {
    super(name, numParams);
  }
  protected AbstractFunction(String name, int numParams, boolean booleanFunction) {
    super(name, numParams, booleanFunction);
  }
  public LazyNumber lazyEval(final List<LazyNumber> lazyParams) {
    return new LazyNumber() {
      private List<BigDecimal> params;
      public BigDecimal eval() {
        return AbstractFunction.this.eval(getParams());
      }
      public String getString() {
        return String.valueOf(AbstractFunction.this.eval(getParams()));
      }
      private List<BigDecimal> getParams() {
        if (params == null) {
          params = new ArrayList<BigDecimal>();
          for (LazyNumber lazyParam : lazyParams) {
            params.add(lazyParam.eval());
          }
        }
        return params;
      }
    };
  }
}