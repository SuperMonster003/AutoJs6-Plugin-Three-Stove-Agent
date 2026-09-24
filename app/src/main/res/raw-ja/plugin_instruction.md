AI Agent は自然言語の目標を, AutoJs6 が動作する Android デバイス上の実際の操作に変えます. ユーザーがエージェント用に登録したスクリプトを選んでパラメーターを補い実行するか, アクセシビリティのノードツリーを通じて画面を観察し, 観察, 判断, 操作, 検証の循環で段階的に操作します. 目標を達成するか, 確認が必要になるか, 予算を使い切るまで続けます. [AutoJs6 ディスカッション #577](https://github.com/SuperMonster003/AutoJs6/discussions/577) への回答です.

開発プレビュー: P6.1 のタスク画面と P6.2 のタスク履歴を利用できます. ai.agent API には AutoJs6 build 5293 以降が必要です. カスタムプリセットなどは P6.3-P6.7, 信頼性とリリースの検証は P7/P8 で続けます. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使い方

1. AutoJs6 ビルド 5289 以降を導入したデバイスに, [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) からプラグインの APK をインストールします.
2. AutoJs6 のプラグインセンターを開き, `AI Agent` が認識されていることを確認して有効にします. 公式リリースのパッケージは署名検証を自動的に通過します.
3. AI Agent を開いて AutoJs6 に接続し, 目標を入力して標準プリセットで開始します. タスクカードで回答や確認を行い, 最近のタスクから詳細を確認できます.
4. ランチャーの "スクリプトディレクトリ" で追加フォルダーを設定します. 1 行に 1 つの絶対パスを入力し, 保存後にホストが検証して適用します. タスクは承認済みの範囲だけを絞り込めます.
5. 最大 200 件 / 32 MiB. 終了済みタスクのうち最も長く閲覧されていないものから削除されます. 再実行は元の目標とプリセットをタスク画面に入力します. 確認して開始ボタンを押すと実行されます. 履歴の消去時も実行中のタスクは保持されます. 診断用の回数, ツール名, 確認結果を保存します. 目標, 引数, 観察内容, スクリプト結果は除去されます. 保存先を選択してください.

接続ガイドと現在の進捗は [プロジェクトの README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) と [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) を参照してください.
