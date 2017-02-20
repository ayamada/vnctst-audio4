(ns vnctst.audio4.demo.server
  (:require [ring.middleware.resource :as resources]
            [ring.util.response :as response]
            [clojure.string :as string]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [project-clj.core :as project-clj]
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
     "(ボタンを連打しても前の音が途切れたりしない。"
     "ただし一部の古い環境では非対応)。")
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
     "(音量値は0.0～1.0の範囲、初期値は0.6)。")
  (p "マスター音量はBGMとSEの両方に影響する。")
  )

(defdesc :set-config-volume-bgm-100
  )

(defdesc :set-config-volume-bgm-25
  (p "BGM音量を設定する"
     "(音量値は0.0～1.0の範囲、初期値は0.6)。")
  (p "実際のBGMの再生音量は、この項目とマスター音量から決定される。")
  (p "初期状態ではマスター音量0.6(60%)かつBGM音量0.6(60%)なので、"
     "実際のBGMの再生音量は0.36(36%)相当となる。")
  (p "このデフォルト音量では小さすぎると思うなら、"
     "もっと大き目の値を設定するとよい。")
  )

(defdesc :set-config-volume-se-100
  )

(defdesc :set-config-volume-se-25
  (p "SE音量を設定する"
     "(音量値は0.0～1.0の範囲、初期値は0.6)。")
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
  (p "概要については上の" [:code "disable-webaudio?"] "の項目を参照。")
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
  (p "一度に大量のファイルのロード要求が来た場合でも、"
     "同時にロードを実行せずに一つずつ順番にロードを実行していく為、"
     "大量のhttpコネクションを消費してしまう事はない。"
     "雑にロード要求を投げてよい。")
  (p "指定したファイルが既にロード中だったりロードが完了している場合は"
     "何も行われない。")
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
  (p "なお、エラーの起こったファイルを再生しようとしても何も起こらないので、"
     "雑に扱っても問題ない"
     "(もちろん、"
     [:code "debug?"]
     "フラグを有効にしていればコンソールへのエラー通知は行われる)。")
  (p "ただし上記について例外があり、別のBGMの再生中のみ、"
     "再生中BGMのフェードアウト終了が開始される仕様としている。"
     "これはBGM変更の状況では、"
     "エラーの起こったファイルの再生が無視されて"
     "元のBGMが鳴り続けるよりも、元のBGMは普通に終わらせておいた方がまだマシ"
     "であると考え、この仕様とした。")
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
  (p "そもそもロードしていない音響ファイルを"
     "アンロードしようとしても何も起こらない(エラーも投げられない)ので、"
     "雑に実行しても問題ない。")
  (p "ロード最中の音響ファイルのアンロードも問題なく行える。")
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
     "(js版ではObject形式での指定のみ可能)。")
  )

(defdesc :bgm-option-c
  (p (expand-autoext-html "bgm/va32.*")
     "をBGMとして再生する。")
  (p "オプションの詳細については前述の説明を参照。")
  )

(defdesc :bgm-option-d
  (p (expand-autoext-html "bgm/ny2017.*")
     "をBGMとして再生する。")
  (p [:code "oneshot?"]
     "に真値を指定する事により、非ループ再生となる。"
     "前述の" [:code "bgm-oneshot!"] "/" [:code "bgmOneshot"] "は、"
     "内部でこのパラメータを設定している。")
  (p [:code "fadein"]
     "にフェードイン秒数を指定する事により、"
     "再生開始時にフェードインが適用されるようになる。"
     "前述の" [:code "bgm-fadein!"] "/" [:code "bgmFadein"] "は、"
     "内部でこのパラメータを設定している。")
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
     "(js版ではObject形式での指定のみ可能)。")
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
     "(「SE再生チャンネルID」はこの個別の停止指定の為の存在なので、"
     "個別停止を行わないのであれば、そのまま捨てても全く問題ない)。")
  (p "第二引数で指定した「SE再生チャンネルID」に対応するSEの再生が"
     "既に完了している場合は何も起きない。")
  (p "第一引数に nil / null を指定した場合は、"
     "設定されたデフォルト値がフェード秒数として適用される"
     "(詳細については既出の「設定項目」内「音量設定」の項目を参照)。")
  (p "第二引数省略時は全てのSEに対して停止処理が行われる。")
  (p "この機能は、登場キャラクタがボイスを発生するような内容のゲームでの"
     "利用を想定している。")
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

