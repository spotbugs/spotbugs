package edu.umd.cs.findbugs.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class QuoteMetaCharactersTest {

    @Test
    void escapesConfiguredMetaCharacters() throws IOException {
        MetaCharacterMap map = new MetaCharacterMap();
        map.addMeta('<', "&lt;");
        map.addMeta('&', "&amp;");

        CollectingQuoteMetaCharacters quote = new CollectingQuoteMetaCharacters("a<&>b", map);

        quote.process();

        assertEquals("a&lt;&amp;>b", quote.output());
    }

    @Test
    void emitsEmptyLiteralForEmptyInput() throws IOException {
        CollectingQuoteMetaCharacters quote = new CollectingQuoteMetaCharacters("", new MetaCharacterMap());

        quote.process();

        assertEquals("", quote.output());
        assertEquals(1, quote.emitCount);
    }

    @Test
    void constructorRejectsNullInputs() {
        MetaCharacterMap map = new MetaCharacterMap();

        assertThrows(NullPointerException.class, () -> new CollectingQuoteMetaCharacters(null, map));
        assertThrows(NullPointerException.class, () -> new CollectingQuoteMetaCharacters("text", null));
    }

    private static final class CollectingQuoteMetaCharacters extends QuoteMetaCharacters {
        private final StringBuilder output = new StringBuilder();
        private int emitCount;

        private CollectingQuoteMetaCharacters(String text, MetaCharacterMap map) {
            super(text, map);
        }

        @Override
        public void emitLiteral(String s) {
            emitCount++;
            output.append(s);
        }

        private String output() {
            return output.toString();
        }
    }
}
