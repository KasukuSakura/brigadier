/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.context;

public class MetadataValue {
    public final String name;
    public final Class<?> type;
    public final Object value;

    public MetadataValue(String name, Class<?> type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
}
