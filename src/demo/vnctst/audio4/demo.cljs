(ns vnctst.audio4.demo
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :as async :refer [>! <!]]
            [clojure.string :as string]
            [vnctst.audio4 :as audio4]
            [vnctst.audio4.js :as audio4-js]))



(defonce display-js-mode? (atom true))


(def config-options
  [:debug? true
   :debug-verbose? true
   ])

(def preload-pathes
  ["se/kick.*"
   "se/launch.*"
   ])

(def button-assign (atom {}))
(defn- defba [k m]
  (swap! button-assign assoc k m))




;;; main

(defba :bgm-va32
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*")
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\")"
   :js "vnctst.audio4.js.bgm(\"bgm/va32.*\")"
   :desc (str "\"bgm/va32.ogg\" もしくは \"bgm/va32.mp3\" を"
              "ループBGMとして再生する。"
              "この引数は \"bgm/va32.ogg\" のように"
              "拡張子を普通に付けて指定してもよいのだが、"
              "そうすると当然、"
              "ogg再生のできないブラウザでは音が出ない。"
              "上記のように \"bgm/va32.*\" 形式で指定する事で、"
              "oggが再生可能ならoggを、そうでなければmp3を"
              "再生する事ができる。"
              "もちろんoggとmp3の両方のファイルを"
              "予め設置しておく必要がある"
              "(逆に言えば、一つしかファイルを用意しないのであれば"
              "拡張子を普通に付けてファイル名を指定した方が分かりやすい)。"
              "最後に、引数は http://... のようなurlも指定可能だが、"
              "その場合は「CORS」の設定が必要になる場合がある事に注意"
              "(詳細はネットで検索)。"
              "「指定した拡張子の再生をブラウザがサポートしていない」"
              "「ファイルが存在しない」等の理由で再生できなかった場合は"
              "何も行われない(エラーも投げられない。ただし後述の debug? フラグ"
              "が有効ならコンソールにメッセージが出力される)。"
              )})

(defba :bgm-cntr
  {:fn #(vnctst.audio4/bgm! "bgm/cntr.*")
   :cljs "(vnctst.audio4/bgm! \"bgm/cntr.*\")"
   :js "vnctst.audio4.js.bgm(\"bgm/cntr.*\")"
   :desc (str "\"bgm/cntr.ogg\" もしくは \"bgm/cntr.mp3\" を"
              "ループBGMとして再生する。"
              "もし既に別のBGMが再生中の場合は、そのBGMの"
              "フェードアウトを開始し、フェードアウトが"
              "完了してから再生が開始される。"
              "この再生/停止回りは雑に操作しても適切に"
              "フェードアウト/フェードイン処理が行われるので、"
              "この辺りの再生/停止ボタンを素早く押しまくっても問題は出ない。"
              "実運用時も雑に扱ってよい。"
              )})

(defba :bgm-oneshot-ny2017
  {:fn #(vnctst.audio4/bgm-oneshot! "bgm/ny2017.*")
   :cljs "(vnctst.audio4/bgm-oneshot! \"bgm/ny2017.*\")"
   :js "vnctst.audio4.js.bgmOneshot(\"bgm/ny2017.*\")"
   :desc (str "\"bgm/ny2017.ogg\" もしくは \"bgm/ny2017.mp3\" を"
              "非ループBGMとして再生する。"
              "ループしない点以外は前述のBGM再生と同じ。"
              )})

(defba :stop-bgm
  {:fn #(vnctst.audio4/stop-bgm!)
   :cljs "(vnctst.audio4/stop-bgm!)"
   :js "vnstst.audio4.js.stopBgm()"
   :desc (str "現在再生中のBGMをデフォルト秒数(1秒)かけてフェード終了させる。"
              "再生中でない場合は何も起きない。"
              "この「デフォルト秒数」は後述の設定項目から変更可能。"
              )})

(defba :stop-bgm-3
  {:fn #(vnctst.audio4/stop-bgm! 3)
   :cljs "(vnctst.audio4/stop-bgm! 3)"
   :js "vnstst.audio4.js.stopBgm(3)"
   :desc (str "現在再生中のBGMを3秒かけてフェード終了させる。"
              )})

(defba :stop-bgm-0
  {:fn #(vnctst.audio4/stop-bgm! 0)
   :cljs "(vnctst.audio4/stop-bgm! 0)"
   :js "vnstst.audio4.js.stopBgm(0)"
   :desc "現在再生中のBGMを即座に停止させる。"
   })

(defba :se-launch
  {:fn #(vnctst.audio4/se! "se/launch.*")
   :cljs "(vnctst.audio4/se! \"se/launch.*\")"
   :js "vnstst.audio4.js.se(\"se/launch.*\")"
   :desc (str "\"se/launch.ogg\" もしくは \"se/launch.mp3\" を"
              "SEとして再生する。"
              "SEとしての再生では、音源の多重再生が可能となる"
              "(ボタンを連打しても前の音が途切れたりしない)。"
              )})

