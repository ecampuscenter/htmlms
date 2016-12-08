(ns htmlms.vimeo
  (:require
    [reagent.core :as r]
    [sablono.core :as sab :include-macros true]
    [clojure.string :as cs]
    [cognitect.transit :as t]
    [cljs.reader :as reader]
    [domina :as domi]
    [domina.xpath :as x]
    ; for converting youtube duration
    [cemerick.url :as cu]
    [cljs.core.async :refer [chan close!]]

    ; see devcards as a standalone website https://github.com/bhauman/devcards
    ; rlwrap lein figwheel
    ; -- do nothing --
    ; lein cljsbuild once hostedcards
    [devcards.core :as dc])

  (:require-macros
    ; for go/timeout
    [cljs.core.async.macros :as m :refer [go]]

    ; rlwrap lein figwheel
    ; [devcards.core :as dc :refer [defcard deftest]]
    ; lein cljsbuild once hostedcards
    [devcards.core :refer [defcard]])

  (:import [goog.net XhrIo]
           [goog.date Interval]))

(enable-console-print!)

; not sure this applies anymore... seems to work to keep it in?
; rlwrap lein figwheel
; -- do nothing --
; lein cljsbuild once hostedcards
(devcards.core/start-devcard-ui!)

(defonce initial-title (atom {:inittitle "Like I Used to Do.mp4"}))
(defonce initial-length (atom {:initlength "0m 0s"}))
(def intervalobj (Interval.fromIsoString (:initlength @initial-length)))


; setting up ted talks plumbing to read the video length
(defn get-id-from-url [u]
  "given a TED URL return the video’s ID"
  ; youtube
  ;(get (:query (cu/url u)) "v")
  ; ted - for now just return original url... this might change if an API key is usesd.
  u)

(println (get-id-from-url "https://www.youtube.com/watch?v=Wfj4g8zh2gk"))

(def r (t/reader :json))

; courtesy dr. nolan - not used atm but came in handy during dev
(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defcard
  "*BlackBoard HTML Generator* \n")

