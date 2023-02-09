/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.resscoppe;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;

class SharedResourceScope extends ResourceScopeImpl {
    SharedResourceScope(Cleaner cleaner) {
        super(new SharedResourceList(), cleaner);
    }

    @Override
    void close0() {
        var prevState = (int) STATE.compareAndExchange(this, OPEN, CLOSING);

        if (prevState < 0) throw alreadyClosed();
        if (prevState != OPEN) {
            throw alreadyAcquired(prevState);
        }
        storageReferences.clear();
        STATE.setVolatile(this, CLOSED);

        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    void release0() {
        int value;
        do {
            value = (int) STATE.getVolatile(this);
            if (value <= OPEN) {
                //cannot get here - we can't close segment twice
                throw alreadyClosed();
            }
        } while (!STATE.compareAndSet(this, value, value - 1));

        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    void acquire0() {
        int value;
        do {
            value = (int) STATE.getVolatile(this);

            if (value < OPEN) {
                throw alreadyClosed();
            }

            if (value == MAX_FORKS) {
                throw tooManyAcquires();
            }

        } while (!STATE.compareAndSet(this, value, value + 1));
    }

    @Override
    public void waitAndClose() {
        try {

            while (true) {
                var prevState = (int) STATE.compareAndExchange(this, OPEN, CLOSING);
                if (prevState < 0) {
                    throw alreadyClosed();
                }
                if (prevState == OPEN) {
                    storageReferences.clear();
                    STATE.setVolatile(this, CLOSED);

                    releaseResources();

                    return;
                }

                synchronized (this) {
                    if (state > 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        } finally {
            Reference.reachabilityFence(this);
        }
    }

    static class SharedResourceList extends ResourceList {

        @Override
        void cleanup() {
            if (FST.getAcquire(this) != ResourceCleanup.CLOSED_LIST) {
                while (true) {
                    var prev = (ResourceCleanup) FST.getVolatile(this);
                    if (prev == ResourceCleanup.CLOSED_LIST) throw alreadyClosed();

                    if (FST.compareAndSet(this, prev, ResourceCleanup.CLOSED_LIST)) {
                        cleanup(prev);
                        return;
                    }
                }
            }
            throw alreadyClosed();
        }

        @Override
        void add(ResourceCleanup cleanup) {
            while (true) {
                var prev = (ResourceCleanup) FST.getVolatile(this);
                if (prev == ResourceCleanup.CLOSED_LIST) {
                    throw alreadyClosed();
                }

                cleanup.next = prev;

                if (FST.compareAndSet(this, prev, cleanup)) return;
            }
        }
    }
}
