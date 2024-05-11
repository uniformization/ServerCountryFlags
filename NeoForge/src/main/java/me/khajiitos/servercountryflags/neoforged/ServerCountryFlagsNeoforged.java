package me.khajiitos.servercountryflags.neoforged;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.ClothConfigCheck;
import me.khajiitos.servercountryflags.common.config.ClothConfigScreenMaker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ServerCountryFlags.MOD_ID)
public class ServerCountryFlagsNeoforged {
    public ServerCountryFlagsNeoforged() {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            ServerCountryFlags.init();

            if (ClothConfigCheck.isInstalled()) {
                ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (modContainer, screen) -> ClothConfigScreenMaker.create(screen));
            }
        }
    }
}