; from http://stackoverflow.com/questions/32386047/greatest-common-divisor-in-clojure
; for some reason the recur answer blows up if the height is empty in the web form.
(def gcd
  (fn [a b]
    (->> (map (fn [x]
                (filter #(zero? (mod x %)) (range 1 (inc x))))
              [a b])
         (map set)
         (apply clojure.set/intersection)
         (apply max))))

(defn receiver [event]
  (let [response (.-target event)]
    (.log js/console (.getResponseText response))))

(defn xhr-data [url cb]
  (XhrIo.send (str url)
              (fn [f]
                (let [xhr (.-target f)]
                  (cb (.getResponseText xhr))))))

; till i can figure out cross origin policy, close chrome and start chrome like this:
; /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --disable-web-security
; alternatively load this chrome plugin https://chrome.google.com/webstore/detail/allow-control-allow-origi/nlfbmbojpeacfghkpbjhddihlkkiljbi
; and addd exemptions for "*://www.ted.com/*" without the quotes. "*://localhost/* for testing...
(defn xhr-data-ted [url content]
  (XhrIo.send (str url) receiver "GET" content))


(defn calc-bmi [bmi-data]
  (let [{:keys [height width bmi yurl length title] :as data} bmi-data
        h (/ height 100)]

    (if (nil? bmi)
      (assoc data :bmi (/ (/ width (gcd width height) (/ height (gcd width height)))))
      (assoc data :width (* bmi h h)))))


(defn slider [bmi-data param value min max]
  (sab/html
    [:input {:type      "text"
             :value     value
             :min       min
             :max       max
             :style     {:width "100%"}
             :on-change (fn [e]
                          (swap! bmi-data assoc param (.-target.value e))
                          ; also swap out new video length
                          (if (= param :yurl)
                            (xhr-data (str (get-id-from-url (.-target.value e)))
                                      (fn [g]
                                        ;(.log js/console (domi/text (c/sel (domi/html-to-dom g) "script[type=\"application/ld+json\"]")))
                                        (.log js/console "new title: " (first (domi/text (x/xpath g "//title"))) (rest (domi/text (x/xpath g "//title"))))
                                       ; (.log js/console "new dur: " (first (domi/text (x/xpath g "//script"))) (rest (domi/text (x/xpath g "//script"))))
                                       ; (.log js/console "new duration: " (.-innerHTML (x/xpath g "/html/body/script[0]")))
                                       ; (.log js/console "different xpath duration: " (domi/text (x/xpath g "//*[@id=\"wrap\"]/div[3]/script[2]")))
                                       ; (.log js/console "sel: "  (x/xpath g "//*[@id=\"wrap\"]/div[3]/script[2]"))
                                       ; (.log js/console "new time: " (re-find #"\"duration\":{\"raw\":\d*,\"formatted\":\"\d*:\d*\"" g))
                                        (.log js/console "new time: "  (re-find #"\d+:\d+" (re-find #"\"formatted\":\"\d+:\d+\"" g)))

                                        ;(.log js/console "dommy: " (.querySelectorAll js/document "script[type=\"application/ld+json\"]"))
                                       ; (.log js/console "dommy: " (.querySelector js/document "script[type='application/ld+json']"))
                                        ; (.log js/console "new duration get: " (domina/get-data (x/xpath g "/html/body/script[1]") "duration"))
                                        ;(.log js/console "getting by dom: " (.getElementsByName js/document "html/body/script[1]"))
                                        (let [response (.-target g)
                                              ;updlength (-> (get-in (t/read r (x/xpath g "//*[@id="wrap"]/div[3]/script[2]")) [0 "duration"]))
                                              ;updlength (domi/text (x/xpath g "//*[@id=\"wrap\"]/div[3]/script[2]"))
                                              ;updlength (-> (get-in (t/read r g) ["items" 0 "contentDetails" "duration"]))
                                              ;updlength (-> (get-in (t/read r g) [0 "duration"]))
                                              updlength (re-find #"\d+:\d+" (re-find #"\"formatted\":\"\d+:\d+\"" g))
                                              ;updlength (.toString (domi/text (x/xpath g "//script")))
                                              ;updlength (-> (get-in (t/read r (x/xpath g "/html/body/script[1]")) [0 "duration"]))
                                              ; updtitle (domi/text (x/xpath g "/html/head/title[1]"))
                                              updtitle (.toString (domi/text (x/xpath g "//title")))
                                              ; (-> (get-in (t/read r g) ["items" 0 "contentDetails" "duration"]))
                                              ;updtitle (-> (get-in (t/read r g) [0 "snippet" "title"]))
                                              ]
                                          ; TED talk html
                                          ;(.log js/console "updtitle: " updtitle)

                                          ; video length xpath
                                          ; //*[@id="player-hero"]/div[1]/div[2]/div/span[1]
                                          ; (.log js/console "xpath: "  (domi/text (x/xpath g "//*[@id=\"player-hero\"]/div[1]/div[2]/div/span[1]")))

                                          ; author
                                          ; <span class="player-hero__speaker__content">Patricia Burchat:</span>
                                          ; //*[@id="player-hero"]/div[1]/div[2]/h1/div[1]/span

                                          ; title
                                          ; <span class="player-hero__title__content">Shedding light on dark matter</span>
                                          ; //*[@id="player-hero"]/div[1]/div[2]/h1/div[2]/span

                                          ; hmm dont know where else to put this but I now see:
                                          ; <meta content='PT12M20S' itemprop='duration'>
                                          ; in view-source:http://www.ted.com/talks/daniel_levitin_how_to_stay_calm_when_you_know_you_ll_be_stressed

                                          (swap! bmi-data assoc :length updlength)
                                          (swap! initial-length assoc :initlength updlength)
                                          (println ":initlength: " (:initlength @initial-length))
                                          (.log js/console "updlength: " updlength)

                                          ; title
                                          (swap! bmi-data assoc :title updtitle)

                                          ; (println "response: " response)
                                          ; (println "url: " value)
                                          ;(println "can i get a new url? " (.-target.value e))
                                          ))))
                          (println "initial-length: " initial-length)

                          (when (not= param :bmi)
                            (println (str "param:" param))
                            (swap! bmi-data assoc :bmi nil)))}]))


(defn ifriendly [url]
  "create iframible ted link"
  (cs/replace-first (cs/replace-first (cs/replace-first url "vimeo.com" "player.vimeo.com/video") "https:" "") "http:" ""))


(defn fluff [skinny width height length title]
  (str "<p>Click the <strong>Play</strong> icon to begin.</p>
<p><iframe width=\"" width "\" height=\"" height "\" src=\"" (ifriendly skinny) "\" frameBorder=\"0\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe></p>
<p>If video doesn't appear, follow this direct link:
<a href=\"" skinny "\" title=\"" title "\" target=\"_blank\">"
title "</a> (" length ")</p><p>Start the video to access more options in the video frame. To display the video captions, click on the <strong>CC</strong> buton and choose the language you want the captions to be displayed in. To expand the video, use the <strong>Full Screen</strong> icon in the bottom right-hand corner or use the direct link above to open the video on the Vimeo website.</p>
"))

(defn get-data [bmi-data param value min max]

  (sab/html
    [:input {:type      "text"
             :min       min
             :max       max
             :style     {:width "100%"}
             :value     value
             :on-change (fn [e]
                          ; (swap! bmi-data assoc param (.-target.value  (.log js/console (-> (get-in (t/read r e) ["items" 0 "contentDetails" "duration"]))    )   )    )
                          ;(swap! bmi-data assoc :length (.-target.value (-> (get-in (t/read r e) ["items" 0 "contentDetails" "duration"])    )   )    )
                          (swap! bmi-data assoc param (.-target.value e))
                          ; (swap! bmi-data assoc param (.-target.value  (.parse js/JSON e)    ))
                          #_(when (not= param :length)
                              (swap! bmi-data assoc :length nil)))}]))



(defn htmlout [bmi-data param value width height min max length title]
  (sab/html

    [:textarea {:cols      max
                :rows      min
                :value     (fluff value width height length title)
                :style     {:width "100%"}
                :on-change (fn [e] (swap! bmi-data assoc param (.-target.value e))
                             (when (not= param :bmi)
                               (swap! bmi-data assoc :bmi nil)))}]))





(defn htmloutvisual [bmi-data param value width height min max length title]
  (sab/html
    [:div
     [:p {:style {:font-size ".8em"}} "Click the "
      [:strong "Play"] " icon to begin."]
     [:iframe {:width           width
               :height          height
               :src             (ifriendly value)
               :frameborder     0
               :allowfullscreen nil
               :on-change       (fn [e] (swap! bmi-data assoc param (.-target.value e))
                                  (when (not= param :bmi)
                                    (swap! bmi-data assoc :bmi nil)))}]


     [:p {:style {:font-size ".8em"}} "If video doesn't appear, follow this direct link: "
      [:a {:href   value
           :title  title
           :target "_blank"
           } title] " (" length ")"
      ]
     [:p {:style {:font-size ".8em"}} "Start the video to access more options in the video frame. To display the video captions,
     click on the " [:strong "CC"] "  button and choose the language you want the captions to be displayed in. To expand the video, use the " [:strong "Full Screen"] " icon in the bottom right-hand corner or use the direct link above to open the video on the Vimeo website."]]))



(defn height-ratio [w h]
  (/ h (gcd w h)))


(defn width-ratio [w h]
  (/ w (gcd w h)))


(defn bmi-component [bmi-data]
  (println "@bmi-data: " @bmi-data)
  (let [{:keys [width height bmi yurl length title]} (calc-bmi @bmi-data)
        [color diagnose] (cond
                           ;(and (> bmi 0) (< bmi 1)) ["green" (str "approx ratio: 16:9. exact ratio:")]
                           (and (> bmi .562) (< bmi .563)) ["green" (str "approx ratio: 16:9. exact ratio: " (width-ratio width height) " by " (height-ratio width height) ".")]
                           (and (> bmi .74) (< bmi .76)) ["inherit" (str "approx ratio: 4:3. exact ratio: " (width-ratio width height) " by " (height-ratio width height) ".")]
                           ; (< bmi 30) ["orange" "overweight"]
                           :else ["red" (str "non-standard ratio " (width-ratio width height) " by " (height-ratio width height) ".")])]
    (sab/html
      [:div
       [:h3 "Parameters"]
       [:div
        [:span (str "url: " yurl)]
        (slider bmi-data :yurl yurl 0 100)]
       [:div
        [:span (str "width: " (int width) "px")]
        (slider bmi-data :width width 30 150)]
       [:div
        [:span (str "height: " (int height) "px")]
        (slider bmi-data :height height 100 220)]
       [:div
        [:span (str "length: " length)]
        (slider bmi-data :length length 0 100)]

       [:div
        [:span (str "Title: " title)]
        (slider bmi-data :title title 0 100)]

       [:div
        [:span (str "ratio: " (cljs.pprint/cl-format nil "~,3f" bmi) " ")]
        [:span {:style {:color color}} diagnose]
        (slider bmi-data :bmi bmi 10 50)]
       [:div
        [:span (str "html:")]
        (htmlout bmi-data :yurl yurl width height 10 50 length title)]

       [:div
        [:span (str "preview:")]
        (htmloutvisual bmi-data :yurl yurl width height 10 50 length title)]])))



(defcard Vimeo
         ;"see [devcards](https://github.com/bhauman/devcards) for deets"
         (fn [data-atom _] (bmi-component data-atom))
         {:height 360 :width 640 :yurl "https://vimeo.com/10570139" :length "00:22" :title "“The Last 3 Minutes” Directed by Po Chan"}
         {:inspect-data false
          :frame        true
          :history      true
          :heading      true})


#_(defcard
    example-counter
    (fn [data-atom owner]
      (sab/html
        [:h3
         "Example Counter w/Shared Initial Atom: "
         (:initlength @data-atom)]))
    initial-length)

;(.log js/console (:initlength @initial-length))
; aiming for Duration.parse(duration).getSeconds()
;(.log js/console  (co/to-long (:initlength @initial-length)) )
;(.log js/console (def intervalobj (Interval.fromIsoString (:initlength @initial-length)) )  )
;(.log js/console  (.toIsoString intervalobj)  )
;(.log js/console   intervalobj.minutes  )

(defn main []
  ;; conditionally start the app based on wether the #main-app-area
  ;; node is on the page
  (if-let [node (.getElementById js/document "main-app-area")]
    (js/React.render (sab/html [:div ""]) node)))




(main)

;; remember to run rlwrap lein figwheel and then browse to
;; http://localhost:3449/cards.html

;; api to youtube to return duration... be sure to supply a key
;; https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id=lm8oxC24QZc&fields=items%2FcontentDetails%2Fduration&key=
;; {"items" :
;;  [{"contentDetails" :
;;     {"duration" : "PT3M57S" } } ] }
