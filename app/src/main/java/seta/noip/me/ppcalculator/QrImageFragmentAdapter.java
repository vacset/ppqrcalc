package seta.noip.me.ppcalculator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.math.BigDecimal;

public class QrImageFragmentAdapter extends FragmentPagerAdapter {

    private final GenerateQrCodeActivity activity;

    public QrImageFragmentAdapter(FragmentManager fm, GenerateQrCodeActivity activity) {
        super(fm);
        this.activity = activity;
    }

    @Override
    public Fragment getItem(int position) {
        AnyId anyId = activity.getAnyId();
        BigDecimal amount = activity.getAmount();
        return QrFragment.newInstance(activity, anyId, amount);
    }

    @Override
    public int getCount() {
        return 1;
    }

    public static class QrFragment extends Fragment {

        static Bitmap showUnknownQR(Context ctx) {

            Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.icon_qr);
            String initPPID = ctx.getString(R.string.qr_error);
            Bitmap.Config config = bm.getConfig();
            int width = bm.getWidth();
            int height = bm.getHeight();

            Bitmap newImage = Bitmap.createBitmap(width, height, config);

            Canvas c = new Canvas(newImage);

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
            return newImage;
        }

        static Bitmap showAnyIdQR(Context ctx, AnyId anyId, BigDecimal amount) {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                String content = PromptPayQR.payloadMoneyTransfer(anyId.getIdType(), anyId.getIdValue(), amount);

                BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
                int width = bitMatrix.getWidth();
                int height = bitMatrix.getHeight();

                Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {

                        newImage.setPixel(x , y, bitMatrix.get(x,y) ? Color.BLACK : Color.WHITE);
                    }
                }

                return newImage;
            } catch (WriterException e) {
                if (LogConfig.LOG) {
                    Log.e(QrImageFragmentAdapter.class.getName(), "cannot create QR bitmap", e);
                }
                return showUnknownQR(ctx);
            }
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View swipeView = inflater.inflate(R.layout.swipe_fragment, container, false);
            ImageView imageView = (ImageView) swipeView.findViewById(R.id.qrImageView);
            Bundle bundle = getArguments();
            Context ctx = getActivity();
            AnyId anyId = new AnyId();
            BigDecimal amount = (BigDecimal) bundle.getSerializable(ctx.getString(R.string.amount));
            anyId.setIdType(bundle.getString(ctx.getString(R.string.proxyType)));
            anyId.setIdValue(bundle.getString(ctx.getString(R.string.proxy)));

            if (null == anyId.getIdType() && null == anyId.getIdValue()) {
                imageView.setImageResource(R.drawable.ic_add_box_black_24dp);
            } else {
                imageView.setImageBitmap(showAnyIdQR(ctx, anyId, amount));
            }
            return swipeView;
        }

        static QrFragment newInstance(Context ctx, AnyId anyId, BigDecimal amount) {
            QrFragment swipeFragment = new QrFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(ctx.getString(R.string.amount), amount);
            if (null != anyId) {
                bundle.putString(ctx.getString(R.string.proxyType), anyId.getIdType());
                bundle.putString(ctx.getString(R.string.proxy), anyId.getIdValue());
            }
            swipeFragment.setArguments(bundle);
            return swipeFragment;
        }
    }
}
