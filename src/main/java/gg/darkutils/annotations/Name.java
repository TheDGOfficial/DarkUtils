package gg.darkutils.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Renames the annotated class, method, or field at the JVM bytecode level.
 *
 * <p><b>Important:</b> References to the original source-level name are not
 * updated automatically by the bytecode processor.
 *
 * <p>This allows assigning names that are not expressible in Java source code,
 * but are valid according to the JVM specification. For example:
 *
 * <ul>
 *     <li>Reserved Java keywords (e.g. {@code class}, {@code enum})</li>
 *     <li>Names containing whitespace</li>
 *     <li>Names containing characters normally disallowed by the Java grammar</li>
 * </ul>
 *
 * <p>Additionally, this may be used to create multiple methods with the same
 * name and parameter types but different return types. While this is illegal
 * in Java source code, it is valid in JVM bytecode because method resolution
 * always uses the full descriptor (name + parameter types + return type).
 *
 * <p>This can be used to preserve binary compatibility across API migrations,
 * for example:
 *
 * <pre>{@code
 * @Synthetic
 * @Name("getOnlinePlayers")
 * Player[] getOnlinePlayersLegacy() { ... }
 *
 * Iterable<Player> getOnlinePlayers() { ... }
 * }</pre>
 *
 * <p>However, such overloads are ambiguous at compile time. Marking one of the
 * implementations with {@link Synthetic} prevents it from being referenced by
 * Java source code.
 *
 * <p>The supplied name must satisfy JVM unqualified name rules
 * (JVMS §4.2.2).
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Name {
    /**
     * The new JVM member or internal class name.
     */
    @NotNull
    String value();
}

