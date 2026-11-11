package com.viscriptquests;

import com.viscriptquests.config.ClientConfig;
import com.viscriptquests.config.QuestConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Screen-typed lambdas must never be declared on the common mod entry class. */
@OnlyIn(Dist.CLIENT)
final class ViScriptQuestsClient {
    private ViScriptQuestsClient() {}

    static void registerConfig() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new QuestConfigScreen(parent)));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC,
                ViScriptQuests.MOD_ID + "_client_config.toml");
    }
}
