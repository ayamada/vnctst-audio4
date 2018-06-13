<div align="center"><img src="https://github.com/ayamada/vnctst-audio4/raw/master/img/logo.png" /></div>


# vnctst-audio4

html5環境の為の、ゲーム向け音響ファイル再生ライブラリ

[![Build Status](https://travis-ci.org/ayamada/vnctst-audio4.svg?branch=master)](https://travis-ci.org/ayamada/vnctst-audio4)
[![Clojars Project](https://img.shields.io/clojars/v/jp.ne.tir/vnctst-audio4.svg)](https://clojars.org/jp.ne.tir/vnctst-audio4)
[![npm](https://img.shields.io/npm/v/vnctst-audio4.svg)](https://www.npmjs.com/package/vnctst-audio4)
[![release version](https://img.shields.io/github/release/ayamada/vnctst-audio4.svg)](https://github.com/ayamada/vnctst-audio4/releases)
[![license](https://img.shields.io/github/license/ayamada/vnctst-audio4.svg)](LICENSE)


# 目次

- [特徴](#特徴)
- [使い方](#使い方)
- [オンラインデモ](#オンラインデモ)
- [対応環境マトリックス](#対応環境マトリックス)
- [Development](#development)
- [TODO](#todo)
- [Link](#link)
- [License](#license)
- [ChangeLog](#changelog)


# 特徴

「ゲーム向け」に特化してチューニングされた、以下の特徴を持つ

- BGMの管理システムとSEの管理システムが分かれている
    - BGMは、厳重に管理された自動フェード機構を持つ(後述)
    - SEは、多重再生の管理/サポート機構を持つ(古いモバイル環境での抑制機能あり)

- 非常にシンプルなインターフェース
    - BGMやSEの操作はゲーム内では頻出であり、この操作が面倒だとゲームの作成コストが大きく上昇する。なので、複雑な状態遷移をシステム内部で厳密に管理しつつも、実際のBGM/SE再生指示は非常に簡潔に行えるようにした。
        - どのようにシンプルかは後述のオンラインデモを参照

- 雑に扱っても問題の出ないシステム
    - たとえインターフェースがシンプルであっても「この順番で処理を実行しなくてはならない」「○○中にこの処理を行ってはならない」「この種類の例外を捕捉しなくてはならない」的な「約束事」が大量にあったのでは全く意味がない。そのような「約束事」を極力なくすように工夫している。つまり「雑に、タイミング等を気にせずに適当に実行しても問題ない」。
        - ライブラリとしての「約束事」は極力なくしたものの、ブラウザ上での音響ファイル再生の「約束事」はどうやっても減らせないので、そこだけはきちんとおさえておく必要がある。これについてもオンラインデモ内に記載した。
        - 他の音響ファイル再生ライブラリの中には、この「約束事」が多すぎて使い物にならないようなものが見られる…。

- BGMの自動フェード機構について
    - 「現在再生中のBGMのフェードアウトを行い、それが完了してから次のBGMを再生する」というケースがBGMの再生では要求される。これは非同期処理になり自前で書くと面倒なものなので、この処理を一発で指示できるようにしてある。
    - 上記だけではなく、「ゲーム内でシーン移動したのに合わせてBGMのフェードアウトを開始したが、すぐにまたシーン移動があったので、フェードアウトはそのままで次に再生するBGMだけ差し替えたい」「すぐに元のシーンに戻ってきたので、フェードアウトを中断して、現在のフェード音量からフェードインして元の音量まで戻す」といった機能にも対応している。
        - もちろんこれらも「非常にシンプルなインターフェース」と「雑に扱っても問題の出ないシステム」の中にあり、ライブラリの利用者が内部の状態遷移を気にする必要はない。

- 2018年頃の各ブラウザの[自動再生ポリシー変更](http://ch.nicovideo.jp/indies-game/blomaga/ar1410968)に対応

- [RPGアツマール](http://game.nicovideo.jp/atsumaru/)環境での再生にも対応
    - 具体的には http://ch.nicovideo.jp/indies-game/blomaga/ar1156958 と同等の対応を行うようにしてある。

- ライセンスとして[zlib](https://ja.wikipedia.org/wiki/Zlib_License)風ライセンスを採用
    - 当ライブラリの利用時にcopyright文等を表示させる義務はない。

以下は「ゲーム向け」に限らない汎用的な特徴

- 再生環境に応じた、適切な再生メソッド(WebAudio, HtmlAudio)の自動選択
    - WebAudio環境では、再生オプションとしてpitchおよびpanの指定も可能

- html5環境特有の様々なバッドノウハウ対応を内包
    - ieやモバイル環境での音響ファイル再生対応も含む

- js環境および[cljs](https://github.com/clojure/clojurescript)環境での利用が可能


# 使い方

下の「オンラインデモ」内へと統合しました


# オンラインデモ

- http://vnctst.tir.jp/vnctst-audio4-demo/


# 対応環境マトリックス

- ◎ : 問題なし
- ○ : おそらく問題なし、ただしハードスペックの低さによる問題があるかも
- △ : 再生開始に遅延あり。またBGMのループの際にも遅延(無音部分)あり。pitchおよびpanの変更に非対応
- × : あまりにも対応状況が悪い為、意図的に無効化(常に再生されない)

分類はかなり適当です、すいません

| OS種別           | ブラウザ     | 対応状況           |
| ----------------:|:------------:| ------------------ |
| windows          | chrome       | ◎ (WebAudio)      |
| windows          | firefox      | ◎ (WebAudio)      |
| windows          | ie9以降      | △ (HtmlAudio)     |
| windows          | edge         | 〇 (WebAudio)※1   |
| windows          | opera        | 未確認(おそらく◎) |
| windows          | safari       | 未確認(おそらく△) |
| ---------------- | ------------ | ------------------ |
| mac              | chrome       | ◎ (WebAudio)      |
| mac              | firefox      | ◎ (WebAudio)      |
| mac              | safari       | ◎ (WebAudio)      |
| ---------------- | ------------ | ------------------ |
| android          | firefox      | ○ (WebAudio)      |
| android5.0以降   | chrome       | ○ (WebAudio)      |
| android4.4.4以前 | chrome       | △ (HtmlAudio)     |
| android4.4.2以前 | 標準ブラウザ | ×                 |
| ---------------- | ------------ | ------------------ |
| ios(7以降？)     | chrome       | ○ (WebAudio)      |
| ios(7以降？)     | safari       | ○ (WebAudio)      |
| 古いios(6以前？) | chrome       | △ (HtmlAudio)     |
| 古いios(6以前？) | safari       | ×                 |

- ※1 : edgeはogg非対応。mp3は再生可能なものの、可変ビットレートだと再生できない場合があるようなので、固定ビットレートでエンコードする事を推奨


# Development

`vnctst-audio4` 自体の開発手順については [DEVEL.md](DEVEL.md) を参照。

cljs開発の知識がある事が前提。


# TODO

- ロゴ画像をもっと良いものに作り直す
- オンラインデモに英文切り替えボタンを追加
- このドキュメントの英語版を作成


# Link

関連する外部ページへのリンク集

- https://github.com/ayamada/vnctst-audio3
    - 当ライブラリの旧版。4には置いてない、開発ポリシーや内部構造の解説などがある

- https://outcloud.blogspot.jp/2015/11/htmlaudio.html
    - モバイルブラウザ回りについてのまとめ記事。とても参考になる。「ぺったんR」というソフト内のライブラリらしい

- https://github.com/CyberAgent/boombox.js
    - vnctst-audio2以前で内部デバイスとして利用していたライブラリ。当時に筆者が試した同類ライブラリ中では最も品質が良かった。ただしフェードおよび同一音源の多重再生機能は付いていない


# License

zlib風ライセンスとします。

- ライセンスの条項全文は [LICENSE](LICENSE) にあります(英語)。
- 当ライブラリの利用時にcopyright文等を表示させる義務はありません。
- zlibライセンスの日本語での解説は https://ja.wikipedia.org/wiki/Zlib_License 等で確認してください。


# ChangeLog

- 0.3.1-SNAPSHOT (2018-06-13)
    - http://ch.nicovideo.jp/indies-game/blomaga/ar1470959 への対応を実装
        - 現在対応中
    - バックグラウンド中に `bgm-position` が nil を返してしまう問題の修正

- 0.3.0 (2018-06-13)
    - http://ch.nicovideo.jp/indies-game/blomaga/ar1410968 への対応を実装
    - `play-bgm!` の引数に `:position` を追加
    - `bgm-position` を追加
    - `length` を追加

- 0.2.2 (2017-11-11)
    - node-webkit環境にてWebAudioが機能しなかった問題を修正
    - `set-config!` に `:path-prefix` を追加
    - dependenciesのバージョン上げ

- 0.2.1 (2017-05-16)
    - `vnctst.audio4.prefetch` でのファイル一覧の取得時に、ドットはじまりのファイルは除外するようにする
    - dependenciesのバージョン上げ

- 0.2.0 (2017-05-12)
    - ユーティリティ関数 `make-play-se-periodically` `make-play-se-personally` を追加
    - dependenciesのバージョン上げ

- 0.1.6 (2017-03-22)
    - 0.1.4でのie向けの不具合修正に、非常に古いandroidおよびiosでのchromeも含まれてしまっていたので、これらについては元々の挙動になるように修正

- 0.1.5 (2017-03-22)
    - `set-config!` に `:additional-query-string` を追加

- 0.1.4 (2017-03-20)
    - ieで高負荷になった場合に、稀にBGMのループに失敗する事がある問題を修正

- 0.1.3 (2017-03-12)
    - `vnctst-audio4.js` のビルドに失敗していたので、npm登録の為にバージョンを上げる

- 0.1.2 (2017-03-12)
    - dependenciesのバージョン上げ
    - プリロード時の完了判定チェックの実行間隔を最適化し、直列ロード処理時間を短縮

- 0.1.1 (2017-02-10)
    - dependenciesのバージョン上げ
    - マスターボリューム、BGMボリューム、SEボリュームの初期値を0.5から0.6へと変更
    - BGMのフェード中にBGMをフェード0秒で停止させた後にすぐ再生しようとした際に起こる各種の不具合を修正
    - 個別の`unload!`の実行時に、対応する音源がBGMとして再生中かつフェードアウト中かつ次に再生する曲が予約されている場合は、アンロードに伴う再生の強制停止後に、次の曲の再生開始を行うようにする(これは個別の`unload!`時のみの対応であり、`unload-all!`の時は行われない)。これは、BGMのフェードアウト完了を待たずにunloadしてしまったような場合のfailsafeとしての挙動となる

- 0.1.0 (2017-01-27)
    - 初回リリース。以下は [vnctst-audio3](https://github.com/ayamada/vnctst-audio3) からの変更点
    - モバイル環境での再生対応の大幅な強化
    - RPGアツマール環境での再生への対応
    - `init!`を廃止し、設定項目はボリューム等も合わせて`set-config!`で行うように変更
    - BGSを廃止し、`channel`オプション指定により好きな数だけBGMの多重再生を可能とした
    - MEを廃止し、`oneshot?`オプション指定により任意のBGMの非ループ再生を可能とした
    - 再生関数のオプション引数の指定方法をキーワード指定へと変更
    - 再生対象pathのキーワード指定は非標準とし、文字列指定を標準とする
    - 前述の変更に伴い、再生対象音源種別の自動判定機能は`.*`拡張子による指定で行う
    - コンパイルフェーズでのファイル一覧取得のインターフェース変更
    - 使い方に関するドキュメントを、オンラインデモ内へと統合

