package edu.umd.cs.findbugs.ba;

import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SignatureParserTest {

    @Test
    void testNoParams() {
        SignatureParser sut = new SignatureParser("()V");
        Iterator<String> i = sut.parameterSignatureIterator();
        Assertions.assertFalse(i.hasNext());
    }

    @Test
    void testManyParams() {
        SignatureParser sut = new SignatureParser("(IJFDZLjava/lang/String;B)Ljava/lang/Object;");
        Iterator<String> i = sut.parameterSignatureIterator();
        Assertions.assertTrue(i.hasNext());
        Assertions.assertEquals(i.next(), "I");
        Assertions.assertTrue(i.hasNext());
        Assertions.assertEquals(i.next(), "J");
        Assertions.assertTrue(i.hasNext());
        Assertions.assertEquals(i.next(), "F");
        Assertions.assertTrue(i.hasNext());
        Assertions.assertEquals(i.next(), "D");
        Assertions.assertTrue(i.hasNext());
        Assertions.assertEquals(i.next(), "Z");
        Assertions.assertTrue(i.hasNext());
        Assertions.assertEquals(i.next(), "Ljava/lang/String;");
        Assertions.assertTrue(i.hasNext());
        Assertions.assertEquals(i.next(), "B");
        Assertions.assertFalse(i.hasNext());
    }
}