(defdesc :me-launch
  (p (expand-autoext-html "se/launch.*")
     "を非ループBGMとして再生する。")
  (p "前述の" [:code "bgm-oneshot!"] "/" [:code "bgmOneshot"]
     "と全く同じ。")
  (p "分かりづらいのでobsoletedとする。")
  )

(defdesc :bgs-noise
  (p (expand-autoext-html "bgm/noise.*")
     "を「" [:code "BGS"] "」という名前のBGM再生チャンネルで再生する。")
  (p "分かりづらいのでobsoletedとする。")
  )

(defdesc :se-kick-keyword
  (p "キーワードでpathを指定する機能はcljs版のみ対応しており、"
     "js版では対応していない。")
  (p "引数にキーワードを指定した場合、"
     [:code ":foo"] "なら" [:code (pr-str "foo.*")] "へと、"
     [:code ":bar/baz"] "なら" [:code (pr-str "bar/baz.*")] "へと、"
     "自動的に展開されて扱われる。")
  (p "キーワード構文の仕様上、二段以上深いディレクトリ内にあるファイルを"
     "キーワード構文で指定する事はできない。"
     "この制約により、利用できる場面はかなり限定される。")
  )

(defdesc :current-device-name
  (p "当ライブラリ内部での再生デバイス名を文字列として返す。")
  (p "具体的に返される値は以下のいずれか。")
  [:ul
   [:li [:code "web-audio"] " : WebAudio"]
   [:li [:code "html-audio-multi"] " : HtmlAudio。SE多重再生サポートあり"]
   [:li [:code "html-audio-single"] " : HtmlAudio。SE多重再生サポートなし"]
   [:li [:code "dumb"] " : 無音"]
   ])








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