(defba :se-kick
  {:fn #(vnctst.audio4/se! "se/kick.*")
   :cljs "(vnctst.audio4/se! \"se/kick.*\")"
   :js "vnstst.audio4.js.se(\"se/kick.*\")"
   :desc (str "\"se/kick.ogg\" もしくは \"se/kick.mp3\" を"
              "SEとして再生する。"
              )})

(defba :stop-se
  {:fn #(vnctst.audio4/stop-se!)
   :cljs "(vnctst.audio4/stop-se!)"
   :js "vnstst.audio4.js.stopSe()"
   :desc (str "現在再生中のSE全てをデフォルト秒数(0秒)かけて"
              "フェード終了させる(0秒の時は即座に終了する)。"
              "再生中でない場合は何も起きない。"
              "この「デフォルト秒数」は後述の設定項目から変更可能。"
              )})

(defba :stop-se-05
  {:fn #(vnctst.audio4/stop-se! 0.5)
   :cljs "(vnctst.audio4/stop-se! 0.5)"
   :js "vnstst.audio4.js.stopSe(0.5)"
   :desc (str "現在再生中のSE全てを0.5秒かけてフェード終了させる。"
              )})


;;; configure


(defba :config-volume-master
  {:fn #(js/alert (vnctst.audio4/config :volume-master))
   :cljs "(vnctst.audio4/config :volume-master)"
   :js "vnctst.audio4.js.getConfig(\"volume-master\")"
   :desc (str "各種の設定値を取得する。"
              "このボタンで指定している volume-master はマスター音量の現在値"
              "(詳細については次の音量設定の項目を参照)。"
              "引数を変更する事で様々な設定値の取得が行えるが、"
              "項目数が多いので以下ではボタン化を省略している。"
              "確認したい場合はコンソールから実行してみるとよい。"
              )})

(defba :set-config-volume-master-100
  {:fn #(vnctst.audio4/set-config! :volume-master 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-master 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-master\", 1.0)"
   :desc ""})

(defba :set-config-volume-master-25
  {:fn #(vnctst.audio4/set-config! :volume-master 0.25)
   :cljs "(vnctst.audio4/set-config! :volume-master 0.25)"
   :js "vnctst.audio4.js.setConfig(\"volume-master\", 0.25)"
   :desc (str "マスター音量を設定する"
              "(音量値は0.0～1.0の範囲、初期値は0.5)。"
              "マスター音量はBGMとSEの両方に影響する。"
              )})

(defba :set-config-volume-bgm-100
  {:fn #(vnctst.audio4/set-config! :volume-bgm 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-bgm 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-bgm\", 1.0)"
   :desc ""})

(defba :set-config-volume-bgm-25
  {:fn #(vnctst.audio4/set-config! :volume-bgm 0.25)
   :cljs "(vnctst.audio4/set-config! :volume-bgm 0.25)"
   :js "vnctst.audio4.js.setConfig(\"volume-bgm\", 0.25)"
   :desc (str "BGM音量を設定する"
              "(音量値は0.0～1.0の範囲、初期値は0.5)。"
              "実際のBGMの再生音量は、この項目とマスター音量から決定される。"
              "初期状態ではマスター音量0.5(50%)かつBGM音量0.5(50%)なので、"
              "実際のBGMの再生音量は0.25(25%)相当となる。"
              "このデフォルト音量では小さすぎると思うなら、"
              "もっと大き目の値を設定するとよい。"
              )})

(defba :set-config-volume-se-100
  {:fn #(vnctst.audio4/set-config! :volume-se 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-se 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-se\", 1.0)"
   :desc ""})

(defba :set-config-volume-se-25
  {:fn #(vnctst.audio4/set-config! :volume-se 0.25)
   :cljs "(vnctst.audio4/set-config! :volume-se 0.25)"
   :js "vnctst.audio4.js.setConfig(\"volume-se\", 0.25)"
   :desc (str "SE音量を設定する"
              "(音量値は0.0～1.0の範囲、初期値は0.5)。"
              "詳細は上のBGM音量の解説文と大体同じ。"
              )})

(defba :set-config-debug?-false
  {:fn #(vnctst.audio4/set-config! :debug? false)
   :cljs "(vnctst.audio4/set-config! :debug? false)"
   :js "vnctst.audio4.js.setConfig(\"debug?\", false)"
   :desc ""})

(defba :set-config-debug?-true
  {:fn #(vnctst.audio4/set-config! :debug? true)
   :cljs "(vnctst.audio4/set-config! :debug? true)"
   :js "vnctst.audio4.js.setConfig(\"debug?\", true)"
   :desc (str "デバッグログをコンソールへ出力したい場合はtrueを設定する"
              "(初期値はfalse、ただしこのデモでは最初からtrueにしてある)。"
              "このvnctst-audio4では「雑に扱っても問題が起こらない」事を"
              "方針としているので、ファイルのロードに失敗したりしていても"
              "再生時にエラーは投げられない。単に何も再生されないだけとなる。"
              "しかしこれでは開発時に不便な為、この設定をtrueにする事で、"
              "エラー等が起こった際に、その内容をコンソールへと"
              "出力するようにできる。"
              )})

