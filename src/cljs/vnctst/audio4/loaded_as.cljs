(ns vnctst.audio4.loaded-as)

;;; NB: これは cache と web-audio の両方から参照されるので、
;;;     モジュールを分離する事になった

;;; ロード済(エラー含む)のasを保持するテーブル
;;; エラー時は「エントリはあるが値はnil」となる
(defonce loaded-audiosource-table (atom {}))




