/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class AnyValueReader {
    public static final Function<Object, CharSequence> DEFAULT_TO_CHAR_SEQUENCE = obj -> {
        if (obj instanceof CharSequence) return (CharSequence) obj;

        return String.valueOf(obj);
    };

    private static final Function<CharSequence, CharSequence> TO_CHAR_SEQUENCE_POST_PROCESS = obj -> {
        if (obj == null) return "null";
        return obj;
    };

    public static final Predicate<Object> DEFAULT_IS_CHAR_SEQUENCE = obj -> obj instanceof CharSequence;

    final Function<Object, CharSequence> objectTransform;
    final Predicate<Object> isCharSequence;
    final List<ProcessSink> sinks;

    int sinkCursor;
    int contentCursor;

    public AnyValueReader(
            Iterator<?> values,
            Function<Object, CharSequence> objectTransform,
            Predicate<Object> isCharSequence
    ) {
        if (objectTransform == null || objectTransform == DEFAULT_TO_CHAR_SEQUENCE) {
            this.objectTransform = DEFAULT_TO_CHAR_SEQUENCE;
        } else {
            this.objectTransform = objectTransform.andThen(TO_CHAR_SEQUENCE_POST_PROCESS);
        }
        this.isCharSequence = isCharSequence == null ? DEFAULT_IS_CHAR_SEQUENCE : isCharSequence;
        List<ProcessSink> sinks = this.sinks = new ArrayList<>();

        if (values.hasNext()) {
            sinks.add(new ObjectProcessSink(values.next()));
        }
        while (values.hasNext()) {
            sinks.add(new SpliterProcessSink());
            sinks.add(new ObjectProcessSink(values.next()));
        }
        sinks.add(new TailProcessSink());
    }

    public AnyValueReader(
            Iterable<?> values,
            Function<Object, CharSequence> objectTransform,
            Predicate<Object> isCharSequence
    ) {
        this(values.iterator(), objectTransform, isCharSequence);
    }

    public AnyValueReader(Object... values) {
        this(Arrays.asList(values));
    }

    public AnyValueReader(Iterable<?> values) {
        this(values, DEFAULT_TO_CHAR_SEQUENCE, DEFAULT_IS_CHAR_SEQUENCE);
    }

    public AnyValueReader(AnyValueReader old) {
        this.objectTransform = old.objectTransform;
        this.isCharSequence = old.isCharSequence;
        this.contentCursor = old.contentCursor;
        this.sinkCursor = old.sinkCursor;
        this.sinks = new ArrayList<>();
        for (ProcessSink op : old.sinks) {
            sinks.add(op.clone());
        }
    }

    public CharSequence toCharSequence(Object value) {
        return objectTransform.apply(value);
    }

    public boolean isCharSequence(Object value) {
        return isCharSequence.test(value);
    }

    public int getCursor() {
        return contentCursor;
    }

    public void setCursor(int cursor) {
        for (ProcessSink sink : sinks) {
            if (sink.isInitialized()) {
                sink.seekCursor(this, cursor);
            }
        }
        for (int i = 0, sinksSize = sinks.size(); i < sinksSize; i++) {
            ProcessSink sink = sinks.get(i);
            if (sink.isInitialized()) {
                sinkCursor = i;
                if (sink.cursorEnd > cursor) break;
            }
        }
        this.contentCursor = cursor;
    }

    public AnyValueReader copy() {
        return new AnyValueReader(this);
    }

    public char peekChar(int offset) {
        if (offset < 0) throw new IllegalArgumentException();
        return sinks.get(sinkCursor).peekChar(this, sinkCursor, offset);
    }

    public char peekChar() {
        return sinks.get(sinkCursor).peekChar(this, sinkCursor, 0);
    }

    public char readChar() {
        return sinks.get(sinkCursor).readChar(this, sinkCursor);
    }

    public Object peekAny() {
        return sinks.get(sinkCursor).peekAny(this, sinkCursor);
    }

    public Object readAny() {
        return sinks.get(sinkCursor).readAny(this, sinkCursor);
    }

    public boolean canRead(int offset) {
        if (offset < 0) {
            return false;
        }
        return sinks.get(sinkCursor).canRead(this, sinkCursor, offset);
    }

    public boolean canRead() {
        return sinks.get(sinkCursor).canRead(this, sinkCursor, 0);
    }

    public CharSequence fetchContent(int start, int end) {
        if (start == end) return "";
        if (start > end) return "";

        StringBuilder sb = new StringBuilder(Math.min(end - start, 256));
        for (int i = 0, sinksSize = sinks.size(); i < sinksSize; i++) {
            ProcessSink sink = sinks.get(i);
            sink.initialize(this, i);
            if (sink.cursorEnd > start) {
                sink.fetchContent(sb, start, end);
            }
            if (sink.cursorEnd >= end) break;
        }
        return sb;
    }

    static abstract class ProcessSink implements Cloneable {
        int cursorStart = -1, cursorEnd = -1;

        @Override
        public ProcessSink clone() {
            try {
                return (ProcessSink) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        boolean isInitialized() {
            return cursorStart != -1;
        }

        void initialize(AnyValueReader reader, int index) {
            if (cursorStart == -1) {
                if (index == 0) {
                    cursorStart = 0;
                } else {
                    ProcessSink prev = reader.sinks.get(index - 1);
                    prev.initialize(reader, index - 1);
                    cursorStart = prev.cursorEnd;
                }

                initialize0(reader, index);
            }
        }

        abstract void initialize0(AnyValueReader reader, int index);

        void seekCursor(AnyValueReader reader, int cursor) {
        }

        abstract CharSequence getContent();

        abstract char peekChar(AnyValueReader reader, int index, int offset);

        abstract char readChar(AnyValueReader reader, int index);

        abstract Object peekAny(AnyValueReader reader, int index);

        abstract Object readAny(AnyValueReader reader, int index);

        abstract boolean canRead(AnyValueReader reader, int index, int offset);

        public abstract void fetchContent(StringBuilder sb, int start, int end);
    }

    static final class TailProcessSink extends ProcessSink {

        @Override
        void initialize0(AnyValueReader reader, int index) {
        }

        @Override
        CharSequence getContent() {
            return "";
        }

        @Override
        char peekChar(AnyValueReader reader, int index, int offset) {
            return 0;
        }

        @Override
        char readChar(AnyValueReader reader, int index) {
            initialize(reader, index);
            reader.contentCursor = cursorStart;
            return 0;
        }

        @Override
        Object peekAny(AnyValueReader reader, int index) {
            return null;
        }

        @Override
        Object readAny(AnyValueReader reader, int index) {
            initialize(reader, index);
            reader.contentCursor = cursorStart;
            return null;
        }

        @Override
        boolean canRead(AnyValueReader reader, int index, int offset) {
            return false;
        }

        @Override
        public void fetchContent(StringBuilder sb, int start, int end) {
        }
    }

    static final class SpliterProcessSink extends ProcessSink {
        @Override
        void initialize0(AnyValueReader reader, int index) {
            cursorEnd = cursorStart + 1;
        }

        @Override
        CharSequence getContent() {
            return " ";
        }

        @Override
        char peekChar(AnyValueReader reader, int index, int offset) {
            if (offset != 0) {
                return reader.sinks.get(index + 1).peekChar(reader, index + 1, offset - 1);
            }
            return ' ';
        }

        @Override
        boolean canRead(AnyValueReader reader, int index, int offset) {
            if (offset != 0) {
                return reader.sinks.get(index + 1).canRead(reader, index + 1, offset - 1);
            }
            return true;
        }

        @Override
        char readChar(AnyValueReader reader, int index) {
            initialize(reader, index);
            reader.contentCursor = cursorEnd;
            reader.sinkCursor = index + 1;
            return ' ';
        }

        @Override
        Object peekAny(AnyValueReader reader, int index) {
            return reader.sinks.get(index + 1).peekAny(reader, index + 1);
        }

        @Override
        Object readAny(AnyValueReader reader, int index) {
            initialize(reader, index);
            reader.sinkCursor = index + 1;
            reader.contentCursor = cursorEnd;
            return reader.sinks.get(index + 1).readAny(reader, index + 1);
        }

        @Override
        public void fetchContent(StringBuilder sb, int start, int end) {
            if (start <= cursorStart && end >= cursorStart) {
                sb.append(' ');
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    static final class ObjectProcessSink extends ProcessSink {
        private final Object value;
        private int contentIndex;
        private CharSequence cs;
        private boolean isCs;

        int contentIndex() {
            return contentIndex;
        }

        ObjectProcessSink(Object value) {
            this.value = value;
        }

        @Override
        void initialize0(AnyValueReader reader, int index) {
            cs = reader.objectTransform.apply(value);
            isCs = reader.isCharSequence.test(value);
            int len = cs.length();
            if (len == 0) {
                cursorEnd = cursorStart + 1;
            } else {
                cursorEnd = cursorStart + len;
            }
        }

        @Override
        CharSequence getContent() {
            return cs;
        }

        @Override
        char peekChar(AnyValueReader reader, int index, int offset) {
            initialize(reader, index);
            int relPoint = contentIndex + offset;
            if (relPoint < cs.length()) {
                return cs.charAt(relPoint);
            }

            ProcessSink next = reader.sinks.get(index + 1);
            return next.peekChar(reader, index + 1, offset - (cs.length() - contentIndex));
        }

        @Override
        boolean canRead(AnyValueReader reader, int index, int offset) {
            initialize(reader, index);
            ProcessSink next = reader.sinks.get(index + 1);

            if (contentIndex + offset < cs.length()) return true;
            if (contentIndex == 0 && offset == 0) return true;

            int cslen = Math.max(cs.length(), 1);
            return next.canRead(reader, index + 1, offset - cslen);
        }

        @Override
        char readChar(AnyValueReader reader, int index) {
            initialize(reader, index);
            int crtIdx = contentIndex;
            if (crtIdx < cs.length()) {
                contentIndex++;
                reader.contentCursor = cursorStart + contentIndex;
                return cs.charAt(crtIdx);
            }

            reader.sinkCursor = index + 1;
            ProcessSink next = reader.sinks.get(index + 1);
            return next.readChar(reader, index + 1);
        }

        @Override
        Object peekAny(AnyValueReader reader, int index) {
            initialize(reader, index);
            if (!isCs && contentIndex == 0) {
                return value;
            }

            ProcessSink next = reader.sinks.get(index + 1);

            int start = -1;
            int csLen = cs.length();
            for (int i = contentIndex; i < csLen; i++) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    start = i;
                    break;
                }
            }
            if (start == -1) {
                return next.peekAny(reader, index + 1);
            }

            int end = csLen;
            for (int i = start; i < csLen; i++) {
                if (Character.isWhitespace(cs.charAt(i))) {
                    end = i;
                    break;
                }
            }

            return cs.subSequence(start, end);
        }

        @Override
        Object readAny(AnyValueReader reader, int index) {
            initialize(reader, index);
            if (!isCs && contentIndex == 0) {
                reader.sinkCursor = index + 1;
                reader.contentCursor = cursorEnd;
                return value;
            }

            ProcessSink next = reader.sinks.get(index + 1);

            int start = -1;
            int csLen = cs.length();
            for (int i = contentIndex; i < csLen; i++) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    start = i;
                    break;
                }
            }
            if (start == -1) {
                reader.sinkCursor++;
                return next.readAny(reader, index + 1);
            }

            int end = csLen;
            for (int i = start; i < csLen; i++) {
                if (Character.isWhitespace(cs.charAt(i))) {
                    end = i;
                    break;
                }
            }

            reader.contentCursor = cursorStart + end;
            contentIndex = end;

            return cs.subSequence(start, end);
        }

        @Override
        void seekCursor(AnyValueReader reader, int cursor) {
            contentIndex = Math.min(Math.max(cursor - cursorStart, 0), cs.length());
        }

        @Override
        public void fetchContent(StringBuilder sb, int start, int end) {

            if (cs.length() == 0) {
                if (!(cursorStart <= start && end >= cursorEnd)) return;

                sb.append(' ');
                return;
            }

            sb.append(cs, Math.max(0, start - cursorStart), Math.min(end - cursorStart, cs.length()));
        }
    }
}
