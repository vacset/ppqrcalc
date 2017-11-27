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
    // contain the latest proper number, between the operator, or after the last operator
    private StringBuilder currentNumber = new StringBuilder();
    private String lastSymbol = null;
    private final String unfinishedFormulaEndingSymbols;
    private boolean invalidEvalBuffer = false;
    private BigDecimal evalCache = null;

    public Formula(Context context) {
        this.ctx = context;
        unfinishedFormulaEndingSymbols = this.ctx.getString(R.string.unfinishedFormulaEndingSymbols);
    }

    public Formula(Context context, @NonNull String initialFormula) {
        this(context);
        try {
            evalBuffer = new StringBuilder(initialFormula);
            initialFormula = initialFormula.
                    replaceAll("\\*", context.getString(R.string.multiplicationSign));
            initialFormula = initialFormula.
                    replaceAll("/", context.getString(R.string.divisionSign));
            displayBuffer = new StringBuilder(initialFormula);
            eval();
            String[] elem = evalBuffer.toString().split("/*-+");
            for (int i =  elem.length-1; i >= 0; i--) {
                if (elem[i].length() > 0) {
                    currentNumber.append(elem[i]);
                    break;
                }
            }
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

        // special case for dot. If current number already has dot, don't add it
        if (".".equals(s) && currentNumber.lastIndexOf(".") >= 0) {
            return;
        }

        if (unfinishedFormulaEndingSymbols.contains(s)) {
            if (displayBuffer.length() == 0) { return; }
            if (lastSymbol == null) {
                lastSymbol = s;
                if (".".equals(s)) {
                    currentNumber.append(s);
                }
            }
            else {
                // do not allow adding more symbol if the formular already ends with symbol
            }
        }
        else {
            // number is appended
            evalCache = null;
            if (lastSymbol != null) {
                if (lastSymbol.equals("*")) {
                    displayBuffer.append(ctx.getString(R.string.multiplicationSign));
                } else if (lastSymbol.equals("/")) {
                    displayBuffer.append(ctx.getString(R.string.divisionSign));
                } else {
                    displayBuffer.append(lastSymbol);
                }
                evalBuffer.append(lastSymbol);
                if (!".".equals(lastSymbol)) {
                    currentNumber = new StringBuilder();
                }
            }
            displayBuffer.append(s);
            evalBuffer.append(s);
            lastSymbol = null;
            currentNumber.append(s);
        }
    }

    public void clear() {
        displayBuffer = new StringBuilder();
        evalBuffer = new StringBuilder();
        currentNumber = new StringBuilder();
        lastSymbol = null;
        invalidEvalBuffer = false;
        evalCache = null;
    }

    public boolean isInvalid() {
        return invalidEvalBuffer;
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
        if (evalCache != null) {
            return evalCache;

        }
        if (evalBuffer.length() == 0) {
            return BigDecimal.ZERO;
        }

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
            evalCache = ret;
            invalidEvalBuffer = false;
            return ret;
        } catch(ArithmeticException caught) {
            if (LogConfig.LOG) {
                Log.e(getClass().getName(), evalBuffer.toString(), caught);
            }
            invalidEvalBuffer = true;
            return BigDecimal.ZERO;
        } catch(IllegalArgumentException caught) {
            if (LogConfig.LOG) {
                Log.e(getClass().getName(), evalBuffer.toString(), caught);
            }
            invalidEvalBuffer = true;
            return BigDecimal.ZERO;
        }
    }
}
