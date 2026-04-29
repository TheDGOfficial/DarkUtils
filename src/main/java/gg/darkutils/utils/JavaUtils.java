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
    @SuppressWarnings("unchecked")
    public static final Optional<Class<?>> getImmediateCallerClass() {
        return (Optional<Class<?>>) (Object) JavaUtils.STACK_WALKER.walk(stream ->
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
     * Sneakily throws the given exception. Normally, checked exceptions must be wrapped in, for example,
     * a {@link RuntimeException} or {@link java.io.UncheckedIOException}, but abusing generics and using
     * a small utility method, we can directly throw any exception, even if it's a checked exception.
     * <p>
     * This is possible since all exceptions extend from the {@link Throwable} class.
     *
     * @param t   The exception to throw, bypassing the checked exception check on Java compiler.
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
     * Checks if the thread's priority is not already set to the given value, then calls the native {@link Thread#setPriority(int)} method.
     * Checking before if priority is already set makes 1 less JNI native method call since {@link Thread#getPriority()} is not a native method,
     * and returns the last-changed priority.
     *
     * @param thread   The thread to perform the priority check/change on.
     * @param priority The priority the thread should already have or will be set to.
     */
    public static final void setThreadPriority(@NotNull final Thread thread, final int priority) {
        if (priority != thread.getPriority()) {
            thread.setPriority(priority);
        }
    }
}

