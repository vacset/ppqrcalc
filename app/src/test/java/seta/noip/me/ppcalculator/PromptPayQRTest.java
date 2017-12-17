package seta.noip.me.ppcalculator;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Created by vachi on 28-Nov-17.
 */
public class PromptPayQRTest {
    @Test
    public void satinizeProxyValue() throws Exception {
        assertEquals("3100602152163", PromptPayQR.satinizeProxyValue("3100602152163"));
        assertEquals("3100602152163", PromptPayQR.satinizeProxyValue("3-100-60-2152-163"));
        assertEquals("0832434300", PromptPayQR.satinizeProxyValue("08 3 243 4300"));
    }

    @Test
    public void payloadMoneyTransfer() throws Exception {
        // amount 12 THB
        assertEquals("00020101021229370016A000000677010111021331006021521635802TH5303764540212630478E1",
                PromptPayQR.payloadMoneyTransfer(PromptPayQR.BOT_ID_MERCHANT_TAX_ID,
                        "3100602152163", BigDecimal.valueOf(12)));
        // amount 174 THB
        assertEquals("00020101021229370016A000000677010111021331006021521635802TH53037645403174630401C8",
                PromptPayQR.payloadMoneyTransfer(PromptPayQR.BOT_ID_MERCHANT_TAX_ID,
                        "3100602152163", BigDecimal.valueOf(174)));
        // amount 0 THB --> treat as static QR
        assertEquals("00020101021129370016A000000677010111021331006021521635802TH530376463043245",
                PromptPayQR.payloadMoneyTransfer(PromptPayQR.BOT_ID_MERCHANT_TAX_ID,
                        "3100602152163", BigDecimal.valueOf(0)));
    }

    @Test
    public void guessProxyType() throws Exception {
        assertEquals(PromptPayQR.BOT_ID_MERCHANT_TAX_ID, PromptPayQR.guessProxyType("3100602152163"));
        assertEquals(PromptPayQR.BOT_ID_MERCHANT_PHONE_NUMBER, PromptPayQR.guessProxyType("0832434300"));
        assertEquals(PromptPayQR.BOT_ID_MERCHANT_WALLET_ID, PromptPayQR.guessProxyType("140000830723996"));
    }

    @Test
    public void leftPad() throws Exception {
        assertEquals("0001", PromptPayQR.leftPad("1", "0", 4));
        assertEquals("0012", PromptPayQR.leftPad("12", "0", 4));
        assertEquals("0123", PromptPayQR.leftPad("123", "0", 4));
        assertEquals("1234", PromptPayQR.leftPad("1234", "0", 4));
        assertEquals("2345", PromptPayQR.leftPad("12345", "0", 4));
    }
}