package com.ruka.dawnautotranslator.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ruka.dawnautotranslator.Translator;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin {
    @Shadow public abstract int draw(PoseStack poseStack, String text, float x, float y, int color);
    @Shadow public abstract int drawShadow(PoseStack poseStack, String text, float x, float y, int color);

    @Inject(method = "draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawString(PoseStack poseStack, String text, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        String translated = Translator.translateForDraw("font.draw.string", text);
        if (Translator.isDifferent(text, translated)) cir.setReturnValue(this.draw(poseStack, translated, x, y, color));
    }

    @Inject(method = "drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawShadowString(PoseStack poseStack, String text, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        String translated = Translator.translateForDraw("font.drawShadow.string", text);
        if (Translator.isDifferent(text, translated)) cir.setReturnValue(this.drawShadow(poseStack, translated, x, y, color));
    }

    @Inject(method = "draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawComponent(PoseStack poseStack, Component component, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        if (component == null) return;
        String text = component.getString();
        String translated = Translator.translateForDraw("font.draw.component", text);
        if (Translator.isDifferent(text, translated)) cir.setReturnValue(this.draw(poseStack, translated, x, y, color));
    }

    @Inject(method = "drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawShadowComponent(PoseStack poseStack, Component component, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        if (component == null) return;
        String text = component.getString();
        String translated = Translator.translateForDraw("font.drawShadow.component", text);
        if (Translator.isDifferent(text, translated)) cir.setReturnValue(this.drawShadow(poseStack, translated, x, y, color));
    }

    @Inject(method = "draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/util/FormattedCharSequence;FFI)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawFormatted(PoseStack poseStack, FormattedCharSequence sequence, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        String translated = Translator.translateForFormattedDraw("font.draw.formatted", sequence, x, y);
        if (translated != null) {
            cir.setReturnValue(this.draw(poseStack, translated, x, y, color));
        }
    }

    @Inject(method = "drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/util/FormattedCharSequence;FFI)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawShadowFormatted(PoseStack poseStack, FormattedCharSequence sequence, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        String translated = Translator.translateForFormattedDraw("font.drawShadow.formatted", sequence, x, y);
        if (translated != null) {
            cir.setReturnValue(this.drawShadow(poseStack, translated, x, y, color));
        }
    }
}
