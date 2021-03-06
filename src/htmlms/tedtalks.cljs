(ns htmlms.tedtalks
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
  (.log js/console (str "47: u url: " u))
  "given a TED URL return the video’s ID"
  ; youtube
  ;(get (:query (cu/url u)) "v")
  ; ted - for now just return original url... this might change if an API key is used.
  u)

(println (get-id-from-url "https://www.youtube.com/watch?v=Wfj4g8zh2gk"))

(def r (t/reader :json))

; courtesy dr. nolan - not used atm but came in handy during dev
(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defcard
  "*BlackBoard HTML Generator* \n
  This application has been developed using the TED API and is not an official service of TED")

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

;(defn xhr-data [url cb]
; "https://www.ted.com/talks/elizabeth_lesser_take_the_other_to_lunch"
(defn xhr-data [url cb]
  (XhrIo.send (str url)
              (fn [f]
                (let [xhr (.-target f)]
                  (cb (.getResponseText xhr))))))

; till i can figure out cross origin policy, close chrome and start chrome like this:
; /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --disable-web-security --user-data-dir=/Volumes/SamsungSSD/Users/michaellopez/chromeuserdata
; also allow flash on [*.]localhost
; alternatively load this chrome plugin https://chrome.google.com/webstore/detail/allow-control-allow-origi/nlfbmbojpeacfghkpbjhddihlkkiljbi
; and addd exemptions for "*://www.ted.com/*" without the quotes. "*://localhost/* for testing...
; use this xhr-data-ted to look at the content returned in case you need to update the length or title e.g. if ted modifies their site
(defn xhr-data-ted [url content]
   (.log js/console (str "url: " url))
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
                          (.persist e)
                          (swap! bmi-data assoc param (.-target.value e))
                          ; also swap out new video length
                          (if (= param :yurl)
                            ;(xhr-data (str (get-id-from-url (.-target.value e)))
                            ; for testing
                            ; (xhr-data-ted (str "https://www.ted.com/talks/elizabeth_lesser_take_the_other_to_lunch")
                            (xhr-data (str (.-target.value e))
                                      (fn [g]
                                        ; (.log js/console  (domi/html-to-dom g))
                                        (.log js/console "testing time")
                                        (def lengthdom (domi/attr (x/xpath g "//*[@id=\"shoji\"]/div[2]/div/div[2]/div/meta[3]") "content"))
                                        (.log js/console lengthdom)

                                        ; works for https://www.ted.com/talks/elizabeth_lesser_take_the_other_to_lunch
                                        ; not for https://www.ted.com/talks/teenaged_boy_wonders_play_bluegrass
                                        ; the index of g shifted to 217 from 207
                                       ; (def targetdom  (aget (.-childNodes (aget (domi/html-to-dom g) 207)) 3))
                                       ; (def targetdom2 (aget (.-childNodes targetdom) 1))
                                       ; (def targetdom3 (aget (.-childNodes targetdom2) 9))
                                       ; (def targetdom4 (aget (.-childNodes targetdom3) 3))
                                       ; (def targetdom5 (aget (.-childNodes targetdom4) 7))
                                       ; (.log js/console (.-content targetdom5))

                                        (let [response (.-target g)
                                              ;updlength (domi/text (x/xpath g "//*[@id=\"player-hero\"]/div[1]/div[2]/div/span[1]"))
                                              ;updtitle (domi/text (x/xpath g "//*[@id=\"player-hero\"]/div[1]/div[2]/h1/div[2]/span"))]

                                              ; as of 20170907
                                              ; test
                                              ;updlength (domi/text "updlen")
                                              ; prod
                                              ; updlength (domi/text (x/xpath g "//*[@id=\"shoji\"]/div[2]/div/div[2]/div/meta[3]"))
                                              ;updlength (.-content targetdom5)
                                              updlength (str lengthdom)
                                              ; test
                                              ; updtitle (domi/text "updtitle")]
                                              ; prod
                                              ; updtitle (cs/replace-first (domi/text (x/xpath g "//title")) " | TED Talk" "")]
                                              updtitle (domi/attr (x/xpath g "//*[@id=\"shoji\"]/div[2]/div/div[2]/div/meta[1]") "content")]




                                          ;(.log js/console  "------you are starting here------")
                                             ; (.log js/console (domi/text (x/xpath g "//title")))
                                             ; (.log js/console (domi/text (x/xpath g "//meta[@property=\"video:duration\"]/@content")))
                                             ;(.log js/console  (x/xpath g "//meta[@property=\"video:duration\"]/@content"))
                                             ;(.log js/console  (x/xpath g "/html"))
                                             ; (.log js/console  (domi/html-to-dom g))
                                             ; (.log js/console  (aget (domi/html-to-dom g) 109))
                                          ;(.log js/console (.-content (aget (domi/html-to-dom g) 109)))
                                          ;(.log js/console (.-childNodes (aget (domi/html-to-dom g) 207)))

                                          ;(.log js/console (aget (.-childNodes (aget (domi/html-to-dom g) 207)) 3))
                                          ;(def targetdom  (aget (.-childNodes (aget (domi/html-to-dom g) 207)) 3))
                                              ;(.log js/console (domi/html-to-dom (aget (.-childNodes (aget (domi/html-to-dom g) 207)) 3)))
                                          ;(.log js/console (str (.-childNodes targetdom)))
                                          ;(def targetdom2  (aget (.-childNodes targetdom ) 1))
                                          ;(.log js/console (.-childNodes targetdom2))
                                          ;(def targetdom3  (aget (.-childNodes targetdom2) 9))
                                          ;(.log js/console (.-childNodes targetdom3))
                                          ;(def targetdom4 (aget (.-childNodes targetdom3) 3))
                                          ;(.log js/console (.-childNodes targetdom4))
                                          ;(def targetdom5 (aget (.-childNodes targetdom4) 7))
                                          ;(.log js/console (.-content targetdom5))

                                          ;(.log js/console (.-childNodes (aget (domi/html-to-dom g) 207 )) 3)

                                          ;(.log js/console (aget (.-childNodes (aget (domi/html-to-dom g) 207 )) 3) 3)
                                          ;(.log js/console (aget (.-childNodes (aget (domi/html-to-dom g) 207 )) 3) 3)
                                          ;(.log js/console (aget (.-childNodes (aget (domi/html-to-dom g) 207)) 3) 1 2)
                                          ; (.log js/console (.-childNodes (.-childNodes (.-childNodes (.-childNodes (.-childNodes (aget (domi/html-to-dom g) 207)) 3) 1) 9) 3) 7)
                                          ;(.log js/console (domi/text (x/xpath g "//*[@id=\"shoji\"]/div[2]/div/div[2]/div/meta[3]/@content")))

                                          ; (.log js/console (first (domi/text (x/xpath g "//title"))) (rest (domi/text (x/xpath g "//title"))))
                                          ; (.log js/console (domi/attr (x/xpath g "/html/head/meta[9]") "content"))
                                          ; (.log js/console (first (domi/text (x/xpath g "//title"))) (rest (domi/text (x/xpath g "//title"))))
                                          ;(.log js/console (domi/text (x/xpath g "//*[@id=\"shoji\"]/div[2]/div/div[2]/div/meta[itemprop=\"duration\"]")))
                                          ;(.log js/console  "------you are finishing here------")

                                          ;(println  (domi/text (x/xpath g "/html/head")))

                                          ; TED talk html
                                          ;(.log js/console (str "g: " response))

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
                                          (swap! bmi-data assoc :length (let [me (Interval.fromIsoString updlength)] (str (if (> me.hours 0) (str me.hours "h ") ) me.minutes "m " me.seconds "s")))
                                          ;(swap! bmi-data assoc :length updlength)
                                          (swap! initial-length assoc :initlength updlength)
                                          (println ":initlength: " (:initlength @initial-length))

                                          ; title
                                          (swap! bmi-data assoc :title updtitle)

                                          ; (println "response: " response)
                                          ; (println "url: " value)
                                          (println "can i get a new url? " (.-target.value e))
                                          ))))
                          (println "initial-length: " initial-length)
                          (when (not= param :bmi)
                            (println (str "param:" param))
                            (swap! bmi-data assoc :bmi nil)))}]))


