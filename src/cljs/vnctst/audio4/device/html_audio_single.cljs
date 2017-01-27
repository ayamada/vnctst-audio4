(ns vnctst.audio4.device.html-audio-single
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.device.entry-table :as entry-table]
            [vnctst.audio4.util :as util]
            [cljs.core.async :as async :refer [>! <!]]
            ))




;;; 実装について
;;; - html-audio-single は「単一再生のみでよい」ので、
;;;   audio-source = audio-channel という構造とし、
;;;   audio-source 側に再生インスタンスの実体を持たせる形式とした。





;;; errorイベントが発火しない環境がある為、
;;; ロードがこの秒数待っても終わらない場合は強制的にロード失敗扱いにする
(def ^:private error-timeout-sec 65)




(def ^:dynamic p-key :html-audio-single)

(defn- p [& args]
  (when entry-table/device-log-verbose?
    (util/logging p-key args)))




(def ^:private loaded-handle-keys
  ["loadeddata"
   "canplay"
   ;; 環境によっては、以下での判定になるものがあるらしい、
   ;; しかしこれらを設定すると他の環境で誤判定になるので、
   ;; これらについては諦めてタイマーでの検知とする
   ;"suspend"
   ;"stalled"
   ])


(defn- ready? [a]
  (<= 2 (.-readyState a)))






;;; seek動作が不完全な端末の為の対応。
;;; どうしてもseekできないパターンもあり、その場合は0からのスタートとなる。
;;; (途中からの再生が必要になるのは、現状では
;;; backgroundからの復帰時のみで、
;;; その場合は曲の最初から再生し直しても大きな問題はない)
;;; seekが失敗した場合にstart-posが変更された事を示す為に、
;;; この関数は返り値として、実際に設定したstart-posを返す。
(defn- play-with-seek! [a start-pos]
  (let [done-pre-seek? (try
                         (set! (.-currentTime a) start-pos)
                         true
                         (catch :default e false))]
    (try
      (.play a)
      (catch :default e nil))
    ;; NB: seekに失敗している場合は再度挑戦する。
    ;;     また二度目のseekにも失敗した場合は、前述の通り、
    ;;     start-posが0だったものとして続行する。
    (if done-pre-seek?
      start-pos
      (try
        (set! (.-currentTime a) start-pos)
        start-pos
        (catch :default e 0)))))




;;; 頻出するaudio-classをここに保持しておく
(defonce audio-class (atom nil))
(defn new-audio [url]
  (let [ac @audio-class]
    (new ac url)))


(defonce locked-stack (atom nil))




;;; NB: この処理はfirefoxのended内で実行すると、内部で謎のundefined例外を
;;;     投げるようだ(stacktraceなし)。よく分からないので、ended内では
;;;     実行しないようにする事
(defn- reset-audio-instance! [a]
  (let [current-time (try
                       (.-currentTime a)
                       (catch :default e 0))
        volume (try
                 (.-volume a)
                 (catch :default e nil))]
    ;; NB: 各種プロパティ値およびハンドラは .load してもリセットされないようだ。
    ;;     .load するとリセットされる可能性のある状態は、具体的には以下。
    ;;     - .-currentTime
    ;;     - .-loop
    ;;     - .-volume
    ;;     - .-playbackRate (現在未使用)
    ;;     - その他、再生に関わるreadonlyなプロパティ値
    (.load a)
    (when volume
      (set! (.-volume a) volume))
    (when-not (zero? (or current-time 0))
      (try
        (set! (.-currentTime a) current-time)
        (catch :default e nil)))
    a))


;;; HtmlAudio対応が壊滅的なモバイル環境かどうか
(def ^:private unavailable-mobile?
  ;; androidもしくはiosである
  (when (or
          (:android util/terminal-type)
          (:ios util/terminal-type))
    ;; firefoxでもchromeでもない
    (and
      ;; TODO: モバイル版firefoxのHtmlAudioには、ループ再生時に
      ;;       一度でもループ部まで再生した音源を、一度停止させた後に、
      ;;       次から再生ができなくなるという問題がある。
      ;;       上手く対処できないので一時的にサポート対象外とする。
      ;;       問題があるのはこれのみなので、これさえ解決できれば
      ;;       またサポート対象に含めてもよい。
      ;;       (terminal-typeの判定で特別扱いしてもよいのだが…)
      ;(not (:firefox util/terminal-type))
      (not (:chrome util/terminal-type)))))

