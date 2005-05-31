package edu.umd.cs.findbugs.ba.npe;

import junit.framework.TestCase;

public class IsNullValueTest extends TestCase {
	public void testMerge1() {
		IsNullValue nullValue = IsNullValue.nullValue();
		IsNullValue nullExceptionValue = IsNullValue.nullValue().toExceptionValue();
		IsNullValue result = IsNullValue.merge(nullValue, nullExceptionValue);
		assertTrue(result.isDefinitelyNull());
		assertFalse(result.isException());
	}
	public void testMerge2() {
		IsNullValue nullValue = IsNullValue.nullValue();
		IsNullValue nullExceptionValue = IsNullValue.nullValue().toExceptionValue();
		IsNullValue nonNullValue = IsNullValue.nonNullValue();
		IsNullValue nsp_e = IsNullValue.merge(nonNullValue, nullExceptionValue);
		assertTrue(nsp_e.isNullOnSomePath());
		assertTrue(nsp_e.isException());
		assertEquals(nsp_e, IsNullValue.nullOnSimplePathValue().toExceptionValue());
	}
	public void testMerge3() {
		IsNullValue nullValue = IsNullValue.nullValue();	
		IsNullValue nsp_e = IsNullValue.nullOnSimplePathValue().toExceptionValue();
		IsNullValue nsp = IsNullValue.merge(nullValue, nsp_e);
		assertTrue(nsp.isNullOnSomePath());
		assertFalse(nsp.isException());
	}
}
