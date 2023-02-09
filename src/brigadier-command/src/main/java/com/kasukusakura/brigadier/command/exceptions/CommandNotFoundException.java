/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.exceptions;

public class CommandNotFoundException extends CommandSyntaxException {
    public CommandNotFoundException() {
    }

    public CommandNotFoundException(String message) {
        super(message);
    }

    public CommandNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandNotFoundException(Throwable cause) {
        super(cause);
    }

    public CommandNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
