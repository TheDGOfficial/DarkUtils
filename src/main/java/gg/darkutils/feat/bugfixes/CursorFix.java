package gg.darkutils.feat.bugfixes;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.TickUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class CursorFix {
    @Nullable
    private static Screen previousScreen;

    private CursorFix() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        TickUtils.queueRepeatingTickTask(CursorFix::onTick, 1);
    }

    private static final void onTick() {
        if (!DarkUtilsConfig.INSTANCE.cursorFix) {
            return;
        }

        final var mc = MinecraftClient.getInstance();
        final var screen = mc.currentScreen;

        if (null == screen && null != CursorFix.previousScreen) {
            // A screen was closed. Set cursor back to default cursor.
            // Prevents mouse cursor staying on screen after closing a menu that set a custom cursor.
            final var windowId = mc.getWindow().getHandle();
            GLFW.glfwSetCursor(windowId, 0L);
        }

        CursorFix.previousScreen = screen;
    }
}
