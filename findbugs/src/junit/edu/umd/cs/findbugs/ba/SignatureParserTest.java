package edu.umd.cs.findbugs.ba;

import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SignatureParserTest extends TestCase {
    SignatureParser noParams;

    SignatureParser manyParams;

    @Override
    protected void setUp() {
        noParams = new SignatureParser("()V");
        manyParams = new SignatureParser("(IJFDZLjava/lang/String;B)Ljava/lang/Object;");
    }

    public void testNoParams() {
        Iterator<String> i = noParams.parameterSignatureIterator();
        Assert.assertFalse(i.hasNext());
    }

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