(defba :set-config-debug-verbose?-false
  {:fn #(vnctst.audio4/set-config! :debug-verbose? false)
   :cljs "(vnctst.audio4/set-config! :debug-verbose? false)"
   :js "vnctst.audio4.js.setConfig(\"debug-verbose?\", false)"
   :desc ""})

(defba :set-config-debug-verbose?-true
  {:fn #(vnctst.audio4/set-config! :debug-verbose? true)
   :cljs "(vnctst.audio4/set-config! :debug-verbose? true)"
   :js "vnctst.audio4.js.setConfig(\"debug-verbose?\", true)"
   :desc (str "些細なデバッグログもコンソールへ出力したい場合はtrueを設定する"
              "(初期値はfalse、ただしこのデモでは最初からtrueにしてある)。"
              "この設定は前述の debug? が有効な時にしか意味を持たない。"
              "これを有効にする事で、前述のエラー以外に、"
              "「このBGMの再生が開始された」「このSEの再生が停止された」"
              "といった、些細な情報までコンソールに出力されるようになる。"
              "多くの場合は邪魔にしかならないので、"
              "開発時であっても普段はfalseにしておき、"
              "再生/停止タイミング等をきちんと調べたい時のみtrueにするとよい。"
              )})

(defba :set-config-default-bgm-fade-sec-0
  {:fn #(vnctst.audio4/set-config! :default-bgm-fade-sec 0)
   :cljs "(vnctst.audio4/set-config! :default-bgm-fade-sec 0)"
   :js "vnctst.audio4.js.setConfig(\"default-bgm-fade-sec\", 0)"
   :desc ""})

(defba :set-config-default-bgm-fade-sec-05
  {:fn #(vnctst.audio4/set-config! :default-bgm-fade-sec 0.5)
   :cljs "(vnctst.audio4/set-config! :default-bgm-fade-sec 0.5)"
   :js "vnctst.audio4.js.setConfig(\"default-bgm-fade-sec\", 0.5)"
   :desc (str "デフォルトのBGMフェード秒数を設定する(初期値は1)。"
              "0を設定するとフェードなしで即座に停止するようになる。"
              )})

(defba :set-config-default-se-fade-sec-0
  {:fn #(vnctst.audio4/set-config! :default-se-fade-sec 0)
   :cljs "(vnctst.audio4/set-config! :default-se-fade-sec 0)"
   :js "vnctst.audio4.js.setConfig(\"default-se-fade-sec\", 0)"
   :desc ""})

(defba :set-config-default-se-fade-sec-05
  {:fn #(vnctst.audio4/set-config! :default-se-fade-sec 0.5)
   :cljs "(vnctst.audio4/set-config! :default-se-fade-sec 0.5)"
   :js "vnctst.audio4.js.setConfig(\"default-se-fade-sec\", 0.5)"
   :desc (str "デフォルトのSEフェード秒数を設定する(初期値は0)。"
              "0を設定するとフェードなしで即座に停止するようになる。"
              )})

(defba :set-config-dont-stop-on-background?-false
  {:fn #(vnctst.audio4/set-config! :dont-stop-on-background? false)
   :cljs "(vnctst.audio4/set-config! :dont-stop-on-background? false)"
   :js "vnctst.audio4.js.setConfig(\"dont-stop-on-background?\", false)"
   :desc ""})

(defba :set-config-dont-stop-on-background?-true
  {:fn #(vnctst.audio4/set-config! :dont-stop-on-background? true)
   :cljs "(vnctst.audio4/set-config! :dont-stop-on-background? true)"
   :js "vnctst.audio4.js.setConfig(\"dont-stop-on-background?\", true)"
   :desc (str "vnctst-audio4は、ブラウザのタブをバックグラウンドにした際に"
              "BGMを自動的に一時停止する機能を持っている"
              "(非対応ブラウザあり。またSEは停止されない)。"
              "この項目にtrueを設定する事で、その機能を無効化できる"
              "(初期値はfalse)。"
              )})

(defba :set-config-se-chattering-sec-0
  {:fn #(vnctst.audio4/set-config! :se-chattering-sec 0)
   :cljs "(vnctst.audio4/set-config! :se-chattering-sec 0)"
   :js "vnctst.audio4.js.setConfig(\"se-chattering-sec\", 0)"
   :desc ""})

