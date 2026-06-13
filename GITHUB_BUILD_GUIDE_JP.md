# GitHub Actionsでビルドする手順

1. このZIPを解凍します。
2. GitHubのリポジトリで中身をすべて差し替えます。
3. `.github/workflows/build.yml` が直下にあることを確認します。
4. Actions → Build DawnAutoTranslator → Run workflow を押します。
5. 成功後、Artifactsからjarをダウンロードします。
6. DawnCraftのmodsフォルダから古いjarを抜き、新しいjarを入れます。

## v1.2注意

DeepL APIキーはGitHubにアップロードしません。ゲームを一度起動後、ローカルPC側の
`config/dawnautotranslator/config.properties` に入力してください。
