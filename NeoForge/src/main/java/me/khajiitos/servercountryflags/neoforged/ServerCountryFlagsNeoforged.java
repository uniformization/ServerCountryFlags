package me.khajiitos.servercountryflags.neoforged;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.ClothConfigCheck;
import me.khajiitos.servercountryflags.common.config.ClothConfigScreenMaker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ServerCountryFlags.MOD_ID)
public class ServerCountryFlagsNeoforged {
    public ServerCountryFlagsNeoforged(IEventBus eventBus) {
        eventBus.addListener(ServerCountryFlagsNeoforged::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent e) {
        ServerCountryFlags.init();

        if (ClothConfigCheck.isInstalled()) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (modContainer, screen) -> ClothConfigScreenMaker.create(screen));
        }
    }
}