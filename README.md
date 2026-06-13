# Dawn Auto Translator

ChatGPTを使用して試作した、DawnCraft 2.0.16 / Minecraft Forge 1.18.2 向けの非公式リアルタイム日本語化支援MODです。

DeepL APIを使用して、NPC会話・クエスト・チャットなどをリアルタイム翻訳します。

※ 使用には DeepL APIキー が必要です。取得方法については各自でご確認ください。

---

## 対応環境

* Minecraft Forge 1.18.2
* DawnCraft 2.0.16

---

## 主な機能

* NPC会話翻訳
* クエスト翻訳
* チャット翻訳
* ツールチップ翻訳
* JEI翻訳
* 翻訳キャッシュ
* DeepL Free / Pro 対応

---

## 注意事項

このMODは個人制作の試作MODです。

ChatGPTを使用して作成しているため、予期しない不具合・翻訳ミス・クラッシュ・文字化け等が発生する可能性があります。

ゲーム進行に支障が出る場合は、直ちにMODを削除してください。

完全に「現状有姿（現状渡し）」での公開となります。
導入・使用はすべて自己責任でお願いします。

* DawnCraft本体は含まれていません
* APIキーは絶対に他人へ共有しないでください

---

## インストール方法

1. `.jar` ファイルを以下へ配置

```txt
curseforge\minecraft\Instances\DawnCraft - Echoes of Legends\mods
```

2. 一度ゲームを起動

3. 以下が生成されます

```txt
config/dawnautotranslator/config.properties
```

4. `config.properties` を編集し、DeepL APIキーを入力してください

```properties
enabled=true
onlineTranslation=true
translateChat=true
showAsyncChatTranslations=true
deeplApiKey=ここにDeepL APIキー
```

---

## 詳細設定

`config/dawnautotranslator/config.properties`

内で以下を切り替えできます。

```properties
translateTooltips=true
translateQuests=true
translateJei=true
translateNpc=true
```

---

## 補足

MOD側の描画仕様によっては、

* 初回のみ英文表示
* 2回目以降キャッシュ翻訳
* 一部画面はログ記録のみ

となる場合があります。

---

## License

MIT License