(defba :set-config-se-chattering-sec-05
  {:fn #(vnctst.audio4/set-config! :se-chattering-sec 0.5)
   :cljs "(vnctst.audio4/set-config! :se-chattering-sec 0.5)"
   :js "vnctst.audio4.js.setConfig(\"se-chattering-sec\", 0.5)"
   :desc (str "同一SE連打防止機能の閾値(秒)を設定する(初期値は0.05)。"
              "0を設定すると無効化できる。"
              "ゲームでは同じSEが特定タイミングで複数同時に発生する事が"
              "よくあるが、"
              "何も考えずにこれを行うと音が重なって音量の増幅が起こり、"
              "爆音や音割れの原因となってしまう"
              "(艦これの爆撃や雷撃などで顕著)。"
              "vnctst-audio4ではこの問題を防ぐ為に、"
              "この設定秒数以内での同一SEの再生は"
              "一つだけになるように内部で制限している。"
              )})

(defba :set-config-autoext-list-a
  {:fn #(vnctst.audio4/set-config! :autoext-list ["ogg"])
   :cljs "(vnctst.audio4/set-config! :autoext-list [\"ogg\"])"
   :js "vnctst.audio4.js.setConfig(\"autoext-list\", [\"ogg\"])"
   :desc ""})

(defba :set-config-autoext-list-b
  {:fn #(vnctst.audio4/set-config! :autoext-list ["m4a" "mp3" "ogg" ["wav" "audio/wav"]])
   :cljs "(vnctst.audio4/set-config! :autoext-list [\"m4a\" \"mp3\" \"ogg\" [\"wav\" \"audio/wav\"])"
   :js "vnctst.audio4.js.setConfig(\"autoext-list\", [\"m4a\", \"mp3\", \"ogg\", [\"wav\", \"audio/wav\"])"
   :desc (str "「filename.*」指定による拡張子自動選択機能(autoext)の"
              "拡張子の候補リストを設定する。"
              "autoext指定した音源ファイルのロード時には、"
              "このリストの順でトライされる。"
              "なお ogg, mp3, m4a 以外の拡張子を指定する際には、"
              "上記のwavのように、"
              "一緒にmime-typeも指定する必要があるので注意"
              "(もちろんブラウザが対応していない場合は再生できない)。"
              "初期値は [\"ogg\" \"mp3\" \"m4a\"] 。"
              "この値を変更した場合は内部状態をリセットする必要がある為、"
              "全ての再生中音源は停止され、"
              "また全てのロード済音源もアンロードされる"
              "(ロード/アンロードについては後述)。"
              )})

(defba :set-config-disable-mobile?-false
  {:fn #(vnctst.audio4/set-config! :disable-mobile? false)
   :cljs "(vnctst.audio4/set-config! :disable-mobile? false)"
   :js "vnctst.audio4.js.setConfig(\"disable-mobile?\", false)"
   :desc ""})

(defba :set-config-disable-mobile?-true
  {:fn #(vnctst.audio4/set-config! :disable-mobile? true)
   :cljs "(vnctst.audio4/set-config! :disable-mobile? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-mobile?\", true)"
   :desc (str "trueを設定する事で、モバイル環境での音源再生の一切を禁止する"
              "(初期値はfalse)。"
              "非モバイル環境では何も起こらない。"
              )})

(defba :set-config-disable-webaudio?-false
  {:fn #(vnctst.audio4/set-config! :disable-webaudio? false)
   :cljs "(vnctst.audio4/set-config! :disable-webaudio? false)"
   :js "vnctst.audio4.js.setConfig(\"disable-webaudio?\", false)"
   :desc ""})

(defba :set-config-disable-webaudio?-true
  {:fn #(vnctst.audio4/set-config! :disable-webaudio? true)
   :cljs "(vnctst.audio4/set-config! :disable-webaudio? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-webaudio?\", true)"
   :desc (str "trueを設定する事で、WebAudioによる音源再生を禁止する"
              "(初期値はfalse)。"
              "初期状態では、WebAudioが利用可能ならWebAudioを使い、"
              "そうでなければHtmlAudioが利用可能ならHtmlAudioを使い、"
              "どちらも使えなければ再生は無効される、"
              "という優先順位になっている。"
              "通常はこのままでも問題ないが、"
              "「HtmlAudioでの動作確認を取りたい」等の時に"
              "この設定項目を有効にするとよい。"
              "この値を変更した場合は内部状態をリセットする必要がある為、"
              "全ての再生中音源は停止され、"
              "また全てのロード済音源もアンロードされる。"
              )})

(defba :set-config-disable-htmlaudio?-false
  {:fn #(vnctst.audio4/set-config! :disable-htmlaudio? false)
   :cljs "(vnctst.audio4/set-config! :disable-htmlaudio? false)"
   :js "vnctst.audio4.js.setConfig(\"disable-htmlaudio?\", false)"
   :desc ""})

