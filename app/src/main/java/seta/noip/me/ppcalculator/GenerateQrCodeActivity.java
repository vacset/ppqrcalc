package seta.noip.me.ppcalculator;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GenerateQrCodeActivity extends AppCompatActivity {
    static final int AMOUNT_REQUEST = 1;

    @BindView(R.id.proxyInfoTextView) TextView proxyInfoTextView;
    @BindView(R.id.amountInfoTextView) TextView amountInfoTextView;
    @BindView(R.id.textView2) TextView textView2;
    @BindView(R.id.btnCalculator) ImageButton btnCalculator;
    @BindView(R.id.btnShare) ImageButton btnShare;
    @BindView(R.id.qrImageView) ImageView qrImageView;

    private BigDecimal amount;
    private Bitmap newImage;
    private String crc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr_code);
        ButterKnife.bind(this);

        setupAliasDisplay();
        setupHandlers();

        amount = (BigDecimal) getIntent().getSerializableExtra("amount");
        if (null == amount) amount = BigDecimal.ZERO;
        amount = amount.setScale(2, BigDecimal.ROUND_DOWN);

        setupQrImage();
        textView2.setText("");
    }

    private void setupQrImage() {
        DecimalFormat f = new DecimalFormat();
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        f.setGroupingUsed(false);

        amountInfoTextView.setText("Amount: THB " + f.format(amount));

        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        QRCodeWriter writer = new QRCodeWriter();
        try {
            String proxyType = pref.getString(getString(R.string.proxyType), null);
            String proxy = pref.getString(getString(R.string.proxy), null);

            if (null == proxyType || null == proxy) {
                showUnknownQR();
                return;
            }

            String content = PromptPayQR.payloadMoneyTransfer(proxyType, proxy, amount);

            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();

            newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {

                    newImage.setPixel(x , y, bitMatrix.get(x,y) ? Color.BLACK : Color.WHITE);
                }
            }

            qrImageView.setImageBitmap(newImage);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void showUnknownQR() {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon_qr);
        String initPPID = getString(R.string.initPPID);
        Bitmap.Config config = bm.getConfig();
        int width = bm.getWidth();
        int height = bm.getHeight();

        newImage = Bitmap.createBitmap(width, height, config);

        Canvas c = new Canvas(newImage);
        c.drawBitmap(bm, 0, 0, null);

        Paint bk = new Paint();
        TextPaint paint = new TextPaint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(24);
        paint.setAntiAlias(true);
        int xpos = c.getWidth()/2 - (int)paint.measureText(initPPID)/2;
        int ypos = (int) ((c.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) ;
        bk.setColor(Color.LTGRAY);
        bk.setStyle(Paint.Style.FILL);
        bk.setAlpha(180);

        c.drawRect(xpos - 30, ypos - 30,
                c.getWidth()/2 + (int)paint.measureText(initPPID)/2 + 30,
                (int) ((c.getHeight() / 2) + ((paint.descent() + paint.ascent()) / 2)) + 30,
                bk);
        c.drawText(initPPID, xpos, ypos, paint);

        qrImageView.setImageBitmap(newImage);
    }

    private void setupAliasDisplay() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        String proxyType;
        String proxy;
        String alias;

        proxyType = pref.getString(getString(R.string.proxyType), null);
        proxy = pref.getString(getString(R.string.proxy), null);
        View.OnClickListener listener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setupProxyInfo();
            }
        };
        qrImageView.setOnClickListener(listener);
        alias = pref.getString(getString(R.string.alias), mask(proxyType, proxy));

        if (null == proxy || null == proxyType) {
            proxyInfoTextView.setText("No PromptPay ID");
        } else {
            proxyInfoTextView.setText(alias);
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

        View.OnClickListener shareListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickShare();
            }
        };
        btnShare.setOnClickListener(shareListener);
    }

    private void onClickShare() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("image/*");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, getImageUri(GenerateQrCodeActivity.this.getApplicationContext(), newImage));
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        try {
            File cachePath = new File(inContext.getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/" + PromptPayQR.getCrcValue() + ".png");
            inImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(inContext,
                    "seta.noip.me.ppcalculator.fileprovider",
                    new File(cachePath, PromptPayQR.getCrcValue() + ".png"));
            return contentUri;
        } catch(IOException caught) {
            if (LogConfig.LOG) {
                Log.e(getClass().getName(), "images cannot be saved", caught);
            }
            return null;
        }
    }

    private String mask(String proxyType, String proxy) {
        if (null == proxy || null == proxyType) {
            return "";
        }

        if (PromptPayQR.BOT_ID_MERCHANT_PHONE_NUMBER.equals(proxyType)) {
            return proxy.replaceFirst("(...)(...)(....)", "$1-XXX-$3");
        }
        else if (PromptPayQR.BOT_ID_MERCHANT_TAX_ID.equals(proxyType)) {
            return proxy.replaceFirst("(.)(....)(.....)(...)", "$1-$2-XXXXX-$4");
        }
        else {
            int n = proxy.length()-4;
            char[] chars = new char[n];
            Arrays.fill(chars, 'X');
            String result = new String(chars) + proxy.substring(proxy.length()-4);
            return result;
        }
    }

    private void setupProxyInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Your PP ID");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String i = input.getText().toString();
                String proxyType;
                String proxy;
                String alias;

                proxyType = PromptPayQR.guessProxyType(i);
                proxy = PromptPayQR.satinizeProxyValue(i);
                alias = mask(proxyType, proxy);
                proxyInfoTextView.setText(alias);
                SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(getString(R.string.proxy), proxy);
                editor.putString(getString(R.string.proxyType), proxyType);
                editor.putString(getString(R.string.alias), alias);
                editor.commit();

                setupQrImage();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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
