(ns netlet.css
  (:use [cssgen.use]))
(css-ns tabsfm.css.screen )
(css-file "../war/cssgen.css"
	 (rule "body" :background-color "red"))