(defba :set-config-disable-htmlaudio?-true
  {:fn #(vnctst.audio4/set-config! :disable-htmlaudio? true)
   :cljs "(vnctst.audio4/set-config! :disable-htmlaudio? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-htmlaudio?\", true)"
   :desc (str "trueを設定する事で、HtmlAudioによる音源再生を禁止する"
              "(初期値はfalse)。"
              "概要については上の disable-htmlaudio? の項目を参照。"
              "この値を変更した場合は内部状態をリセットする必要がある為、"
              "全ての再生中音源は停止され、"
              "また全てのロード済音源もアンロードされる。"
              )})


;;; preload / unload


(defba :load-noise
  {:fn #(vnctst.audio4/load! "bgm/noise.*")
   :cljs "(vnctst.audio4/load! \"bgm/noise.*\")"
   :js "vnctst.audio4.js.load(\"bgm/noise.*\")"
   :desc (str "BGMやSEの音響ファイルの初回再生時は、実は内部で"
              "ファイルのロードを行いそれが完了してから再生している。"
              "なので初回再生時のみ実際に再生されるまでタイムラグがある"
              "(ファイルサイズが小さかったりブラウザキャッシュがなされていれば"
              "目立たないが)。"
              "このタイムラグをなくすには、再生するよりずっと前の段階で"
              "ロードを行っておけばよい。"
              "この関数はそのロードをバックグラウンドで行わせる。"
              "既にロード中だったりロードが完了している場合は何も行われない。"
              )})

(defba :loaded?
  {:fn #(js/alert (vnctst.audio4/loaded? "bgm/noise.*"))
   :cljs "(vnctst.audio4/loaded? \"bgm/noise.*\")"
   :js "vnctst.audio4.js.isLoaded(\"bgm/noise.*\")"
   :desc (str "音響ファイルのロードはバックグラウンドで非同期に実行される。"
              "この関数は、そのロードが正常終了/異常終了のどちらにせよ"
              "完了しているかどうかを真偽値で返す。"
              "ローディング画面等では、定期的にこの関数を呼んで"
              "ロードが完了したかを確認するとよい。"
              )})

(defba :error?
  {:fn #(js/alert (vnctst.audio4/error? "bgm/noise.*"))
   :cljs "(vnctst.audio4/error? \"bgm/noise.*\")"
   :js "vnctst.audio4.js.isError(\"bgm/noise.*\")"
   :desc (str "前述の loaded? / isLoaded ではロードの完了は分かるものの、"
              "正常にロードできたかまでは分からない。"
              "ロード時にエラーが起こったかどうかを調べたい時は、"
              "真偽値としてそれを返すこの関数が使える。"
              )})

(defba :unload-noise
  {:fn #(vnctst.audio4/unload! "bgm/noise.*")
   :cljs "(vnctst.audio4/unload! \"bgm/noise.*\")"
   :js "vnctst.audio4.js.unload(\"bgm/noise.*\")"
   :desc (str "音響ファイルの数が非常に多い場合や"
              "サーバで動的に生成した音響ファイルを扱う場合、"
              "ロード済の音源が多くなりメモリを圧迫してしまう事がある。"
              "その場合はこの関数を使い、"
              "再生しない音源をアンロードするとよい。"
              "もしアンロード時にまだその音響ファイルが再生中だった場合、"
              "その再生は強制停止される。"
              "アンロード後にその音響ファイルを再生しようとした場合は"
              "内部でファイルのロードが実行し直されるので、"
              "効率は悪いものの再生自体に支障が出る事はない。"
              "アンロード後は、前述の loaded? / isLoaded が"
              "またfalseを返すようになる。"
              )})

(defba :unload-all
  {:fn #(vnctst.audio4/unload-all!)
   :cljs "(vnctst.audio4/unload-all!"
   :js "vnctst.audio4.js.unloadAll()"
   :desc (str "ロード済の全ての音響ファイルをアンロードする。")})


;;; more BGM


(defba :bgm-option-a
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*" {:volume 0.5 :pitch 1.5 :pan -0.5})
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\" {:volume 0.5 :pitch 1.5 :pan -0.5})"
   :js "vnstst.audio4.js.bgm(\"bgm/va32.*\", {volume: 0.5, pitch: 1.5, pan: -0.5})"
   :desc (str "\"bgm/va32.ogg\" もしくは \"bgm/va32.mp3\" を"
              "BGMとして再生する。"
              ""
              "volumeは個別の音量。通常 0.0 ～ 1.0 の数値。"
              "指定しない場合は 1.0 が指定された事になる。"
              "volumeに1.0以上の数値を指定する事も可能だが、"
              "マスターボリュームとBGMボリュームの設定によっては効果が出ない"
              "(「volume * マスターボリューム * BGMボリューム」の値を"
              "1.0以上にする事はできない為)。"
              ""
              "pitchは再生レート。 0.1 ～ 10.0 の数値。"
              "指定しない場合は 1.0 が指定された事になる。"
              "この数値が1.0より小さいと再生速度と音程が低下し、"
              "1.0より大きいと再生速度と音程が上昇する。"
              "ブラウザによっては常に1.0固定となる為、"
              "この数値に依存するような処理は避けた方が無難。"
              ""
              "panはステレオでの左右への寄りの値。 -1.0 ～ 1.0 の数値。"
              "-1.0が最も左寄り、0なら中央、1.0が最も右寄りに再生される。"
              "指定しない場合は 0 が指定された事になる。"
              "ブラウザによっては常に中央固定になる為、"
              "この数値に依存するような処理は避けた方が無難。"
              )})

