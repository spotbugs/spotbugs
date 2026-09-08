package edu.umd.cs.findbugs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

class TokenizerTest {

    @Test
    void tokenizesWordsStringsCommentsAndSymbols() throws IOException {
        Tokenizer tokenizer = new Tokenizer(new StringReader(" foo //comment\n/*multi*/ 'a' \"b\\\"c\" /+\n"));

        assertToken(tokenizer.next(), Token.WORD, "foo");
        assertToken(tokenizer.next(), Token.COMMENT, "//comment");
        assertToken(tokenizer.next(), Token.EOL, "");
        assertToken(tokenizer.next(), Token.COMMENT, "/*multi*/");
        assertToken(tokenizer.next(), Token.STRING, "'a'");
        assertToken(tokenizer.next(), Token.STRING, "\"b\\\"c\"");
        assertToken(tokenizer.next(), Token.SINGLE, "/");
        assertToken(tokenizer.next(), Token.SINGLE, "+");
        assertToken(tokenizer.next(), Token.EOL, "");
        assertToken(tokenizer.next(), Token.EOF, "");
    }

    @Test
    void slashThatIsNotCommentIsSingleToken() throws IOException {
        Tokenizer tokenizer = new Tokenizer(new StringReader("/word"));

        assertToken(tokenizer.next(), Token.SINGLE, "/");
        assertToken(tokenizer.next(), Token.WORD, "word");
        assertToken(tokenizer.next(), Token.EOF, "");
    }

    @Test
    void handlesUnterminatedMultilineCommentAndString() throws IOException {
        Tokenizer commentTokenizer = new Tokenizer(new StringReader("/*unterminated"));
        assertToken(commentTokenizer.next(), Token.COMMENT, "/*unterminated");
        assertToken(commentTokenizer.next(), Token.EOF, "");

        Tokenizer stringTokenizer = new Tokenizer(new StringReader("\"unterminated"));
        assertToken(stringTokenizer.next(), Token.STRING, "\"unterminated");
        assertToken(stringTokenizer.next(), Token.EOF, "");
    }

    private static void assertToken(Token token, int kind, String lexeme) {
        assertEquals(kind, token.getKind());
        assertEquals(lexeme, token.getLexeme());
    }
}
