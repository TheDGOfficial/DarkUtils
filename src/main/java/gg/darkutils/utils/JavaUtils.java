package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class JavaUtils {
    private static final @NotNull StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private JavaUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    @NotNull
    public static final Optional<StackWalker.StackFrame> getImmediateCaller() {
        return JavaUtils.STACK_WALKER.walk(stream ->
                stream.skip(2L) // [0]=getImmediateCaller, [1]=callerOfThisMethod, [2]=callerOfTheMethodCallingThisMethod
                        .findFirst()
        );
    }

    @NotNull
    public static final Optional<? extends Class<?>> getImmediateCallerClass() {
        return JavaUtils.STACK_WALKER.walk(stream ->
                stream.skip(2L) // [0]=getImmediateCallerClass, [1]=callerOfThisMethod, [2]=callerOfTheMethodCallingThisMethod
                        .map(StackWalker.StackFrame::getDeclaringClass)
                        .findFirst()
        );
    }

    @NotNull
    public static final Optional<StackWalker.StackFrame> getExternalCaller() {
        return JavaUtils.STACK_WALKER.walk(stream ->
                stream
                        .filter(stackFrame -> !stackFrame.getClassName().startsWith("gg.darkutils.")) // no skip, if the caller is already external, return that, this class is in gg.darkutils.utils package so it will never return the class containing getExternalCaller.
                        .findFirst()
        );
    }

    /**
     * Creates a generic array instance. Those are normally not possible, but
     * with the help of the varargs feature, we can accomplish it with a small wrapper method.
     *
     * @param values The values to create a generic array of.
     * @return A generic array from the given values, created by the compiler at call site.
     * @param <T> The type of the generic array.
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    @NotNull
    public static final <T> T[] createGenericArray(@NotNull final T... values) {
        return values;
    }

    /**
     * Sneakily throws the given exception. Normally, checked exceptions must be wrapped in, for example,
     * a {@link RuntimeException} or {@link java.io.UncheckedIOException}, but abusing generics and using
     * a small utility method, we can directly throw any exception, even if it's a checked exception.
     * <p>
     * This is possible since all exceptions extend from the {@link Throwable} class.
     *
     * @param t The exception to throw, bypassing the checked exception check on Java compiler.
     * @param <T> The type of the exception to throw.
     * @return Nothing, this method always throws. This only for your convenience, so you can use throw yourself
     * to not have to return a value from your method.
     * @throws T The given exception.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static final <T extends Throwable> T sneakyThrow(@NotNull final Throwable t) throws T {
        throw (T) t;
    }

    /**
     * Gets the class of the given type parameter. This method only shows this possible, and for testing,
     * you should not call this method passing type parameter from another method as compiler will just make
     * this method receive the type parameter as {@link Object}, instead you should copy what trick this
     * method does into your own method.
     *
     * @param doNotPassThisParameter Do not pass this parameter. It will be passed by compiler.
     *                               The compiler creates an empty array at call site with the inferred type.
     * @return The class of the given type parameter.
     * @param <T> The type to get the class of.
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    @NotNull
    public static final <T> Class<T> getReifiedType(@NotNull final T... doNotPassThisParameter) {
        return (Class<T>) doNotPassThisParameter.getClass().getComponentType();
    }
}