(defba :bgm-option-b
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*" :volume 1.0 :pitch 1.0 :pan 0.5)
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\" :volume 1.0 :pitch 1.0 :pan 0.5)"
   :js "vnstst.audio4.js.bgm(\"bgm/va32.*\", {volume: 1.0, pitch: 1.0, pan: 0.5})"
   :desc (str "\"bgm/va32.ogg\" もしくは \"bgm/va32.mp3\" を"
              "BGMとして再生する。"
              "各オプションの詳細については前述の説明を参照。"
              "cljs版では、追加の引数は一つのmapで指定してもよいし、"
              "複数のkey-value値として指定してもよい"
              "(js版ではObjectでの指定のみ可能)。"
              )})

(defba :bgm-option-c
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*" :volume 1.5 :pitch 0.5 :pan 0)
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\" :volume 1.5 :pitch 0.5 :pan 0)"
   :js "vnstst.audio4.js.bgm(\"bgm/va32.*\", {volume: 1.5, pitch: 0.5, pan: 0})"
   :desc (str "\"bgm/va32.ogg\" もしくは \"bgm/va32.mp3\" を"
              "BGMとして再生する。"
              "オプションの詳細については前述の説明を参照。"
              )})

(defba :bgm-noise-ch
  {:fn #(vnctst.audio4/bgm! "bgm/noise.*" :channel "BGS")
   :cljs "(vnctst.audio4/bgm! \"bgm/noise.*\" :channel \"BGS\")"
   :js "vnctst.audio4.js.bgm(\"bgm/noise.*\", {channel: \"BGS\"})"
   :desc (str "\"bgm/noise.ogg\" もしくは \"bgm/noise.mp3\" を"
              "「\"BGS\"」という名前のBGM再生チャンネルにて、"
              "ループBGMとして再生する。"
              ""
              "BGM再生チャンネルは必要な数だけ作成でき、"
              "違うBGM再生チャンネルは同時に再生される"
              "(これは「BGMと同時に風音などの環境音を再生したい」ような"
              "用途に利用できる)。"
              "BGM再生チャンネル名には好きな数値、文字列、キーワード等を"
              "指定できる。"
              "BGM再生チャンネル名が省略された場合はデフォルト値として"
              "「0」が指定されたものとして扱われる。"
              )})

(defba :stop-bgm-ch-a
  {:fn #(vnctst.audio4/stop-bgm! nil "BGS")
   :cljs "(vnctst.audio4/stop-bgm! nil \"BGS\")"
   :js "vnstst.audio4.js.stopBgm(null, \"BGS\")"
   :desc (str "第二引数で指定した「BGM再生チャンネル名」で再生中のBGMだけを、"
              "第一引数で指定したフェード秒数かけて終了する。"
              "第一引数に nil / null を指定した場合は、"
              "設定されたデフォルト値がフェード秒数として適用される"
              "(詳細については既出の「設定項目」内「音量設定」の項目を参照)。"
              "第二引数省略時は全てのBGMチャンネルに対して停止処理が行われる。"
              ""
              "この例では「\"BGS\"」だけが停止される。"
              )})

(defba :stop-bgm-ch-b
  {:fn #(vnctst.audio4/stop-bgm! 0.25 0)
   :cljs "(vnctst.audio4/stop-bgm! 0.25 0)"
   :js "vnstst.audio4.js.stopBgm(0.25, 0)"
   :desc (str "第二引数で指定した「BGM再生チャンネルID」で再生中のBGMだけを、"
              "第一引数で指定したフェード秒数かけて終了する。"
              ""
              "この例では「0」、つまりチャンネル無指定でのBGM再生だけが"
              "停止される。"
              )})


;;; more SE


