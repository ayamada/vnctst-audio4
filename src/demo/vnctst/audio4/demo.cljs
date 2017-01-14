(ns vnctst.audio4.demo
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :as async :refer [>! <!]]
            [clojure.string :as string]
            [vnctst.audio4 :as audio4]
            [vnctst.audio4.js :as audio4-js]))



(defonce display-js-mode? (atom true))


(def config-options
  [:debug? true
   ])

(def preload-pathes
  ["se/launch.*"
   "se/shootout.*"
   ])

(def button-assign (atom {}))
(defn- defba [k m]
  (swap! button-assign assoc k m))

(defba :bgm-va32
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*")
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\")"
   :js "vnctst.audio4.js.bgm(\"bgm/va32.*\")"
   :desc (str "\"bgm/va32.ogg\" もしくは \"bgm/va32.mp3\" を"
              "ループBGMとして再生する。"
              "この引数は \"bgm/va32.ogg\" のように"
              "普通に拡張子を付けて指定してもよいのだが、"
              "そうすると当然、"
              "ogg再生のできないブラウザでは音が出ない。"
              "上記のように \"bgm/va32.*\" 形式で指定する事で、"
              "oggが再生可能ならoggを、そうでなければmp3を"
              "再生する事ができる。"
              "もちろんoggとmp3の両方のファイルを"
              "予め設置しておく必要がある"
              "(逆に言えば、一つしかファイルを用意しないのであれば"
              "普通に拡張子を付けて指定した方が分かりやすい)。"
              "最後に、引数は http://... のようなurlも指定可能だが、"
              "その場合は「CORS」の設定が必要になる場合がある事に注意"
              "(詳細はネットで検索)。"
              )})

