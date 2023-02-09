/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.reader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AnyValueReaderTest {
    static AnyValueReader.ProcessSink ps(AnyValueReader reader) {
        return reader.sinks.get(reader.sinkCursor);
    }

    record SpecialWord(String value) {
        @Override
        public String toString() {
            return value;
        }
    }

    @Nested
    class ReadingTest {
        @Test
        void testCharReading() {
            var reader = new AnyValueReader("12 34", "5");
            Assertions.assertEquals('1', reader.peekChar());
            Assertions.assertEquals('2', reader.peekChar(1));
            Assertions.assertEquals(' ', reader.peekChar(2));
            Assertions.assertEquals('3', reader.peekChar(3));
            Assertions.assertEquals('4', reader.peekChar(4));
            Assertions.assertEquals(' ', reader.peekChar(5));
            Assertions.assertEquals('5', reader.peekChar(6));
            Assertions.assertEquals(0, reader.peekChar(7));

            Assertions.assertEquals(0, reader.getCursor());
            Assertions.assertEquals('1', reader.peekChar());
            Assertions.assertEquals('1', reader.readChar());

            Assertions.assertEquals(1, reader.getCursor());
            Assertions.assertEquals('2', reader.peekChar());
            Assertions.assertEquals('2', reader.readChar());

            Assertions.assertEquals(2, reader.getCursor());
            Assertions.assertEquals(' ', reader.peekChar());
            Assertions.assertEquals(' ', reader.readChar());

            Assertions.assertEquals(3, reader.getCursor());
            Assertions.assertEquals('3', reader.peekChar());
            Assertions.assertEquals('3', reader.readChar());


            Assertions.assertEquals(0, reader.sinkCursor);
            Assertions.assertEquals(4, reader.contentCursor);
            Assertions.assertEquals(4, ((AnyValueReader.ObjectProcessSink) reader.sinks.get(0)).contentIndex());

            Assertions.assertEquals(4, reader.getCursor());
            Assertions.assertEquals('4', reader.peekChar());
            Assertions.assertEquals('4', reader.readChar());

            Assertions.assertEquals(5, reader.getCursor());
            Assertions.assertEquals(' ', reader.peekChar());
            Assertions.assertEquals(' ', reader.readChar());

            Assertions.assertEquals(6, reader.getCursor());
            Assertions.assertEquals('5', reader.peekChar());
            Assertions.assertEquals('5', reader.readChar());


            Assertions.assertEquals(7, reader.getCursor());
            Assertions.assertEquals(0, reader.peekChar());
            Assertions.assertEquals(0, reader.readChar());
            Assertions.assertEquals(7, reader.getCursor());

            reader.setCursor(4);
            Assertions.assertTrue(reader.canRead());
            Assertions.assertEquals(0, reader.sinkCursor);
            Assertions.assertEquals(4, reader.contentCursor);
            Assertions.assertEquals(4, ((AnyValueReader.ObjectProcessSink) reader.sinks.get(0)).contentIndex());

            Assertions.assertEquals(4, reader.getCursor());
            Assertions.assertEquals('4', reader.peekChar());
            Assertions.assertEquals('4', reader.readChar());

            Assertions.assertEquals(5, reader.getCursor());
            Assertions.assertEquals(' ', reader.peekChar());
            Assertions.assertEquals(' ', reader.readChar());

            Assertions.assertEquals(6, reader.getCursor());
            Assertions.assertEquals('5', reader.peekChar());
            Assertions.assertEquals('5', reader.readChar());

            Assertions.assertFalse(reader.canRead());
            Assertions.assertEquals(7, reader.getCursor());
            Assertions.assertEquals(0, reader.peekChar());
            Assertions.assertEquals(0, reader.readChar());
            Assertions.assertEquals(7, reader.getCursor());


            reader.setCursor(5);
            Assertions.assertTrue(reader.canRead());
            Assertions.assertEquals(1, reader.sinkCursor);
            Assertions.assertEquals(5, reader.contentCursor);

            Assertions.assertEquals(5, reader.getCursor());
            Assertions.assertEquals(' ', reader.peekChar());
            Assertions.assertEquals(' ', reader.readChar());

            Assertions.assertEquals(6, reader.getCursor());
            Assertions.assertEquals('5', reader.peekChar());
            Assertions.assertEquals('5', reader.readChar());

            Assertions.assertFalse(reader.canRead());
            Assertions.assertEquals(7, reader.getCursor());
            Assertions.assertEquals(0, reader.peekChar());
            Assertions.assertEquals(0, reader.readChar());
            Assertions.assertEquals(7, reader.getCursor());


        }

        @Test
        void testReadSplitting() {
            var special1 = new SpecialWord("s 1");
            var special2 = new SpecialWord("s 2");

            var reader = new AnyValueReader(special1, " w1 w2  \n\n", special2, " ");

            Assertions.assertEquals(0, reader.getCursor());
            Assertions.assertSame(special1, reader.readAny());
            Assertions.assertEquals(3, reader.getCursor());

            reader.setCursor(1);
            Assertions.assertEquals("1", reader.readAny());
            Assertions.assertEquals(3, reader.getCursor());


            Assertions.assertEquals("w1", reader.readAny());
            Assertions.assertEquals(7, reader.getCursor());

            Assertions.assertEquals("w2", reader.readAny());
            Assertions.assertEquals(10, reader.getCursor());

            Assertions.assertSame(special2, reader.readAny());
            Assertions.assertEquals(18, reader.getCursor());
            Assertions.assertTrue(reader.canRead());

            Assertions.assertSame(null, reader.readAny());
            Assertions.assertEquals(20, reader.getCursor());
            Assertions.assertFalse(reader.canRead());
        }
    }

    @Nested
    class FetchContentTest {

        @Test
        void test1() {
            var reader = new AnyValueReader("1", "23", "45", new SpecialWord("67"));

            Assertions.assertEquals("", reader.fetchContent(0, 0).toString());
            Assertions.assertEquals("1", reader.fetchContent(0, 1).toString());
            Assertions.assertEquals("1 ", reader.fetchContent(0, 2).toString());
            Assertions.assertEquals("1 2", reader.fetchContent(0, 3).toString());
            Assertions.assertEquals(" 23", reader.fetchContent(1, 4).toString());
            Assertions.assertEquals(" 23 45", reader.fetchContent(1, 7).toString());
            Assertions.assertEquals(" 23 45 ", reader.fetchContent(1, 8).toString());
            Assertions.assertEquals(" 23 45 6", reader.fetchContent(1, 9).toString());
            Assertions.assertEquals(" 23 45 67", reader.fetchContent(1, 10).toString());
            Assertions.assertEquals(" 23 45 67", reader.fetchContent(1, 11).toString());
            Assertions.assertEquals(" 23 45 67", reader.fetchContent(1, 12).toString());

            Assertions.assertEquals("3", reader.fetchContent(3, 4).toString());
            Assertions.assertEquals("3 ", reader.fetchContent(3, 5).toString());
            Assertions.assertEquals("3 4", reader.fetchContent(3, 6).toString());
        }

        @Test
        void testEmptyReading() {
            var reader = new AnyValueReader(new SpecialWord(""), new SpecialWord("hello"));
            Assertions.assertEquals(" ", reader.fetchContent(0, 1).toString());
            Assertions.assertEquals("  ", reader.fetchContent(0, 2).toString());
            Assertions.assertEquals("  he", reader.fetchContent(0, 4).toString());
        }
    }
}
