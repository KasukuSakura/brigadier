/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.resscoppe;

import java.io.Closeable;
import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A resource scope manages the lifecycle of one or more resources.
 */
public interface ResourceScope extends Closeable {
    boolean isAlive();

    boolean isCloseable();

    /**
     * Runs a critical action while this resource scope is kept alive.
     *
     * @throws IllegalStateException if this scope is closed
     * @apiNote While running this method, this scope remains alive and cannot be closed
     */
    void whileAlive(Runnable action);

    <T> T whileAlive(Supplier<T> action);

    /**
     * Add a custom cleanup action which will be executed when the scope is closed.
     * The order in which custom cleanup actions are invoked once the memory session is closed is unspecified.
     *
     * @throws IllegalStateException if resource is not alive.
     * @apiNote The provided action should not keep a strong reference to this resource scope, so that implicitly
     * closed sessions can be handled correctly by a {@link Cleaner} instance.
     */
    void onDispose(Runnable runnable);


    /**
     * Closes this memory session. If this operation completes without exceptions, this session
     * will be marked as <em>not alive</em>, the {@linkplain #onDispose(Runnable) close actions} associated
     * with this session will be executed, and all the resources associated with this session will be released.
     *
     * @throws IllegalStateException         if this resource scope is not {@linkplain #isAlive() alive}.
     * @throws IllegalStateException         if this scope is {@linkplain #whileAlive(Runnable) kept alive} by another client.
     * @throws UnsupportedOperationException if this memory session is not {@linkplain #isCloseable() closeable}.
     * @apiNote This operation is not idempotent; that is, closing an already closed memory session <em>always</em> results in an
     * exception being thrown. This reflects a deliberate design choice: memory session state transitions should be
     * manifest in the client code; a failure in any of these transitions reveals a bug in the underlying application
     * logic.
     * @see #isAlive()
     */
    @Override
    void close();

    /**
     * Wait all {@linkplain #whileAlive(Runnable)} critical action} complete and close this scope
     *
     * @throws IllegalStateException if this resource scope is not {@linkplain #isAlive() alive}.
     */
    void waitAndClose();

    ResourceScope asNonCloseable();

    /**
     * Add a strong reference that will be held by this scope.
     *
     * @throws IllegalStateException if this scope is closed
     */
    void addStrongReference(Object reference);

    /**
     * Remove a strong reference from this scope.
     */
    void removeStrongReference(Object reference);


    /**
     * Create a closeable shared resource scope.
     *
     * @apiNote This scope will not be managed by GC system. This scope required be closed manually.
     */
    static ResourceScope openShared() {
        return new SharedResourceScope(null);
    }

    /**
     * Create a closeable shared resource scope, managed by the provided cleaner instance
     */
    static ResourceScope openShared(Cleaner cleaner) {
        return new SharedResourceScope(Objects.requireNonNull(cleaner, "cleaner"));
    }

    /**
     * Create a non-closeable shared resource scope, managed by the provided cleaner instance.
     * Equivalent to (but likely more efficient than) the following code:
     * {@code
     * openShared(Cleaner.create()).asNonCloseable();
     * }
     */
    static ResourceScope openImplicit(Cleaner cleaner) {
        return new ImplicitResourceScope(Objects.requireNonNull(cleaner, "cleaner"));
    }

}
