package me.khajiitos.servercountryflags.common.mixin;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin extends Screen {
    @Shadow
    private ServerList servers;

    private JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructor(CallbackInfo info) {
        if (Config.cfg.reloadOnRefresh) {
            ServerCountryFlags.servers.clear();
        }
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo info) {
        for (int i = 0; i < this.servers.size(); i++) {
            if (!ServerCountryFlags.servers.containsKey(this.servers.get(i).ip)) {
                ServerCountryFlags.updateServerLocationInfo(this.servers.get(i).ip);
            }
        }
    }
}
