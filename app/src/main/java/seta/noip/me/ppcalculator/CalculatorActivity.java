package seta.noip.me.ppcalculator;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.View;

import java.math.BigDecimal;
import java.text.NumberFormat;

import butterknife.BindInt;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Simple calculator that can display PromptPay QR code
 * I added formula display, intent return value, and qr intent invocation
 */
public class CalculatorActivity extends AppCompatActivity {

    // IDs of all the numeric buttons
    private int[] numericButtons = {R.id.btnZero, R.id.btnOne, R.id.btnTwo, R.id.btnThree, R.id.btnFour, R.id.btnFive, R.id.btnSix, R.id.btnSeven, R.id.btnEight, R.id.btnNine};
    // IDs of all the operator buttons
    private int[] operatorButtons = {R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide};
    // TextView used to display the output
    @BindView(R.id.txtFormula) TextView txtFormula;
    @BindView(R.id.txtResult) TextView txtResult;
    @BindView(R.id.btnQr) ImageButton btnQr;
    @BindInt(R.integer.formula_limit) int formulaLimit;
    @BindString(R.string.unfinishedFormulaEndingSymbols) String unfinishedFormulaEndingSymbols;

    // flag whether this activity is started by someone else who need result or not
    private boolean returnResult = false;
    // current valid calculation result
    private Formula formula;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);
        ButterKnife.bind(this);

        // an initial value is passed in, populate it in formula
        BigDecimal amount = (BigDecimal) getIntent().getSerializableExtra("amount");
        if (null != amount) {
            formula = new Formula(this, amount.toString());
        }
        else {
            formula = new Formula(this);
        }
        txtFormula.setText(formula.display());
        txtResult.setText(formatAmount());
        returnResult = (null != getCallingActivity());

        // Find and set OnClickListener to numeric buttons
        setNumericOnClickListener();
        // Find and set OnClickListener to operator buttons, equal button and decimal point button
        setOperatorOnClickListener();
        
        setQrClickListener();
    }

    private void setQrClickListener() {

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BigDecimal amount = formula.eval();
                if (formula.isInvalid()) return;
                // pass the amount to QR generator

                Intent i = new Intent();
                i.putExtra("amount", amount);
                i.setClassName(GenerateQrCodeActivity.class.getPackage().getName(),
                        GenerateQrCodeActivity.class.getCanonicalName());

                // it is called first as calculator. when click QR button, just start the new QR activity
                if (returnResult) {
                    setResult(Activity.RESULT_OK, i);
                    finish();
                } else {
                    startActivity(i);
                }
            }
        };
        btnQr.setOnClickListener(listener);
    }

    /**
     * Find and set OnClickListener to numeric buttons.
     */
    private void setNumericOnClickListener() {
        // Create a common OnClickListener
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Just append/set the text of clicked button
                Button button = (Button) v;
                CharSequence s = button.getText();
                if (txtFormula.length() + s.length() > formulaLimit) {
                    Animation flasher = new AlphaAnimation(0.0f, 1.0f);
                    flasher.setDuration(50);
                    flasher.setStartOffset(0);
                    flasher.setRepeatMode(Animation.REVERSE);
                    flasher.setRepeatCount(1);
                    txtFormula.startAnimation(flasher);
                } else {
                    formula.append(s.toString());
                    txtFormula.setText(formula.display());
                    txtResult.setText(formatAmount());
                    if (formula.isInvalid()) {
                        txtResult.setText(R.string.error_label);
                    }
                }
            }
        };
        // Assign the listener to all the numeric buttons
        for (int id : numericButtons) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    /**
     * Find and set OnClickListener to operator buttons, equal button and decimal point button.
     */
    private void setOperatorOnClickListener() {
        // Create a common OnClickListener for operators
        // Create a common OnClickListener
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Just append/set the text of clicked button
                Button button = (Button) v;
                String s = button.getTag().toString();
                if (txtFormula.length() + s.length() > formulaLimit) {
                    Animation flasher = new AlphaAnimation(0.0f, 1.0f);
                    flasher.setDuration(50);
                    flasher.setStartOffset(0);
                    flasher.setRepeatMode(Animation.REVERSE);
                    flasher.setRepeatCount(1);
                    txtFormula.startAnimation(flasher);
                } else {
                    formula.append(s);
                    txtFormula.setText(formula.display());
                    txtResult.setText(formatAmount());
                    if (formula.isInvalid()) {
                        txtResult.setText(R.string.error_label);
                    }
                }
            }
        };
        // Assign the listener to all the operator buttons
        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(listener);
        }
        // Decimal point
        findViewById(R.id.btnDot).setOnClickListener(listener);
        // Clear button
        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                formula.clear();
                txtFormula.setText(formula.display());
                txtResult.setText(formatAmount());
            }
        });
        // Equal button
        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqual();
            }
        });
    }

    /**
     * Logic to calculate the solution.
     */
    private void onEqual() {
        BigDecimal amount = formula.eval();
        if (formula.isInvalid()) {
            return;
        }

        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(false);
        formula = new Formula(this, nf.format(amount));
        txtFormula.setText(formula.display());
        txtResult.setText(formatAmount());
    }

    private String formatAmount () {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        return nf.format(formula.eval());
    }

}
