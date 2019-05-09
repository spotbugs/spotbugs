package edu.umd.cs.findbugs.ba.npe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

public class IsNullValueTest {

    @Test
    public void testMerge1() {
        IsNullValue nullValue = IsNullValue.nullValue();
        IsNullValue nullExceptionValue = IsNullValue.nullValue().toExceptionValue();
        IsNullValue result = IsNullValue.merge(nullValue, nullExceptionValue);
        assertTrue(result.isDefinitelyNull());
        assertFalse(result.isException());
    }

    @Test
    public void testMerge2() {
        IsNullValue nullExceptionValue = IsNullValue.nullValue().toExceptionValue();
        IsNullValue nonNullValue = IsNullValue.nonNullValue();
        IsNullValue nsp_e = IsNullValue.merge(nonNullValue, nullExceptionValue);
        assertTrue(nsp_e.isNullOnSomePath());
        assertTrue(nsp_e.isException());
        assertEquals(nsp_e, IsNullValue.nullOnSimplePathValue().toExceptionValue());
    }

    @Test
    public void testMerge3() {
        IsNullValue nullValue = IsNullValue.nullValue();
        IsNullValue nsp_e = IsNullValue.nullOnSimplePathValue().toExceptionValue();
        IsNullValue nsp = IsNullValue.merge(nullValue, nsp_e);
        assertTrue(nsp.isNullOnSomePath());
        assertFalse(nsp.isException());
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION",
            justification = "Test is disabled, would need to be fixed anyway")
    @Ignore
    @Test
    public void testMerge4() {
        IsNullValue noKaboom = IsNullValue.noKaboomNonNullValue(null);
        IsNullValue nsp_e = IsNullValue.nullOnSimplePathValue().toExceptionValue();
        IsNullValue nsp_e2 = IsNullValue.merge(noKaboom, nsp_e);
        assertTrue(nsp_e2.isNullOnSomePath());
        assertTrue(nsp_e2.isException());
    }

    @Test
    public void testMerge5() {
        IsNullValue checkedNonNull = IsNullValue.checkedNonNullValue();
        IsNullValue nsp_e = IsNullValue.nullOnSimplePathValue().toExceptionValue();
        IsNullValue nsp_e2 = IsNullValue.merge(checkedNonNull, nsp_e);
        assertTrue(nsp_e2.isNullOnSomePath());
        assertTrue(nsp_e2.isException());
    }

    @Test
    public void testMerge6() {
        IsNullValue checkedNull_e = IsNullValue.checkedNullValue().toExceptionValue();
        IsNullValue unknown = IsNullValue.nonReportingNotNullValue();
        IsNullValue nsp_e = IsNullValue.merge(checkedNull_e, unknown);
        assertTrue(nsp_e.isNullOnSomePath());
        assertTrue(nsp_e.isException());
    }
}
