/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.exceptions;

public class CommandSyntaxException extends RuntimeException {
    public CommandSyntaxException() {
    }

    public CommandSyntaxException(String message) {
        super(message);
    }

    public CommandSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandSyntaxException(Throwable cause) {
        super(cause);
    }

    public CommandSyntaxException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
