(ns vnctst.audio4.prefetch
  (:require [clojure.java.io :as io]))



;;; 指定ディレクトリのファイル一覧取得
(defn- get-filenames-from-dir-path [dir-path]
  (remove #(= \. (first %)) (seq (.list (io/file dir-path)))))



;;; 指定したlocal-dirからファイル一覧を読み込み、
;;; http-dir内のファイル一覧(vec)として返す。
;;; dont-use-autoext?が偽値ならautoext形式("filename.*"形式)にまとめる。
;;; dont-use-autoext?が真値ならautoext形式にまとめず、個別の拡張子のままで返す。
;;; NB: local-dir と http-dir は即値の文字列でなくてはならない！
;;;     シンボルや式を渡すとコンパイルに失敗する。要注意！
(defmacro pathlist-from-directory [local-dir http-dir & [dont-use-autoext?]]
  (assert (string? local-dir)
          "local-dir must be immediate string, must not symbol or expr")
  (assert (string? http-dir)
          "http-dir must be immediate string, must not symbol or expr")
  (assert (and
            (not (symbol? dont-use-autoext?))
            (not (list? dont-use-autoext?)))
          "dont-use-autoext? must be immediate value, must not symbol or expr")
  (let [files (get-filenames-from-dir-path local-dir)
        http-dir (cond
                   (empty? http-dir) ""
                   (re-find #"/$" http-dir) http-dir
                   :else (str http-dir "/"))
        file->path (partial str http-dir)
        autoextize #(if-let [[_ basename] (re-find #"(.*)\.[^.]*$" %)]
                      (str basename ".*")
                      %)]
    (mapv file->path (if dont-use-autoext?
                       files
                       (set (map autoextize files))))))




