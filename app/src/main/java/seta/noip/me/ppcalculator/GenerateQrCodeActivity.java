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
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
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

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GenerateQrCodeActivity extends AppCompatActivity {
    static final int AMOUNT_REQUEST = 1;

    @BindView(R.id.left_nav) ImageButton leftNav;
    @BindView(R.id.right_nav) ImageButton rightNav;
    @BindView(R.id.proxyInfoTextView) TextView proxyInfoTextView;
    @BindView(R.id.amountInfoTextView) TextView amountInfoTextView;
    @BindView(R.id.textView2) TextView textView2;
    @BindView(R.id.btnCalculator) ImageButton btnCalculator;
    @BindView(R.id.btnShare) ImageButton btnShare;
    @BindView(R.id.viewpager) ViewPager viewPager;

    private ShowcaseView tutorialView;
    private SwipeFragmentAdapter swipeFragmentAdapter;

    private BigDecimal amount;
    private AnyId anyId;

    public BigDecimal getAmount() {
        return amount;
    }

    public AnyId getAnyId() {
        return  anyId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr_code);
        ButterKnife.bind(this);

        loadPreferencePPID();

        setupAliasDisplay();
        setupHandlers();

        amount = (BigDecimal) getIntent().getSerializableExtra("amount");
        if (null == amount) amount = BigDecimal.ZERO;
        amount = amount.setScale(2, BigDecimal.ROUND_DOWN);

        setupQrImage();
        swipeFragmentAdapter = new SwipeFragmentAdapter(getSupportFragmentManager(), this);
        viewPager.setAdapter(swipeFragmentAdapter);

        textView2.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null == anyId) {
            setupTutorialInputPPID();
        } else {
            setupTutorialChangePPID();
        }
    }

    private void loadPreferencePPID() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        AnyId id = new AnyId();
        id.setIdType(pref.getString(getString(R.string.proxyType), null));
        id.setIdValue(pref.getString(getString(R.string.proxy), null));
        id.setAliasName(pref.getString(getString(R.string.alias), id.mask()));

        if (null != id.getIdType() && null != id.getIdValue()) {
            anyId = id;
        } else {
            anyId = null;
        }
    }

    private void savePreferencePPID() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (null != anyId) {
            editor.putString(getString(R.string.proxy), anyId.getIdValue());
            editor.putString(getString(R.string.proxyType), anyId.getIdType());
            editor.putString(getString(R.string.alias), anyId.getAliasName());
            editor.apply();
        }
    }

    private void setupQrImage() {
        DecimalFormat f = new DecimalFormat();
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        f.setGroupingUsed(false);

        amountInfoTextView.setText(String.format(getString(R.string.qr_amount), f.format(amount)));

    }

    private void setupTutorialInputPPID() {
        Target viewTarget = new ViewTarget(R.id.viewpager, this);
        tutorialView = new ShowcaseView.Builder(this)
                .setTarget(viewTarget)
                .setContentTitle(getString(R.string.tutorial_title_qrinput))
                .setContentText(getString(R.string.tutorial_detail_qrinput))
                .setStyle(R.style.CustomShowcaseTheme2)
                .singleShot(R.integer.tutorial_id_qrinput)
                .build();
    }

    private void setupTutorialChangePPID() {
        Target viewTarget = new ViewTarget(R.id.viewpager, this);
        tutorialView = new ShowcaseView.Builder(this)
                .setTarget(viewTarget)
                .setContentTitle(getString(R.string.tutorial_title_qrchange))
                .setContentText(getString(R.string.tutorial_detail_qrchange))
                .setStyle(R.style.CustomShowcaseTheme2)
                .singleShot(R.integer.tutorial_id_qrchange)
                .build();
    }

    private void setupAliasDisplay() {
        if (null == anyId) {
            proxyInfoTextView.setText(R.string.promptpay_id_not_set);
        } else {
            proxyInfoTextView.setText(anyId.getAliasName());
        }
    }

    private void setupHandlers() {
        btnCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickCalculator();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickShare();
            }
        });

        leftNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.arrowScroll(View.FOCUS_LEFT);
            }
        });

        // Images right navigation
        rightNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.arrowScroll(View.FOCUS_RIGHT);
            }
        });

        viewPager.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                tutorialView.hide();
                setupProxyInfo();
            }
        });

    }

    private void onClickCalculator() {
        // pass the amount back to calculator if any, and prepare to receive new amount back
        Intent i = new Intent();
        i.putExtra("amount", amount);
        i.setClassName(CalculatorActivity.class.getPackage().getName(), //"seta.noip.me.ppcalculator",
                CalculatorActivity.class.getCanonicalName()); //"seta.noip.me.ppcalculator.CalculatorActivity");
        startActivityForResult(i, AMOUNT_REQUEST);
    }

    private void onClickShare() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.share_note_label);

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        Fragment f = swipeFragmentAdapter.getItem(viewPager.getCurrentItem());
        ImageView imgView = (ImageView) f.getView().findViewById(R.id.qrImageView);
        final Bitmap newImage = ((BitmapDrawable)imgView.getDrawable()).getBitmap();
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                NumberFormat nf = NumberFormat.getInstance();
                nf.setGroupingUsed(true);
                String amountLine = "Amount: " + nf.format(amount);
                String noteToPayerLine = input.getText().toString();

                // copy the QR into a new bitmap, to add some stuff like amount and note
                Bitmap forSharing = Bitmap.createBitmap(newImage.getWidth(), newImage.getHeight() + 60,
                        newImage.getConfig());
                Canvas c = new Canvas(forSharing);
                // create white background
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                c.drawRect(0, 0, newImage.getWidth(), newImage.getHeight() + 60, paint);

                c.drawBitmap(newImage, 0, 0, null);

                TextPaint txtPaint = new TextPaint();
                txtPaint.setColor(Color.BLACK);
                txtPaint.setStyle(Paint.Style.FILL);
                txtPaint.setTextSize(24);
                txtPaint.setAntiAlias(true);
                int xpos = c.getWidth()/2 - (int)txtPaint.measureText(amountLine)/2;
                int ypos = (int) ((c.getHeight() - 50) - ((txtPaint.descent() + txtPaint.ascent()) / 2)) ;

                c.drawText(amountLine, xpos, ypos, txtPaint);

                xpos = c.getWidth()/2 - (int)txtPaint.measureText(noteToPayerLine)/2;
                ypos = (int) ((c.getHeight() - 20) - ((txtPaint.descent() + txtPaint.ascent()) / 2)) ;
                c.drawText(noteToPayerLine, xpos, ypos, txtPaint);

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("image/*");
                sharingIntent.putExtra(Intent.EXTRA_STREAM,
                        getImageUri(GenerateQrCodeActivity.this.getApplicationContext(), forSharing));
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, noteToPayerLine);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, noteToPayerLine);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
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

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        try {
            cleanCache(new File(inContext.getCacheDir(), "images"));
            File cachePath = new File(inContext.getCacheDir(), "images");
            if(!cachePath.mkdirs()) { // don't forget to make the directory
                if (LogConfig.LOG) {
                    Log.e(getClass().getName(), "could not create directory: " + cachePath.toString());
                }
            }
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

    private void cleanCache(File dir) {
        if (!dir.exists()) return;

        File[] files = dir.listFiles();

        for (File file : files) {
            if(!file.delete()) {
                if (LogConfig.LOG) {
                    Log.e(getClass().getName(), "could not delete cache file: " + file.toString());
                }
            }
        }
    }

    private void setupProxyInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ppid_input_title));

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
                anyId = new AnyId();
                anyId.setIdType(PromptPayQR.guessProxyType(i));
                anyId.setIdValue(PromptPayQR.satinizeProxyValue(i));
                anyId.setAliasName(anyId.mask());
                proxyInfoTextView.setText(anyId.getAliasName());
                savePreferencePPID();

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
