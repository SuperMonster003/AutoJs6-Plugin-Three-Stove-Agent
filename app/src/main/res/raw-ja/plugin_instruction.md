AI Agent は自然言語の目標を, AutoJs6 が動作する Android デバイス上の実際の操作に変えます. ユーザーがエージェント用に登録したスクリプトを選んでパラメーターを補い実行するか, アクセシビリティのノードツリーを通じて画面を観察し, 観察, 判断, 操作, 検証の循環で段階的に操作します. 目標を達成するか, 確認が必要になるか, 予算を使い切るまで続けます. [AutoJs6 ディスカッション #577](https://github.com/SuperMonster003/AutoJs6/discussions/577) への回答です.

インストール版は現在もホスト状態のみを表示します. P1 ホストインターフェースと P2.1-P2.3 のツール, 決定, ランナーのコアは実装とテストが完了しています. 実際のタスク実行には P2.4/P2.5 のモデル/ホスト接続と P3/P4 の実行アダプターが必要です. スクリプト API とタスク画面は P5/P6 で実装します. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使い方

1. AutoJs6 ビルド 5285 以降を導入したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `AI Agent` が認識されていることを確認して有効にします. 公式リリースのパッケージは署名検証を自動的に通過します.
3. ランチャーまたは AutoJs6 ドロワーの管理操作から AI Agent を開きます. このプレビューはホスト状態のみ表示します. タスク画面と `ai.agent` API は後続段階で提供します.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) を参照してください.
