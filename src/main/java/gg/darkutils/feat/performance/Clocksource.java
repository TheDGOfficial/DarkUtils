package gg.darkutils.feat.performance;

import org.lwjgl.sdl.SDLTimer;

public final class Clocksource {
    // Keep the SDL epoch when switching clocks so existing client timestamps stay comparable.
    private static final long SDL_TIME_OFFSET = SDLTimer.SDL_GetTicksNS() - System.nanoTime();

    private Clocksource() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final long getNanos() {
        return System.nanoTime() + Clocksource.SDL_TIME_OFFSET;
    }
}
