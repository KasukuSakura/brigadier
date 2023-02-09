/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.resscoppe;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;

class ImplicitResourceScope extends SharedResourceScope {
    ImplicitResourceScope(Cleaner cleaner) {
        super(cleaner);
    }

    @Override
    void release0() {
        Reference.reachabilityFence(this);
    }

    @Override
    void acquire0() {
    }

    @Override
    public boolean isCloseable() {
        return false;
    }

    @Override
    void close0() {
        throw nonCloseable();
    }

    @Override
    public void waitAndClose() {
        throw nonCloseable();
    }
}
