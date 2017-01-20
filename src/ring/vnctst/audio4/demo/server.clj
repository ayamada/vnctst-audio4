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





(def desc-table (atom {}))

(defn- defdesc [k & bodies]
  (swap! desc-table assoc k bodies)
  nil)


(defn- p [& args]
  (apply vector :div args))

(defn- a [url & [label]]
  [:a {:href url :target "_blank"} (or label url)])

(defn- file [filename]
  [:code "\"" filename "\""])

(defn- expand-autoext-html [path]
  (let [[_ basename] (re-find #"(.*)\.[^.]*$" path)
         basename (or basename path)]
    [:span
     [:code "\"" basename ".ogg\""]
     "もしくは"
     [:code "\"" basename ".mp3\""]
     "もしくは"
     [:code "\"" basename ".m4a\""]
     ]))





(defdesc :bgm-va32
  (p (expand-autoext-html "bgm/va32.*")
     "をループBGMとして再生する。")
  (p "この引数は" (file "bgm/va32.ogg") "のように"
     "拡張子を普通に付けて指定してもよいのだが、"
     "そうすると当然、"
     "ogg再生のできないブラウザでは音が出ない事になる。")
  (p "そこで" (file "bgm/va32.*") "形式で指定する事で、"
     "oggが再生可能ならoggを、そうでなければmp3やm4aを"
     "再生する事ができる。"
     "ただし、もちろん各拡張子のファイルを"
     "予め設置しておく必要がある"
     "(ogg/mp3/m4aの三種類全てを設置する必要はなく、"
     "mp3かm4aのどちらかは省略できる。"
     "当ライブラリではm4aを省略してoggとmp3の二つだけ設置しておくのを推奨)。")
  (p "引数は" (file "http://...") "のようなurlも指定可能だが、"
     "その場合は"
     (a "https://www.google.com/search?nfpr=1&q=CORS" "CORS")
     "の設定が必要になる場合がある事に注意。")
  (p "「指定した拡張子の再生をブラウザがサポートしていない」"
     "「ファイルが存在しない」等の理由で再生できなかった場合は"
     "何も行われない(例外も投げられない。ただし後述の"
     [:code "debug?"]
     "フラグが有効ならコンソールにエラーメッセージが出力される)。")
  )

(defdesc :bgm-cntr
  (p (expand-autoext-html "bgm/cntr.*")
     "をループBGMとして再生する。")
  (p "もし既に別のBGMが再生中の場合は、そのBGMの"
     "フェードアウトを開始し、フェードアウトが"
     "完了してから再生が開始される。")
  (p "この再生/停止回りは雑に操作しても適切に"
     "フェードアウト/フェードイン処理が行われるので、"
     "この辺りの再生/停止ボタンを素早く押しまくっても問題は出ない。"
     "実運用時も雑に扱ってよい。")
  )

(defdesc :bgm-oneshot-ny2017
  (p (expand-autoext-html "bgm/ny2017.*")
     "を非ループBGMとして再生する。")
  (p "ループしない点以外は前述のBGM再生と同じ。")
  )

(defdesc :bgm-fadein-cntr
  (p (expand-autoext-html "bgm/cntr.*")
     "をループBGMとして再生するが、再生開始時に"
     "フェードインがかかるようにする。")
  (p "フェードイン開始する事以外は前述のBGM再生と同じ。")
  )

(defdesc :stop-bgm
  (p "現在再生中のBGMをデフォルト秒数(1秒)かけてフェード終了させる。")
  (p "再生中でない場合は何も起きない。")
  (p "この「デフォルト秒数」は後述の「設定項目」から変更可能。")
  )

(defdesc :stop-bgm-3
  (p "現在再生中のBGMを3秒かけてフェード終了させる。"))

(defdesc :stop-bgm-0
  (p "現在再生中のBGMを即座に停止させる。"))

(defdesc :se-launch
  (p (expand-autoext-html "se/launch.*")
     "をSEとして再生する。")
  (p "SEとしての再生では、音源の多重再生が可能となる"
     "(ボタンを連打しても前の音が途切れたりしない)。")
  )

(defdesc :se-kick
  (p (expand-autoext-html "se/kick.*")
     "をSEとして再生する。")
  )

(defdesc :stop-se
  (p "現在再生中のSE全てをデフォルト秒数(0秒)かけて"
     "フェード終了させる(0秒の時は即座に終了する)。")
  (p "再生中でない場合は何も起きない。")
  (p "この「デフォルト秒数」は後述の「設定項目」から変更可能。")
  )

(defdesc :stop-se-05
  (p "現在再生中のSE全てを0.5秒かけてフェード終了させる。"))

(defdesc :config-volume-master
  (p "各種の設定値を取得する。")
  (p "このボタンで指定している" [:code "volume-master"]
     "は「マスター音量」の現在値"
     "(詳細については次の「音量設定」の項目を参照)。")
  (p "引数を変更する事で様々な設定値の取得が行えるが、"
     "項目数が多いので以下ではボタン化を省略している。"
     "確認したい場合は実際にコンソールから実行してみるとよい。")
  )

(defdesc :set-config-volume-master-100
  )

(defdesc :set-config-volume-master-25
  (p "マスター音量を設定する"
     "(音量値は0.0～1.0の範囲、初期値は0.5)。")
  (p "マスター音量はBGMとSEの両方に影響する。")
  )

(defdesc :set-config-volume-bgm-100
  )

(defdesc :set-config-volume-bgm-25
  (p "BGM音量を設定する"
     "(音量値は0.0～1.0の範囲、初期値は0.5)。")
  (p "実際のBGMの再生音量は、この項目とマスター音量から決定される。")
  (p "初期状態ではマスター音量0.5(50%)かつBGM音量0.5(50%)なので、"
     "実際のBGMの再生音量は0.25(25%)相当となる。")
  (p "このデフォルト音量では小さすぎると思うなら、"
     "もっと大き目の値を設定するとよい。")
  )

(defdesc :set-config-volume-se-100
  )

(defdesc :set-config-volume-se-25
  (p "SE音量を設定する"
     "(音量値は0.0～1.0の範囲、初期値は0.5)。")
  (p "詳細は上のBGM音量の解説文と大体同じ。")
  )

(defdesc :set-config-debug?-false
  )

(defdesc :set-config-debug?-true
  (p "デバッグログをコンソールへ出力したい場合はtrueを設定する"
     "(初期値はfalse、ただしこのデモでは最初からtrueにしてある)。")
  (p "このvnctst-audio4では「雑に扱っても問題が起こらない」事を"
     "方針としているので、ファイルのロードに失敗したりしていても"
     "再生時に例外は投げられない。単に何も再生されないだけとなる。"
     "しかしこれでは開発時に不便な為、この設定をtrueにする事で、"
     "エラー等が起こった際に、その内容をコンソールへと"
     "出力するようにできる。")
  )

(defdesc :set-config-debug-verbose?-false
  )

(defdesc :set-config-debug-verbose?-true
  (p "些細なデバッグログもコンソールへ出力したい場合はtrueを設定する"
     "(初期値はfalse、ただしこのデモでは最初からtrueにしてある)。")
  (p "この設定は前述の" [:code "debug?"] "が有効な時にしか意味を持たない。")
  (p "これを有効にする事で、前述のエラー以外に、"
     "「このBGMの再生が開始された」「このSEの再生が停止された」"
     "といった、些細な情報までコンソールに出力されるようになる。"
     "しかしほとんどの場合は邪魔にしかならないので、"
     "開発時であっても普段はfalseにしておき、"
     "再生/停止タイミング等をきちんと調べたい時のみtrueにするとよい。")
  )

(defdesc :set-config-default-bgm-fade-sec-0
  )

(defdesc :set-config-default-bgm-fade-sec-05
  (p "デフォルトのBGMフェード秒数を設定する(初期値は1)。"
     "0を設定するとフェードなしで即座に停止するようになる。")
  )

(defdesc :set-config-default-se-fade-sec-0
  )

(defdesc :set-config-default-se-fade-sec-05
  (p "デフォルトのSEフェード秒数を設定する(初期値は0)。"
     "0を設定するとフェードなしで即座に停止するようになる。")
  )

(defdesc :set-config-dont-stop-on-background?-false
  )

(defdesc :set-config-dont-stop-on-background?-true
  (p "vnctst-audio4は、ブラウザのタブをバックグラウンドにした際に"
     "BGMを自動的に一時停止する機能を持っている"
     "(非対応ブラウザあり。また既に再生中のSEは停止されず、"
     "新たに再生されようとしたSEのみ無音化される)。")
  (p "この項目にtrueを設定する事で、その機能を無効化できる"
     "(初期値はfalse)。")
  )

(defdesc :set-config-se-chattering-sec-0
  )

(defdesc :set-config-se-chattering-sec-05
  (p "同一SE連打防止機能の閾値(秒)を設定する(初期値は0.05)。")
  (p "0を設定すると無効化できる。")
  (p "ゲームでは同じSEが特定タイミングで複数同時に発生する事が"
     "よくあるが、"
     "何も考えずにこれを行うと音が重なって音量の増幅が起こり、"
     "爆音や音割れの原因となってしまう"
     "(例えば、「艦これ」での爆撃シーンや雷撃シーンなどで顕著)。"
     "vnctst-audio4ではこの問題を防ぐ為に、"
     "この設定秒数以内での同一SEの再生は"
     "一つだけになるように内部で制限している。")
  )

(defdesc :set-config-autoext-list-a
  )

(defdesc :set-config-autoext-list-b
  (p "「" (file "filename.*") "」指定による拡張子自動選択機能(autoext)の"
     "拡張子の候補リストを設定する。")
  (p "autoext指定した音源ファイルのロード時には、"
     "このリストの順でトライされる"
     "(ブラウザが対応していない拡張子はスキップされる)。")
  (p "なお ogg, mp3, m4a 以外の拡張子を指定する際には、"
     "上記のwavのように、"
     "一緒にmime-typeも指定する必要があるので注意。")
  (p "初期値は" [:code (pr-str ["ogg" "mp3" "m4a"])] "。")
  (p "この値を変更した場合は内部状態をリセットする必要がある為、"
     "全ての再生中音源は停止され、"
     "また全てのロード済音源もアンロードされる"
     "(ロード/アンロードについては後述)。")
  )

(defdesc :set-config-disable-mobile?-false
  )

(defdesc :set-config-disable-mobile?-true
  (p "trueを設定する事で、モバイル環境での音源再生の一切を禁止する"
     "(初期値はfalse)。")
  (p "非モバイル環境では何も起こらない。")
  )

(defdesc :set-config-disable-webaudio?-false
  )

(defdesc :set-config-disable-webaudio?-true
  (p "trueを設定する事で、WebAudioによる音源再生を禁止する"
     "(初期値はfalse)。")
  (p "初期状態では、WebAudioが利用可能ならWebAudioを使い、"
     "そうでなければHtmlAudioが利用可能ならHtmlAudioを使い、"
     "どちらも使えなければ再生は無効化される、"
     "という優先順位になっている。")
  (p "通常はこのままで適切に機能するが、"
     "「HtmlAudioでの動作確認を取りたい」等の時は"
     "この設定項目を有効にするとよい。")
  (p "この値を変更した場合は内部状態をリセットする必要がある為、"
     "全ての再生中音源は停止され、"
     "また全てのロード済音源もアンロードされる。")
  )

(defdesc :set-config-disable-htmlaudio?-false
  )

(defdesc :set-config-disable-htmlaudio?-true
  (p "trueを設定する事で、HtmlAudioによる音源再生を禁止する"
     "(初期値はfalse)。")
  (p "概要については上の" [:code "disable-htmlaudio?"] "の項目を参照。")
  (p "この値を変更した場合は内部状態をリセットする必要がある為、"
     "全ての再生中音源は停止され、"
     "また全てのロード済音源もアンロードされる。")
  )

(defdesc :load-noise
  (p "BGMやSEの音響ファイルの初回再生時は、実は内部で"
     "ファイルのロードを行いそれが完了してから再生している。"
     "その為、初回再生時のみ実際に再生されるまでタイムラグがある"
     "(ファイルサイズが小さかったりブラウザキャッシュがなされていれば"
     "目立たないが)。"
     "このタイムラグをなくすには、再生するよりずっと前の段階で"
     "ロードを行っておけばよい。")
  (p "この関数はそのロードをバックグラウンドで行わせる。")
  (p "既にロード中だったりロードが完了している場合は何も行われない。")
  )

(defdesc :loaded?
  (p "音響ファイルのロードはバックグラウンドで非同期に実行される。"
     "この関数は、そのロード処理が正常終了/異常終了のどちらにせよ"
     "完了しているかどうかを真偽値で返す。")
  (p "ローディング画面等では、定期的にこの関数を呼んで"
     "ロードが完了したかを確認するとよい。")
  )

(defdesc :error?
  (p "前述の" [:code "loaded?"] "/" [:code "isLoaded"]
     "ではロードの完了は分かるものの、"
     "正常にロードできたかまでは分からない。")
  (p "ロード時にエラーが起こったかどうかを調べたい時は、"
     "真偽値としてそれを返すこの関数が使える。")
  )

(defdesc :unload-noise
  (p "音響ファイルの数が非常に多い場合や"
     "サーバで動的に生成した音響ファイルを扱う場合、"
     "ロード済の音源が多くなりメモリを圧迫してしまう事がある。"
     "その場合はこの関数を使い、"
     "再生しない音源をアンロードするとよい。")
  (p "もしアンロード時にまだその音響ファイルが再生中だった場合、"
     "その再生は強制停止される。")
  (p "アンロードを行った音響ファイルを再度再生しようとした場合は"
     "内部でファイルのロードが実行し直されるので、"
     "効率は悪いものの再生自体に支障が出る事はない。")
  (p "アンロード後は、前述の"
     [:code "loaded?"] "/" [:code "isLoaded"]
     "が、falseを返すように戻る。")
  )

(defdesc :unload-all
  (p "ロード済の全ての音響ファイルをアンロードする。"))



(defdesc :bgm-option-a
  (p (expand-autoext-html "bgm/va32.*")
     "をBGMとして再生する。")
  (p [:code "volume"] "は個別の音量。通常 0.0 ～ 1.0 の数値。"
     "指定しない場合は 1.0 が指定された事になる。"
     "volumeに1.0以上の数値を指定する事も可能だが、"
     "マスターボリュームとBGMボリュームの設定によっては効果が出ない"
     "(「volume * マスターボリューム * BGMボリューム」の値を"
     "1.0以上にする事はできない為)。")
  (p [:code "pitch"] "は再生レート。 0.1 ～ 10.0 の数値。"
     "指定しない場合は 1.0 が指定された事になる。"
     "この数値が1.0より小さいと再生速度と音程が低下し、"
     "1.0より大きいと再生速度と音程が上昇する。"
     "ブラウザによっては常に1.0固定となる為、"
     "この数値に依存するような処理は避けた方が無難。")
  (p [:code "pan"] "はステレオでの左右への寄りの値。 -1.0 ～ 1.0 の数値。"
     "-1.0が最も左寄り、0なら中央、1.0が最も右寄りに再生される。"
     "指定しない場合は 0 が指定された事になる。"
     "ブラウザによっては常に中央固定になる為、"
     "この数値に依存するような処理は避けた方が無難。")
  )

(defdesc :bgm-option-b
  (p (expand-autoext-html "bgm/va32.*")
     "をBGMとして再生する。")
  (p "各オプションの詳細については前述の説明を参照。")
  (p "cljs版では、追加の引数は一つのmapで指定してもよいし、"
     "複数のkey-value値として指定してもよい"
     "(js版ではObjectでの指定のみ可能)。")
  )

(defdesc :bgm-option-c
  (p (expand-autoext-html "bgm/va32.*")
     "をBGMとして再生する。")
  (p "オプションの詳細については前述の説明を参照。")
  )

(defdesc :bgm-noise-ch
  (p (expand-autoext-html "bgm/noise.*")
     "を「" [:code "background-sound"] "」という名前のBGM再生チャンネルにて、"
     "ループBGMとして再生する。")
  (p "BGM再生チャンネルは必要な数だけ作成でき、"
     "違うBGM再生チャンネルは同時に再生される。"
     "これは「BGMと同時に雨の音などの環境音を再生したい」ような"
     "用途に利用できる。")
  (p "BGM再生チャンネル名には、cljs版では任意のキーワードが、"
     "js版では任意の文字列が指定できる。")
  (p "BGM再生チャンネル名が省略された場合はデフォルト値として"
     "「" [:code "BGM"] "」が指定されたものとして扱われる。")
  (p "BGM再生チャンネル名以外にも、前述の他のオプションも同時に指定可能。")
  )

(defdesc :stop-bgm-ch-a
  (p "第二引数で指定した「BGM再生チャンネル名」で再生中のBGMだけを、"
     "第一引数で指定したフェード秒数かけて終了する。")
  (p "第一引数に nil / null を指定した場合は、"
     "設定されたデフォルト値がフェード秒数として適用される"
     "(デフォルト値の詳細については「設定項目」セクションを参照)。")
  (p "第二引数省略時は全てのBGMチャンネルに対して停止処理が行われる。")
  (p "この例では「" [:code "background-sound"] "」だけが停止される。")
  )

(defdesc :stop-bgm-ch-b
  (p "第二引数で指定した「BGM再生チャンネルID」で再生中のBGMだけを、"
     "第一引数で指定したフェード秒数かけて終了する。")
  (p "この例では「" [:code "BGM"] "」、つまりチャンネル無指定でのBGM再生だけが"
     "停止される。")
  )

(defdesc :se-option-a
  (p (expand-autoext-html "se/launch.*")
     "をSEとして再生する。")
  (p [:code "volume"] "は個別の音量。通常 0.0 ～ 1.0 の数値。"
     "指定しない場合は 1.0 が指定された事になる。"
     "volumeに1.0以上の数値を指定する事も可能だが、"
     "マスターボリュームとBGMボリュームの設定によっては効果が出ない"
     "(「volume * マスターボリューム * SEボリューム」の値を"
     "1.0以上にする事はできない為)。")
  (p [:code "pitch"] "は再生レート。 0.1 ～ 10.0 の数値。"
     "指定しない場合は 1.0 が指定された事になる。"
     "この数値が1.0より小さいと再生速度と音程が低下し、"
     "1.0より大きいと再生速度と音程が上昇する。"
     "ブラウザによっては常に1.0固定となる為、"
     "この数値に依存するような処理は避けた方が無難。")
  (p [:code "pan"] "はステレオでの左右への寄りの値。 -1.0 ～ 1.0 の数値。"
     "-1.0が最も左寄り、0なら中央、1.0が最も右寄りに再生される。"
     "指定しない場合は 0 が指定された事になる。"
     "ブラウザによっては常に中央固定になる為、"
     "この数値に依存するような処理は避けた方が無難。")
  )

(defdesc :se-option-b
  (p (expand-autoext-html "se/launch.*")
     "をSEとして再生する。")
  (p "各オプションの詳細については前述の説明を参照。")
  (p "cljs版では、追加の引数は一つのmapで指定してもよいし、"
     "複数のkey-value値として指定してもよい"
     "(js版ではObjectでの指定のみ可能)。")
  )

(defdesc :se-option-c
  (p (expand-autoext-html "se/launch.*")
     "をSEとして再生する。")
  (p "オプションの詳細については前述の説明を参照。")
  (p "SE再生関数は、返り値として「SE再生チャンネルID」を返す。"
     "これについての詳細は次の項目を参照。")
  )

(defdesc :stop-se-ch
  (p "第二引数で指定した「SE再生チャンネルID」に対応するSEだけを、"
     "第一引数で指定したフェード秒数かけて終了する。")
  (p "「SE再生チャンネルID」は、SE再生関数の返り値として得られる"
     "(この「SE再生チャンネルID」は、そのまま捨てても全く問題ない)。")
  (p "第二引数で指定した「SE再生チャンネルID」に対応するSEの再生が"
     "既に完了している場合は何も起きない。")
  (p "第一引数に nil / null を指定した場合は、"
     "設定されたデフォルト値がフェード秒数として適用される"
     "(詳細については既出の「設定項目」内「音量設定」の項目を参照)。")
  (p "第二引数省略時は全てのSEに対して停止処理が行われる。")
  )

(defdesc :alarm-kick
  (p (expand-autoext-html "se/kick.*")
     "をSEとして再生する。")
  (p "ただしバックグラウンドタブであっても強制的に再生が行われる"
     "(通常はバックグラウンドタブ時はSEの再生が行われない)。")
  (p "通常のSE再生と同様に、追加の引数を取る事もできる"
     "(詳細は上記参照)。")
  )

(defdesc :version-js
  (p "vnctst-audio4のライブラリとしてのバージョン文字列。js版のみ提供。"))

(defdesc :can-play-ogg
  (p "oggが再生可能なら真値を返す。"))

(defdesc :can-play-mp3
  (p "mp3が再生可能なら真値を返す。"))

(defdesc :can-play-m4a
  (p "m4aが再生可能なら真値を返す。"))

(defdesc :can-play
  (p "引数として渡したmime-typeが再生可能なら真値を返す。"))

(defdesc :terminal-type
  (p "この環境が引数として渡した端末タイプなら"
     "真値を返す。")
  (p "端末タイプは"
     [:code "tablet"]
     " "
     [:code "mobile"]
     " "
     [:code "android"]
     " "
     [:code "ios"]
     " "
     [:code "chrome"]
     " "
     [:code "firefox"]
     "が指定可能。"
     "ただしこれは User-Agent による判定の為、"
     "誤判定する場合もある事に注意。")
  (p "これはjs版では関数だが、cljs版では単なるsetでありset向けの各種の"
     "操作を適用する事ができる。")
  )

(defdesc :float->percent
  (p "各ボリューム設定は0.0～1.0の小数値で指定するが、"
     "これを0～100のパーセント値へと変換する"
     "単純なユーティリティ関数。"))

(defdesc :percent->float
  (p "上記 float->percent / floatToPercent の"
     "逆変換を行うユーティリティ。"))





(defn- demo-button2 [id]
  (let [desc-id (str (name id) "-desc")
        desc-htmls (get @desc-table id)
        ]
    [:dl
     [:dt [:button {:id (name id)} " "]]
     (when-not (empty? desc-htmls)
       (apply vector
              :dd
              {:id desc-id}
              desc-htmls))]))

(def heading-index (atom []))

(defmacro heading [label & [anchor]]
  (let [anchor (or anchor label)]
    (when (empty? (filter #(= anchor (second %))
                          @heading-index))
      (swap! heading-index conj [label anchor]))
    `[:div
      [:div [:a {:name ~anchor} " "]]
      [:br]
      [:br]
      [:h2 ~label]]))

(defn- index-item [label & [anchor]]
  [:li [:a {:href (str "#" (or anchor label))} label]])







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
                 [:h2 "目次"]
                 [:ul (map #(apply index-item %)
                           @heading-index)]]
                [:div
                 (heading "前書き")
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
                  [:li
                   "このデモでは、ページのロード時に"
                   [:code "debug?"] "フラグ(詳細は「設定項目」を参照)を"
                   "有効化している為、"
                   "再生や停止等の操作を行うとデバッグコンソールに"
                   "ログが表示されます。これを確認したい場合も"
                   "デバッグコンソールを開いておくとよいでしょう。"]
                  ]]
                [:hr]
                [:div
                 (heading "基本操作")
                 [:p
                  "このセクションで紹介している機能だけでも"
                  "大体なんとかなります"]
                 [:h3 "BGMを鳴らす"]
                 (demo-button2 :bgm-va32)
                 (demo-button2 :bgm-cntr)
                 (demo-button2 :bgm-oneshot-ny2017)
                 (demo-button2 :bgm-fadein-cntr)
                 [:h3 "BGMを止める"]
                 (demo-button2 :stop-bgm)
                 (demo-button2 :stop-bgm-3)
                 (demo-button2 :stop-bgm-0)
                 [:h3 "SEを鳴らす"]
                 (demo-button2 :se-launch)
                 (demo-button2 :se-kick)
                 (demo-button2 :stop-se)
                 (demo-button2 :stop-se-05)
                 ]
                [:hr]
                [:div
                 (heading "設定項目")
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
                 (heading "高度な操作")
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
                 ;(demo-button2 :me-cntr)
                 [:h3 "SEの再生オプション"]
                 (demo-button2 :se-option-a)
                 (demo-button2 :se-option-b)
                 (demo-button2 :se-option-c)
                 (demo-button2 :stop-se-ch)
                 (demo-button2 :alarm-kick)
                 ]
                [:hr]
                [:div
                 (heading "おまけ機能")
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