;;; タッチでのアンロックが必要なモバイル環境かどうか
(def ^:private need-unlock?
  (and
    (not unavailable-mobile?)
    (or
      (:mobile util/terminal-type)
      (:android util/terminal-type)
      (:ios util/terminal-type))))


(defn init!? []
  (p 'init!?)
  ;; NB: 古いandroidのデフォルトブラウザおよび、古いiosのsafariでの、
  ;;     HtmlAudio対応は壊滅的(複数同時再生不可、音量変更不可、ループ不可、等々)
  ;;     なので、完全に無効化する事にした。
  ;;     最近の端末はWebAudioが実装され、そっちは問題ないようなので、
  ;;     HtmlAudio利用＝古い端末という事で切り捨てる。
  ;;     なお、古い端末であってもfirefoxやchromeの場合、HtmlAudio対応は
  ;;     そこそこいけるようなので、これのみ特別扱いする事となった
  (when-not unavailable-mobile?
    (if @audio-class
      true
      (let [ac (or
                 (aget js/window "Audio")
                 (aget js/window "webkitAudio"))
            audio (when ac
                    (or
                      (try
                        (new ac)
                        (catch :default e
                          nil))
                      (try
                        (new ac "")
                        (catch :default e
                          nil))))]
        (when audio
          (reset! audio-class ac)
          ;; モバイル環境でのみ、タッチによるアンロックを行う
          (when need-unlock?
            (util/register-touch-unlock-fn!
              (fn []
                (doseq [as @locked-stack]
                  (try
                    (let [a (:audio as)
                          playing-info @(:playing-info as)]
                      ;; NB: アンロックは .load .play のどちらかを実行。
                      ;;     ただし、 .load するとAudioインスタンスの状態が
                      ;;     初期化されてしまうし、 .play すると再生が開始する。
                      ;;     そこで、以下の基準で処理を行う。
                      ;;     - 現在既に再生中なら .play する。
                      ;;       これは現在の再生状態に影響を与えない筈…
                      ;;     - 現在再生中でないなら .load した上で必要な
                      ;;       パラメータを再設定する。
                      ; (:start-pos playing-info)
                      ; (:begin-msec playing-info)
                      ; (js/Date.now)
                      (if (and playing-info (not (:end-msec playing-info)))
                        (.play a)
                        (reset-audio-instance! a)))
                    (catch :default e nil)))
                (reset! locked-stack nil)
                ;; NB: :html-audioでのアンロックはAudioインスタンス毎に
                ;;     行う必要がある為、常に実行し続ける必要がある
                false)))
          true)))))





;;; NB: 現状では、audio-source = audio-channel の扱い
(defn _load-audio-source! [url loaded-handle error-handle]
  ;; NB: よく分からないが、ieのみ、 .addEventListener で各種イベントを
  ;;     捕捉できない時があるので、イベントが捕捉できなくても
  ;;     ロード完了を検知する必要がある
  (let [a (new-audio url)
        h-loaded (atom nil)
        h-error (atom nil)
        h-ended (atom nil)
        as {:url url
            :audio a
            :error? (atom false)
            :loaded? (atom false)
            :playing-info (atom nil)
            :play-request (atom nil)
            :h-loaded h-loaded
            :h-error h-error
            :h-ended h-ended
            }]
    (reset! h-loaded (fn [e]
                       (when-not @(:loaded? as)
                         (reset! (:loaded? as) true)
                         (when need-unlock?
                           (swap! locked-stack conj as))
                         (doseq [k loaded-handle-keys]
                           (.removeEventListener a k @h-loaded))
                         (loaded-handle as))))
    (doseq [k loaded-handle-keys]
      (.addEventListener a k @h-loaded))
    (reset! h-error (fn [e]
                      (when-not @(:loaded? as)
                        (.removeEventListener a "error" @h-error)
                        (reset! (:loaded? as) true)
                        (reset! (:error? as) true)
                        (error-handle (str "cannot load url " url)))))
    (.addEventListener a "error" @h-error)
    (reset! h-ended (fn [e]
                      (when-let [pi @(:playing-info as)]
                        (if-not (:loop? pi)
                          (swap! (:playing-info as) assoc
                                 :end-msec (js/Date.now))
                          (when-not (:end-msec pi)
                            (when-not (or
                                        (:firefox util/terminal-type)
                                        (:chrome util/terminal-type))
                              (reset-audio-instance! a))
                            (play-with-seek! a 0))))))
    (.addEventListener a "ended" @h-ended)
    (set! (.-preload a) "auto")
    (set! (.-autoplay a) false)
    (set! (.-muted a) false)
    (set! (.-controls a) false)
    (.load a)
    (go-loop [elapsed-sec 0]
      (<! (async/timeout 1000))
      (when-not @(:loaded? as)
        (if (ready? a)
          (@h-loaded nil)
          (do
            (if (< error-timeout-sec elapsed-sec)
              (do
                (reset! (:loaded? as) true)
                (reset! (:error? as) true)
                (error-handle (str "timeout to load url " url)))
              (recur (inc elapsed-sec)))))))
    as))

(defn load-audio-source! [url loaded-handle error-handle]
  (p 'load-audio-source! url)
  (_load-audio-source! url loaded-handle error-handle))




(defn dispose-audio-source! [audio-source]
  (p 'dispose-audio-source! (:url audio-source))
  (when-let [a (:audio audio-source)]
    ;; NB: _load-audio-source! で設定しているハンドルが環状参照になっている為、
    ;;     それを壊す必要がある
    (let [h-loaded @(:h-loaded audio-source)
          h-error @(:h-error audio-source)
          h-ended @(:h-ended audio-source)]
      (doseq [k loaded-handle-keys]
        (.removeEventListener a k h-loaded))
      (.removeEventListener a "error" h-error)
      (.removeEventListener a "ended" h-ended))
    ;; モバイル環境では、明示的な解放を行う
    ;; (PC向けだとコンソールにエラーが出るので避ける)
    (when need-unlock?
      (try
        (set! (.-src a) "")
        (catch :default e nil))
      (try
        (.load a)
        (catch :default e nil)))))




(defn spawn-audio-channel [audio-source]
  (p 'spawn-audio-channel (:url audio-source))
  (atom (merge audio-source
               {:type :audio-channel
                :audio-source audio-source
                })))






(defn playing? [ch]
  (p 'playing? (:url @ch))
  (when-let [playing-info @(:playing-info @ch)]
    (not (:end-msec playing-info))))



(defn preparing? [ch]
  (when (playing? ch)
    (when-let [a (:audio @ch)]
      (try
        ;; 再生ポジションが0なら準備中扱い
        (zero? (.-currentTime a))
        ;; currentTimeの取得に失敗する場合は準備中扱い
        (catch :default e true)))))



(defn pos [ch]
  (p 'pos (:url @ch))
  (or
    (try
      (when (playing? ch)
        (.-currentTime (:audio @ch)))
      (catch :default e nil))
    (let [playing-info @(:playing-info @ch)
          offset-msec (* (:start-pos playing-info) 1000)
          begin-msec (:begin-msec playing-info)
          end-msec (or (:end-msec playing-info) (js/Date.now))]
      (max 0 (/ (+ offset-msec (- end-msec begin-msec)) 1000)))))


(defn- _set-pitch! [audio pitch]
  ;; 試してみたが、ブラウザ側の実装が悪いようで、音程が変化せずに
  ;; 再生が途切れたりするようになるだけなので、無効化する事にした
  ;(try
  ;  (set! (.-playbackRate audio pitch)
  ;  (catch :default e nil))
  nil)




(defn play! [ch start-pos loop? volume pitch pan alarm?]
  (p 'play! (:url @ch) start-pos loop? volume pitch pan alarm?)
  (when-not @(:error? @ch)
    ;; NB: html-audio-multiからの再生時に、
    ;;     実際のロード生成が遅延するケースがある。
    ;;     これにきちんと対応できなくてはならない。
    ;;     (以下の .-readyState が4以外だった時の処理)
    (let [a (:audio @ch)]
      ;; 即座に再生するか、再生予約を入れるだけか
      (if (ready? a)
        (do
          ;(.pause a)
          (_set-pitch! a pitch)
          ;; NB: seekよりも先にplayの実行が必要となる環境があるらしい。
          ;;     しかしそうでない環境ではseekを優先したい
          ;;     (seek前の音が一瞬鳴ってしまう為)。
          ;;     そこで (try (seek)) → (play) → (try (seek)) という
          ;;     順番で実行を行う事にする。
          ;;     ただしこの場合、環境によってはseek直前の音が
          ;;     一瞬聴こえてしまう問題がある。ので、初回はvolume=0にしておき、
          ;;     seekが上手くいってから改めてボリュームを再設定する
          (set! (.-volume a) 0)
          (let [start-pos (if (and start-pos (pos? start-pos)) start-pos 0)
                start-pos (play-with-seek! a start-pos)]
            (set! (.-volume a) volume)
            (reset! (:playing-info @ch) {:start-pos start-pos
                                         :begin-msec (js/Date.now)
                                         :end-msec nil
                                         :loop? loop?
                                         }))
          ;; 非ループ時は、再生終了時に状態を変更するgoスレッドを起動する
          ;; (endedイベント非対応の環境がある為)
          (when-not loop?
            (go-loop []
              (<! (async/timeout 888))
              ;; dispose-audio-channel!されたら終了
              (when-let [playing-info @(:playing-info @ch)]
                ;; stop!されたら終了
                (when-not (:end-msec playing-info)
                  ;; endedが真値になってたら終了
                  (if (.-ended (:audio @ch))
                    (swap! (:playing-info @ch) assoc :end-msec (js/Date.now))
                    (recur)))))))
        (do
          (reset! (:play-request @ch)
                  [start-pos loop? volume pitch pan alarm?])
          (go-loop []
            (<! (async/timeout 1))
            (when-let [play-request @(:play-request @ch)]
              (if (ready? a)
                (do
                  (reset! (:play-request @ch) nil)
                  (apply play! ch play-request))
                (recur)))))))))



(defn stop! [ch]
  (p 'stop! (:url @ch))
  (when-let [a (:audio @ch)]
    (try
      (.pause a)
      (catch :default e nil)))
  ;; NB: :play-request のキャンセルが必須
  (reset! (:play-request @ch) nil)
  (swap! (:playing-info @ch) assoc :end-msec (js/Date.now)))

(defn set-volume! [ch volume]
  (p 'set-volume! (:url @ch) volume)
  (when-let [a (:audio @ch)]
    (set! (.-volume a) volume)))

(defn set-pitch! [ch pitch]
  (p 'set-pitch! (:url @ch) pitch)
  (when-let [a (:audio @ch)]
    (_set-pitch! a pitch))
  nil)

(defn set-pan! [ch pan]
  (p 'set-pan! (:url @ch) pan)
  ;; NB: :html-audio は pan 非対応
  nil)

(defn dispose-audio-channel! [ch]
  (p 'dispose-audio-channel! (:url @ch))
  (reset! (:playing-info @ch) nil)
  nil)






(entry-table/register!
  :html-audio-single
  {:init!? init!?
   :load-audio-source! load-audio-source!
   :dispose-audio-source! dispose-audio-source!
   :spawn-audio-channel spawn-audio-channel
   :pos pos
   :play! play!
   :playing? playing?
   :preparing? preparing?
   :stop! stop!
   :set-volume! set-volume!
   :set-pitch! set-pitch!
   :set-pan! set-pan!
   :dispose-audio-channel! dispose-audio-channel!
   :name (constantly "html-audio-single")
   })


