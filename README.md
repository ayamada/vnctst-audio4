<div align="center"><img src="https://github.com/ayamada/vnctst-audio4/raw/master/img/logo.png" /></div>


# vnctst-audio4

[![release version](https://img.shields.io/github/release/ayamada/vnctst-audio4.svg)](https://github.com/ayamada/vnctst-audio4/releases)
[![Build Status](https://travis-ci.org/ayamada/vnctst-audio4.svg?branch=master)](https://travis-ci.org/ayamada/vnctst-audio4)
[![Clojars Project](https://img.shields.io/clojars/v/jp.ne.tir/vnctst-audio4.svg)](https://clojars.org/jp.ne.tir/vnctst-audio4)
[![npm](https://img.shields.io/npm/v/vnctst-audio4.svg)](https://www.npmjs.com/package/vnctst-audio4)
[![license zlib](https://img.shields.io/badge/license-zlib-blue.svg)](LICENSE)

html5環境の為の、ゲーム向け音響ファイル再生ライブラリ


# 目次

- [特徴](#特徴)
- [使い方](#使い方)
- [オンラインデモ](#オンラインデモ)
- [必要知識](#必要知識)
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
    - BGMやSEの操作はゲーム内では頻出である為、これを簡潔に扱えるのとそうでないのとではゲーム作成コストに結構な差がある。なので、後述している複雑な状態をシステム内部で厳密に管理しつつも、実際のBGM/SE再生指示は非常に簡潔に行えるようにした。

- 雑に扱っても問題の出ないシステム
    - たとえインターフェースがシンプルであっても「この順番で処理を実行しなくてはならない」「○○中にこの処理を行ってはならない」「この種類の例外を捕捉しなくてはならない」的な「約束事」が大量にあったのでは全く意味がない。そのような「約束事」を極力なくすように工夫している。つまり「雑に、タイミング等を気にせずに適当に実行しても問題ない」。
        - ライブラリとしての「約束事」は極力なくしたものの、ブラウザ上での音響ファイル再生の「約束事」はどうやっても減らせないので、そこだけはきちんと抑えておく必要がある。これについては別セクションに記載した。
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
    - WebAudio環境では、再生オプションとしてpitchおよびpanの指定も可能。

- html5環境特有の様々なバッドノウハウ対応を内包

- js環境および[cljs](https://github.com/clojure/clojurescript)環境での利用が可能


# 使い方

下の「オンラインデモ」内へと統合しました


# オンラインデモ

- http://vnctst.tir.jp/vnctst-audio4-demo/


# 必要知識

あとで


# FAQ

あとで


# TODO

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
    - [vnctst-audio3](https://github.com/ayamada/vnctst-audio3)をベースに開発