(defba :bgm-rnr
  {:fn #(vnctst.audio4/bgm! "bgm/rnr.*")
   :cljs "(vnctst.audio4/bgm! \"bgm/rnr.*\")"
   :js "vnctst.audio4.js.bgm(\"bgm/rnr.*\")"
   :desc (str "\"bgm/rnr.ogg\" もしくは \"bgm/rnr.mp3\" を"
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
              "この「デフォルト秒数」は後述の設定機能で変更可能。"
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

(defba :se-shootout
  {:fn #(vnctst.audio4/se! "se/shootout.*")
   :cljs "(vnctst.audio4/se! \"se/shootout.*\")"
   :js "vnstst.audio4.js.se(\"se/shootout.*\")"
   :desc (str "\"se/shootout.ogg\" もしくは \"se/shootout.mp3\" を"
              "SEとして再生し、その再生チャネルを返り値として返す"
              "(再生チャネルは通常そのまま捨てて構わない)。"
              "SEとしての再生では、音源の多重再生が可能となる。"
              )})

;;; TODO: 以下を使うボタンの追加が残っている(追加が完了したら消していく事)
;bgm/noise.*
;
;stop-bgm!
;bgm!
;bgm-oneshot!
;me!
;bgs!
;load!
;unload!
;loaded?
;error?
;unload-all!
   ;; SE
   ;:play-se-jump {:fn #(vnctst.audio4/play! :se/jump)
   ;               :cljs "(vnctst.audio4/play! :se/jump)"
   ;               :js "vnctst.audio4.js.play({se: \"jump\"})"
   ;               :desc (str "\"audio/se/jump.{ogg,mp3}\" をSEとして再生する。"
   ;                          "連打での多重再生が可能")
   ;               }
   ;:play-se-yarare {:fn #(vnctst.audio4/play! :se/yarare)
   ;                 :cljs "(vnctst.audio4/play! :se/yarare)"
   ;                 :js "vnctst.audio4.js.play({se: \"yarare\"})"
   ;                 :desc "\"audio/se/yarare.{ogg,mp3}\" をSEとして再生する"
   ;                 }
   ;:play-se-yarare-ogg {:fn #(vnctst.audio4/se! "audio/se/yarare.ogg")
   ;                     :cljs "(vnctst.audio4/se! \"audio/se/yarare.ogg\")"
   ;                     :js "vnctst.audio4.js.se(\"audio/se/yarare.ogg\")"
   ;                     :desc "\"audio/se/yarare.ogg\" をSEとして再生する"
   ;                     }
   ;; BGM
   ;:play-bgm-drop {:fn #(vnctst.audio4/play! :bgm/drop)
   ;                :cljs "(vnctst.audio4/play! :bgm/drop)"
   ;                :js "vnctst.audio4.js.play({bgm: \"drop\"})"
   ;                :desc "\"audio/bgm/drop.{ogg,mp3}\" をBGMとして再生する"
   ;                }
   ;:play-bgm-drop-2 {:fn #(vnctst.audio4/play! :bgm/drop 1.5 1.2 0.2)
   ;                  :cljs "(vnctst.audio4/play! :bgm/drop 1.5 1.2 0.2)"
   ;                  :js "vnctst.audio4.js.play({bgm: \"drop\"}, 1.5, 1.2, 0.2)"
   ;                  :desc (str "\"audio/bgm/drop.{ogg,mp3}\" を"
   ;                             "BGMとして再生する。"
   ;                             "引数は音量(省略時1.0)、"
   ;                             "ピッチ(再生速度倍率、省略時1.0)、"
   ;                             "パン(左右に寄せる、省略時0、-1が左最大、"
   ;                             "1が右最大)。"
   ;                             "環境によってはピッチ、パンが無効な場合あり")
   ;                  }
   ;:play-me-unmei {:fn #(vnctst.audio4/play! :me/unmei)
   ;                :cljs "(vnctst.audio4/play! :me/unmei)"
   ;                :js "vnctst.audio4.js.play({me: \"unmei\"})"
   ;                :desc "\"audio/me/unmei.{ogg,mp3}\" をMEとして再生する"
   ;                }
   ;:play-me-unmei-2 {:fn #(vnctst.audio4/play! :me/unmei 1.5 1.2 0.2)
   ;                  :cljs "(vnctst.audio4/play! :me/unmei 1.5 1.2 0.2)"
   ;                  :js "vnctst.audio4.js.play({me: \"unmei\"}, 1.5, 1.2, 0.2)"
   ;                  :desc (str "\"audio/me/unmei.{ogg,mp3}\" を"
   ;                             "MEとして再生する。"
   ;                             "引数は音量(省略時1.0)、"
   ;                             "ピッチ(再生速度倍率、省略時1.0)、"
   ;                             "パン(左右に寄せる、省略時0、-1が左最大、"
   ;                             "1が右最大)。"
   ;                             "環境によってはピッチ、パンが無効な場合あり")
   ;                  }
   ;:play-bgm-drop-ogg {:fn #(vnctst.audio4/bgm! "audio/bgm/drop.ogg")
   ;                    :cljs "(vnctst.audio4/bgm! \"audio/bgm/drop.ogg\")"
   ;                    :js "vnctst.audio4.js.bgm(\"audio/bgm/drop.ogg\")"
   ;                    :desc (str "\"audio/bgm/drop.ogg\" を"
   ;                               "BGMとして再生する。"
   ;                               "任意のurlを指定可能"
   ;                               "(外部サーバ指定時は要CORS設定)。"
   ;                               "この環境でoggが再生可能かどうかは"
   ;                               "後述の方法で確認可能。"
   ;                               "再生できない環境の場合は何も再生されない。")
   ;                    }
   ;:play-bgm-drop-mp3 {:fn #(vnctst.audio4/bgm! "audio/bgm/drop.mp3")
   ;                    :cljs "(vnctst.audio4/bgm! \"audio/bgm/drop.mp3\")"
   ;                    :js "vnctst.audio4.js.bgm(\"audio/bgm/drop.mp3\")"
   ;                    :desc (str "\"audio/bgm/drop.mp3\" を"
   ;                               "BGMとして再生する。"
   ;                               "再生できない環境の場合は何も再生されない。")
   ;                    }
   ;:play-me-unmei-ogg {:fn #(vnctst.audio4/me! "audio/me/unmei.ogg")
   ;                    :cljs "(vnctst.audio4/me! \"audio/me/unmei.ogg\")"
   ;                    :js "vnctst.audio4.js.me(\"audio/me/unmei.ogg\")"
   ;                    :desc "\"audio/me/unmei.ogg\" をMEとして再生する"
   ;                    }
   ;:play-bgm-nil {:fn #(vnctst.audio4/bgm! nil)
   ;               :cljs "(vnctst.audio4/bgm! nil)"
   ;               :js "vnctst.audio4.js.bgm(null)"
   ;               :desc "BGM / ME をフェード停止させる"
   ;               }
   ;:play-me-nil {:fn #(vnctst.audio4/me! nil)
   ;              :cljs "(vnctst.audio4/me! nil)"
   ;              :js "vnctst.audio4.js.me(null)"
   ;              :desc "BGM / ME をフェード停止させる"
   ;              }
   ;:stop-bgs {:fn #(vnctst.audio4/stop-bgs!)
   ;           :cljs "(vnctst.audio4/stop-bgs!)"
   ;           :js "vnctst.audio4.js.stopBgs()"
   ;           :desc "BGSをフェード停止させる"
   ;           }
   ;:stop-bgs-0 {:fn #(vnctst.audio4/stop-bgs! 0)
   ;             :cljs "(vnctst.audio4/stop-bgs! 0)"
   ;             :js "vnctst.audio4.js.stopBgs(0)"
   ;             :desc "BGSを即座に停止させる(引数はフェード秒数)"
   ;             }
   ;:play-bgs-noise {:fn #(vnctst.audio4/play! :bgs/noise)
   ;                 :cljs "(vnctst.audio4/play! :bgs/noise)"
   ;                 :js "vnctst.audio4.js.play({bgs: \"noise\"})"
   ;                 :desc "\"audio/bgs/noise.{ogg,mp3}\" をBGSとして再生する"
   ;                 }
   ;:play-bgs-nil {:fn #(vnctst.audio4/bgs! nil)
   ;               :cljs "(vnctst.audio4/bgs! nil)"
   ;               :js "vnctst.audio4.js.bgs(null)"
   ;               :desc "BGSをフェード停止させる"
   ;               }

;;; Configure
(defba :config-volume-master
  {:fn #(js/alert (vnctst.audio4/config :volume-master))
   :cljs "(vnctst.audio4/config :volume-master)"
   :js "vnctst.audio4.js.getConfig(\"volume-master\")"
   :desc (str "各種の設定値を取得する。"
              "この volume-master は、マスター音量の現在値。"
              "後述の他の設定値の取得もこれで行える"
              "(それぞれのボタンを用意するのは面倒なので、"
              "あとは自分でコンソール等から試してください)")
   })

(defba :set-config-volume-master-25
  {:fn #(vnctst.audio4/set-config! :volume-master 0.25)
   :cljs "(vnctst.audio4/set-config! :volume-master 0.25)"
   :js "vnctst.audio4.js.setConfig(\"volume-master\", 0.25)"
   :desc (str "マスター音量を25%に設定する"
              "(音量値は0.0～1.0の範囲、初期値は0.5)")
   })

(defba :set-config-volume-master-50
  {:fn #(vnctst.audio4/set-config! :volume-master 0.5)
   :cljs "(vnctst.audio4/set-config! :volume-master 0.5)"
   :js "vnctst.audio4.js.setConfig(\"volume-master\", 0.5)"
   :desc "マスター音量を50%に設定する"
   })

(defba :set-config-volume-master-100
  {:fn #(vnctst.audio4/set-config! :volume-master 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-master 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-master\", 1.0)"
   :desc "マスター音量を100%に設定する"
   })

(defba :set-config-volume-bgm-100
  {:fn #(vnctst.audio4/set-config! :volume-bgm 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-bgm 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-bgm\", 1.0)"
   :desc (str "BGM音量を100%に設定する"
              "(音量値は0.0～1.0の範囲、初期値は0.5)")
   })

(defba :set-config-volume-se-100
  {:fn #(vnctst.audio4/set-config! :volume-se 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-se 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-se\", 1.0)"
   :desc (str "SE音量を100%に設定する"
              "(音量値は0.0～1.0の範囲、初期値は0.5)")
   })

(defba :set-config-debug?-true
  {:fn #(vnctst.audio4/set-config! :debug? true)
   :cljs "(vnctst.audio4/set-config! :debug? true)"
   :js "vnctst.audio4.js.setConfig(\"debug?\", true)"
   :desc (str "デバッグコンソールへの出力を有効化する"
              "(初期値はfalse、ただしこのデモでは最初からtrueにしてある)")
   })

(defba :set-config-se-chattering-sec-0
  {:fn #(vnctst.audio4/set-config! :se-chattering-sec 0)
   :cljs "(vnctst.audio4/set-config! :se-chattering-sec 0)"
   :js "vnctst.audio4.js.setConfig(\"se-chattering-sec\", 0)"
   :desc (str "同一SE連打防止機能の閾値(秒)を設定する。"
              "0を設定すると無効化できる"
              "(初期値は0.05)")
   })

(defba :set-config-default-bgm-fade-sec-2
  {:fn #(vnctst.audio4/set-config! :default-bgm-fade-sec 2)
   :cljs "(vnctst.audio4/set-config! :default-bgm-fade-sec 2)"
   :js "vnctst.audio4.js.setConfig(\"default-bgm-fade-sec\", 2)"
   :desc (str "デフォルトのBGMフェード秒数を設定する。"
              "0を設定するとフェードなしになる"
              "(初期値は1)")
   })

(defba :set-config-default-se-fade-sec-1
  {:fn #(vnctst.audio4/set-config! :default-se-fade-sec 1)
   :cljs "(vnctst.audio4/set-config! :default-se-fade-sec 1)"
   :js "vnctst.audio4.js.setConfig(\"default-se-fade-sec\", 1)"
   :desc (str "デフォルトのSEフェード秒数を設定する。"
              "0を設定するとフェードなしになる"
              "(初期値は0)")
   })

(defba :set-config-autoext-list
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
              "(もちろんブラウザが対応していなければ再生はできない)。"
              "初期値は [\"ogg\" \"mp3\" \"m4a\"] 。"
              "この値を変更した場合、一旦、全ての再生中音源は停止され、"
              "また全てのロード済音源もアンロードされる。")
   })

(defba :set-config-dont-stop-on-background?-true
  {:fn #(vnctst.audio4/set-config! :dont-stop-on-background? true)
   :cljs "(vnctst.audio4/set-config! :dont-stop-on-background? true)"
   :js "vnctst.audio4.js.setConfig(\"dont-stop-on-background?\", true)"
   :desc (str "ブラウザのタブをバックグラウンドにした際にBGMが自動的に"
              "一時停止されるが、この機能を無効にしたい場合にtrueを設定する"
              "(初期値はfalse)")
   })

(defba :set-config-disable-mobile?-true
  {:fn #(vnctst.audio4/set-config! :disable-mobile? true)
   :cljs "(vnctst.audio4/set-config! :disable-mobile? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-mobile?\", true)"
   :desc (str "trueを設定する事で、モバイル環境での音源再生を禁止する"
              "(初期値はfalse)。")
   })

(defba :set-config-disable-webaudio?-true
  {:fn #(vnctst.audio4/set-config! :disable-webaudio? true)
   :cljs "(vnctst.audio4/set-config! :disable-webaudio? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-webaudio?\", true)"
   :desc (str "trueを設定する事で、WebAudioによる音源再生を禁止する"
              "(初期値はfalse)。"
              "この値を変更した場合、一旦、全ての再生中音源は停止され、"
              "また全てのロード済音源もアンロードされる。")
   })

(defba :set-config-disable-htmlaudio?-true
  {:fn #(vnctst.audio4/set-config! :disable-htmlaudio? true)
   :cljs "(vnctst.audio4/set-config! :disable-htmlaudio? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-htmlaudio?\", true)"
   :desc (str "trueを設定する事で、HtmlAudioによる音源再生を禁止する"
              "(初期値はfalse)。"
              "この値を変更した場合、一旦、全ての再生中音源は停止され、"
              "また全てのロード済音源もアンロードされる。")
   })

;;; Misc
(defba :can-play-ogg
  {:fn #(js/alert (vnctst.audio4/can-play-ogg?))
   :cljs "(vnctst.audio4/can-play-ogg?)"
   :js "vnctst.audio4.js.canPlayOgg()"
   :desc "oggが再生可能なら真値を返す"
   })

(defba :can-play-mp3
  {:fn #(js/alert (vnctst.audio4/can-play-mp3?))
   :cljs "(vnctst.audio4/can-play-mp3?)"
   :js "vnctst.audio4.js.canPlayMp3()"
   :desc "mp3が再生可能なら真値を返す"
   })

(defba :can-play-m4a
  {:fn #(js/alert (vnctst.audio4/can-play-m4a?))
   :cljs "(vnctst.audio4/can-play-m4a?)"
   :js "vnctst.audio4.js.canPlayM4a()"
   :desc "m4aが再生可能なら真値を返す"
   })

(defba :can-play
  {:fn #(js/alert (vnctst.audio4/can-play? "audio/wav"))
   :cljs "(vnctst.audio4/can-play? \"audio/wav\")"
   :js "vnctst.audio4.js.canPlay(\"audio/wav\")"
   :desc "引数として渡したmime-typeが再生可能なら真値を返す"
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
              "誤判定する場合もある事に注意。")
   })

(defba :float->percent
  {:fn #(js/alert (vnctst.audio4/float->percent 0.25))
   :cljs "(vnctst.audio4/float->percent 0.25)"
   :js "vnctst.audio4.js.floatToPercent(0.25)"
   :desc (str "ボリューム値は0.0～1.0の小数値で指定するが、"
              "これを0～100のパーセント値へと変換する"
              "単純なユーティリティ関数")
   })

(defba :percent->float
  {:fn #(js/alert (vnctst.audio4/percent->float 25))
   :cljs "(vnctst.audio4/percent->float 25)"
   :js "vnctst.audio4.js.percentToFloat(25)"
   :desc (str "float->percent / floatToPercent の"
              "逆変換を行うユーティリティ")
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
            (show-buttons!)
            (display-msg! "Loaded.")))))))



