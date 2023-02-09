/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.resscoppe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

abstract class ResourceScopeImpl implements ResourceScope {
    static final int OPEN = 0;
    static final int CLOSING = -1;
    static final int CLOSED = -2;

    final ResourceList resourceList;
    final Cleaner.Cleanable cleanable;

    final ConcurrentLinkedQueue<Object> storageReferences = new ConcurrentLinkedQueue<>();


    public ResourceScopeImpl(ResourceList list, Cleaner cleaner) {
        this.resourceList = list;
        this.cleanable = cleaner == null ? null : cleaner.register(this, resourceList);
    }

    int state = OPEN;
    static final VarHandle STATE;
    static final int MAX_FORKS = Integer.MAX_VALUE;


    static {
        try {
            STATE = MethodHandles.lookup().findVarHandle(ResourceScopeImpl.class, "state", int.class);
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    @Override
    public boolean isAlive() {
        return state >= OPEN;
    }

    @Override
    public boolean isCloseable() {
        return true;
    }


    @Override
    public final void close() {
        try {
            close0();
            releaseResources();
        } finally {
            Reference.reachabilityFence(this);
        }
    }

    final void releaseResources() {
        try {
            if (cleanable != null) {
                cleanable.clean();
            } else {
                resourceList.cleanup();
            }
        } finally {
            Reference.reachabilityFence(this);
        }
    }

    abstract void close0();


    abstract void release0();

    abstract void acquire0();

    @Override
    public void whileAlive(Runnable action) {
        Objects.requireNonNull(action);
        acquire0();
        try {
            action.run();
        } finally {
            release0();
        }
    }

    @Override
    public <T> T whileAlive(Supplier<T> action) {
        Objects.requireNonNull(action);
        acquire0();
        try {
            return action.get();
        } finally {
            release0();
        }
    }

    @Override
    public void onDispose(Runnable runnable) {
        if (state < OPEN) throw alreadyClosed();

        resourceList.add(ResourceList.ResourceCleanup.ofRunnable(runnable));
    }

    @Override
    public void addStrongReference(Object reference) {
        Objects.requireNonNull(reference);

        acquire0();
        try {
            storageReferences.add(reference);
        } finally {
            release0();
            Reference.reachabilityFence(reference);
            Reference.reachabilityFence(this);
        }
    }

    @Override
    public void removeStrongReference(Object reference) {
        Objects.requireNonNull(reference);

        acquire0();
        try {
            storageReferences.remove(reference);
        } finally {
            release0();

            Reference.reachabilityFence(reference);
            Reference.reachabilityFence(this);
        }
    }

    @Override
    public ResourceScope asNonCloseable() {
        if (!isCloseable()) return this;

        return new NonClosableView(this);
    }

    abstract static class ResourceList implements Runnable {
        ResourceCleanup fst;
        static final VarHandle FST;

        static {
            try {
                FST = MethodHandles.lookup().findVarHandle(ResourceList.class, "fst", ResourceCleanup.class);
            } catch (Throwable ex) {
                throw new ExceptionInInitializerError();
            }
        }


        abstract void cleanup();


        public final void run() {
            cleanup(); // cleaner interop
        }

        static void cleanup(ResourceCleanup first) {
            Throwable topError = null;

            ResourceCleanup current = first;
            while (current != null) {
                try {
                    current.cleanup();
                } catch (Throwable e) {
                    if (topError == null) {
                        topError = e;
                    } else {
                        topError.addSuppressed(e);
                    }
                }
                current = current.next;
            }

            if (topError != null) {
                throw new IllegalStateException(topError);
            }
        }

        abstract void add(ResourceCleanup cleanup);

        abstract static class ResourceCleanup {
            ResourceCleanup next;

            abstract void cleanup();

            static final ResourceCleanup CLOSED_LIST = new ResourceCleanup() {
                @Override
                public void cleanup() {
                    throw new IllegalStateException("This resource list has already been closed!");
                }
            };

            static ResourceCleanup ofRunnable(Runnable cleanupAction) {
                return new ResourceCleanup() {
                    @Override
                    public void cleanup() {
                        cleanupAction.run();
                    }
                };
            }
        }
    }

    final static class NonClosableView implements ResourceScope {
        private final ResourceScope delegate;

        public NonClosableView(ResourceScope delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isAlive() {
            return delegate.isAlive();
        }

        @Override
        public boolean isCloseable() {
            return delegate.isCloseable();
        }

        @Override
        public void whileAlive(Runnable action) {
            delegate.whileAlive(action);
        }

        @Override
        public <T> T whileAlive(Supplier<T> action) {
            return delegate.whileAlive(action);
        }

        @Override
        public void onDispose(Runnable runnable) {
            delegate.onDispose(runnable);
        }

        @Override
        public void close() {
            throw nonCloseable();
        }

        @Override
        public void waitAndClose() {
            throw nonCloseable();
        }

        @Override
        public ResourceScope asNonCloseable() {
            return this;
        }

        @Override
        public void addStrongReference(Object reference) {
            delegate.addStrongReference(reference);
        }

        @Override
        public void removeStrongReference(Object reference) {
            delegate.removeStrongReference(reference);
        }
    }


    static IllegalStateException tooManyAcquires() {
        return new IllegalStateException("Session acquire limit exceeded");
    }

    static IllegalStateException alreadyAcquired(int acquires) {
        return new IllegalStateException(String.format("Session is acquired by %d clients", acquires));
    }

    static IllegalStateException alreadyClosed() {
        return new IllegalStateException("Already closed");
    }

    static UnsupportedOperationException nonCloseable() {
        return new UnsupportedOperationException("Attempted to close a non-closeable session");
    }
}
