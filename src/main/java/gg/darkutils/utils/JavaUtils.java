package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

final class JavaUtils {
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
}

