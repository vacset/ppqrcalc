package seta.noip.me.ppcalculator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;

public class QrImageFragmentAdapter extends FragmentPagerAdapter {

    private final GenerateQrCodeActivity activity;

    public QrImageFragmentAdapter(FragmentManager fm, GenerateQrCodeActivity activity) {
        super(fm);
        this.activity = activity;
    }

    @Override
    public Fragment getItem(int position) {
        BigDecimal amount = activity.getAmount();
        return QrFragment.newInstance(activity, position, amount, activity.changeLsnr);
    }

    @Nullable
    public Bitmap getQR(int position) {
        BigDecimal amount = activity.getAmount();
        AnyId id = QrFragment.loadPreferencePPID(activity, position);
        if (null == id) {
            return null;
        }
        return QrFragment.showAnyIdQR(activity, id, amount);
    }

    @Override
    public int getCount() {
        return 1;
    }

    public static class QrFragment extends Fragment {
        private AnyId anyId;
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.removePropertyChangeListener(listener);
        }

        private static AnyId loadPreferencePPID(Activity ctx, int position) {
            SharedPreferences pref = ctx.getPreferences(Context.MODE_PRIVATE);
            AnyId id = new AnyId();
            if (position == 0) {
                id.setIdType(pref.getString(ctx.getString(R.string.proxyType), null));
                id.setIdValue(pref.getString(ctx.getString(R.string.proxy), null));
                id.setAliasName(pref.getString(ctx.getString(R.string.alias), id.mask()));
            } else {
                id.setIdType(pref.getString(ctx.getString(R.string.proxyTypeIndex, position), null));
                id.setIdValue(pref.getString(ctx.getString(R.string.proxyIndex, position), null));
                id.setAliasName(pref.getString(ctx.getString(R.string.aliasIndex, position), id.mask()));
            }

            if (null != id.getIdType() && null != id.getIdValue()) {
                return id;
            } else {
                return null;
            }
        }

        public void setValue(AnyId anyId) {
            AnyId oldValue = anyId;
            this.anyId = anyId;
            SharedPreferences pref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            if (null != anyId) {
                editor.putString(getString(R.string.proxy), anyId.getIdValue());
                editor.putString(getString(R.string.proxyType), anyId.getIdType());
                editor.putString(getString(R.string.alias), anyId.getAliasName());
                editor.apply();
            }

            this.pcs.firePropertyChange("anyId", oldValue, anyId);
        }

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
            Activity ctx = getActivity();
            BigDecimal amount = (BigDecimal) bundle.getSerializable(ctx.getString(R.string.amount));
            int position = bundle.getInt("position");
            anyId = loadPreferencePPID(ctx, position);

            if (null == anyId) {
                imageView.setImageResource(R.drawable.ic_add_box_black_24dp);
            } else {
                imageView.setImageBitmap(showAnyIdQR(ctx, anyId, amount));
                this.pcs.firePropertyChange("anyId", null, anyId);
            }

            return swipeView;
        }

        static QrFragment newInstance(Context ctx, int position, BigDecimal amount, PropertyChangeListener changeLsnr) {
            QrFragment swipeFragment = new QrFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(ctx.getString(R.string.amount), amount);
            bundle.putInt("position", position);
            swipeFragment.setArguments(bundle);
            swipeFragment.addPropertyChangeListener(changeLsnr);
            return swipeFragment;
        }
    }
}
