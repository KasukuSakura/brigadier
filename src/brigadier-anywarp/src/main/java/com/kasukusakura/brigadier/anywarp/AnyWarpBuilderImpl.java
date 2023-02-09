/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.anywarp;

import org.objectweb.asm.*;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
class AnyWarpBuilderImpl implements AnyWarpBuilder {
    enum DefaultConstructorFieldNameGenerator implements ConstructorFieldNameGenerator {
        INSTANCE;

        @Override
        public String generateFieldName(int index, Class<?> klass) {
            return "arg" + index;
        }
    }

    final List<Class<?>> supItfs = new ArrayList<>();
    final List<String> directItfs = new ArrayList<>();
    final List<MethodInfo> extraMethods = new ArrayList<>();
    final List<Class<?>> ctrArgs = new ArrayList<>();
    ConstructorFieldNameGenerator constructorFieldNameGenerator = DefaultConstructorFieldNameGenerator.INSTANCE;
    MethodHandles.Lookup definer;

    MethodResolveHandle resolveHandle;
    boolean useAnonymous = true;


    static final MethodHandle METHOD_HANDLE_Lookup_defineHiddenClass;
    static final Predicate<Class<?>> CLASS_PREDICATE_isSealed;

    static {
        try {
            // public Lookup defineHiddenClass(byte[] bytes, boolean initialize, ClassOption... options)
            MethodHandle mh = null;
            var lk = MethodHandles.lookup();
            try {
                var copt = (Class<? extends Enum>) Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption");

                mh = lk.findVirtual(MethodHandles.Lookup.class, "defineHiddenClass", MethodType.methodType(
                        MethodHandles.Lookup.class, byte[].class, boolean.class,
                        copt.arrayType()
                ));
                var vvv = List.of(
                        Enum.valueOf(copt, "NESTMATE")
                ).toArray((Object[]) Array.newInstance(copt, 0));

                mh = MethodHandles.insertArguments(mh, 3, (Object) vvv);

            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            }
            METHOD_HANDLE_Lookup_defineHiddenClass = mh;

            mh = null;
            try {
                mh = lk.findVirtual(Class.class, "isSealed", MethodType.methodType(boolean.class));
            } catch (NoSuchMethodException ignored) {
            }
            if (mh == null) {
                mh = MethodHandles.dropArguments(MethodHandles.constant(boolean.class, false), 0, Class.class);
            }

            //noinspection unchecked
            CLASS_PREDICATE_isSealed = (Predicate<Class<?>>) LambdaMetafactory.metafactory(lk,
                    "test",
                    MethodType.methodType(Predicate.class),
                    MethodType.methodType(boolean.class, Object.class),
                    mh,
                    MethodType.methodType(boolean.class, Class.class)
            ).dynamicInvoker().invoke();
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public AnyWarpBuilder types(Class<?>... itfs) {
        for (var itf : itfs) {
            if (!itf.isInterface()) {
                throw new IllegalArgumentException(itf + " is not a interface");
            }
            if (CLASS_PREDICATE_isSealed.test(itf)) {
                throw new IllegalArgumentException("Cannot warp a sealed interface");
            }
        }
        supItfs.addAll(List.of(itfs));
        return this;
    }

    @Override
    public AnyWarpBuilder types(String... itfs) {
        directItfs.addAll(List.of(itfs));
        return this;
    }

    @Override
    public AnyWarpBuilder addMethod(String name, String descriptor, int modifier) {
        extraMethods.add(new MethodInfo(
                Objects.requireNonNull(name, "name"),
                Objects.requireNonNull(descriptor, "descriptor"),
                modifier
        ));
        return this;
    }

    @Override
    public AnyWarpBuilder constructors(Class<?>... argTypes) {
        ctrArgs.addAll(List.of(argTypes));
        return this;
    }

    @Override
    public AnyWarpBuilder consturctorFieldNameGenerator(ConstructorFieldNameGenerator generator) {
        this.constructorFieldNameGenerator = Objects.requireNonNull(generator, "constructorFieldNameGenerator");
        return this;
    }

    @Override
    public AnyWarpBuilder definer(MethodHandles.Lookup definer) {
        Objects.requireNonNull(definer, "definer");
        //noinspection deprecation
        if (!definer.hasPrivateAccess()) {
            throw new IllegalArgumentException("Require full privilege access method lookup: " + definer);
        }
        this.definer = definer;
        return this;
    }

    @Override
    public AnyWarpBuilder resolver(MethodResolveHandle resolveHandle) {
        this.resolveHandle = resolveHandle;
        return this;
    }

    @Override
    public AnyWarpBuilder useAnonymous(boolean useAnonymous) {
        this.useAnonymous = useAnonymous;
        return this;
    }

    @Override
    public MethodHandles.Lookup doDefine() {
        if (definer == null) throw new IllegalStateException("definer not found");
        if (resolveHandle == null) throw new IllegalStateException("resolve handle not found");

        String wrpClassName;
        {
            var bwrpname = definer.lookupClass().getName().replace('.', '/') + "$$AnyWarp$$";
            if (METHOD_HANDLE_Lookup_defineHiddenClass == null || !useAnonymous) {
                bwrpname += UUID.randomUUID();
            }
            wrpClassName = bwrpname;
        }

        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_FINAL, wrpClassName, null, "java/lang/Object",
                Stream.concat(
                        supItfs.stream().map(Type::getType).map(Type::getInternalName),
                        directItfs.stream()
                ).toArray(String[]::new)

        );

        {
            var initc = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", Type.getMethodDescriptor(
                    Type.VOID_TYPE, ctrArgs.stream().map(Type::getType).toArray(Type[]::new)
            ), null, null);

            initc.visitVarInsn(Opcodes.ALOAD, 0);
            initc.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            int slot = 1;
            int index = 0;

            for (var argc : ctrArgs) {
                var argt = Type.getType(argc);

                initc.visitVarInsn(Opcodes.ALOAD, 0);
                initc.visitVarInsn(argt.getOpcode(Opcodes.ILOAD), slot);

                var fname = constructorFieldNameGenerator.generateFieldName(index, argc);
                cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fname, argt.getDescriptor(), null, null);
                initc.visitFieldInsn(Opcodes.PUTFIELD, wrpClassName, fname, argt.getDescriptor());

                index++;
                slot += argt.getSize();
            }

