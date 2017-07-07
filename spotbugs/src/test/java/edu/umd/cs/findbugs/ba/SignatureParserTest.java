package edu.umd.cs.findbugs.ba;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SignatureParserTest {

    SignatureParser noParams;

    SignatureParser manyParams;

    @Before
    public void setUp() {
        noParams = new SignatureParser("()V");
        manyParams = new SignatureParser("(IJFDZLjava/lang/String;B)Ljava/lang/Object;");
    }

    @Test
    public void testNoParams() {
        Iterator<String> i = noParams.parameterSignatureIterator();
        Assert.assertFalse(i.hasNext());
    }

    @Test
    public void testManyParams() {
        Iterator<String> i = manyParams.parameterSignatureIterator();
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), "I");
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), "J");
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), "F");
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), "D");
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), "Z");
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), "Ljava/lang/String;");
        Assert.assertTrue(i.hasNext());
        Assert.assertEquals(i.next(), "B");
        Assert.assertFalse(i.hasNext());
    }
}