(defmacro heading [label & [anchor folding-id]]
  (let [anchor (or anchor label)
        label-html (if-not folding-id
                     [:span label]
                     [:a.folding-label {:id (name folding-id)
                                        :href "#"}
                      label])]
    (when (empty? (filter #(= anchor (second %))
                          @heading-index))
      (swap! heading-index conj [label anchor]))
    `[:div
      [:div [:a {:name ~anchor} " "]]
      [:br]
      [:br]
      [:h2 ~label-html]]))

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
                 [:span "この環境の端末フラグ："]
                 [:code#terminal-flags "(terminal-flags)"]
                 [:br]
                 [:span "使用デバイス名："]
                 [:code#device-name "(device-name)"]
                 [:br]
                 [:span "事前設定された設定項目："]
                 [:code#config-info "(config-info)"]
                 [:br]
                 [:span "事前ロードされた音源ファイル："]
                 [:code#preload-info "(preload-info)"]
                 ]
                [:hr]
                [:div
                 [:h2 "目次"]
                 [:ul (map #(apply index-item %)
                           @heading-index)]]
                [:div
                 (heading "前書き" nil :h-introduction)
                 [:ul#introduction
                  [:li
                   "これは、ゲーム向けの音響ファイル再生ライブラリ"
                   "である「vnctst-audio4」のオンラインデモです。"]
                  [:li
                   "vnctst-audio4についての概要は"
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
                 (heading "前準備" nil :h-preparation)
                 [:div#preparation
                  [:p
                   "実運用時に当ライブラリを利用する前準備です。"
                   "cljs版とjs版とで内容が違います"]
                  [:dl
                   [:dt "cljs版"]
                   [:dd
                    [:ol
                     [:li
                      [:code "project.clj"]
                      "の"
                      [:code ":dependencies"]
                      "に"
                      [:code (pr-str ['jp.ne.tir/vnctst-audio4
                                      (project-clj/get :version)])]
                      "を追加"
                      [:br]
                      "("
                      (a "https://clojars.org/jp.ne.tir/vnctst-audio4"
                         "stable releaseの最新版はclojars")
                      "で確認)"
                      ]
                     [:li
                      "利用したい"
                      [:code "ns"]
                      "内にて"
                      [:code (pr-str '(:require [vnctst.audio4 :as va4]))]
                      "のような感じでrequireする"]]]
                   [:dt "js版"]
                   [:dd
                    [:p "以下のどちらか好きな方を選ぶ"]
                    [:ul
                     [:li
                      [:code "vnctst-audio4.js"]
                      "ファイルを"
                      (a "https://github.com/ayamada/vnctst-audio4/releases")
                      "から取ってきて配置し、"
                      "html内に"
                      [:code
                       (hiccup/h "<script src=\"vnctst-audio4.js\" type=\"text/javascript\"></script>")
                       ] "タグを入れて読み込む"]
                     [:li
                      (a "https://www.npmjs.com/" "npm")
                      "に"
                      [:code (a "https://www.npmjs.com/package/vnctst-audio4" "vnctst-audio4")]
                      "の名前で登録しているので、"
                      "自分の使っているパッケージマネージャの流儀で"
                      "適切に取り込む"
                      ]]
                    ;[:p ""]
                    ]
                   ]
                  ]
                 ]
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
                 (demo-button2 :bgm-option-d)
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
                 [:h3 "その他"]
                 [:p
                  "旧版であるvnctst-audio3"
                  "由来のインターフェースです"]
                 (demo-button2 :me-launch)
                 (demo-button2 :bgs-noise)
                 (demo-button2 :se-kick-keyword)
                 ]
                [:hr]
                [:div
                 (heading "補助機能")
                 [:p "便利関数類です"]
                 (demo-button2 :version-js)
                 (demo-button2 :can-play-ogg)
                 (demo-button2 :can-play-mp3)
                 (demo-button2 :can-play-m4a)
                 (demo-button2 :can-play)
                 (demo-button2 :terminal-type)
                 (demo-button2 :float->percent)
                 (demo-button2 :percent->float)
                 (demo-button2 :current-device-name)
                 ]
                ;; prefetch
                [:hr]
                [:div
                 (heading "ファイル一覧の取得")
                 [:p "これはcljs版専用の機能です。js版では利用できません"]
                 [:dl
                  [:dt "これは何？"]
                  [:dd
                   "実際のゲームで利用する場合、"
                   "「最初のローディング画面で音源ファイルのプリロードを行う」"
                   "みたいな事がよくある。"
                   "しかし、いちいちゲーム中で使っている音源ファイル全てを"
                   "手で列挙していくのはとても面倒なので、"
                   "例えば" [:code (pr-str "se/")] "ディレクトリを指定したら"
                   [:code (pr-str ["se/launch.*" "se/kick.*"])]
                   "のような形式でファイルの一覧を取得できる機能を用意した。"
                   ]
                  [:dt "重要な注意点"]
                  [:dd
                   "ブラウザ上で「ファイルの一覧を取得」するような事は"
                   "通常できない為、この機能は"
                   "「マクロの展開フェーズにローカルファイルの一覧を取得」"
                   "する事によって実現している。"
                   "その為"
                   "「ローカルのpathとブラウザ上でのpathの"
                   "二つを引数に指定する必要がある」"
                   "「引数は即値の文字列でないといけない」"
                   "「マクロ展開後に音源ファイルを追加しても、"
                   "再コンパイルするまでは認識されない」"
                   "という制約がある。"
                   ]
                  ]
                 [:dl
                  [:dt
                   [:code
                    "(ns foo.bar (:require [vnctst.audio4.prefetch"
                    " :include-macros true]))"]]
                  [:dd
                   "このファイル一覧取得機能は"
                   [:code "vnctst.audio4.prefetch"]
                   "に入っているので、requireしておく。"
                   [:br]
                   "ここには書いてないが"
                   [:code ":as"]
                   "を指定して短い別名を付けておくとよい。"
                   [:br]
                   "この機能はマクロで実装されている為"
                   [:code ":include-macros true"]
                   "を必ず付ける事。"]
                  [:dt
                   [:code
                    "(vnctst.audio4.prefetch/pathlist-from-directory "
                    (pr-str "resources/public/se/")
                    " "
                    (pr-str "se/")
                    ")"
                    ]]
                  [:dd
                   "第一引数は、ファイル一覧を取得するローカルディレクトリ。"
                   "マクロの制約により、これは即値の文字列でなくてはならない。"
                   "変数や式を指定した場合はコンパイルエラーが投げられる。"
                   "末尾のスラッシュはあってもなくてもよい。"
                   [:br]
                   "第二引数は、httpサーバ上でのディレクトリ。"
                   "第一引数に対応するものを指定する事。"
                   "末尾のスラッシュはあってもなくてもよい。"
                   [:br]
                   "この式はコンパイルフェーズに評価され、"
                   [:code (pr-str ["se/launch.*" "se/kick.*"])]
                   "のようなvecとしてソースに埋め込まれる。"
                   "このvecを適当な変数にdefしておき、"
                   "早いタイミングでdoseq等を使って"
                   [:code "load!"]
                   "にかけておくとよい。"
                   [:br]
                   "なお、第一引数で指定したローカルディレクトリ内には"
                   "音響ファイルだけを入れておく事"
                   "(そうしないと音響ファイル以外も"
                   "このリストに追加されてしまう為)。"
                   ]
                  [:dt
                   [:code
                    "(vnctst.audio4.prefetch/pathlist-from-directory "
                    (pr-str "resources/public/se/")
                    " "
                    (pr-str "se/")
                    " true"
                    ")"
                    ]]
                  [:dd
                   "第三引数に即値の真値を指定すると、"
                   "autoext変換が行われなくなる。"
                   [:br]
                   "具体的には、上ではマクロの展開結果が"
                   [:code (pr-str ["se/launch.*" "se/kick.*"])]
                   "のようになるが、こちらでは"
                   [:code (pr-str ["se/launch.ogg"
                                   "se/launch.mp3"
                                   "se/kick.ogg"
                                   "se/kick.mp3"])]
                   "のようになる。"
                   "autoext指定をしない運用をする場合は"
                   "こちらを使う必要がある。"
                   ]
                  ]]
                [:hr]
                [:div
                 (heading "必要知識")
                 [:p "html5上の音響システム固有の前提知識。"
                  "ライブラリレベルでは吸収できないバッドノウハウ類です"]
                 [:h4
                  "「この拡張子なら全ブラウザで再生できる」という"
                  "万能ファイル形式は存在しない"]
                 [:ul
                  [:li
                   "なので、なるべく多くのブラウザでの再生をサポートしたい場合"
                   "複数種類の拡張子のファイルを用意する必要があります。"]
                  [:li
                   "再生回りでのトラブルの少なさは「ogg、mp3、m4a、他」の順"
                   "なので、この優先順位で二つほど選ぶと良いでしょう"
                   "(「oggとmp3をセットで配置」もしくは"
                   "「oggとm4aをセットで配置」あたりが安定)。"]
                  [:li
                   "どうしても一つに絞る場合は、"
                   "「oggを採用し、ieとsafariでの再生を切り捨てる」"
                   "「mp3を採用し、一部os向けfirefoxを切り捨て、"
                   "一部モバイル環境で出る不具合には目をつぶる」"
                   "「m4aを採用し、一部os向けfirefoxを切り捨て、"
                   "一部モバイル環境で出る不具合には目をつぶる」"
                   "のどれかを選択する事になります。"]
                  [:li
                   "なお、"
                   "「oggは内部コーデックが通常のvorbisのもののみ対応」"
                   "「mp3でcbr(可変ビットレート)を有効にしていると"
                   "一部モバイル環境で不安定になる」"
                   "「m4aは内部コーデックがaacのもののみ対応」"
                   "という制約もあるので、音源ファイルのエンコード時には"
                   "この辺りにも気を付けておくとよいでしょう"
                   "(特にmp3は、エンコーダのデフォルト設定でcbrが有効に"
                   "なっている事が多いので注意)。"]]
                 [:h4
                  "無音であっても支障が出ないようにする"
                  ]
                 [:ul
                  [:li
                   "前述の拡張子対応をどんなにがんばっても、"
                   "古いモバイル端末では、音源の一部/全部が再生されないものも"
                   "あります。"]
                  [:li
                   "またそもそも、ハード側の音量を0にした状態で使われる事も"
                   "よくあります。"
                   "なので、「音が出なくても利用できる」ようになっているのが"
                   "ベターでしょう。"
                   ]]
                 [:h4
                  "違うドメインにあるファイルを再生する場合は"
                  (a "https://www.google.com/search?nfpr=1&q=CORS" "CORS設定")
                  "が必要"]
                 [:ul
                  [:li
                   "詳細はリンクから自分で調べてください"
                   ]]
                 ]
                [:hr]
                [:div
                 (heading "FAQ")
                 [:p "その他の質問など"]
                 [:h4 "音が出ない"]
                 [:ul
                  [:li
                   [:code "debug?"]
                      "フラグを有効にして、コンソールにエラーが出ていないか"
                      "確認してみよう"]
                  [:li
                   "多くのブラウザでは、ローカルファイル("
                   [:code "file:///..."]
                   "形式のurl)からの再生はできません。"
                   "対応しているブラウザで動作確認するか、"
                   "httpサーバを用意してその中に配置して動作確認しよう"]
                  [:li
                   (a (str "https://github.com/ayamada/vnctst-audio4"
                           "#対応環境マトリックス")
                      "対応環境マトリックス")
                   "を確認してみよう"]
                  ]
                 [:h4 "音が小さい"]
                 [:ul
                  [:li
                   "デフォルトの音量は25%相当です。"
                   "オンラインデモのサンプルコードを確認して、"
                   "マスターボリューム、BGMボリューム、SEボリュームを"
                   "大き目に設定してみよう"]]
                 [:h4 [:code "pitch"] "の効果が出ない"]
                 [:ul
                  [:li
                   [:code "pitch"] "と" [:code "pan"] "は"
                   "機能しない環境があります"
                   "(WebAudio非対応環境および一部のモバイル環境)。"
                   "pitch指定が効かない場合でもそれなりに聴けるように"
                   "しておくとベターです"
                   ]]
                 [:h4
                  "再生環境によっては、"
                  "BGMのループの際に少しだけ無音の時間がはさまる"]
                 [:ul
                  [:li
                   "HtmlAudioモードで動作する環境(ieおよび古いモバイル環境)"
                   "にて、この問題が起こるようです。"
                   [:br]
                   "再生環境側の問題なので、"
                   "ライブラリレベルでの対応はとても厳しいです。"]]
                 [:h4
                  "モバイル環境で、再生開始までに"
                  "ものすごく時間のかかる時がある"]
                 [:ul
                  [:li
                   "モバイル環境では"
                   "「タッチイベントを実行するまでは再生できない」"
                   "という仕様になっているブラウザが主流です。"
                   "画面のどこかをタッチする事で再生が開始された場合、"
                   "この仕様に引っかかっています。"
                   [:br]
                   "モバイル向けを意識する場合は"
                   "「タイトル画面は無音にする」等、"
                   "この仕様を意識した作りにするとよいでしょう"
                   ]]
                 [:h4
                  "いちいち"
                  [:code "vnctst.audio4.js.bgm(\"hoge.ogg\")"]
                  "って書くのは長くて面倒"]
                 [:ul
                  [:li
                   "最初に"
                   [:code "var va4 = vnctst.audio4.js;"]
                   "を実行しておけば、以降は"
                   [:code "va4.bgm(\"hoge.ogg\")"]
                   "ですみます"]]
                 [:h4
                  "いちいち"
                  [:code "va4.bgm(\"foo/bar/hoge.ogg\")"]
                  "って毎回pathを指定するのは微妙"]
                 [:ul
                  [:li
                   "変数やhash-mapを使いましょう。"
                   [:br]
                   [:br]
                   "他の音響再生ライブラリには"
                   "「ロードした音源を自分で指定したキーワードに割り当て、"
                   "再生したい時にはそのキーワードで指定する」"
                   "ような機構を持つものが多く、"
                   "またvnctst-audioの過去のバージョンでも類似の仕組みを"
                   "採用していました。"
                   [:br]
                   "このキーワード割り当てを行う方式は"
                   "「再生する事前に必ずロードを実行して完了させておく」"
                   "制約のあるシステム上では確かに有用なのですが"
                   "(キーワードに割り当てられている＝事前に再生する対象であると"
                   "指定された事が保証されている)、"
                   "「雑に扱える」事をポリシーとするvnctst-audio4では"
                   "前述の制約のない実装にできたので、結果として"
                   "「いちいちキーワードに割り当てる手間」"
                   "というデメリットだけが残った為、"
                   "キーワード割り当て方式は廃止されました。"
                   [:br]
                   "もちろんキーワード的な管理をした方がよいケースは"
                   "普通に存在しますが、その場合は"
                   "自分で変数やhash-mapに入れて管理した方が"
                   "ずっと扱いやすいでしょう"]]
                 [:h4 "このオンラインデモのサンプルBGM/SEについて"]
                 [:ul
                  [:li
                   "これらのBGM/SEは当プロジェクトのメンテナであるayamadaが"
                   "作成したものです。"
                   [:br]
                   "これらのライセンスは"
                   (a "http://sciencecommons.jp/cc0/about" "CC0")
                   "とします"]]
                 [:h4 "なんかおかしい / バグ報告したい"]
                 [:ul
                  [:li
                   (a "https://github.com/ayamada/vnctst-audio4/issues"
                      "githubリポジトリのissue")
                   "から可能です。"
                   "日本語でokです"
                   ]]
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



