(ns vnctst.audio4.demo.server
  (:require [ring.middleware.resource :as resources]
            [ring.util.response :as response]
            [clojure.string :as string]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            ))

(defn- prevent-cache [path]
  (str path "?" (.getTime (java.util.Date.))))

(def error-404
  {:status 404
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body (page/html5 {} [:body [:h1 "404 NOT FOUND"]])})


(defn- error-text [code text]
  {:status code
   :headers {"Content-Type" "text/plain; charset=UTF-8"
             "Pragma" "no-cache"
             "Cache-Control" "no-cache"
             }
   :body text})


(def ^:private title "vnctst-audio4 demo")
(def ^:private bg-color "#BFBFBF")
(def ^:private pressed-bg-color "#BFBFBF")

(defn- demo-button [id]
  (let [id (name id)
        desc-id (str (name id) "-desc")]
    [:span [:button {:id id} " "] " : " [:span {:id desc-id} " "]]))

(defn- demo-button2 [id]
  (let [id (name id)
        desc-id (str (name id) "-desc")]
    [:dl
     [:dt [:button {:id id} " "]]
     [:dd [:span {:id desc-id} " "]]]))

(defn render-app [req]
  (let [github-url "https://github.com/ayamada/vnctst-audio4"
        link-home (fn [label]
                    [:a {:href github-url
                       :target "_blank"}
                     label])
        address [:p (link-home "(vnctst-audio4 github repos)")]]
    {:status 200
     :headers {"Content-Type" "text/html; charset=UTF-8"
               "Pragma" "no-cache"
               "Cache-Control" "no-cache"
               }
     :body (page/html5
             [:head
              [:meta {:http-equiv "X-UA-Compatible", :content "IE=edge"}]
              [:meta {:charset "UTF-8"}]
              ;[:meta {:name "viewport", :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
              ;[:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
              [:meta {:http-equiv "Pragma", :content "no-cache"}]
              [:meta {:http-equiv "Cache-Control", :content "no-cache"}]
              [:title title]
              ;[:link {:href "css/reset.css", :rel "stylesheet", :type "text/css"}]
              [:link {:href (prevent-cache "css/default.css"), :rel "stylesheet", :type "text/css"}]
              [:style {:type "text/css"}
               "button {font-family:monospace; font-size: 1.2em; padding:0.1em; margin:0.2em}\n"
               "code {padding:0.5em; margin:0.5em}"]
              ]
             [:body
              {:onload "vnctst.audio4.demo.bootstrap()"}
               ;; cljs/js切り替えボタン
               [:div#floating-header
                [:button {:onclick "vnctst.audio4.demo.jsmode(false)"}
                 "cljs表記"]
                [:button {:onclick "vnctst.audio4.demo.jsmode(true)"}
                 "js表記"]
                ]
              [:div#github-ribbon
               [:a {:href github-url
                    :target "_blank"}
                [:img {:style "position: absolute; top: 0; right: 0; border: 0; z-index: 10000"
                       :src "https://camo.githubusercontent.com/365986a132ccd6a44c23a9169022c0b5c890c387/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f7265645f6161303030302e706e67"
                       :alt "Fork me on GitHub"
                       :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_red_aa0000.png"}]]]
              [:div#main-content
               [:h1 title]
               address
               [:div#message "Loading ..."]
               [:div#main {:style "display: none"}
                [:div#version "Version:"]
                [:hr]
                ;; init実行時の情報表示
                [:div
                 [:span "以下の設定を実行しました："]
                 [:br]
                 [:code#config-info "(config-info)"]
                 [:br]
                 [:br]
                 [:span "以下の音源ファイルの事前ロードを実行しました："]
                 [:br]
                 [:code#preload-info "(preload-info)"]
                 ]
                [:hr]
                [:div
                 [:h2 "前書き"]
                 [:ul
                  [:li (str "これは、ゲーム向けの音響ファイル再生ライブラリ"
                            "である「vnctst-audio4」のオンラインデモです。")]
                  [:li
                   "vnctst-audio4についての詳細は、"
                   (link-home "vnctst-audio4のgithubリポジトリ")
                   "を参照してください。"]
                  [:li (str "vnctst-audio4には「cljs版」と「js版」があります。"
                            "サンプルコードの表記を変更したい場合は上の方にある"
                            "ボタンを押してください。")]
                  [:li (str "サンプルボタンに書いてある以外の操作も可能です。"
                            "ブラウザのデバッグコンソールを開き、"
                            "サンプルボタンの表記を参考に"
                            "js式を入力してください。")]
                  [:li (str "このデモでは、ページのロード時に debug? フラグを"
                            "有効化している為、"
                            "再生や停止等の操作を行うとデバッグコンソールに"
                            "ログが表示されます。これを確認したい場合も"
                            "デバッグコンソールを開いておくとよいでしょう。")]
                  ]]
                [:hr]
                [:div
                 [:h2 "基本操作"]
                 [:p
                  "このセクションで紹介している機能だけでも"
                  "大体なんとかなります"]
                 [:h3 "BGMを鳴らす"]
                 (demo-button2 :bgm-va32)
                 (demo-button2 :bgm-rnr)
                 (demo-button2 :bgm-oneshot-ny2017)
                 [:h3 "BGMを止める"]
                 (demo-button2 :stop-bgm)
                 (demo-button2 :stop-bgm-3)
                 (demo-button2 :stop-bgm-0)
                 [:h3 "SEを鳴らす"]
                 (demo-button2 :se-launch)
                 (demo-button2 :se-buy1)
                 (demo-button2 :se-open-wood)
                 (demo-button2 :stop-se)
                 (demo-button2 :stop-se-05)
                 ]
                [:hr]
                [:div
                 [:h2 "設定項目"]
                 [:p
                  "項目は多いですが、実際にいじる必要があるのは"
                  "音量設定とデバッグ出力ぐらいです"]
                 [:h3 "現在の設定項目の値を取得する"]
                 (demo-button2 :config-volume-master)
                 [:h3 "音量設定"]
                 (demo-button2 :set-config-volume-master-100)
                 (demo-button2 :set-config-volume-master-25)
                 (demo-button2 :set-config-volume-bgm-100)
                 (demo-button2 :set-config-volume-bgm-25)
                 (demo-button2 :set-config-volume-se-100)
                 (demo-button2 :set-config-volume-se-25)
                 [:h3 "デバッグ出力"]
                 (demo-button2 :set-config-debug?-false)
                 (demo-button2 :set-config-debug?-true)
                 (demo-button2 :set-config-debug-verbose?-false)
                 (demo-button2 :set-config-debug-verbose?-true)
                 [:h3 "あまり使われない項目"]
                 [:p "ここは読み飛ばしても問題ありません"]
                 (demo-button2 :set-config-default-bgm-fade-sec-0)
                 (demo-button2 :set-config-default-bgm-fade-sec-05)
                 (demo-button2 :set-config-default-se-fade-sec-0)
                 (demo-button2 :set-config-default-se-fade-sec-05)
                 (demo-button2 :set-config-dont-stop-on-background?-false)
                 (demo-button2 :set-config-dont-stop-on-background?-true)
                 (demo-button2 :set-config-se-chattering-sec-0)
                 (demo-button2 :set-config-se-chattering-sec-05)
                 (demo-button2 :set-config-autoext-list-a)
                 (demo-button2 :set-config-autoext-list-b)
                 (demo-button2 :set-config-disable-mobile?-false)
                 (demo-button2 :set-config-disable-mobile?-true)
                 (demo-button2 :set-config-disable-webaudio?-false)
                 (demo-button2 :set-config-disable-webaudio?-true)
                 (demo-button2 :set-config-disable-htmlaudio?-false)
                 (demo-button2 :set-config-disable-htmlaudio?-true)
                 ]
                [:hr]
                [:div
                 [:h2 "やや複雑な操作"]
                 [:h3 "プリロード / アンロード"]
                 (demo-button2 :load-noise)
                 (demo-button2 :loaded?)
                 (demo-button2 :error?)
                 (demo-button2 :unload-noise)
                 (demo-button2 :unload-all)
                 [:h3 "BGMの再生オプション"]
                 (demo-button2 :bgm-option-a)
                 (demo-button2 :bgm-option-b)
                 (demo-button2 :bgm-option-c)
                 (demo-button2 :bgm-noise-ch)
                 (demo-button2 :stop-bgm-ch-a)
                 (demo-button2 :stop-bgm-ch-b)
                 ;(demo-button2 :bgs-noise)
                 ;(demo-button2 :me-rnr)
                 [:h3 "SEの再生オプション"]
                 (demo-button2 :se-option-a)
                 (demo-button2 :se-option-b)
                 (demo-button2 :se-option-c)
                 (demo-button2 :stop-se-ch)
                 (demo-button2 :alarm-buy1)
                 ]
                [:hr]
                [:div
                 [:h2 "その他の補助的な機能"]
                 (demo-button2 :version-js)
                 (demo-button2 :can-play-ogg)
                 (demo-button2 :can-play-mp3)
                 (demo-button2 :can-play-m4a)
                 (demo-button2 :can-play)
                 (demo-button2 :terminal-type)
                 (demo-button2 :float->percent)
                 (demo-button2 :percent->float)
                 ]
                ;; footer
                [:hr]
                address
                ]]
              [:script {:src (prevent-cache "cljs/cl.js")
                        :type "text/javascript"} ""]])}))


(defn- app-handler [req]
  (let [uri (:uri req)]
    (case uri
      "/" (render-app req)
      ;"/hoge" (hoge! req)
      error-404)))









(def content-type-table
  {"html" "text/html; charset=UTF-8"
   "txt" "text/plain; charset=UTF-8"
   "css" "text/css"
   "js" "text/javascript"
   "png" "image/png"
   "jpg" "image/jpeg"
   "ico" "image/x-icon"
   "woff" "application/font-woff"
   "ttf" "application/octet-stream"
   "ttc" "application/octet-stream"
   "ogg" "audio/ogg"
   "mp3" "audio/mpeg"
   "aac" "audio/aac"
   "m4a" "audio/mp4"
   ;; TODO: add more types
   })

;;; IE must needs content-type for css files !!!
(defn- fix-content-type [req res]
  (if (get-in res [:headers "Content-Type"])
    res
    (let [filename (:uri req)
          [_ ext] (re-find #"\.(\w+)$" filename)
          content-type (content-type-table (string/lower-case (or ext "")))]
      ;(println (pr-str :DEBUG filename ext content-type))
      (if content-type
        (response/content-type res content-type)
        res))))

(def handler
  (let [h (resources/wrap-resource app-handler "public")]
    (fn [req]
      (let [res (h req)
            res (response/header res "Cache-Control" "no-cache")
            res (response/header res "Pragma" "no-cache")
            ]
        (fix-content-type req res)))))



