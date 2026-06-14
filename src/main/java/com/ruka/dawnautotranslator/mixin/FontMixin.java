package com.ruka.dawnautotranslator.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ruka.dawnautotranslator.Translator;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin {
    @Shadow public abstract int draw(PoseStack poseStack, String text, float x, float y, int color);
    @Shadow public abstract int drawShadow(PoseStack poseStack, String text, float x, float y, int color);
    @Shadow public abstract int drawInBatch(String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, boolean seeThrough, int backgroundColor, int packedLight);
    @Shadow public abstract int drawInBatch(Component component, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, boolean seeThrough, int backgroundColor, int packedLight);
    @Shadow public abstract int drawInBatch(FormattedCharSequence sequence, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, boolean seeThrough, int backgroundColor, int packedLight);

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
            int fixedColor = Translator.colorForTranslatedFormatted("font.draw.formatted", color);
            cir.setReturnValue(this.draw(poseStack, translated, x, y, fixedColor));
        }
    }

    @Inject(method = "drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/util/FormattedCharSequence;FFI)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawShadowFormatted(PoseStack poseStack, FormattedCharSequence sequence, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        String translated = Translator.translateForFormattedDraw("font.drawShadow.formatted", sequence, x, y);
        if (translated != null) {
            int fixedColor = Translator.colorForTranslatedFormatted("font.drawShadow.formatted", color);
            cir.setReturnValue(this.drawShadow(poseStack, translated, x, y, fixedColor));
        }
    }

    // v1.5: Some HUD/overlay title UIs do not use normal Font#draw/drawShadow.
    // They render through drawInBatch, especially centered/fading popup labels.
    @Inject(method = "drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawInBatchString(String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, boolean seeThrough, int backgroundColor, int packedLight, CallbackInfoReturnable<Integer> cir) {
        String translated = Translator.translateForDraw("font.drawInBatch.string", text);
        if (Translator.isDifferent(text, translated)) {
            cir.setReturnValue(this.drawInBatch(translated, x, y, color, dropShadow, matrix, buffer, seeThrough, backgroundColor, packedLight));
        }
    }

    @Inject(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawInBatchComponent(Component component, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, boolean seeThrough, int backgroundColor, int packedLight, CallbackInfoReturnable<Integer> cir) {
        if (component == null) return;
        String text = component.getString();
        String translated = Translator.translateForDraw("font.drawInBatch.component", text);
        if (Translator.isDifferent(text, translated)) {
            cir.setReturnValue(this.drawInBatch(translated, x, y, color, dropShadow, matrix, buffer, seeThrough, backgroundColor, packedLight));
        }
    }

    @Inject(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void dat$drawInBatchFormatted(FormattedCharSequence sequence, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, boolean seeThrough, int backgroundColor, int packedLight, CallbackInfoReturnable<Integer> cir) {
        String translated = Translator.translateForFormattedDraw("font.drawInBatch.formatted", sequence, x, y);
        if (translated != null) {
            int fixedColor = Translator.colorForTranslatedFormatted("font.drawInBatch.formatted", color);
            cir.setReturnValue(this.drawInBatch(translated, x, y, fixedColor, dropShadow, matrix, buffer, seeThrough, backgroundColor, packedLight));
        }
    }

}