(defba :se-option-a
  {:fn #(vnctst.audio4/se! "se/launch.*" {:volume 0.5 :pitch 2.0 :pan -0.5})
   :cljs "(vnctst.audio4/se! \"se/launch.*\" {:volume 0.5 :pitch 2.0 :pan -0.5})"
   :js "vnstst.audio4.js.se(\"se/launch.*\", {volume: 0.5, pitch: 2.0, pan: -0.5})"
   :desc (str "\"se/launch.ogg\" もしくは \"se/launch.mp3\" を"
              "SEとして再生する。"
              ""
              "volumeは個別の音量。通常 0.0 ～ 1.0 の数値。"
              "指定しない場合は 1.0 が指定された事になる。"
              "volumeに1.0以上の数値を指定する事も可能だが、"
              "マスターボリュームとSEボリュームの設定によっては効果が出ない"
              "(「volume * マスターボリューム * SEボリューム」の値を"
              "1.0以上にする事はできない為)。"
              ""
              "pitchは再生速度。 0.1 ～ 10.0 の数値。"
              "指定しない場合は 1.0 が指定された事になる。"
              "この数値が1.0より小さいと再生速度が減速され、"
              "1.0より大きいと再生速度が加速される。"
              "ブラウザによっては常に1.0固定となる為、"
              "この数値に依存するような処理は避けた方が無難。"
              ""
              "panはステレオでの左右への寄りの値。 -1.0 ～ 1.0 の数値。"
              "-1.0が最も左寄り、0なら中央、1.0が最も右寄りに再生される。"
              "指定しない場合は 0 が指定された事になる。"
              "ブラウザによっては常に中央固定になる為、"
              "この数値に依存するような処理は避けた方が無難。"
              )})

(defba :se-option-b
  {:fn #(vnctst.audio4/se! "se/launch.*" :volume 1.0 :pitch 1.0 :pan 0.5)
   :cljs "(vnctst.audio4/se! \"se/launch.*\" :volume 1.0 :pitch 1.0 :pan 0.5)"
   :js "vnstst.audio4.js.se(\"se/launch.*\", {volume: 1.0, pitch: 1.0, pan: 0.5})"
   :desc (str "\"se/launch.ogg\" もしくは \"se/launch.mp3\" を"
              "SEとして再生する。"
              "各オプションの詳細については前述の説明を参照。"
              "cljs版では、追加の引数は一つのmapで指定してもよいし、"
              "複数のkey-value値として指定してもよい"
              "(js版ではObjectでの指定のみ可能)。"
              )})

(defba :se-option-c
  {:fn #(vnctst.audio4/se! "se/launch.*" :volume 1.5 :pitch 0.5 :pan 0)
   :cljs "(vnctst.audio4/se! \"se/launch.*\" :volume 1.5 :pitch 0.5 :pan 0)"
   :js "vnstst.audio4.js.se(\"se/launch.*\", {volume: 1.5, pitch: 0.5, pan: 0})"
   :desc (str "\"se/launch.ogg\" もしくは \"se/launch.mp3\" を"
              "SEとして再生する。"
              "オプションの詳細については前述の説明を参照。"
              ""
              "SE再生関数は、返り値として「SE再生チャンネルID」を返す。"
              "これについての詳細は次の項目を参照。"
              )})

(defba :stop-se-ch
  {:fn #(when-let [se-ch (vnctst.audio4/last-played-se-channel-id)]
          (vnctst.audio4/stop-se! 0 se-ch))
   :cljs "(vnctst.audio4/stop-se! 0 se-channel-id)"
   :js "vnstst.audio4.js.stopSe(0, seChannelId)"
   :desc (str "第二引数で指定した「SE再生チャンネルID」に対応するSEだけを、"
              "第一引数で指定したフェード秒数かけて終了する。"
              "「SE再生チャンネルID」は、SE再生関数の返り値として得られる"
              "(この「SE再生チャンネルID」は、そのまま捨てても全く問題ない)。"
              "第二引数で指定した「SE再生チャンネルID」に対応するSEの再生が"
              "既に完了している場合は何も起きない。"
              "第一引数に nil / null を指定した場合は、"
              "設定されたデフォルト値がフェード秒数として適用される"
              "(詳細については既出の「設定項目」内「音量設定」の項目を参照)。"
              "第二引数省略時は全てのSEに対して停止処理が行われる。"
              )})

(defba :alarm-kick
  {:fn #(vnctst.audio4/alarm! "se/kick.*")
   :cljs "(vnctst.audio4/alarm! \"se/kick.*\")"
   :js "vnstst.audio4.js.alarm(\"se/kick.*\")"
   :desc (str "\"se/kick.ogg\" もしくは \"se/kick.mp3\" を"
              "SEとして再生する。"
              "ただしバックグラウンドタブであっても強制的に再生が行われる"
              "(通常はバックグラウンドタブ時はSEの再生が行われない)。"
              "通常のSE再生と同様に、追加の引数を取る事もできる"
              "(詳細は上記参照)。"
              )})


;;; misc


(defba :version-js
  {:fn #(js/alert vnctst.audio4.js/version)
   :cljs "----"
   :js "vnctst.audio4.js.version"
   :desc "vnctst-audio4のライブラリとしてのバージョン文字列。js版のみ提供。"
   })

(defba :can-play-ogg
  {:fn #(js/alert (vnctst.audio4/can-play-ogg?))
   :cljs "(vnctst.audio4/can-play-ogg?)"
   :js "vnctst.audio4.js.canPlayOgg()"
   :desc "oggが再生可能なら真値を返す。"
   })

(defba :can-play-mp3
  {:fn #(js/alert (vnctst.audio4/can-play-mp3?))
   :cljs "(vnctst.audio4/can-play-mp3?)"
   :js "vnctst.audio4.js.canPlayMp3()"
   :desc "mp3が再生可能なら真値を返す。"
   })

