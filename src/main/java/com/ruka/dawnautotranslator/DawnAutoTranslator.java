package com.ruka.dawnautotranslator;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

@Mod(DawnAutoTranslator.MODID)
public class DawnAutoTranslator {
    public static final String MODID = "dawnautotranslator";
    public DawnAutoTranslator() {
        Translator.init();
        MinecraftForge.EVENT_BUS.register(new ChatHandler());
    }
}
