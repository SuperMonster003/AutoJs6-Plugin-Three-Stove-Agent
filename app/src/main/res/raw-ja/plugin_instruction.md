AI Agent は自然言語の目標を, AutoJs6 が動作する Android デバイス上の実際の操作に変えます. ユーザーがエージェント用に登録したスクリプトを選んでパラメーターを補い実行するか, アクセシビリティのノードツリーを通じて画面を観察し, 観察, 判断, 操作, 検証の循環で段階的に操作します. 目標を達成するか, 確認が必要になるか, 予算を使い切るまで続けます. [AutoJs6 ディスカッション #577](https://github.com/SuperMonster003/AutoJs6/discussions/577) への回答です.

バージョン 1.0.0 はロードマップの P0 開発プレビューです. プラグインの識別情報, AutoJs6 の検出契約 (INFO サービス, Wake Activity, `org.autojs.plugin.AI_AGENT` サービスの仮実装), ホストの状態を表示する起動画面を含みます. エージェントループ, スクリプトカタログ, `ai.agent` API, タスク画面はまだ実装されていません. 進捗と証拠は [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) に記録しています. プラグインは AutoJs6 ビルド 5283 以降を必要とする予定です.

### 使い方

1. AutoJs6 ビルド 5283 以降を導入したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `AI Agent` が認識されていることを確認して有効にします. 公式リリースのパッケージは署名検証を自動的に通過します.
3. ランチャーから AI Agent を開きます. このプレビューでは互換性のある AutoJs6 ホストがインストールされているかどうかだけを表示します. タスク画面, ドロワーの項目, `ai.agent` API はロードマップの後続段階で提供されます.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) を参照してください.
