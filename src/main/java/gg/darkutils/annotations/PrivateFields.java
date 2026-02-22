package gg.darkutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Causes all fields to be made private in a given class when annotated with it.
 * <p>
 * This is useful when javac generates fields that you can't control for such as
 * inner classes or method local classes capturing local variables, javac generates those
 * as package-private, even if you never access the field outside of the class itself (in
 * which case it can be made private safely).
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface PrivateFields {
}

