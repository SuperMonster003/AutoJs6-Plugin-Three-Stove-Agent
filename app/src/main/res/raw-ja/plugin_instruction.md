AI Agent は自然言語の目標を, AutoJs6 が動作する Android デバイス上の実際の操作に変えます. ユーザーがエージェント用に登録したスクリプトを選んでパラメーターを補い実行するか, アクセシビリティのノードツリーを通じて画面を観察し, 観察, 判断, 操作, 検証の循環で段階的に操作します. 目標を達成するか, 確認が必要になるか, 予算を使い切るまで続けます. [AutoJs6 ディスカッション #577](https://github.com/SuperMonster003/AutoJs6/discussions/577) への回答です.

開発プレビュー: タスク画面で目標入力, 進捗表示, インライン回答, 最近のタスク詳細に対応. ai.agent タスク API には AutoJs6 build 5293 以降が必要です. 履歴管理や独自プリセットなどは P6, 堅牢性の検証は P7 で続行します. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使い方

1. AutoJs6 ビルド 5289 以降を導入したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `AI Agent` が認識されていることを確認して有効にします. 公式リリースのパッケージは署名検証を自動的に通過します.
3. AI Agent を開いて AutoJs6 に接続し, 目標を入力して標準プリセットで開始します. タスクカードで回答や確認を行い, 最近のタスクから詳細を確認できます.
4. ランチャーの "スクリプトディレクトリ" で追加フォルダーを設定します. 1 行に 1 つの絶対パスを入力し, 保存後にホストが検証して適用します. タスクは承認済みの範囲だけを絞り込めます.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) を参照してください.
