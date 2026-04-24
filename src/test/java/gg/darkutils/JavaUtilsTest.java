package gg.darkutils;

import gg.darkutils.utils.JavaUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

final class JavaUtilsTest {
    private static final Optional<StackWalker.StackFrame> level1() {
        return JavaUtilsTest.level2();
    }

    private static final Optional<StackWalker.StackFrame> level2() {
        return JavaUtils.getImmediateCaller();
    }

    private static final Optional<Class<?>> level1Class() {
        return JavaUtilsTest.level2Class();
    }

    private static final Optional<Class<?>> level2Class() {
        return JavaUtils.getImmediateCallerClass();
    }

    @Test
    final void getImmediateCaller_returnsCallerOfCaller() {
        final var frame = JavaUtilsTest.level1();

        Assertions.assertTrue(frame.isPresent(), "Frame should be present");
        Assertions.assertEquals(JavaUtilsTest.class, frame.get().getDeclaringClass(), "Declaring class should be JavaUtilsTest");
        Assertions.assertEquals("level1", frame.get().getMethodName(), "Method name should be level1");
    }

    @Test
    final void getImmediateCallerClass_returnsCorrectClass() {
        final var clazz = JavaUtilsTest.level1Class();

        Assertions.assertTrue(clazz.isPresent(), "Class should be present");
        Assertions.assertEquals(JavaUtilsTest.class, clazz.get(), "Class should be JavaUtilsTest");
    }

    @Test
    final void getExternalCaller_returnsExternalFrame() {
        final var frame = JavaUtils.getExternalCaller();

        Assertions.assertTrue(frame.isPresent(), "External frame should be present");
        Assertions.assertFalse(frame.get().getClassName().startsWith("gg.darkutils."), "External caller should not be from internal package");
    }
}