package com.ruka.dawnautotranslator;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.TextComponent;
import java.util.concurrent.*;
import java.util.regex.*;

public final class Translator {
    private static final Properties CACHE = new Properties();
    private static final Properties DICT = new Properties();
    private static final Properties CONFIG = new Properties();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final Map<String, String[]> CLASS_DESC_EN = new LinkedHashMap<>();
    private static final Map<String, String[]> CLASS_DESC_JA = new LinkedHashMap<>();
    private static volatile String currentClass = null;
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DawnAutoTranslator-Worker");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static Path DIR, CACHE_FILE, DICT_FILE, LOG_FILE;
    private static volatile boolean enabled = true;
    private static volatile boolean onlineTranslation = true;
    private static volatile int minAutoTranslateLength = 10;

    private static final Pattern HAS_LATIN = Pattern.compile("[A-Za-z]");
    private static final Pattern HAS_JA = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    private static final Pattern PLACEHOLDER = Pattern.compile("(§.|%\\d+\\$[a-zA-Z]|%[a-zA-Z]|\\{\\d+}|\\$\\{[^}]+})");

    private Translator() {}

    public static void init() {
        try {
            DIR = FMLPaths.CONFIGDIR.get().resolve("dawnautotranslator");
            Files.createDirectories(DIR);
            CACHE_FILE = DIR.resolve("translate_cache.properties");
            DICT_FILE = DIR.resolve("dictionary.properties");
            LOG_FILE = DIR.resolve("seen_english.log");
            Path CONFIG_FILE = DIR.resolve("config.properties");

            if (Files.notExists(CONFIG_FILE)) {
                Files.writeString(CONFIG_FILE,
                        "# Dawn Auto Translator prototype for DawnCraft 2.0.16 / Forge 1.18.2\n" +
                        "# DeepL対応版。APIキーはここへ入れてください。jar本体には入れません。\n" +
                        "# deeplApiKey は DeepL API Free なら末尾が :fx のキーです。\n" +
                        "enabled=true\n" +
                        "onlineTranslation=true\n" +
                        "deeplApiKey=\n" +
                        "targetLang=JA\n" +
                        "# DeepL Free is https://api-free.deepl.com/v2/translate, Pro is https://api.deepl.com/v2/translate\n" +
                        "deeplApiUrl=auto\n" +
                        "minAutoTranslateLength=10\n" +
                        "maxLength=600\n" +
                        "translateChat=true\n" +
                        "showAsyncChatTranslations=true\n" +
                        "replaceCachedChat=true\n" +
                        "preserveChatColors=true\n" +
                        "# v1.4: these toggles let you disable broad draw-hook translation if a screen becomes noisy.\n" +
                        "translateTooltips=true\n" +
                        "translateQuests=true\n" +
                        "translateJei=true\n" +
                        "translateNpc=true\n" +
                        "# Patchouli/book-like GUIs can break if every tiny layout fragment is replaced.\n" +
                        "translatePatchouliBooks=true\n" +
                        "patchouliSafeMode=true\n" +
                        "# NPC会話の翻訳が横にはみ出る場合は小さくしてください。\n" +
                        "maxNpcLineChars=34\n" +
                        "chatTranslationPrefix=[翻訳]\n" +
                        "protectPlayerName=true\n" +
                        "# If true, untranslated English strings are also written to seen_english.log.\n" +
                        "logSeenEnglish=true\n" +
                        "# Avoid repeated spam in seen_english.log.\n" +
                        "dedupeSeenEnglish=true\n" +
                        "# Comma separated language-key prefixes to never translate.\n" +
                        "excludedKeyPrefixes=chat.,commands.,multiplayer.,death.,entity.minecraft.player,gui.socialInteractions,options.online\n",
                        StandardCharsets.UTF_8);
            }
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) { CONFIG.load(in); }
            ensureConfigDefault("enabled", "true");
            ensureConfigDefault("onlineTranslation", "true");
            ensureConfigDefault("deeplApiKey", "");
            ensureConfigDefault("targetLang", "JA");
            ensureConfigDefault("deeplApiUrl", "auto");
            ensureConfigDefault("minAutoTranslateLength", "10");
            ensureConfigDefault("maxLength", "600");
            if (parseInt(CONFIG.getProperty("maxLength", "0"), 0) < 600) CONFIG.setProperty("maxLength", "600");
            ensureConfigDefault("translateChat", "true");
            ensureConfigDefault("showAsyncChatTranslations", "true");
            ensureConfigDefault("replaceCachedChat", "true");
            ensureConfigDefault("preserveChatColors", "true");
            ensureConfigDefault("translateTooltips", "true");
            ensureConfigDefault("translateQuests", "true");
            ensureConfigDefault("translateJei", "true");
            ensureConfigDefault("translateNpc", "true");
            ensureConfigDefault("translatePatchouliBooks", "true");
            ensureConfigDefault("patchouliSafeMode", "true");
            ensureConfigDefault("maxNpcLineChars", "34");
            ensureConfigDefault("chatTranslationPrefix", "[翻訳]");
            ensureConfigDefault("protectPlayerName", "true");
            ensureConfigDefault("logSeenEnglish", "true");
            ensureConfigDefault("dedupeSeenEnglish", "true");
            saveConfig(CONFIG_FILE);
            enabled = Boolean.parseBoolean(CONFIG.getProperty("enabled", "true"));
            onlineTranslation = Boolean.parseBoolean(CONFIG.getProperty("onlineTranslation", "true"));
            minAutoTranslateLength = parseInt(CONFIG.getProperty("minAutoTranslateLength", "10"), 10);

            if (Files.exists(CACHE_FILE)) try (InputStream in = Files.newInputStream(CACHE_FILE)) { CACHE.load(in); }
            if (Files.exists(DICT_FILE)) try (InputStream in = Files.newInputStream(DICT_FILE)) { DICT.load(in); }
            installDefaultDictionary();
            saveDictionaryIfMissing();
        } catch (Exception e) {
            enabled = false;
            System.err.println("[DawnAutoTranslator] init failed: " + e);
        }
    }

    public static String[] classDescriptionReplacementForCentered(String original) {
        if (!enabled || original == null) return null;
        if (!isClassSelectionScreen()) return null;
        String text = original.trim();
        if (text.isEmpty()) return null;
        updateCurrentClass(text);
        String cls = currentClass;
        if (cls == null) return null;
        String[] en = CLASS_DESC_EN.get(cls);
        String[] ja = CLASS_DESC_JA.get(cls);
        if (en == null || ja == null) return null;
        for (int i = 0; i < en.length; i++) {
            if (text.equals(en[i])) {
                if (i == 0) {
                    logSeen("class.description.replace", cls + " -> Japanese block");
                    return ja;
                }
                return new String[0];
            }
        }
        return null;
    }

    public static boolean isClassSelectionScreen() {
        return currentScreenName().equals("com.afunproject.dawncraft.classes.client.ClassSelectionScreen");
    }

    private static void updateCurrentClass(String text) {
        if (CLASS_DESC_EN.containsKey(text)) currentClass = text;
    }

    public static String translateForDraw(String source, String original) {
        if (!enabled || original == null) return original;
        updateCurrentClass(original.trim());
        String manual = DICT.getProperty(original);
        if (manual != null && !manual.isBlank()) {
            logSeen(source == null ? "replace.draw" : source + ".replace", original + " -> " + manual);
            return manual;
        }
        String cached = CACHE.getProperty(original);
        if (cached != null && !cached.isBlank()) return decodeEscapes(cached);
        observeText(source, original);

        // v1.5.2: Quest selection overlays render labels as raw strings.
        // Do not send unknown selection/button labels to DeepL here, because replacing
        // button-like text can interfere with that custom UI. Manual dictionary entries
        // above are still allowed, so known quest labels can be safely translated.
        String screenName = currentScreenName();
        if (screenName.equals("com.feywild.quest_giver.screen.SelectQuestScreen")) {
            return original;
        }

        maybeQueueOnlineTranslation(original);
        return original;
    }

    private static void maybeQueueOnlineTranslation(String original) {
        if (!onlineTranslation || original == null) return;
        String s = original.trim();
        String prefix = CONFIG.getProperty("chatTranslationPrefix", "[翻訳]");
        if (s.startsWith(prefix)) return;
        if (s.startsWith("/") || s.startsWith("@")) return;
        if (isLikelyCommandSuggestion(s)) return;
        if (!looksEnglish(s)) return;
        if (s.length() < minAutoTranslateLength) return;
        if (isIgnoredOnlineScreen()) return;
        if (!isEnabledForCurrentArea()) return;
        if (CACHE.containsKey(original) || DICT.containsKey(original)) return;
        if (PENDING.add(original)) {
            EXEC.submit(() -> {
                try {
                    String translated = requestTranslate(original);
                    if (translated != null && !translated.isBlank() && !translated.equals(original)) {
                        CACHE.setProperty(original, translated);
                        saveCache();
                        logSeen("deepl.cache", original + " -> " + translated);
                    }
                } catch (Exception e) {
                    logSeen("deepl.error", original + " / " + e.getMessage());
                } finally {
                    PENDING.remove(original);
                }
            });
        }
    }

    private static boolean isEnabledForCurrentArea() {
        String screen = currentScreenName().toLowerCase(java.util.Locale.ROOT);
        // Best-effort broad support: tooltips often draw with screen=none; keep them enabled by default.
        if (screen.contains("com.feywild.quest_giver.screen.selectquestscreen")) return false;
        if (screen.contains("com.feywild.quest_giver.screen.displayquestscreen")) return Boolean.parseBoolean(CONFIG.getProperty("translateNpc", "true"));
        if (screen.contains("jei") || screen.contains("mezz.jei")) return Boolean.parseBoolean(CONFIG.getProperty("translateJei", "true"));
        if (screen.contains("quest") || screen.contains("ftbquests")) return Boolean.parseBoolean(CONFIG.getProperty("translateQuests", "true"));
        if (screen.contains("dialog") || screen.contains("npc") || screen.contains("conversation")) return Boolean.parseBoolean(CONFIG.getProperty("translateNpc", "true"));
        if (screen.contains("patchouli") || screen.contains("book") || screen.contains("guide") || screen.contains("lexicon")) return Boolean.parseBoolean(CONFIG.getProperty("translatePatchouliBooks", "true"));
        return Boolean.parseBoolean(CONFIG.getProperty("translateTooltips", "true"));
    }

    private static boolean isIgnoredOnlineScreen() {
        String screen = currentScreenName();
        if (screen.equals("net.minecraft.client.gui.screens.TitleScreen")) return true;
        // ChatScreen の候補や入力欄はチャット受信イベント側だけで扱う。
        if (screen.equals("net.minecraft.client.gui.screens.ChatScreen")) return true;
        // クラス画面は固定辞書/ブロック翻訳を優先。未知文のAPI連打を避ける。
        if (screen.equals("com.afunproject.dawncraft.classes.client.ClassSelectionScreen")) return true;
        return false;
    }

    public static boolean isDifferent(String a, String b) {
        return a != null && b != null && !a.equals(b);
    }

    public static String translate(String key, String original) {
        if (!enabled || original == null || skipKey(key)) return original;
        String s = original.trim();
        if (!looksEnglish(s)) return original;

        String manual = DICT.getProperty(original);
        if (manual != null) return manual;

        String cached = CACHE.getProperty(original);
        if (cached != null && !cached.isBlank()) return decodeEscapes(cached);

        logSeen(key, original);
        if (!onlineTranslation) return original;

        if (PENDING.add(original)) {
            EXEC.submit(() -> {
                try {
                    String translated = requestTranslate(original);
                    if (translated != null && !translated.isBlank() && !translated.equals(original) && !looksEnglish(translated)) {
                        CACHE.setProperty(original, translated);
                        saveCache();
                        System.out.println("[DawnAutoTranslator] " + original + " -> " + translated);
                    }
                } catch (Exception e) {
                    System.err.println("[DawnAutoTranslator] translate failed: " + original + " / " + e.getMessage());
                } finally {
                    PENDING.remove(original);
                }
            });
        }
        return original;
    }




    public static TextComponent replacementComponentForChatMessage(net.minecraft.network.chat.Component message) {
        if (!enabled || !Boolean.parseBoolean(CONFIG.getProperty("translateChat", "true")) ||
                !Boolean.parseBoolean(CONFIG.getProperty("replaceCachedChat", "true")) || message == null) return null;
        String original = message.getString();
        if (original == null) return null;
        String s = original.trim();
        String prefix = CONFIG.getProperty("chatTranslationPrefix", "[翻訳]");
        if (s.startsWith(prefix)) return null;
        if (!looksEnglish(s)) return null;
        if (isProbablyPlayerChat(s)) return null;
        String translated = translateForChatImmediate(original);
        if (translated == null || translated.equals(original)) return null;
        translated = applyChatFormattingHints(original, translated);
        logSeen("chat.replace.originalPosition", original + " -> " + translated);
        return new TextComponent(translated);
    }

    public static void observeAndQueueChatComponent(String source, String original) {
        if (!enabled || original == null) return;
        String s = original.trim();
        if (s.isEmpty()) return;
        String prefix = CONFIG.getProperty("chatTranslationPrefix", "[翻訳]");
        if (s.startsWith(prefix)) return;
        if (!looksEnglish(s)) return;
        if (isProbablyPlayerChat(s)) return;
        logSeen(source == null ? "chatcomponent.seen" : source, s);
        String cached = CACHE.getProperty(original);
        if (cached != null && !cached.isBlank()) {
            String decoded = decodeEscapes(cached);
            if (Boolean.parseBoolean(CONFIG.getProperty("showAsyncChatTranslations", "true"))) {
                showTranslatedChatLine(decoded);
            }
            return;
        }
        queueChatTranslation(original);
    }

    public static String translateForChatImmediate(String original) {
        if (!enabled || !Boolean.parseBoolean(CONFIG.getProperty("translateChat", "true")) || original == null) return original;
        String s = original.trim();
        if (!looksEnglish(s)) return original;
        if (isProbablyPlayerChat(s)) return original;
        String manual = DICT.getProperty(original);
        if (manual != null && !manual.isBlank()) return manual;
        String cached = CACHE.getProperty(original);
        if (cached != null && !cached.isBlank()) {
            String decoded = decodeEscapes(cached);
            decoded = applyChatFormattingHints(original, decoded);
            logSeen("chat.replace.cache", original + " -> " + decoded);
            return decoded;
        }
        return original;
    }

    public static void queueChatTranslation(String original) {
        if (!enabled || !Boolean.parseBoolean(CONFIG.getProperty("translateChat", "true")) || original == null) return;
        if (!onlineTranslation) return;
        String s = original.trim();
        String prefix = CONFIG.getProperty("chatTranslationPrefix", "[翻訳]");
        if (s.startsWith(prefix)) return;
        if (!looksEnglish(s)) return;
        if (isProbablyPlayerChat(s)) return;
        if (CACHE.containsKey(original) || DICT.containsKey(original)) return;
        if (PENDING.add("chat:" + original)) {
            EXEC.submit(() -> {
                try {
                    String requestText = protectRuntimeNames(original);
                    String translated = requestTranslate(requestText);
                    translated = restoreRuntimeNames(translated);
                    if (translated != null && !translated.isBlank() && !translated.equals(original)) {
                        CACHE.setProperty(original, translated);
                        saveCache();
                        logSeen("chat.deepl.cache", original + " -> " + translated);
                        if (Boolean.parseBoolean(CONFIG.getProperty("showAsyncChatTranslations", "true"))) {
                            showTranslatedChatLine(applyChatFormattingHints(original, translated));
                        }
                    }
                } catch (Exception e) {
                    logSeen("chat.deepl.error", original + " / " + e.getMessage());
                } finally {
                    PENDING.remove("chat:" + original);
                }
            });
        }
    }

    private static boolean isLikelyCommandSuggestion(String s) {
        if (s == null) return false;
        String t = s.trim();
        // Command suggestions are usually one lower-case token, often mod command ids. Avoid wasting DeepL quota.
        return t.matches("^[a-z][a-z0-9_:-]{2,32}$");
    }

    private static boolean isProbablyPlayerChat(String s) {
        // Keep normal multiplayer chat untouched. System/tutorial messages usually do not use these shapes.
        return s.startsWith("<") || s.matches("^[A-Za-z0-9_]{3,16}: .*");
    }

    private static String runtimePlayerName() {
        try {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().player != null) {
                return Minecraft.getInstance().player.getName().getString();
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private static String protectRuntimeNames(String s) {
        if (!Boolean.parseBoolean(CONFIG.getProperty("protectPlayerName", "true"))) return s;
        String name = runtimePlayerName();
        if (name == null || name.isBlank()) return s;
        return s.replace(name, "DATPLAYER0DATPLAYER");
    }

    private static String restoreRuntimeNames(String s) {
        if (s == null) return null;
        String name = runtimePlayerName();
        if (name == null || name.isBlank()) return s;
        return s.replace("DATPLAYER0DATPLAYER", name);
    }


    private static String applyChatFormattingHints(String original, String translated) {
        if (translated == null) return null;
        if (!Boolean.parseBoolean(CONFIG.getProperty("preserveChatColors", "true"))) return translated;
        String out = translated;
        // DawnCraft tutorial messages often color these terms. Keep the same visual hints in Japanese.
        if (original != null && original.contains("Guild Master")) {
            out = out.replace("ギルドマスター", "§bギルドマスター§r");
        }
        if (original != null && original.contains("knowledge")) {
            out = out.replace("知識", "§a知識§r");
        }
        return out;
    }

    private static void showTranslatedChatLine(String translated) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            String prefix = CONFIG.getProperty("chatTranslationPrefix", "[翻訳]");
            mc.execute(() -> {
                try {
                    if (Minecraft.getInstance().gui != null) {
                        Minecraft.getInstance().gui.getChat().addMessage(new TextComponent(prefix + " " + translated));
                    }
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}
    }



    public static boolean isBookLikeScreen() {
        String screen = currentScreenName().toLowerCase(java.util.Locale.ROOT);
        return screen.contains("patchouli") || screen.contains("book") || screen.contains("guide") || screen.contains("lexicon");
    }

    private static boolean shouldSkipBookFragment(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.length() < 12) return true;
        // Patchouli sometimes draws isolated layout fragments like "the" or "bonuses.".
        // Translating those tiny fragments causes unreadable partial text on the page.
        int letters = 0;
        int spaces = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetter(c)) letters++;
            if (Character.isWhitespace(c)) spaces++;
        }
        if (letters < 8) return true;
        if (spaces == 0 && t.length() < 18) return true;
        return false;
    }

    public static int colorForTranslatedFormatted(String source, int originalColor) {
        if (!isBookLikeScreen()) return originalColor;
        // Some book GUIs pass white/transparent colors while their background expects dark ink.
        // Force translated replacement text to a readable dark color.
        int rgb = originalColor & 0x00FFFFFF;
        int alpha = originalColor & 0xFF000000;
        if (alpha == 0) alpha = 0xFF000000;
        if (rgb == 0xFFFFFF || rgb == 0xF0F0F0 || rgb == 0xE0E0E0 || rgb == 0) {
            return alpha | 0x3F3F3F;
        }
        return originalColor;
    }

    public static String translateForFormattedDraw(String source, FormattedCharSequence sequence, float x, float y) {
        if (!enabled || sequence == null) return null;
        String text = formattedToString(sequence);
        if (text == null) return null;
        String original = text.trim();
        if (original.isEmpty()) return null;

        // v1.5.1.1: Patchouli/book-style pages often split text into tiny layout fragments.
        // Replacing fragments like "the" or "bonuses." breaks wrapping and makes text unreadable.
        // Only allow replacement for substantial lines; short fragments are observed/queued only.
        if (isBookLikeScreen()) {
            observeTextAt(source, original, x, y);
            if (!Boolean.parseBoolean(CONFIG.getProperty("translatePatchouliBooks", "true"))) return null;
            if (Boolean.parseBoolean(CONFIG.getProperty("patchouliSafeMode", "true")) && shouldSkipBookFragment(original)) {
                maybeQueueOnlineTranslation(original);
                return null;
            }
        }

        // v1.4.2 safety: Feywild quest-giver screens have selectable UI elements.
        // Translate only the main dialogue body on DisplayQuestScreen; leave SelectQuestScreen
        // and buttons/options untouched so choices do not disappear.
        String screenName = currentScreenName();
        if (screenName.equals("com.feywild.quest_giver.screen.SelectQuestScreen")) {
            observeTextAt(source, original, x, y);
            return null;
        }
        if (screenName.equals("com.feywild.quest_giver.screen.DisplayQuestScreen")) {
            int ix = Math.round(x);
            int iy = Math.round(y);
            boolean isDialogueBody = ix >= 55 && ix <= 90 && iy >= 120 && iy <= 220;
            if (!isDialogueBody) {
                observeTextAt(source, original, x, y);
                return null;
            }
        }

        observeTextAt(source, original, x, y);

        String manual = DICT.getProperty(original);
        if (manual != null && !manual.isBlank()) {
            logSeen((source == null ? "formatted" : source) + ".replace", original + " -> " + manual);
            return fitReplacementForCurrentScreen(original, manual);
        }

        String cached = CACHE.getProperty(original);
        if (cached != null && !cached.isBlank()) {
            String decoded = decodeEscapes(cached);
            decoded = applyChatFormattingHints(original, decoded);
            logSeen((source == null ? "formatted" : source) + ".cacheReplace", original + " -> " + decoded);
            return fitReplacementForCurrentScreen(original, decoded);
        }

        // Quest/NPC/tooltip/JEI formatted text often reaches Minecraft as FormattedCharSequence only.
        // Queue it for DeepL now; the next render/cache hit can replace it in-place.
        maybeQueueOnlineTranslation(original);
        return null;
    }

    private static String fitReplacementForCurrentScreen(String original, String translated) {
        if (translated == null) return null;
        String screenName = currentScreenName();
        if (!screenName.equals("com.feywild.quest_giver.screen.DisplayQuestScreen")) return translated;
        // Feywild quest dialogue is drawn in a fixed-width area. Japanese DeepL output can be
        // much longer than the original line and run through the choice buttons. Keep each
        // rendered line inside the dialogue box. The full translation remains in cache/logs.
        int max = parseInt(CONFIG.getProperty("maxNpcLineChars", "34"), 34);
        if (max < 16) max = 16;
        if (translated.length() <= max) return translated;
        String cut = translated.substring(0, Math.max(1, max - 1));
        // Avoid ending immediately after a color code marker.
        if (cut.endsWith("§") && cut.length() > 1) cut = cut.substring(0, cut.length() - 1);
        String fitted = cut + "…";
        logSeen("npc.dialogue.fit", translated + " -> " + fitted);
        return fitted;
    }

    public static void observeText(String source, String text) {
        observeTextAt(source, text, Float.NaN, Float.NaN);
    }

    public static void observeTextAt(String source, String text, float x, float y) {
        if (!enabled || text == null) return;
        String s = text.trim();
        if (!looksEnglish(s)) return;
        String src = source == null ? "draw" : source;
        String screen = currentScreenName();
        String location = Float.isNaN(x) ? "" : " @" + Math.round(x) + "," + Math.round(y);
        logSeen(src + location + " [screen=" + screen + "]", s);
    }

    public static void observeFormattedAt(String source, FormattedCharSequence sequence, float x, float y) {
        String s = formattedToString(sequence);
        if (s != null && !s.isBlank()) observeTextAt(source, s, x, y);
    }

    private static String formattedToString(FormattedCharSequence sequence) {
        if (sequence == null) return null;
        StringBuilder sb = new StringBuilder();
        try {
            sequence.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String currentScreenName() {
        try {
            if (Minecraft.getInstance() == null || Minecraft.getInstance().screen == null) return "none";
            return Minecraft.getInstance().screen.getClass().getName();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static boolean skipKey(String key) {
        if (key == null) return false;
        String prefixes = CONFIG.getProperty("excludedKeyPrefixes", "");
        for (String raw : prefixes.split(",")) {
            String prefix = raw.trim();
            if (!prefix.isEmpty() && key.startsWith(prefix)) return true;
        }
        return false;
    }

    private static boolean looksEnglish(String s) {
        int max = parseInt(CONFIG.getProperty("maxLength", "220"), 220);
        if (s.length() < 2 || s.length() > max) return false;
        if (HAS_JA.matcher(s).find()) return false;
        if (!HAS_LATIN.matcher(s).find()) return false;
        if (s.startsWith("key.") || s.startsWith("<") || s.endsWith(">")) return false;
        return true;
    }

    private static void ensureConfigDefault(String key, String value) {
        if (!CONFIG.containsKey(key)) CONFIG.setProperty(key, value);
    }

    private static void saveConfig(Path configFile) {
        try (OutputStream out = Files.newOutputStream(configFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            CONFIG.store(out, "Dawn Auto Translator config");
        } catch (IOException e) {
            System.err.println("[DawnAutoTranslator] config save failed: " + e);
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private static void installDefaultDictionary() {
        // Common UI
        putDefault("Confirm", "決定");
        putDefault("Items", "アイテム");
        putDefault("Skills", "スキル");

        // Quest selection labels observed in DawnCraft/Feywild quest-giver UI.
        // These are translated by fixed dictionary only to avoid breaking custom buttons.
        putDefault("First Assignment", "最初の課題");
        putDefault("Finding Pillagers", "略奪者を探す");
        putDefault("Reforging and Socketing", "鍛造とソケット");

        // DawnCraft Classes - names
        putDefault("Warrior", "戦士");
        putDefault("Berserker", "狂戦士");
        putDefault("Tank", "タンク");
        putDefault("Lancer", "槍兵");
        putDefault("Roamer", "放浪者");
        putDefault("Brawler", "格闘家");
        putDefault("Ronin", "浪人");
        putDefault("Astrologer", "占星術師");

        // Warrior description
        putDefault("Formidable champions with above", "平均以上の体力とスタミナを持つ");
        putDefault("average stamina and health,", "頼もしい戦士。");
        putDefault("making them a balanced force on", "戦場ではバランスの取れた");
        putDefault("the battlefield.", "戦力となる。");
        putDefault("battlefield.", "戦場の守護者となる。");

        // Berserker description
        putDefault("Relentless ferocity balanced with", "尽きない獰猛さと");
        putDefault("average health and", "平均的な体力、そして");
        putDefault("above-average stamina.", "平均以上のスタミナを持つ。");
        putDefault("Berserkers excels in extended", "狂戦士は長期戦に優れ、");
        putDefault("battles with their special skills", "専用スキルによって");
        putDefault("tailored for prolonged combat.", "粘り強く戦い続ける。");

        // Tank description
        putDefault("High health and below-average", "高い体力と平均以下の");
        putDefault("stamina define the Tank class,", "スタミナを持つタンクは、");
        putDefault("built to endure relentless", "絶え間ない攻撃に耐える");
        putDefault("assaults. With a unique stun", "ために作られている。独自の");
        putDefault("resistant skill, Tanks are", "スタン耐性スキルにより、");
        putDefault("unyielding protectors on the", "決して退かない");

        // Lancer description
        putDefault("Lancers, with average health and", "槍兵は平均的な体力と");
        putDefault("stamina, showcase precision and", "スタミナを持ち、精密さと");
        putDefault("agility. They excel at exploiting", "機敏さを発揮する。敵の");
        putDefault("enemy weak points and employ a", "弱点を突くことに長け、");
        putDefault("swift dodge for nimble combat", "素早い回避で軽快な戦闘");
        putDefault("maneuvers.", "動作を行う。");

        // Roamer description
        putDefault("Armed with a bow and arrows,", "弓と矢で武装した");
        putDefault("Roamers embrace agility,", "放浪者は機敏さを重視し、");
        putDefault("featuring below-average health", "平均以下の体力と");
        putDefault("and high stamina.", "高いスタミナを持つ。");

        // Brawler description
        putDefault("Brawlers feature well", "格闘家は非常に");
        putDefault("above-average health and", "高い体力と");
        putDefault("average stamina, making them", "平均的なスタミナを持ち、");
        putDefault("formidable in close-quarters", "接近戦で恐るべき");
        putDefault("combat with enduring strength.", "持久力を発揮する。");

        // Ronin description
        putDefault("With low health and average", "低い体力と平均的な");
        putDefault("stamina, Ronins are agile katana", "スタミナを持つ浪人は、機敏な刀の");
        putDefault("wielding warriors, delivering", "使い手として、的確な一撃を");
        putDefault("precise strikes while skillfully", "繰り出しながら巧みに");
        putDefault("avoiding harm.", "攻撃を避ける。");

        // Astrologer description
        putDefault("Astrologers, with low health and", "占星術師は低い体力と");
        putDefault("below-average stamina, their", "平均以下のスタミナを持つが、");
        putDefault("strength lies in the powerful", "その強みは強力な");
        putDefault("Ender Step for strategic", "戦略的なエンダーステップと");
        putDefault("movements, complemented by a", "移動能力にあり、さらに");
        putDefault("basic spell book for magic", "基本の魔導書による魔法");
        putDefault("casting.", "詠唱で補われる。");

        installClassBlocks();
    }

    private static void installClassBlocks() {
        classBlock("Warrior",
            new String[]{"Formidable champions with above", "average stamina and health,", "making them a balanced force on", "the battlefield."},
            new String[]{"平均以上の体力とスタミナを持つ", "頼もしい戦士。", "戦場ではバランスの取れた", "戦力となる。"});
        classBlock("Berserker",
            new String[]{"Relentless ferocity balanced with", "average health and", "above-average stamina.", "Berserkers excels in extended", "battles with their special skills", "tailored for prolonged combat."},
            new String[]{"平均的な体力と高いスタミナを持つ", "獰猛な戦士。", "長期戦に優れ、", "専用スキルで粘り強く戦う。"});
        classBlock("Tank",
            new String[]{"High health and below-average", "stamina define the Tank class,", "built to endure relentless", "assaults. With a unique stun", "resistant skill, Tanks are", "unyielding protectors on the", "battlefield."},
            new String[]{"高い体力と低めのスタミナを持つ", "防御型のクラス。", "猛攻に耐え、スタン耐性で", "戦場の盾となる。"});
        classBlock("Lancer",
            new String[]{"Lancers, with average health and", "stamina, showcase precision and", "agility. They excel at exploiting", "enemy weak points and employ a", "swift dodge for nimble combat", "maneuvers."},
            new String[]{"平均的な体力とスタミナを持つ槍兵。", "精密さと機敏さに優れ、", "敵の弱点を突きながら", "素早い回避で戦う。"});
        classBlock("Roamer",
            new String[]{"Armed with a bow and arrows,", "Roamers embrace agility,", "featuring below-average health", "and high stamina."},
            new String[]{"弓と矢で戦う機敏な放浪者。", "体力は低めだが、", "高いスタミナを持つ。"});
        classBlock("Brawler",
            new String[]{"Brawlers feature well", "above-average health and", "average stamina, making them", "formidable in close-quarters", "combat with enduring strength."},
            new String[]{"非常に高い体力と平均的な", "スタミナを持つ格闘家。", "接近戦で強力な持久力を発揮する。"});
        classBlock("Ronin",
            new String[]{"With low health and average", "stamina, Ronins are agile katana", "wielding warriors, delivering", "precise strikes while skillfully", "avoiding harm."},
            new String[]{"低い体力と平均的なスタミナを持つ", "機敏な刀の使い手。", "的確な一撃を放ち、", "巧みに攻撃を避ける。"});
        classBlock("Astrologer",
            new String[]{"Astrologers, with low health and", "below-average stamina, their", "strength lies in the powerful", "Ender Step for strategic", "movements, complemented by a", "basic spell book for magic", "casting."},
            new String[]{"低い体力と低めのスタミナを持つ", "魔法寄りのクラス。", "強力なエンダーステップで立ち回り、", "基本の魔導書で魔法を扱う。"});
    }

    private static void classBlock(String name, String[] englishLines, String[] japaneseLines) {
        CLASS_DESC_EN.put(name, englishLines);
        CLASS_DESC_JA.put(name, japaneseLines);
    }

    private static void putDefault(String key, String value) {
        if (!DICT.containsKey(key)) DICT.setProperty(key, value);
    }

    private static synchronized void saveDictionaryIfMissing() {
        try (OutputStream out = Files.newOutputStream(DICT_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            DICT.store(out, "Dawn Auto Translator dictionary - edit freely");
        } catch (IOException e) {
            System.err.println("[DawnAutoTranslator] dictionary save failed: " + e);
        }
    }

    private static String requestTranslate(String text) throws Exception {
        String apiKey = sanitizeApiKey(CONFIG.getProperty("deeplApiKey", ""));
        if (!apiKey.isEmpty()) {
            return requestDeepL(text, apiKey);
        }
        return requestMyMemory(text);
    }

    private static String sanitizeApiKey(String raw) {
        if (raw == null) return "";
        // Properties files sometimes keep invisible chars when copied from browsers/editors.
        return raw.replace("\uFEFF", "")
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .trim();
    }

    private static String requestDeepL(String text, String apiKey) throws Exception {
        ProtectedText protectedText = protectPlaceholders(text);
        String configuredEndpoint = CONFIG.getProperty("deeplApiUrl", "auto").trim();
        String endpoint;
        if (!configuredEndpoint.isEmpty() && !configuredEndpoint.equalsIgnoreCase("auto")) {
            endpoint = configuredEndpoint;
        } else {
            endpoint = apiKey.endsWith(":fx")
                    ? "https://api-free.deepl.com/v2/translate"
                    : "https://api.deepl.com/v2/translate";
        }
        logSeen("deepl.endpoint", endpoint);
        logSeen("deepl.keyInfo", "length=" + apiKey.length() + ", endsWithFx=" + apiKey.endsWith(":fx"));

        String baseBody = "text=" + URLEncoder.encode(protectedText.text, StandardCharsets.UTF_8)
                + "&source_lang=EN"
                + "&target_lang=" + URLEncoder.encode(CONFIG.getProperty("targetLang", "JA"), StandardCharsets.UTF_8)
                + "&preserve_formatting=1";

        // v1.2.4: Try auth_key in POST body first. Some environments reject the header method.
        DeepLResult bodyAuth = sendDeepL(endpoint,
                "auth_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) + "&" + baseBody,
                null);
        if (bodyAuth.statusCode == 200) return protectedText.restore(parseDeepLText(bodyAuth.body));

        // Fallback: official Authorization header form. This helps identify which auth form is accepted.
        DeepLResult headerAuth = sendDeepL(endpoint, baseBody, apiKey);
        if (headerAuth.statusCode == 200) return protectedText.restore(parseDeepLText(headerAuth.body));

        String bodyMsg = summarizeHttpBody(bodyAuth.body);
        String headerMsg = summarizeHttpBody(headerAuth.body);
        throw new IOException("DeepL HTTP bodyAuth=" + bodyAuth.statusCode
                + ", headerAuth=" + headerAuth.statusCode
                + " @ " + endpoint
                + " / keyLength=" + apiKey.length()
                + " / endsWithFx=" + apiKey.endsWith(":fx")
                + " / body=" + bodyMsg
                + " / header=" + headerMsg);
    }

    private record DeepLResult(int statusCode, String body) {}

    private static DeepLResult sendDeepL(String endpoint, String body, String headerApiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "DawnAutoTranslator/1.4.1")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (headerApiKey != null && !headerApiKey.isBlank()) {
            builder.header("Authorization", "DeepL-Auth-Key " + headerApiKey);
        }
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new DeepLResult(response.statusCode(), response.body());
    }

    private static String parseDeepLText(String responseBody) {
        Matcher m = Pattern.compile("\\\"text\\\"\\s*:\\s*\\\"(.*?)\\\"").matcher(responseBody);
        if (!m.find()) return null;
        return unescapeJson(m.group(1));
    }

    private static String summarizeHttpBody(String body) {
        if (body == null) return "";
        String s = body.replace('\n', ' ').replace('\r', ' ').trim();
        if (s.length() > 180) s = s.substring(0, 180) + "...";
        return s;
    }

    private static String requestMyMemory(String text) throws Exception {
        ProtectedText protectedText = protectPlaceholders(text);
        String q = URLEncoder.encode(protectedText.text, StandardCharsets.UTF_8);
        String url = "https://api.mymemory.translated.net/get?q=" + q + "&langpair=en%7Cja";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "DawnAutoTranslator/1.4.1")
                .GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) return null;
        Matcher m = Pattern.compile("\\\"translatedText\\\"\\s*:\\s*\\\"(.*?)\\\"").matcher(response.body());
        if (!m.find()) return null;
        String translated = unescapeJson(m.group(1));
        return protectedText.restore(translated);
    }

    private static synchronized void saveCache() {
        try (OutputStream out = Files.newOutputStream(CACHE_FILE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            CACHE.store(out, "Dawn Auto Translator cache");
        } catch (IOException e) {
            System.err.println("[DawnAutoTranslator] cache save failed: " + e);
        }
    }

    private static synchronized void logSeen(String key, String text) {
        if (!Boolean.parseBoolean(CONFIG.getProperty("logSeenEnglish", "true"))) return;
        try {
            String line = key + " = " + text;
            if (Boolean.parseBoolean(CONFIG.getProperty("dedupeSeenEnglish", "true")) && !LOGGED.add(line)) return;
            Files.writeString(LOG_FILE, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private static ProtectedText protectPlaceholders(String text) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuffer sb = new StringBuffer();
        java.util.List<String> values = new java.util.ArrayList<>();
        int i = 0;
        while (m.find()) {
            values.add(m.group(1));
            m.appendReplacement(sb, "DATPH" + (i++) + "DATPH");
        }
        m.appendTail(sb);
        return new ProtectedText(sb.toString(), values);
    }

    private record ProtectedText(String text, java.util.List<String> values) {
        String restore(String s) {
            String out = s;
            for (int i = 0; i < values.size(); i++) out = out.replace("DATPH" + i + "DATPH", values.get(i));
            return out;
        }
    }

    private static String unescapeJson(String s) {
        return decodeEscapes(s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\/", "/"));
    }

    private static String decodeEscapes(String s) {
        if (s == null || s.indexOf('\\') < 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'u' && i + 5 < s.length()) {
                    String hex = s.substring(i + 2, i + 6);
                    try {
                        out.append((char) Integer.parseInt(hex, 16));
                        i += 5;
                        continue;
                    } catch (NumberFormatException ignored) {}
                } else if (n == 'n') {
                    out.append('\n');
                    i++;
                    continue;
                } else if (n == 't') {
                    out.append('\t');
                    i++;
                    continue;
                } else if (n == 'r') {
                    out.append('\r');
                    i++;
                    continue;
                } else if (n == '\\' || n == '"' || n == '/') {
                    out.append(n);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
