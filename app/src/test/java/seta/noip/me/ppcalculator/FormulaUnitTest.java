package seta.noip.me.ppcalculator;

import android.content.Context;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class FormulaUnitTest {
    @Mock
    Context mMockContext;

    private void prepareMock() {
        // Given a mocked Context injected into the object under test...
        when(mMockContext.getString(R.string.unfinishedFormulaEndingSymbols))
                .thenReturn(".+/*-");
        when(mMockContext.getString(R.string.divisionSign))
                .thenReturn("\u00F7");
        when(mMockContext.getString(R.string.multiplicationSign))
                .thenReturn("\u00D7");
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void testInitialValue() throws Exception {
        prepareMock();

        Formula f = new Formula(mMockContext);

        assertEquals(BigDecimal.ZERO, f.eval());
        assertEquals("", f.display());
    }

    @Test
    public void testSimpleCalculations() throws Exception {
        // Given a mocked Context injected into the object under test...
        prepareMock();

        Formula f = new Formula(mMockContext);
        f.append("1");
        assertEquals(1, f.eval().doubleValue(), 0.0);
        assertEquals("1", f.display());

        f.append("2");
        assertEquals(12, f.eval().doubleValue(), 0.0);
        assertEquals("12", f.display());

        f.append("+");
        assertEquals(12, f.eval().doubleValue(), 0.0);
        assertEquals("12+", f.display());

        f.append("1");
        assertEquals(13, f.eval().doubleValue(), 0.0);
        assertEquals("12+1", f.display());

        f.append("-");
        assertEquals(13, f.eval().doubleValue(), 0.0);
        assertEquals("12+1-", f.display());

        f.append("1");
        assertEquals(12, f.eval().doubleValue(), 0.0);
        assertEquals("12+1-1", f.display());

        f.append("/");
        assertEquals(12, f.eval().doubleValue(), 0.0);
        assertEquals("12+1-1÷", f.display());

        f.append("2");
        assertEquals(12.5, f.eval().doubleValue(), 0.0);
        assertEquals("12+1-1÷2", f.display());

        f.append("*");
        assertEquals(12.5, f.eval().doubleValue(), 0.0);
        assertEquals("12+1-1÷2×", f.display());

        f.append("4");
        assertEquals(11, f.eval().doubleValue(), 0.0);
        assertEquals("12+1-1÷2×4", f.display());

        f.clear();

        f.append("-");
        assertEquals(0, f.eval().doubleValue(), 0.0);
        assertEquals("-", f.display());

        f.append("1");
        assertEquals(-1, f.eval().doubleValue(), 0.0);
        assertEquals("-1", f.display());

        f.append("-");
        assertEquals(-1, f.eval().doubleValue(), 0.0);
        assertEquals("-1-", f.display());

        f.append("1");
        assertEquals(-2, f.eval().doubleValue(), 0.0);
        assertEquals("-1-1", f.display());

        f.clear();
        f.append("3");
        f.append(".");
        assertEquals(3, f.eval().doubleValue(), 0.0);
        assertEquals("3.", f.display());
        f.append("5");
        assertEquals(3.5, f.eval().doubleValue(), 0.0);
        assertEquals("3.5", f.display());
    }

    @Test
    public void testConstructor() throws Exception {
        prepareMock();

        Formula f = new Formula(mMockContext, "120");
        assertEquals(120, f.eval().doubleValue(), 0.0);
        assertEquals("120", f.display());

        f = new Formula(mMockContext, "120.50");
        assertEquals(120.50, f.eval().doubleValue(), 0.0);
        assertEquals("120.50", f.display());

        f = new Formula(mMockContext, "120.50*2+16");
        assertEquals(120.50*2+16, f.eval().doubleValue(), 0.0);
        assertEquals("120.50×2+16", f.display());
    }

    @Test
    public void testMalformed() throws Exception {
        // Given a mocked Context injected into the object under test...
        prepareMock();

        Formula f = new Formula(mMockContext);
        f.append("*");
        assertEquals(BigDecimal.ZERO, f.eval());
        assertEquals("", f.display());

        f.append("+");
        assertEquals(BigDecimal.ZERO, f.eval());
        assertEquals("", f.display());

        f.append("/");
        assertEquals(BigDecimal.ZERO, f.eval());
        assertEquals("", f.display());

        f.append("-");
        assertEquals(0, f.eval().doubleValue(), 0.0);
        assertEquals("-", f.display());
        // try double minus...
        f.append("-");
        assertEquals(0, f.eval().doubleValue(), 0.0);
        assertEquals("-", f.display());

        f.append("1");
        assertEquals(-1, f.eval().doubleValue(), 0.0);
        assertEquals("-1", f.display());

        // divide by zero is error
        f.append("/");
        f.append("0");
        assertEquals(0, f.eval().doubleValue(), 0.0);
        assertEquals("-1÷0", f.display());
        assertTrue(f.isInvalid());

        f = new Formula(mMockContext, "1.00");
        f.append(".");
        assertEquals(1, f.eval().doubleValue(), 0.0);
        assertEquals("1.00", f.display());

        f = new Formula(mMockContext, "1.00+2.5");
        f.append(".");
        assertEquals(3.5, f.eval().doubleValue(), 0.0);
        assertEquals("1.00+2.5", f.display());

        f = new Formula(mMockContext, "1.00+2.5");
        f.append("+");
        f.append("+");
        assertEquals(3.5, f.eval().doubleValue(), 0.0);
        assertEquals("1.00+2.5+", f.display());

        f.clear();
        f.append("3");
        f.append(".");
        f.append("5");
        f.append("+");
        f.append("5");
        f.append(".");
        f.append("2");
        f.append("2");
        assertEquals(3.5+5.22, f.eval().doubleValue(), 0.01);
        f.append(".");
        assertEquals(3.5+5.22, f.eval().doubleValue(), 0.01);
        assertEquals("3.5+5.22", f.display());
        f.append("1");
        assertEquals(3.5+5.221, f.eval().doubleValue(), 0.01);
        assertEquals("3.5+5.221", f.display());
    }


}