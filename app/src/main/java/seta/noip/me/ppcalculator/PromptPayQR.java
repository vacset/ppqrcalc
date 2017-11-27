package seta.noip.me.ppcalculator;

import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class PromptPayQR {
    private static String ID_PAYLOAD_FORMAT = "00";
    private static String ID_POI_METHOD = "01";
    private static String ID_MERCHANT_INFORMATION_BOT = "29";
    private static String ID_TRANSACTION_CURRENCY = "53";
    private static String ID_TRANSACTION_AMOUNT = "54";
    private static String ID_COUNTRY_CODE = "58";
    private static String ID_CRC = "63";

    private static String PAYLOAD_FORMAT_EMV_QRCPS_MERCHANT_PRESENTED_MODE = "01";
    private static String POI_METHOD_STATIC = "11";
    private static String POI_METHOD_DYNAMIC = "12";

    private static String MERCHANT_INFORMATION_TEMPLATE_ID_GUID = "00";
    public static String BOT_ID_MERCHANT_PHONE_NUMBER = "01";
    public static String BOT_ID_MERCHANT_TAX_ID = "02";
    public static String BOT_ID_MERCHANT_WALLET_ID = "15";//1400-0-0830723996
    private static String GUID_PROMPTPAY = "A000000677010111";
    private static String TRANSACTION_CURRENCY_THB = "764";
    private static String COUNTRY_CODE_TH = "TH";

    private static String crcValue = "0000";

    public static String satinizeProxyValue(String proxyValue) {
        if (null == proxyValue) return null;
        return proxyValue.replaceAll("[^0-9]", "");
    }

    private static String formatProxy(String proxyType, String proxyValue) {
        if (BOT_ID_MERCHANT_PHONE_NUMBER.equals(proxyType)) {
            String tmp = "0000000000000" + proxyValue.replaceFirst("^0", "66");
            return tmp.substring(tmp.length() - 13);
        }

        return proxyValue;
    }

    private static String formatAmount(@NonNull BigDecimal amount) {
        if (isWholeNumber(amount)) {
            NumberFormat f = NumberFormat.getIntegerInstance();
            f.setGroupingUsed(false);
            return f.format(amount);
        }
        else {
            DecimalFormat f = new DecimalFormat();
            amount = amount.setScale(2, BigDecimal.ROUND_DOWN);
            f.setMinimumFractionDigits(2);
            f.setMaximumFractionDigits(2);
            f.setGroupingUsed(false);
            return f.format(amount);
        }
    }

    @NonNull
    private static String tag(String id, String value) {
        String v =  ("00" + value.length());
        return id + v.substring(v.length()-2) + value;
    }

    private static StringBuffer serialize(String ... s) {
        StringBuffer buf = new StringBuffer();
        for (String e : s) {
            buf.append(e);
        }
        return buf;
    }

    private static String crc16(@NonNull String args) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        // byte[] testBytes = "123456789".getBytes("ASCII");

        byte[] bytes = args.getBytes();

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return Integer.toHexString(crc);
    }

    @NonNull
    public static String payloadMoneyTransfer(String proxyType, String proxyValue, BigDecimal amount) {
        if (null == proxyType || null == proxyValue) {
            return "";
        }

        String sanitizedValue = satinizeProxyValue(proxyValue);

        String [] elem =  {
                tag(ID_PAYLOAD_FORMAT, PAYLOAD_FORMAT_EMV_QRCPS_MERCHANT_PRESENTED_MODE),
                tag(ID_POI_METHOD, POI_METHOD_DYNAMIC),
                tag(ID_MERCHANT_INFORMATION_BOT, serialize(
                        tag(MERCHANT_INFORMATION_TEMPLATE_ID_GUID, GUID_PROMPTPAY),
                        tag(proxyType, formatProxy(proxyType, sanitizedValue))
                ).toString()),
                tag(ID_COUNTRY_CODE, COUNTRY_CODE_TH),
                tag(ID_TRANSACTION_CURRENCY, TRANSACTION_CURRENCY_THB),
        };

        StringBuffer payload = serialize(elem);

        if (null != amount) {
            payload = payload.append(tag(ID_TRANSACTION_AMOUNT, formatAmount(amount)));
        }

        String crcTarget = payload.toString() + ID_CRC + "04";
        crcValue = crc16(crcTarget);
        payload.append(tag(ID_CRC, crcValue));
        return payload.toString().toUpperCase();
    }

    public static String guessProxyType(String proxyValue) {
        if (null == proxyValue) return null;
        String sanitizedValue = satinizeProxyValue(proxyValue);
        if (sanitizedValue.length() == 13) return BOT_ID_MERCHANT_TAX_ID;
        if (sanitizedValue.length() == 10) return BOT_ID_MERCHANT_PHONE_NUMBER;
        if (sanitizedValue.length() == 15) return BOT_ID_MERCHANT_WALLET_ID;

        return null; // default
    }

    public static String getCrcValue() {
        return crcValue;
    }

    private static boolean isWholeNumber(@NonNull BigDecimal number) {
        return (number.scale() <= 0) ||
                (number.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0);
    }
}
