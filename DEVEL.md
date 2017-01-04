# vnctst-audio4 開発手順書


## コーディングスタイルについて

@ayamada 以外の人がコードを書く場合は、こちらの記事を参照しておいてください。

- http://rnkv.hatenadiary.jp/entry/2016/06/29/034625


## 開発手順

オンラインデモ( http://vnctst.tir.jp/vnctst-audio4-demo/ に設置しているもの)をローカルで動かし、開発や動作確認を行う。

1. `master`ブランチから`dev`ブランチに切り替える
    - `master`ブランチはgithubの紹介ページおよび最新リリース版のChangeLogを兼ねているおり安易に更新できない為
    - `master`ブランチの内容と`dev`ブランチの内容に乖離がある場合は注意する事
        - 既に何らかの修正が`dev`ブランチに入っているケース、`master`ブランチにてドキュメントへの修正が入っているケース、等々がありえる

2. ringサーバを起動する
    - `lein with-profile ring ring server-headless` を別コンソールで実行

3. figwheelサーバを起動する
    - `rlwrap lein with-profile +demo-dev figwheel demo-dev` を別コンソールで実行

4. figwheelのログを流しておく
    - `tail -F figwheel_server.log` を別コンソールで実行

5. ブラウザを開く
    - `open http://localhost:8003/`

6. ソースをいじって開発


## リリース手順

1. figwheelサーバおよびringサーバが動いているなら、先に停止しておく

2. `README.md`内の`ChangeLog`セクションに、今回のリリースの更新内容を書く
    - これはできれば、コミットを行うタイミングで同時に追記しておくのが望ましい

3. `project.clj` をリリース向けに変更
    - `version`から`-SNAPSHOT`を抜く、等々

4. このタイミングで一度コミットしておく。
    - コミットメッセージは `'Releasing 0.1.2'` とかで。

5. js向け配布物である `vnctst-audio4.js` をリビルドする
    - `rm vnctst-audio4.js`
        - まず先に古いファイルを消す。これを行わないと更新されない時がある
          (cljsbuildの不具合っぽい？)
    - `lein clean && lein with-profile for-js cljsbuild once for-js`
    - ビルドしたら忘れずにコミットしておく事。
        - コミットメッセージは上記同様 `'Releasing 0.1.2'` とかでよい
          (バージョン番号をコミットログに含めておいた方が後で分かりやすい)

6. オンラインデモのリリースビルドを生成し、運用サーバにデプロイする
    - 詳細は後述セクション参照

7. clojarsにデプロイする
    - `lein deploy clojars`

8. タグをふる
    - `git tag -a 0.1.2 -m 'Tag description'`

9. `project.clj` を非リリース向けに戻す
    - `version`のpatchを上げ、`-SNAPSHOT`を付ける、等々

10. このタイミングでまたコミットしておく。
    - コミットメッセージは `'Released 0.1.2'` とかで。

11. リポジトリを保存する。この際に、タグの分も送らないといけない点に注意
    - `git push && git push origin --tags`

12. ここまでの作業は`dev`ブランチで行っていたので、ここまでの作業内容を`master`ブランチへと反映する
    - `git checkout master`
    - `git merge dev`
    - `git push`

13. github側にて、リリース版の `vnctst-audio4.js` をダウンロード可能にする。
    - https://github.com/ayamada/vnctst-audio4/releases にて
      「Draft a new release」のボタンを押す
    - さっき振ったタグを選択し、適当にタイトルを入力する
    - `vnctst-audio4.js` の実ファイルをアップロード欄に追加
    - 「Publish release」ボタンを押して公開し、ダウンロード可能にする
    - なんか上手くいかない時はuBlockとかが邪魔している。一時的に無効化するか
      ブラウザを変更するとよい


## オンラインデモのリリースビルド生成およびデプロイ手順

この作業は @ayamada しか行わない筈です。
なので @ayamada の環境にものすごく依存しています(ssh設定やローカルコマンド等)。

あくまで「オンラインデモ」単体でのリリースビルドおよびデプロイです。
配布物のリリースビルド生成ではない点に一応注意。

1. リリース版のcljsコンパイル
    - `lein clean && lein with-profile demo-prod cljsbuild once demo-prod`

2. リリース版のcljsコンパイル＆ringサーバ起動
    - `lein with-profile ring ring server-headless` を別コンソールで実行
    - ついでに、このタイミングでリリース版の動作確認を取っておくとベター

3. ringサーバから`index.html`を取得
    - `curl http://localhost:8003/ > resources/public/index.html`
    - このタイミングでコミットしておくとよい。
        - コミットメッセージは `'Update index.html'` とかで。

4. デプロイ実行
    1. 本番サーバにて `ls ~/public` を行い、非存在を確認する。存在していた場合は削除する事。
    2. ローカル環境にて `scp -r resources/public m:` を実行し、本番サーバに転送
    3. 以下のような感じのコマンドで本番環境に反映する
        - `(AID=vnctst-audio4-demo; test -e public && touch htdocs.vnctst.tir.jp/$AID && drop htdocs.vnctst.tir.jp/$AID && mv public htdocs.vnctst.tir.jp/$AID)`

5. 起動していたリリース版のringサーバを停止させる


