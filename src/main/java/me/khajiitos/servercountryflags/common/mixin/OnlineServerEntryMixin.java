package me.khajiitos.servercountryflags.common.mixin;

import com.maxmind.geoip2.model.CityResponse;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.FlagPosition;
import me.khajiitos.servercountryflags.common.util.FlagRenderInfo;
import me.khajiitos.servercountryflags.common.util.TooltipUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin extends ServerSelectionList.Entry {

    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    @Final
    private JoinMultiplayerScreen screen;

    @Shadow @Final private Minecraft minecraft;

    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", ordinal = 0), method = "renderContent", index = 2)
    public int serverNameX(int oldX) {
        if (Config.cfg.flagPosition == FlagPosition.BEHIND_NAME) {
            CityResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
            FlagRenderInfo renderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

            if (renderInfo != null) {
                return oldX + (int)(renderInfo.flagAspectRatio() * 8.f) + 3;
            }
        }

        return oldX;
    }

    @Inject(at = @At("TAIL"), method = "renderContent")
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        CityResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
        FlagRenderInfo flagRenderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

        if (flagRenderInfo == null) {
            return;
        }

        if (Config.cfg.flagPosition == FlagPosition.TOOLTIP_SERVER_NAME) {
            int serverNameStartX = getX() + 35;
            int serverNameStartY = getY() + 1;

            int serverNameWidth = this.minecraft.font.width(this.serverData.name);
            int serverNameHeight = 8;

            if (mouseX >= serverNameStartX && mouseX <= serverNameStartX + serverNameWidth && mouseY >= serverNameStartY && mouseY <= serverNameStartY + serverNameHeight) {
                guiGraphics.setTooltipForNextFrame(flagRenderInfo.tooltip(), mouseX, mouseY);
            }

            // TODO: maybe render the flag in the future
            // But I'm too lazy to figure out how to do that.
            return;
        } else if (Config.cfg.flagPosition == FlagPosition.TOOLTIP_PING) {
            return;
        }

        int height = Config.cfg.flagPosition == FlagPosition.BEHIND_NAME ? 8 : 12;
        int width = (int)(flagRenderInfo.flagAspectRatio() * height);

        int startingX, startingY;
        switch (Config.cfg.flagPosition) {
            case LEFT -> {
                startingX = getX() - width - 6;
                startingY = getY() + (getHeight() / 2) - (height / 2);
            }
            case RIGHT -> {
                startingX = getX() + getWidth() + 10;
                startingY = getY() + (getHeight() / 2) - (height / 2);
            }
            case BEHIND_NAME -> {
                startingX = getX() + 35;
                startingY = getY() + 1;
            }
            default -> {
                startingX = getX() + getWidth() - width - 6;
                startingY = getY() + getHeight() - height - 4;
            }
        }

        Identifier textureId = Identifier.fromNamespaceAndPath(ServerCountryFlags.MOD_ID, "textures/gui/flags/" + flagRenderInfo.countryCode() + ".png");

        //RenderSystem.enableBlend();
        guiGraphics.pose().pushMatrix();
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, textureId, startingX, startingY, 0.0F, 0.F, width, height, width, height);
        guiGraphics.pose().popMatrix();

        if (Config.cfg.flagBorder) {
            guiGraphics.renderOutline(startingX - 1, startingY - 1, width + 2, height + 2, Config.cfg.borderColor.toARGB());
        }

        //RenderSystem.disableBlend();

        if (mouseX >= startingX && mouseX <= startingX + width && mouseY >= startingY && mouseY <= startingY + height) {
            guiGraphics.setTooltipForNextFrame(flagRenderInfo.tooltip(), mouseX, mouseY);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;setTooltipForNextFrame(Lnet/minecraft/network/chat/Component;II)V", ordinal = 0, shift = At.Shift.AFTER), method = "renderContent")
    public void onSetTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        if (Config.cfg.flagPosition == FlagPosition.TOOLTIP_PING) {
            CityResponse apiResponse = ServerCountryFlags.servers.get(serverData.ip);
            FlagRenderInfo flagRenderInfo = ServerCountryFlags.getFlagRenderInfo(apiResponse);

            if (flagRenderInfo == null) {
                return;
            }

            List<FormattedCharSequence> newTooltip = new ArrayList<>(TooltipUtils.getTooltipOfScreenOrEmpty(screen));
            newTooltip.add(Component.literal(" ").getVisualOrderText());
            newTooltip.addAll(flagRenderInfo.tooltip());
            guiGraphics.setTooltipForNextFrame(newTooltip, mouseX, mouseY);
        }
    }
}