            initc.visitInsn(Opcodes.RETURN);
            initc.visitMaxs(0, 0);
        }
        {
            var rsp = supItfs.stream()
                    .flatMap(k -> Stream.of(k.getMethods()))
                    .map(MethodInfo::new)
                    .collect(Collectors.toCollection(HashSet::new));

            extraMethods.forEach(rsp::remove);
            rsp.addAll(extraMethods);

            var metResolver = new MethodResolver() {
                private void loadVars(MethodVisitor cv, Type args) {

                    var slot = 1;
                    for (var arg : args.getArgumentTypes()) {
                        cv.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), slot);
                        slot += arg.getSize();
                    }
                }

                @Override
                public InsnWriter redirectToField(int index) {
                    return (writer, metname, methodDesc, wrpname) -> {

                        var redirectedType = ctrArgs.get(index);

                        writer.visitVarInsn(Opcodes.ALOAD, 0);
                        writer.visitFieldInsn(
                                Opcodes.GETFIELD,
                                wrpClassName,
                                constructorFieldNameGenerator.generateFieldName(
                                        index, redirectedType
                                ),
                                Type.getDescriptor(redirectedType)
                        );

                        loadVars(writer, methodDesc);
                        boolean isItf = redirectedType.isInterface();
                        writer.visitMethodInsn(
                                isItf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                                Type.getInternalName(redirectedType),
                                metname, methodDesc.getDescriptor(),
                                isItf
                        );


                        writer.visitInsn(methodDesc.getReturnType().getOpcode(Opcodes.IRETURN));
                    };
                }

                @Override
                public InsnWriter invokeDynamic(String bootstrap, String bootstrapName, String bootstrapDescription, Object[] bootstrapArgs) {
                    return invokeDynamic(bootstrap, bootstrapName, bootstrapDescription, null, bootstrapArgs);
                }

                @Override
                public InsnWriter invokeDynamic(String bootstrap, String bootstrapName, String bootstrapDescription, String fncName, Object[] bootstrapArgs) {
                    return (writer, metName, methodDesc, wrapperClassName) -> {
                        writer.visitVarInsn(Opcodes.ALOAD, 0);

                        loadVars(writer, methodDesc);

                        writer.visitInvokeDynamicInsn(
                                fncName == null ? metName : fncName,
                                // Sadly JVM cannot resolve the anonymous class itself
                                //"(L" + wrapperClassName + ";" + methodDesc.getDescriptor().substring(1),
                                "(Ljava/lang/Object;" + methodDesc.getDescriptor().substring(1),
                                new Handle(
                                        Opcodes.H_INVOKESTATIC, bootstrap, bootstrapName, bootstrapDescription, false
                                ),
                                bootstrapArgs == null ? new Object[0] : bootstrapArgs
                        );

                        writer.visitInsn(methodDesc.getReturnType().getOpcode(Opcodes.IRETURN));
                    };
                }
            };

            for (var metInfo : rsp) {
                var metWriter = cw.visitMethod(metInfo.modifiers, metInfo.name, metInfo.desc, null, null);
                resolveHandle.resolve(metResolver, metInfo.name, metInfo.desc, metInfo.modifiers)
                        .doWrite(metWriter, metInfo.name, Type.getMethodType(metInfo.desc), wrpClassName);

                metWriter.visitMaxs(0, 0);
            }
        }

        try {
            if (METHOD_HANDLE_Lookup_defineHiddenClass == null || !useAnonymous) {
                return MethodHandles.privateLookupIn(definer.defineClass(cw.toByteArray()), definer);
            } else {
                return (MethodHandles.Lookup) METHOD_HANDLE_Lookup_defineHiddenClass.invoke(definer, cw.toByteArray(), false);
            }
        } catch (RuntimeException runtimeException) {
            throw runtimeException;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
