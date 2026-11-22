package com.xulai.ArmorCurve;

import com.xulai.ArmorCurve.expression.Expression;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class Formula {
    private final String text;
    private final ThreadLocal<Expression> threadExpression;
    private final AtomicLong failures = new AtomicLong();
    private final boolean constant;
    private final BigDecimal constantValue;

    private Formula(String text) {
        this.text = text;
        this.threadExpression = ThreadLocal.withInitial(() -> new Expression(text));
        BigDecimal probed = probeConstant(text);
        this.constant = probed != null;
        this.constantValue = probed;
    }

    static Formula compile(String text, String fallback, Map<String, BigDecimal> probe) {
        try {
            Expression candidate = new Expression(text);
            probe.forEach(candidate::with);
            BigDecimal result = candidate.eval();
            if (!Double.isFinite(result.doubleValue())) {
                throw new ArithmeticException("Formula \"" + text + "\" evaluates to a non-finite value: " + result.toPlainString());
            }
            return new Formula(text);
        } catch (RuntimeException exception) {
            ArmorCurve.LOGGER.warn("Formula \"{}\" is unusable ({}), falling back to \"{}\".", text, exception.getMessage(), fallback);
            return new Formula(fallback);
        }
    }

    private static BigDecimal probeConstant(String text) {
        try {
            BigDecimal result = new Expression(text).eval();
            return Double.isFinite(result.doubleValue()) ? result : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public String text() {
        return text;
    }

    public boolean passthrough(String variable) {
        return text.trim().equalsIgnoreCase(variable);
    }

    public boolean isOne() {
        return constant && constantValue.compareTo(BigDecimal.ONE) == 0;
    }

    public BigDecimal evaluate(String first, BigDecimal firstValue, String second, BigDecimal secondValue) {
        return threadExpression.get().with(first, firstValue).and(second, secondValue).eval();
    }

    public BigDecimal evaluate(String first, BigDecimal firstValue, String second, BigDecimal secondValue, String third, BigDecimal thirdValue) {
        return threadExpression.get().with(first, firstValue).and(second, secondValue).and(third, thirdValue).eval();
    }

    public void reportFailure(RuntimeException exception) {
        long count = failures.incrementAndGet();
        if (count == 1L || count % 100L == 0L) {
            ArmorCurve.LOGGER.warn("Formula \"{}\" failed {} time(s): {}. The untouched damage is kept for this hit.", text, count, exception.getMessage());
        }
    }
}
