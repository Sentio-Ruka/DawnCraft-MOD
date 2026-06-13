package com.ruka.dawnautotranslator.mixin;

import com.ruka.dawnautotranslator.Translator;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), require = 0, cancellable = true)
    private void dawnautotranslator$onAddMessageSimple(Component message, CallbackInfo ci) {
        Component replacement = Translator.replacementComponentForChatMessage(message);
        if (replacement != null) {
            ci.cancel();
            ((ChatComponent)(Object)this).addMessage(replacement);
            return;
        }
        Translator.observeAndQueueChatComponent("chatcomponent.addMessage.simple", message == null ? null : message.getString());
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;IIZ)V", at = @At("HEAD"), require = 0, cancellable = true)
    private void dawnautotranslator$onAddMessageFull(Component message, int messageId, int timestamp, boolean refresh, CallbackInfo ci) {
        Component replacement = Translator.replacementComponentForChatMessage(message);
        if (replacement != null) {
            ci.cancel();
            ((ChatComponent)(Object)this).addMessage(replacement);
            return;
        }
        Translator.observeAndQueueChatComponent("chatcomponent.addMessage.full", message == null ? null : message.getString());
    }
}
