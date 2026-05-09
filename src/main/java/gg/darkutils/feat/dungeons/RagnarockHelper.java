package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.SimpleColor;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

public final class RagnarockHelper {
    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
            "[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you.", event -> {
                if (event.isStyledWith(SimpleColor.DARK_RED)) {
                    RagnarockHelper.notifyUseRagnarock();
                }
            },
            "[BOSS] Livid: I can now turn those Spirits into shadows of myself, identical to their creator.", event -> {
                if (event.isStyledWith(SimpleColor.DARK_RED)) {
                    RagnarockHelper.notifyUseRagnarock();
                }
            }
    );

    private RagnarockHelper() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(RagnarockHelper::onChat);
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.ragnarockHelper) {
            return;
        }

        event.match(RagnarockHelper.MESSAGE_HANDLERS);
    }

    private static final void notifyUseRagnarock() {
        Helpers.notify(SoundEvents.NOTE_BLOCK_PLING.value(), "§6§lRAG!", 60);
    }
}
