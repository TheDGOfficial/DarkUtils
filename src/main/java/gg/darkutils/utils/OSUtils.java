package gg.darkutils.utils;

import gg.darkutils.DarkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class OSUtils {
    private OSUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    /**
     * Runs the given command in the operating system safely.
     * <p>
     * This will redirect error stream to stderr, log the commands output as info level, block till
     * the command finishes execution, and check that the exit code of the command is zero, while
     * handling interruptions.
     * <p>
     * It is important to note that the command might fail without setting exit code properly if it's
     * a non-standard misbehaving command.
     *
     * @param command The command to run in the operating system.
     * @throws IOException If any operating-system dependant error occurs whilst starting the process.
     */
    public static final void runCommand(@NotNull final String... command) throws IOException {
        final var commandWithArguments = String.join(" ", command);

        DarkUtils.info(OSUtils.class, "Running command: {}", commandWithArguments);
        final var process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        final var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        DarkUtils.info(OSUtils.class, output.isBlank() ? "No output from the command; assume OK." : output);

        try {
            final var exitCode = process.waitFor();

            if (0 != exitCode) {
                throw new IOException(
                        "Command \"" + commandWithArguments + "\" failed with exit code "
                                + exitCode
                                + (output.isBlank() ? "" : ": " + output)
                );
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IOException(
                    "Interrupted while executing command \"" + commandWithArguments + '"',
                    exception
            );
        }
    }

    /**
     * Returns true if an environment variable with the given name is defined and equals exactly to the given value, case-sensitive.
     *
     * @param environmentVariableName  The environment variable name.
     * @param environmentVariableValue The expected environment variable value.
     * @return True if the environment variable has a value that equals exactly the expected value.
     */
    public static final boolean environmentVariableEquals(@NotNull final String environmentVariableName, @NotNull final String environmentVariableValue) {
        return environmentVariableValue.equals(OSUtils.getEnvironmentVariable(environmentVariableName));
    }

    /**
     * Gets the environment variable value with the given name, returning null if it is not defined, defined to an empty value, or defined to whitespace only.
     *
     * @param variableName The name of the environment variable to obtain the value of.
     * @return The environment variable's value if defined, not-empty and not-whitespace; or null otherwise.
     */
    @Nullable
    public static final String getEnvironmentVariable(@NotNull final String variableName) {
        final var environmentVariable = System.getenv(variableName);

        return null == environmentVariable || environmentVariable.isBlank() ? null : environmentVariable;
    }
}
