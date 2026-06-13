package com.ruka.dawnautotranslator;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChatHandler {
    @SubscribeEvent
    public void onClientChat(ClientChatReceivedEvent event) {
        Component message = event.getMessage();
        if (message == null) return;
        String original = message.getString();
        String translated = Translator.translateForChatImmediate(original);
        if (translated != null && !translated.equals(original)) {
            event.setMessage(new TextComponent(translated));
        } else {
            Translator.queueChatTranslation(original);
        }
    }
}