(defn ifriendly [url]
  "create iframible ted link"
  (.log js/console (str "ifriendlyurl: " url))
  (cs/replace-first (cs/replace-first url "www.ted.com/talks" "embed-ssl.ted.com/talks") "http:" "https:"))


(defn fluff [skinny width height length title]
  (str "<p>Click the <strong>Play</strong> icon to begin.</p>
<p><iframe width=\"" width "\" height=\"" height "\" src=\"" (ifriendly skinny) "\" frameBorder=\"0\" scrolling=\"no\" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe></p>
<p>If video doesn't appear, follow this direct link:
<a href=\"" (cs/replace-first skinny "http:" "https:") "\" title=\"" title "\" target=\"_blank\">"
title "</a> (" length ")</p><p>Start the video to access more options in the video frame. To display the video captions, click on the <strong>gray speech bubble</strong> with three dots in the center and choose the language you want the captions to be displayed in. To expand the video, use the <strong>Full Screen</strong> icon in the bottom right-hand corner or use the direct link above to open the video on the TED website. To navigate the video using the transcript, click <strong>Interactive Transcript</strong>.</p>
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



; todo can i take the ?rel=0 out -- done

(defn htmloutvisual [bmi-data param value width height min max length title]
  (sab/html
    [:div
     [:p {:style {:font-size ".8em"}} "Click the "
      [:strong "Play"] " icon to begin."]
     [:iframe {:width           width
               :height          height
               :src             (ifriendly (str value))
               :frameBorder     0
               :allowFullScreen nil
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
     click on the " [:strong "gray speech bubble"] " with three dots in the center and choose the language you want the captions to be displayed in. To expand the video, use the " [:strong "Full Screen"] " icon in the bottom right-hand corner or use the direct link above to open the video on the TED website.
     To navigate the video using the transcript, click " [:strong "Interactive Transcript"] "."]]))



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
        [:span (str "url: " (cs/replace-first yurl "http:" "https:"))]
        (slider bmi-data :yurl (cs/replace-first yurl "http:" "https:") 0 100)]
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



(defcard ©TED-CC-BY-NC-ND-3.0
         ;"see [devcards](https://github.com/bhauman/devcards) for deets"
         (fn [data-atom _] (bmi-component data-atom))
         {:height 315 :width 560 :yurl "https://www.ted.com/talks/teenaged_boy_wonders_play_bluegrass" :length "4m 55s" :title "Sleep Man Banjo Boys: Teen wonders play bluegrass"}
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
    (js/ReactDOM.render (sab/html [:div ""]) node)))




(main)

;; remember to run rlwrap lein figwheel and then browse to
;; http://localhost:3449/cards.html

;; api to youtube to return duration... be sure to supply a key
;; https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id=lm8oxC24QZc&fields=items%2FcontentDetails%2Fduration&key=
;; {"items" :
;;  [{"contentDetails" :
;;     {"duration" : "PT3M57S" } } ] }
