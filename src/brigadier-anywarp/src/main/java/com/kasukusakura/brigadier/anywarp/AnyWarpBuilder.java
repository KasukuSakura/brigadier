/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.anywarp;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;

public interface AnyWarpBuilder {
    public static AnyWarpBuilder builder() {
        return new AnyWarpBuilderImpl();
    }


    public AnyWarpBuilder types(Class<?>... itfs);

    public AnyWarpBuilder types(String... itfs);

    public AnyWarpBuilder addMethod(String name, String descriptor, int modifier);

    public AnyWarpBuilder constructors(Class<?>... argTypes);

    public AnyWarpBuilder consturctorFieldNameGenerator(ConstructorFieldNameGenerator generator);

    public interface ConstructorFieldNameGenerator {
        public String generateFieldName(int index, Class<?> klass);
    }

    public AnyWarpBuilder definer(MethodHandles.Lookup definer);

    public interface MethodResolver {
        public InsnWriter redirectToField(int index);

        public InsnWriter invokeDynamic(
                String bootstrap,
                String bootstrapName,
                String bootstrapDescription,
                Object[] bootstrapArgs
        );

        public InsnWriter invokeDynamic(
                String bootstrap,
                String bootstrapName,
                String bootstrapDescription,
                String fncName,
                Object[] bootstrapArgs
        );

        @FunctionalInterface
        public interface InsnWriter {
            public void doWrite(MethodVisitor writer, String metName, Type methodDesc, String wrapperClassName);
        }
    }

    public interface MethodResolveHandle {
        public MethodResolver.InsnWriter resolve(MethodResolver resolver, String method, String desc, int modifiers);
    }

    public AnyWarpBuilder resolver(MethodResolveHandle resolveHandle);

    public AnyWarpBuilder useAnonymous(boolean useAnonymous);

    public MethodHandles.Lookup doDefine();
}
