package edu.umd.cs.findbugs.sarif;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GUIDCalculatorTest {

    @Test
    void testGuidFromNamespaceUsingAsciiChars() {
        // output of: uuid -v 5 '6ba7b811-9dad-11d1-80b4-00c04fd430c8' '22'
        UUID expectedUUID = UUID.fromString("51e4ac9a-92ed-5ca5-95ab-f9260e15f813");

        UUID namespace = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
        UUID actualGUID = GUIDCalculator.fromNamespaceAndString(namespace, "22");

        assertThat(actualGUID, is(expectedUUID));
    }

    @Test
    void testGuidFromNamespaceUsingSmiley() {
        // output of: uuid -v 5 '6ba7b811-9dad-11d1-80b4-00c04fd430c8' '😀'
        UUID expectedUUID = UUID.fromString("f0793165-4aab-598c-9164-4efc598481b2");

        UUID namespace = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
        UUID actualGUID = GUIDCalculator.fromNamespaceAndString(namespace, "😀");

        assertThat(actualGUID, is(expectedUUID));
    }

    @Test
    void testGuidFromStringUsingAsciiChars() {
        UUID expectedUUID = UUID.fromString("12c6fc06-c99a-5623-b5ee-b3f43dfd832b");

        UUID actualGUID = GUIDCalculator.fromString("22");

        assertThat(actualGUID, is(expectedUUID));
    }


    @Test
    void testGuidFromStringUsingSmiley() {
        UUID expectedUUID = UUID.fromString("9c533688-a979-5858-8bd6-a43c9f91aba6");

        UUID actualGUID = GUIDCalculator.fromString("😀");

        assertThat(actualGUID, is(expectedUUID));
    }
}
