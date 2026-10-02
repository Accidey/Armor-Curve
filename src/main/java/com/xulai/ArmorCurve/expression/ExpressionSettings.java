package com.xulai.ArmorCurve.expression;
import java.math.MathContext;
public class ExpressionSettings {
  private MathContext mathContext;
  private int powerOperatorPrecedence;
  private ExpressionSettings() {
  }
  public ExpressionSettings(MathContext mathContext, int powerOperatorPrecedence) {
    this.mathContext = mathContext;
    this.powerOperatorPrecedence = powerOperatorPrecedence;
  }
  public MathContext getMathContext() {
    return mathContext;
  }
  public int getPowerOperatorPrecedence() {
    return powerOperatorPrecedence;
  }
  public static Builder builder() {
    return new Builder();
  }
  public static class Builder {
    private MathContext mathContext = MathContext.DECIMAL32;
    private int powerOperatorPrecedence = Expression.OPERATOR_PRECEDENCE_POWER;
    public Builder mathContext(MathContext mathContext) {
      this.mathContext = mathContext;
      return this;
    }
    public Builder powerOperatorPrecedenceHigher() {
      this.powerOperatorPrecedence = Expression.OPERATOR_PRECEDENCE_POWER_HIGHER;
      return this;
    }
    public Builder powerOperatorPrecedence(int powerOperatorPrecedence) {
      this.powerOperatorPrecedence = powerOperatorPrecedence;
      return this;
    }
    public ExpressionSettings build() {
      return new ExpressionSettings(mathContext, powerOperatorPrecedence);
    }
  }
}