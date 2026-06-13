# DawnCraft-MOD
ChatGPTを使用して試作したDawnCraft 2.0.16 / Minecraft Forge 1.18.2 向けの非公式リアルタイム日本語化支援MODです。使用にはDeepLAPIキーが必要です。APIキーの取得方法についてはご自身で調べてください。
DawnCraft 2.0.16 / Minecraft Forge 1.18.2 向けの試作日本語化MODです。一度英文が表示されたあと日本語に置換されます。

※一時の欲でChatGPTを使い作成したMODです。万が一当MODを導入して不具合が生じても対応しません。というか知識がないので恐らく対応できません。文字化けやクラッシュ等不具合が起き、ゲームの進行を妨げるような場合は直ちにmodsから削除してください。
完全に「現状有姿（現状渡し）」での公開となります。導入はすべて自己責任でお願いいたします。


## インストール

.jarをcurseforge\minecraft\Instances\DawnCraft - Echoes of Legends\mods内に入れ一度ゲームを起動すると、curseforge\minecraft\Instances\DawnCraft - Echoes of Legends\config内にdawnautotranslatorが生成されますのでdawnautotranslator内のconfig.propertiesを編集してDeepL APIキーを貼り付けてください。

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


### 設定
翻訳箇所を詳しく設定したい方向けに。

`config/dawnautotranslator/config.properties` で以下を切り替えできます。

```properties
translateTooltips=true
translateQuests=true
translateJei=true
translateNpc=true
```

※ 画面によってはMOD側の描画方法が特殊で、初回は英文のまま・2回目以降キャッシュで置換、またはログ記録のみになる場合があります。


