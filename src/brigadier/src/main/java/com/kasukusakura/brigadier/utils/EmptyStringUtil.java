/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.utils;

public class EmptyStringUtil {
    public static final String EMPTY;

    static {
        StringBuilder sb = new StringBuilder(100);
        int count = 100;
        while (count-- > 0) {
            sb.append(' ');
        }
        EMPTY = sb.toString();
    }

    public static void appendEmptyString(StringBuilder builder, int count) {
        while (count > 0) {
            int appended = Math.min(count, EMPTY.length());
            builder.append(EMPTY, 0, appended);
            count -= appended;
        }
    }
}
