# DawnCraft-MOD
ChatGPTを使用して試作したDawnCraft 2.0.16 / Minecraft Forge 1.18.2 向けの非公式リアルタイム日本語化支援MODです。使用にはDeepLAPIキーが必要です。
DawnCraft 2.0.16 / Minecraft Forge 1.18.2 向けの試作日本語化MODです。


## 設定

.jarをmods内に入れゲームを起動すると、config内にdawnautotranslatorが生成されます。
以下を編集してください。

`config/dawnautotranslator/config.properties`

```properties
enabled=true
onlineTranslation=true
translateChat=true
showAsyncChatTranslations=true
deeplApiKey=ここにDeepL APIキー
```
APIキーの取得方法についてはご自身で調べてください。

APIキーはjarには入れないでください。




`config/dawnautotranslator/config.properties` で以下を切り替えできます。

```properties
translateTooltips=true
translateQuests=true
translateJei=true
translateNpc=true
```

※ 画面によってはMOD側の描画方法が特殊で、初回は英文のまま・2回目以降キャッシュで置換、またはログ記録のみになる場合があります。


