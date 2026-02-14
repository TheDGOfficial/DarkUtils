package gg.darkutils;

import gg.darkutils.utils.JavaUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

final class JavaUtilsTest {
    @Test
    void getImmediateCaller_returnsCallerOfCaller() {
        final var frame = JavaUtilsTest.level1();

        Assertions.assertTrue(frame.isPresent());
        Assertions.assertEquals(JavaUtilsTest.class, frame.get().getDeclaringClass());
        Assertions.assertEquals("level1", frame.get().getMethodName());
    }

    private static java.util.Optional<StackWalker.StackFrame> level1() {
        return JavaUtilsTest.level2();
    }

    private static java.util.Optional<StackWalker.StackFrame> level2() {
        return JavaUtils.getImmediateCaller();
    }

    @Test
    void getImmediateCallerClass_returnsCorrectClass() {
        final var clazz = JavaUtilsTest.level1Class();

        Assertions.assertTrue(clazz.isPresent());
        Assertions.assertEquals(JavaUtilsTest.class, clazz.get());
    }

    private static java.util.Optional<? extends Class<?>> level1Class() {
        return JavaUtilsTest.level2Class();
    }

    private static java.util.Optional<? extends Class<?>> level2Class() {
        return JavaUtils.getImmediateCallerClass();
    }

    @Test
    void getExternalCaller_returnsExternalFrame() {
        final var frame = JavaUtils.getExternalCaller();

        Assertions.assertTrue(frame.isPresent());
        Assertions.assertFalse(frame.get().getClassName().startsWith("gg.darkutils."));
    }
}

