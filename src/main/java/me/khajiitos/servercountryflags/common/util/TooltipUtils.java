package me.khajiitos.servercountryflags.common.util;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;

public class TooltipUtils {

    public static List<FormattedCharSequence> getTooltipOfScreen(@NotNull Screen screen) {
        // TODO: find a way not to use Reflection

        try {
            // Trying to find "DeferredTooltipRendering deferredTooltipRendering"
            // Without having access to it
            for (Field field : Screen.class.getDeclaredFields()) {
                if (Record.class.isAssignableFrom(field.getType())) {
                    for (Field innerField : field.getType().getDeclaredFields()) {
                        if (innerField.getType() == List.class) {
                            // Hopefully, "field" is "deferredTooltipRendering"
                            // and "innerField" is "tooltip"

                            field.setAccessible(true);
                            innerField.setAccessible(true);

                            Object deferredTooltipRendering = field.get(screen);
                            return (List<FormattedCharSequence>) innerField.get(deferredTooltipRendering);
                        }
                    }
                }
            }
        } catch (IllegalAccessException ignored) {}

        return null;
    }

    public static @NotNull List<FormattedCharSequence> getTooltipOfScreenOrEmpty(@NotNull Screen screen) {
        List<FormattedCharSequence> list = getTooltipOfScreen(screen);
        return list != null ? list : List.of();
    }
}
