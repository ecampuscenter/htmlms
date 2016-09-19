(defproject htmlms "1.0.0"
  :description "Generates some HTML embed code given a YouTube URL and height and width. Trying out bootstrap a la http://www.webjars.org and http://blog.michielborkent.nl/blog/2015/06/06/from-leiningen-to-boot/"
  :url "https://ecampuscenter.github.io/"
  :license {:name "Creative Commons Attribution 4.0 International License"
            :url "http://creativecommons.org/licenses/by/4.0/"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [devcards "0.2.1-7"]
                 [sablono "0.7.4"]
                 #_[org.omcljs/om "0.9.0"]
                 [reagent "0.6.0"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.cemerick/url "0.1.1"]
                 [org.clojure/core.async "0.2.391"]
                 [org.webjars/bootstrap "4.0.0-alpha.3"]
                 ; trying to get bootstrap to work... get an error Uncaught Error: Bootstrap tooltips require Tether (http://github.hubspot.com/tether/)(anonymous function) @ bootstrap.min.js:7(anonymous function) @ bootstrap.min.js:7(anonymous function) @ bootstrap.min.js:7
                 ; [cljsjs/tether "1.1.1-0"]
                 [domina "1.0.3"]
                 ; https://github.com/bhauman/devcards/issues/106
                 [cljsjs/react-dom "0.14.3-1"]
                 ]


  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.7"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]
  
  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "devcards"
                        :source-paths ["src"]
                        :figwheel { :devcards true} ;; <- note this
                        :compiler { :main       "htmlms.start-ui"
                                    :asset-path "js/compiled/devcards_out"
                                    :output-to  "resources/public/js/compiled/htmlms_devcards.js"
                                    :output-dir "resources/public/js/compiled/devcards_out"
                                    :source-map-timestamp true}}
                       {:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main       "htmlms.start-ui"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/htmlms.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:main       "htmlms.start-ui"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/htmlms.js"
                                   :optimizations :advanced}}
                       {:id "hostedcards"
                        :source-paths ["src"]
                        :compiler {:main "htmlms.start-ui"
                                   :devcards true ; <- note this
                                   :asset-path "js/compiled/hostedcards"
                                   :output-to  "resources/public/js/compiled/htmlms.js"
                                   :output-dir "resources/public/js/compiled/hostedcards"
                                   :optimizations :advanced}}]}

  :figwheel { :css-dirs ["resources/public/css"]})
