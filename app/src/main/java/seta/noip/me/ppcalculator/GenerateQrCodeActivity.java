package seta.noip.me.ppcalculator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GenerateQrCodeActivity extends AppCompatActivity {
    static final int AMOUNT_REQUEST = 1;

    @BindView(R.id.textView) TextView textView;
    @BindView(R.id.textView2) TextView textView2;
    @BindView(R.id.btnCalculator) Button btnCalculator;
    @BindView(R.id.btnShare) Button btnShare;
    @BindView(R.id.imageView) ImageView qrImageView;

    BigDecimal amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr_code);
        ButterKnife.bind(this);

        setupHandlers();

        amount = (BigDecimal) getIntent().getSerializableExtra("amount");
        if (null == amount) amount = BigDecimal.ZERO;

        setupQrImage();
    }

    private void setupQrImage() {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            String content = PromptPayQR.payloadMoneyTransfer(
                    PromptPayQR.BOT_ID_MERCHANT_TAX_ID, "3100602152163", amount);

            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {

                    bmp.setPixel(x , y, bitMatrix.get(x,y) ? Color.BLACK : Color.WHITE);
                }
            }

            textView.setText(amount.toString());
            qrImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void setupHandlers() {
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
            setupQrImage();
        }
    }
}