(defba :can-play-m4a
  {:fn #(js/alert (vnctst.audio4/can-play-m4a?))
   :cljs "(vnctst.audio4/can-play-m4a?)"
   :js "vnctst.audio4.js.canPlayM4a()"
   :desc "m4aが再生可能なら真値を返す。"
   })

(defba :can-play
  {:fn #(js/alert (vnctst.audio4/can-play? "audio/wav"))
   :cljs "(vnctst.audio4/can-play? \"audio/wav\")"
   :js "vnctst.audio4.js.canPlay(\"audio/wav\")"
   :desc "引数として渡したmime-typeが再生可能なら真値を返す。"
   })

(defba :terminal-type
  {:fn #(js/alert
          (boolean (vnctst.audio4/terminal-type :firefox)))
   :cljs "(vnctst.audio4/terminal-type :firefox)"
   :js "vnctst.audio4.js.hasTerminalType(\"firefox\")"
   :desc (str "この環境が引数として渡した端末タイプなら"
              "真値を返す。"
              "端末タイプは"
              " tablet"
              " mobile"
              " android"
              " ios"
              " chrome"
              " firefox"
              " が指定可能。"
              "ただしこれは User-Agent による判定の為、"
              "誤判定する場合もある事に注意。"
              ""
              "これはjs版では関数だが、cljs版ではsetでありset用の各種の"
              "操作が適用可能。")
   })

(defba :float->percent
  {:fn #(js/alert (vnctst.audio4/float->percent 0.25))
   :cljs "(vnctst.audio4/float->percent 0.25)"
   :js "vnctst.audio4.js.floatToPercent(0.25)"
   :desc (str "各ボリューム設定は0.0～1.0の小数値で指定するが、"
              "これを0～100のパーセント値へと変換する"
              "単純なユーティリティ関数。")
   })

(defba :percent->float
  {:fn #(js/alert (vnctst.audio4/percent->float 25))
   :cljs "(vnctst.audio4/percent->float 25)"
   :js "vnctst.audio4.js.percentToFloat(25)"
   :desc (str "float->percent / floatToPercent の"
              "逆変換を行うユーティリティ。")
   })





(defn- sync-button-labels! []
  (when-let [dom (js/document.getElementById "config-info")]
    (let [msg (if @display-js-mode?
                (str "vnctst.audio4.js.setConfig("
                     (string/join ", "
                                  (map #(if (keyword? %)
                                          (pr-str (name %))
                                          (pr-str %))
                                       config-options))
                     ")")
                (str "(vnctst.audio4/set-config! "
                     (string/join " " (map pr-str config-options))
                     ")"))]
      (set! (.. dom -textContent) msg)))
  (when-let [dom (js/document.getElementById "preload-info")]
    (let [msg (string/join ", " (map pr-str preload-pathes))]
      (set! (.. dom -textContent) msg)))
  (doseq [[k m] (seq @button-assign)]
    (when-let [dom (js/document.getElementById (name k))]
      ;(js/addEventListener dom "click" (:fn m))
      (aset dom "onclick" (:fn m))
      (set! (.. dom -textContent) (if @display-js-mode?
                                    (:js m)
                                    (:cljs m))))
    (when-let [dom (js/document.getElementById (str (name k) "-desc"))]
      (set! (.. dom -textContent) (:desc m)))))


(defn- display-msg! [msg & more-msgs]
  (when-let [dom (js/document.getElementById "message")]
    (set! (.. dom -textContent) (apply print-str msg more-msgs))))

(defn- display-version! []
  (when-let [dom (js/document.getElementById "version")]
    (set! (.. dom -textContent) (str "Version: "
                                     audio4-js/version))))

(defn- show-floating-header! []
  (when-let [dom (js/document.getElementById "floating-header")]
    (set! (.. dom -style -display) "block")))

(defn- show-buttons! []
  (when-let [dom (js/document.getElementById "main")]
    (set! (.. dom -style -display) "block")))


(defn ^:export jsmode [bool]
  (reset! display-js-mode? bool)
  (sync-button-labels!))

(defn ^:export bootstrap []
  (sync-button-labels!)
  (apply audio4/set-config! config-options)
  ;; プリセットのプリロードとロード待ちを行う
  (let [target-num (count preload-pathes)
        display-progress! #(display-msg! (str "Loading ... "
                                              %
                                              " / "
                                              target-num))]
    (display-version!)
    (display-progress! 0)
    (doseq [path preload-pathes]
      (audio4/load! path))
    (go-loop []
      (<! (async/timeout 200))
      (let [c (count (filter audio4/loaded? preload-pathes))]
        (display-progress! c)
        (if (< c target-num)
          (recur)
          (do
            (show-floating-header!)
            (show-buttons!)
            (display-msg! "Loaded.")))))))



