package seta.noip.me.ppcalculator;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GenerateQrCodeActivity extends AppCompatActivity {
    static final int AMOUNT_REQUEST = 1;

    @BindView(R.id.txtAmount) TextView txtAmount;
    @BindView(R.id.btnCalculator) Button btnCalculator;

    BigDecimal amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr_code);
        ButterKnife.bind(this);

        amount = (BigDecimal) getIntent().getSerializableExtra("amount");
        if (null == amount) amount = BigDecimal.ZERO;

        txtAmount.setText(amount.toString());

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pass the amount back to calculator if any, and prepare to receive new amount back
                Intent i = new Intent();
                i.putExtra("amount", amount);
                i.setClassName("seta.noip.me.ppcalculator",
                        "seta.noip.me.ppcalculator.CalculatorActivity");
                startActivityForResult(i, AMOUNT_REQUEST);
            }
        };
        btnCalculator.setOnClickListener(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == AMOUNT_REQUEST) {
            amount = (BigDecimal) data.getSerializableExtra("amount");
            if (null == amount) amount = BigDecimal.ZERO;
            txtAmount.setText(amount.toString());
        }
    }
}
