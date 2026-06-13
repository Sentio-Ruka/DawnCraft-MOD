package com.ruka.dawnautotranslator.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ruka.dawnautotranslator.Translator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiComponent.class)
public abstract class GuiComponentMixin {
    @Inject(method = "drawString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dat$drawString(PoseStack poseStack, Font font, String text, int x, int y, int color, CallbackInfo ci) {
        String translated = Translator.translateForDraw("gui.drawString.string", text);
        if (Translator.isDifferent(text, translated)) {
            font.drawShadow(poseStack, translated, (float)x, (float)y, color);
            ci.cancel();
        }
    }

    @Inject(method = "drawString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dat$drawStringComponent(PoseStack poseStack, Font font, Component component, int x, int y, int color, CallbackInfo ci) {
        if (component == null) return;
        String text = component.getString();
        String translated = Translator.translateForDraw("gui.drawString.component", text);
        if (Translator.isDifferent(text, translated)) {
            font.drawShadow(poseStack, translated, (float)x, (float)y, color);
            ci.cancel();
        }
    }

    @Inject(method = "drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dat$drawCenteredString(PoseStack poseStack, Font font, String text, int x, int y, int color, CallbackInfo ci) {
        String[] block = Translator.classDescriptionReplacementForCentered(text);
        if (block != null) {
            for (int i = 0; i < block.length; i++) {
                String line = block[i];
                font.drawShadow(poseStack, line, (float)(x - font.width(line) / 2), (float)(y + i * 9), color);
            }
            ci.cancel();
            return;
        }
        String translated = Translator.translateForDraw("gui.drawCenteredString.string", text);
        if (Translator.isDifferent(text, translated)) {
            font.drawShadow(poseStack, translated, (float)(x - font.width(translated) / 2), (float)y, color);
            ci.cancel();
        }
    }

    @Inject(method = "drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dat$drawCenteredStringComponent(PoseStack poseStack, Font font, Component component, int x, int y, int color, CallbackInfo ci) {
        if (component == null) return;
        String text = component.getString();
        String[] block = Translator.classDescriptionReplacementForCentered(text);
        if (block != null) {
            for (int i = 0; i < block.length; i++) {
                String line = block[i];
                font.drawShadow(poseStack, line, (float)(x - font.width(line) / 2), (float)(y + i * 9), color);
            }
            ci.cancel();
            return;
        }
        String translated = Translator.translateForDraw("gui.drawCenteredString.component", text);
        if (Translator.isDifferent(text, translated)) {
            font.drawShadow(poseStack, translated, (float)(x - font.width(translated) / 2), (float)y, color);
            ci.cancel();
        }
    }

    @Inject(method = "drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dat$drawCenteredFormatted(PoseStack poseStack, Font font, FormattedCharSequence sequence, int x, int y, int color, CallbackInfo ci) {
        String translated = Translator.translateForFormattedDraw("gui.drawCenteredString.formatted", sequence, x, y);
        if (translated != null) {
            font.drawShadow(poseStack, translated, (float)(x - font.width(translated) / 2), (float)y, color);
            ci.cancel();
        }
    }
}
