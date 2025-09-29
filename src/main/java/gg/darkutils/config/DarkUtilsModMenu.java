package gg.darkutils.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class DarkUtilsModMenu implements ModMenuApi {
    public DarkUtilsModMenu() {
        super();
    }

    @Override
    public final ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DarkUtilsConfigScreen::create;
    }
}

