package gg.darkutils.utils;

import java.lang.StackWalker;
import java.lang.StackWalker.StackFrame;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class JavaUtils {
    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @NotNull
    public static final Optional<StackFrame> getImmediateCaller() {
        return JavaUtils.STACK_WALKER.walk(stream ->
                stream.skip(2) // [0]=getImmediateCaller, [1]=callerOfThisMethod, [2]=callerOfTheMethodCallingThisMethod
                      .findFirst()
        );
    }

    @NotNull
    public static final Optional<Class<?>> getImmediateCallerClass() {
        return JavaUtils.STACK_WALKER.walk(stream ->
                stream.skip(2) // [0]=getImmediateCallerClass, [1]=callerOfThisMethod, [2]=callerOfTheMethodCallingThisMethod
                      .map(StackFrame::getDeclaringClass)
                      .findFirst()
        );
    }

    @NotNull
    public static final Optional<StackFrame> getExternalCaller() {
        return JavaUtils.STACK_WALKER.walk(stream ->
                stream
                    .filter(f -> !f.getClassName().startsWith("gg.darkutils.")) // no skip, if the caller is already external, return that, this class is in gg.darkutils.utils package so it will never return the class containing getExternalCaller.
                    .findFirst()
        );
    }

    private JavaUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }
}

