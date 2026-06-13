package com.ruka.dawnautotranslator.mixin;

import com.ruka.dawnautotranslator.Translator;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public abstract class LanguageMixin {
    @Inject(method = "getOrDefault", at = @At("RETURN"), cancellable = true)
    private void dawnautotranslator$translateMissingEnglish(String key, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(Translator.translate(key, cir.getReturnValue()));
    }
}
