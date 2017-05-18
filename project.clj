(defproject htmlms "1.0.0"
  :description "Generates some HTML embed code given a YouTube URL and height and width. Trying out bootstrap a la http://www.webjars.org and http://blog.michielborkent.nl/blog/2015/06/06/from-leiningen-to-boot/"
  :url "https://ecampuscenter.github.io/"
  :license {:name "Creative Commons Attribution 4.0 International License"
            :url "http://creativecommons.org/licenses/by/4.0/"}

  :dependencies [ [org.clojure/clojure "1.9.0-alpha16"]
                 #_[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.542"]
                 [devcards "0.2.3" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [sablono "0.8.0"]
                 #_[org.omcljs/om "0.9.0"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.cemerick/url "0.1.1"]
                 [org.clojure/core.async "0.3.442"]
                 [org.webjars/bootstrap "4.0.0-alpha.6-1"]
                 ; trying to get bootstrap to work... get an error Uncaught Error: Bootstrap tooltips require Tether (http://github.hubspot.com/tether/)(anonymous function) @ bootstrap.min.js:7(anonymous function) @ bootstrap.min.js:7(anonymous function) @ bootstrap.min.js:7
                 ; [cljsjs/tether "1.1.1-0"]
                 [domina "1.0.3"]
                 ; https://github.com/bhauman/devcards/issues/106
                 [cljsjs/react-dom "15.5.0-0" :exclusions [cljsjs/react]]
                 [cljsjs/react-dom-server "15.5.0-0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.5.0-0"]
                 #_[reagent "0.6.1" :exclusions [cljsjs/react cljsjs/react-dom-server]]
                 [reagent "0.6.1" :exclusions [cljsjs/react]]
                 ]

  :jvm-opts ^:replace ["-Xmx1g" "-server" "--add-modules=java.se.ee"]

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.10"]]

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
                        :warning-handlers [(fn [warning-type env extra]
                                             (when (warning-type cljs.analyzer/*cljs-warnings*)
                                               (when-let [s (cljs.analyzer/error-message warning-type extra)]
                                                 (binding [*out* *err*]
                                                   (println "WARNING:" (cljs.analyzer/message env s)))
                                                )))]
                        :compiler {:main "htmlms.start-ui"
                                   :devcards true ; <- note this
                                   :asset-path "js/compiled/hostedcards"
                                   :output-to  "resources/public/js/compiled/htmlms.js"
                                   :output-dir "resources/public/js/compiled/hostedcards"
                                   :optimizations :whitespace}}]}

  :figwheel { :css-dirs ["resources/public/css"]})
