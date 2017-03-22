(def use-advanced-optimizations? true)

(def compiler-option-common
  {:output-to "resources/public/cljs/cl.js"
   :language-in :ecmascript5
   :language-out :ecmascript5
   :optimizations (if use-advanced-optimizations? :advanced :simple)
   :pretty-print false})

(def compiler-option-js
  (merge compiler-option-common
         {:main 'vnctst.audio4.js
          :output-dir "target/out-js" ; unused
          :output-to "for-npm/vnctst-audio4.js"}))

(def compiler-option-prod
  (merge compiler-option-common
         {:main 'vnctst.audio4.demo
          :output-dir "target/out-prod" ; unused
          }))

(def compiler-option-dev
  (merge compiler-option-common
         {:main 'vnctst.audio4.demo-dev
          :output-dir "resources/public/cljs/out"
          :asset-path "cljs/out"
          :optimizations :none
          :source-map true
          :source-map-timestamp true
          :cache-analysis true
          :pretty-print true
          }))

(def figwheel-version "0.5.9")

(def plugins-cljs-prod '[[lein-cljsbuild "1.1.5"]])

(def plugins-cljs-dev (vec (concat plugins-cljs-prod
                                   [['lein-figwheel figwheel-version]])))

(def dependencies-cljs-prod
  '[[org.clojure/clojure "1.8.0"]
    [org.clojure/clojurescript "1.9.456"]
    [jp.ne.tir/project-clj "0.1.6"]])

(def dependencies-cljs-dev
  (vec (concat dependencies-cljs-prod
               [['figwheel figwheel-version]])))


(defproject jp.ne.tir/vnctst-audio4 "0.1.6"
  :min-lein-version "2.5.0"
  :description "Audio playback library for html5 game"
  :url "https://github.com/ayamada/vnctst-audio4"
  :license {:name "Zlib License"
            :url "https://opensource.org/licenses/Zlib"}
  :dependencies [[org.clojure/core.async "0.3.441"]]
  :source-paths ["src/cljs"]
  :clean-targets ^{:protect false} [:target-path
                                    :compile-path
                                    "resources/public/cljs"
                                    "figwheel_server.log"]
  ;; NB: リリース時には resources/ 配下は明示的に含めないようにする
  ;;     (開発時に使うものを入れており、リリース時には全く不要な為)
  :resource-paths []
  :jar-exclusions []
  ;; 基本的に、以下の場合分けになる
  ;; - 開発用(サンプルデモ使用)
  ;;   - ringサーバ起動
  ;;     - lein with-profile ring ring server-headless
  ;;   - figwheelサーバ起動
  ;;     - rlwrap lein with-profile +demo-dev figwheel demo-dev
  ;; - リリース用
  ;;   - clojars登録用(profileなし状態)
  ;;     - lein deploy clojars
  ;;   - vnctst-audio4.js のリリース版ビルド(ビルド後にコミットする想定)
  ;;     - lein clean && lein with-profile for-js cljsbuild once for-js
  ;;   - サンプルデモのリリースビルド
  ;;     - lein clean && lein with-profile demo-prod cljsbuild once demo-prod
  :profiles {:ring {:source-paths ["src/ring"]
                    :dependencies [[org.clojure/clojure "1.8.0"]
                                   [ring/ring-core "1.5.1"]
                                   [jp.ne.tir/project-clj "0.1.6"]
                                   [hiccup "1.0.5"]]
                    :resource-paths ["resources"]
                    :plugins [[lein-ring "0.11.0"]]
                    :ring {:port 8004
                           :handler vnctst.audio4.demo.server/handler}}
             :for-js {:dependencies ~dependencies-cljs-prod
                      :plugins ~plugins-cljs-prod}
             :demo-dev {:dependencies ~dependencies-cljs-dev
                        :plugins ~plugins-cljs-dev
                        :resource-paths ["resources"]}
             :demo-prod {:dependencies ~dependencies-cljs-prod
                         :plugins ~plugins-cljs-prod
                         :resource-paths ["resources"]}}
  :cljsbuild {:builds {:for-js {:compiler ~compiler-option-js
                                :source-paths ["src/cljs" "src/for-js"]
                                :jar true}
                       :demo-dev {:compiler ~compiler-option-dev
                                  :source-paths ["src/cljs"
                                                 "src/for-js"
                                                 "src/demo"
                                                 "src/demo-dev"]
                                  :jar false}
                       :demo-prod {:compiler ~compiler-option-prod
                                   :source-paths ["src/cljs"
                                                  "src/for-js"
                                                  "src/demo"]
                                   :jar true}
                       }}
  :figwheel {:http-server-root "public"
             :server-ip "0.0.0.0"
             :server-port 9004
             :server-logfile "figwheel_server.log"}
  )
