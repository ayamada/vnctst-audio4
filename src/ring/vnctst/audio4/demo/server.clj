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
               "button {font-family:monospace; padding:0.5em; margin:0.2em}\n"
               "code {padding:0.5em; margin:0.5em}"]
              ]
             [:body
              {:onload "vnctst.audio4.demo.bootstrap()"}
              [:div#github-ribbon
               [:a {:href github-url
                    :target "_blank"}
                [:img {:style "position: absolute; top: 0; right: 0; border: 0;"
                       :src "https://camo.githubusercontent.com/365986a132ccd6a44c23a9169022c0b5c890c387/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f7265645f6161303030302e706e67"
                       :alt "Fork me on GitHub"
                       :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_red_aa0000.png"}]]]
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
               ;; cljs/js切り替えボタン
               [:div
                [:button {:onclick "vnctst.audio4.demo.jsmode(false)"}
                 "cljs向けの表示にする"]
                [:br]
                [:button {:onclick "vnctst.audio4.demo.jsmode(true)"}
                 "js向けの表示にする"]
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
                           "サンプルボタンを参考にjs式を入力してください。")]
                 [:li (str "このデモでは、ページのロード時に debug? フラグを"
                           "有効化している為、"
                           "再生や停止等の操作を行うとデバッグコンソールに"
                           "ログが表示されます。これを確認したい場合も"
                           "デバッグコンソールを開いておくとよいでしょう。")]
                 ]]
               [:hr]
               [:div
                [:h2 "最もシンプルな使い方"]
                [:h3 "BGMを鳴らす"]
                (demo-button2 :bgm-va32)
                (demo-button2 :bgm-rnr)
                (demo-button2 :bgm-oneshot-ny2017)
                [:h3 "BGMを止める"]
                (demo-button2 :stop-bgm)
                (demo-button2 :stop-bgm-3)
                (demo-button2 :stop-bgm-0)
                [:h3 "SEを鳴らす"]
                (demo-button2 :se-shootout)
                (demo-button2 :se-launch)
                ]
               [:hr]
               [:div
                [:h2 ""]
                [:h3 ""]
                ]
               [:hr]
               ;; SE
               ;[:div
               ; "SE :"
               ; [:br]
               ; "- SEは多数の音源を多重に並列再生できる。"
               ; "また、BGM/ME/BGSの状態に干渉しない(同時に再生できる)"
               ; [:br]
               ; [:br]
               ; (demo-button :play-se-jump)
               ; [:br]
               ; (demo-button :play-se-yarare)
               ; [:br]
               ; [:br]
               ; (demo-button :play-se-yarare-ogg)
               ; ]
               ;[:hr]
               ;; BGM
               [:div
                "BGM :"
                [:br]
                "- BGMの再生"
                [:br]
                [:br]
                ;(demo-button :bgm-va32)
                ;[:br]
                ;(demo-button :bgm-rnr)
                ;[:br]
                ;[:br]
                ;(demo-button :stop-bgm)
                ;[:br]
                ;(demo-button :stop-bgm-0)
                ;[:br]
                ;[:br]
                ;(demo-button :play-bgm-va3)
                ;[:br]
                ;(demo-button :play-bgm-drop)
                ;[:br]
                ;(demo-button :play-bgm-drop-2)
                ;[:br]
                ;[:br]
                ;(demo-button :play-me-unmei)
                ;[:br]
                ;(demo-button :play-me-unmei-2)
                ;[:br]
                ;[:br]
                ;(demo-button :play-bgm-drop-ogg)
                ;[:br]
                ;(demo-button :play-bgm-drop-mp3)
                ;[:br]
                ;(demo-button :play-me-unmei-ogg)
                ;[:br]
                ;[:br]
                ;(demo-button :play-bgm-nil)
                ;[:br]
                ;(demo-button :play-me-nil)
                ;[:br]
                ;[:br]
                ;(demo-button :stop-bgs)
                ;[:br]
                ;(demo-button :stop-bgs-0)
                ;[:br]
                ;[:br]
                ;(demo-button :play-bgs-noise)
                ;[:br]
                ;[:br]
                ;(demo-button :play-bgs-nil)
                ]
               [:hr]
               ;; configure
               [:div
                "Configure :"
                [:br]
                "- 全体に影響する設定項目の値の取得および変更"
                [:br]
                [:br]
                (demo-button :config-volume-master)
                [:br]
                [:br]
                (demo-button :set-config-volume-master-25)
                [:br]
                (demo-button :set-config-volume-master-50)
                [:br]
                (demo-button :set-config-volume-master-100)
                [:br]
                [:br]
                (demo-button :set-config-volume-bgm-100)
                [:br]
                (demo-button :set-config-volume-se-100)
                [:br]
                (demo-button :set-config-debug?-true)
                [:br]
                (demo-button :set-config-se-chattering-sec-0)
                [:br]
                (demo-button :set-config-default-bgm-fade-sec-2)
                [:br]
                (demo-button :set-config-default-se-fade-sec-1)
                [:br]
                (demo-button :set-config-autoext-list)
                [:br]
                (demo-button :set-config-dont-stop-on-background?-true)
                [:br]
                (demo-button :set-config-disable-mobile?-true)
                [:br]
                (demo-button :set-config-disable-webaudio?-true)
                [:br]
                (demo-button :set-config-disable-htmlaudio?-true)
                ]
               [:hr]
               ;; misc
               [:div
                "Misc :"
                [:br]
                "- その他の補助的な機能"
                [:br]
                [:br]
                (demo-button :can-play-ogg)
                [:br]
                (demo-button :can-play-mp3)
                [:br]
                (demo-button :can-play-m4a)
                [:br]
                (demo-button :can-play)
                [:br]
                [:br]
                (demo-button :terminal-type)
                [:br]
                (demo-button :float->percent)
                [:br]
                (demo-button :percent->float)
                ]
               ;; footer
               [:hr]
               address
               ]
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



