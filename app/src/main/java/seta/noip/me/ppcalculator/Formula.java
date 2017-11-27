package seta.noip.me.ppcalculator;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * Represent current math formula input by user.
 * Handles the evaluation to numeric value (or NaN)
 * as well as display proper String representing the formula
 *
 * Created by vachi on 24-Nov-17.
 */
class Formula {
    private Context ctx;
    private StringBuilder displayBuffer = new StringBuilder();
    private StringBuilder evalBuffer = new StringBuilder();
    private String lastSymbol = null;
    private final String unfinishedFormulaEndingSymbols;

    public Formula(Context context) {
        this.ctx = context;
        unfinishedFormulaEndingSymbols = this.ctx.getString(R.string.unfinishedFormulaEndingSymbols);
    }

    public Formula(Context context, @NonNull String initialFormula) {
        this(context);
        try {
            Expression expression = new ExpressionBuilder(initialFormula).build();
            expression.evaluate();
            evalBuffer = new StringBuilder(initialFormula);
            displayBuffer = new StringBuilder(initialFormula.
                    replaceAll("\\*", context.getString(R.string.multiplicationSign)).
                    replaceAll("/", context.getString(R.string.divisionSign)));
        } catch(ArithmeticException caught) {
            if (LogConfig.LOG) {
                Log.e(getClass().getName(), evalBuffer.toString(), caught);
            }
        } catch(IllegalArgumentException caught) {
            if (LogConfig.LOG) {
                Log.e(getClass().getName(), evalBuffer.toString(), caught);
            }
        }
    }

    public void append(@NonNull String s) {
        if (s.length() > 1) {
            throw new IllegalArgumentException("only accept 1 char, but got " + s);
        }

        // special case for minus sign as first character
        if ("-".equals(s) && lastSymbol == null && displayBuffer.length() == 0) {
            lastSymbol = s;
            return;
        }

        if (unfinishedFormulaEndingSymbols.contains(s)) {
            if (displayBuffer.length() == 0) { return; }
            if (lastSymbol == null) {
                lastSymbol = s;
            }
            else {
                // do not allow adding more symbol if the formular already ends with symbol
            }
        }
        else {
            if (lastSymbol != null) {
                if (lastSymbol.equals("*")) {
                    displayBuffer.append(ctx.getString(R.string.multiplicationSign));
                } else if (lastSymbol.equals("/")) {
                    displayBuffer.append(ctx.getString(R.string.divisionSign));
                } else {
                    displayBuffer.append(lastSymbol);
                }
                evalBuffer.append(lastSymbol);
            }
            displayBuffer.append(s);
            evalBuffer.append(s);
            lastSymbol = null;
        }
    }

    public void clear() {
        displayBuffer = new StringBuilder();
        evalBuffer = new StringBuilder();
        lastSymbol = null;
    }

    public String display() {
        if (lastSymbol == null) {
            return displayBuffer.toString();
        } else if (lastSymbol.equals("*")) {
            return displayBuffer.toString() + ctx.getString(R.string.multiplicationSign);
        } else if (lastSymbol.equals("/")) {
            return displayBuffer.toString() + ctx.getString(R.string.divisionSign);
        } else {
            return displayBuffer.toString() + lastSymbol;
        }
    }

    public BigDecimal eval() throws ArithmeticException {
        // Read the expression
        String txt = evalBuffer.toString();
        // Calculate the result and display
        try {
            Expression expression = new ExpressionBuilder(txt).build();
            double result = expression.evaluate();
            BigDecimal ret = new BigDecimal(result);
            ret = ret.setScale(2, RoundingMode.HALF_UP);
            NumberFormat nf = NumberFormat.getInstance();
            nf.setGroupingUsed(true);
            return ret;
        } catch(ArithmeticException caught) {
            if (LogConfig.LOG) {
                Log.e(getClass().getName(), evalBuffer.toString(), caught);
            }
            return BigDecimal.ZERO;
        } catch(IllegalArgumentException caught) {
            if (LogConfig.LOG) {
                Log.e(getClass().getName(), evalBuffer.toString(), caught);
            }
            return BigDecimal.ZERO;
        }
    }
}
