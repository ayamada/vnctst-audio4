<div align="center"><img src="https://github.com/ayamada/vnctst-audio4/raw/master/img/logo.png" /></div>


# vnctst-audio4

[![release version](https://img.shields.io/github/release/ayamada/vnctst-audio4.svg)](https://github.com/ayamada/vnctst-audio4/releases)
[![Clojars Project](https://img.shields.io/clojars/v/jp.ne.tir/vnctst-audio4.svg)](https://clojars.org/jp.ne.tir/vnctst-audio4)
[![npm](https://img.shields.io/npm/v/vnctst-audio4.svg)](https://www.npmjs.com/package/vnctst-audio4)
[![Build Status](https://travis-ci.org/ayamada/vnctst-audio4.svg?branch=master)](https://travis-ci.org/ayamada/vnctst-audio4)
[![license zlib](https://img.shields.io/badge/license-zlib-blue.svg)](LICENSE)

html5環境の為の、ゲーム向け音響ファイル再生ライブラリ


# 目次

- [特徴](#特徴)
- [使い方](#使い方)
- [オンラインデモ](#オンラインデモ)
- [FAQ](#faq)
- [TODO](#todo)
- [Development](#development)
- [License](#license)
- [ChangeLog](#changelog)


# 特徴

「ゲーム向け」に特化してチューニングされた、以下の特徴を持つ

- BGMの管理システムとSEの管理システムが分かれている
    - BGMは、厳重に管理された自動フェード機構を持つ(後述)
    - SEは、多重再生の管理/サポート機構を持つ

- 非常にシンプルなインターフェース
    - BGMやSEの操作はゲーム内では頻出であり、この操作が面倒だとゲームの作成コストが大きく上昇する。なので、複雑な状態遷移をシステム内部で厳密に管理しつつも、実際のBGM/SE再生指示は非常に簡潔に行えるようにした。
        - どのようにシンプルかは後述のオンラインデモを参照

- 雑に扱っても問題の出ないシステム
    - たとえインターフェースがシンプルであっても「この順番で処理を実行しなくてはならない」「○○中にこの処理を行ってはならない」「この種類の例外を捕捉しなくてはならない」的な「約束事」が大量にあったのでは全く意味がない。そのような「約束事」を極力なくすように工夫している。つまり「雑に、タイミング等を気にせずに適当に実行しても問題ない」。
        - ライブラリとしての「約束事」は極力なくしたものの、ブラウザ上での音響ファイル再生の「約束事」はどうやっても減らせないので、そこだけはきちんと抑えておく必要がある。これについてもオンラインデモ内に記載した。
        - 他の音響ファイル再生ライブラリの中には、この「約束事」が多すぎて使い物にならないようなものが見られる…。

- BGMの自動フェード機構について
    - 「現在再生中のBGMのフェードアウトを行い、それが完了してから次のBGMを再生する」というケースがBGMの再生では要求される。これは非同期処理になり自前で書くと面倒なものなので、この処理を一発で指示できるようにしてある。
    - 上記だけではなく、「ゲーム内でシーン移動したのに合わせてBGMのフェードアウトを開始したが、すぐにまたシーン移動があったので、フェードアウトはそのままで次に再生するBGMだけ差し替えたい」「すぐに元のシーンに戻ってきたので、フェードアウトを中断して、現在のフェード音量からフェードインして元の音量まで戻す」といった機能にも対応している。
        - もちろんこれらも「非常にシンプルなインターフェース」と「雑に扱っても問題の出ないシステム」の中にあり、ライブラリの利用者が内部の状態遷移を気にする必要はない。

- [RPGアツマール](http://game.nicovideo.jp/atsumaru/)環境での再生にも対応(experimental)
    - 具体的には http://ch.nicovideo.jp/indies-game/blomaga/ar1156958 と同等の対応を行うようにしてある。
    - ただし2017年1月現在、「ツクールMV以外で作成したゲームをRPGアツマール上で実行する」事自体がまだexperimentalな扱いである事に注意。
        - この件についての詳細は http://qiita.com/hajimehoshi/items/2a28b16a2e587c82ac5d の記事が詳しい(2017年1月現在)。

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


# FAQ

- 音が出ない
    - `debug?` フラグを有効にして、コンソールにエラー内容が出ていないか確認してみよう
    - 多くのブラウザでは、ローカルファイル(`file:///...`形式のurl)からの再生はできません。httpサーバを用意し、その中で動作確認しよう

- 音が小さい
    - デフォルトの音量は25%相当です。オンラインデモのサンプルコードを確認して、マスターボリューム、BGMボリューム、SEボリュームを設定してみよう

- いちいち `vnctst.audio4.js.bgm("hoge.ogg")` って書くのは長くて面倒
    - `var va4 = vnctst.audio4.js;` を実行しておけば、 `va4.bgm("hoge.ogg")` ですみます

- オンラインデモのサンプルBGM/SEについて
    - ayamadaが作成したものです。ライセンスはCC0とします


# TODO

- https://outcloud.blogspot.jp/2015/11/htmlaudio.html を見て、モバイル対応をより補強する
- ロゴ画像をもっと良いものに作り直す
- オンラインデモに英文切り替えボタンを追加
- このドキュメントの英語版を作成


# Development

`vnctst-audio4` 自体の開発手順については [DEVEL.md](DEVEL.md) を参照。

cljs開発の知識がある事が前提。


# License

zlib風ライセンスとします。

- ライセンスの条項全文は [LICENSE](LICENSE) にあります(英語)。
- 当ライブラリの利用時にcopyright文等を表示させる義務はありません。
- zlibライセンスの詳細は https://ja.wikipedia.org/wiki/Zlib_License 等で確認してください。


# ChangeLog

<!--
- 0.1.1-SNAPSHOT (XXXX-XX-XX 次リリース予定)
    - ？？？
-->

- 0.1.0-SNAPSHOT (XXXX-XX-XX 現在作成中)
    - [vnctst-audio3](https://github.com/ayamada/vnctst-audio3)をベースに開発を開始

