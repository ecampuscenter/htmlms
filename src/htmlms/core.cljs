(ns htmlms.core
  (:require
    #_[om.core :as om :include-macros true]
    [reagent.core :as r]
    [sablono.core :as sab :include-macros true]
    [clojure.string :as cs]
    ; for hostedcards builid see devcards as a standalone website https://github.com/bhauman/devcards
    ; [devcards.core :as dc]
    )
  (:require-macros
    ; mal figwheel see above hostedcards
    [devcards.core :as dc :refer [defcard deftest]]
    ; mal hostedcards build alternatively swapp commenting on these two requries and above
    ; [devcards.core :refer [defcard]]
    ))

(enable-console-print!)

; mal figwheel for hostedcards builid see devcards as a standalone website https://github.com/bhauman/devcards
; (devcards.core/start-devcard-ui!)

(defonce first-example-state (atom {:yourl "https://youtube.com/watch?v=2FpW1ctrDHE"}))


(defcard
  "*BlackBoard HTML Generator*")

; from http://stackoverflow.com/questions/32386047/greatest-common-divisor-in-clojure
; for some reason the recur answer blows up if the height is empty in the web form.
(def gcd (fn [a b] (->> (map (fn [x]
                               (filter #(zero? (mod x %)) (range 1 (inc x))))
                             [a b])
                        (map set)
                        (apply clojure.set/intersection)
                        (apply max))))

(defn calc-bmi [bmi-data]
  (let [{:keys [height width bmi yurl] :as data} bmi-data
        h (/ height 100)]
    (if (nil? bmi)
      (assoc data :bmi (/ (/ width (gcd width height) (/ height (gcd width height)))))
      (assoc data :width (* bmi h h)))
    ;   (assoc data :yurl yurl)
    )

  )

(defn slider [bmi-data param value min max]
  (sab/html
    [:input {:type      "text" :value value :min min :max max
             :style     {:width "100%"}
             :on-change (fn [e]
                          (swap! bmi-data assoc param (.-target.value e))
                          (when (not= param :bmi)
                            (swap! bmi-data assoc :bmi nil)
                            )
                          )}]))


(defn ifriendly [url]
  "create iframible youtube link for display http://stackoverflow.com/questions/20498831/refused-to-display-in-a-frame-because-it-set-x-frame-options-to-sameorigin"
  (cs/replace-first (cs/replace-first url "watch?v=" "embed/") "https:" "")
  )

(defn fluff [skinny width height length]
  (str "<p>Click the <strong>Play</strong> icon to begin.</p>
<p><iframe width=\"" width "\" height=\"" height "\" src=\"" (ifriendly skinny) "\" frameBorder=\"0\" allowfullscreen></iframe></p>
<p>If video doesn't appear, follow this direct link:
<a href=\"" skinny "\" title=\"Video\" target=\"_blank\">"
       skinny "</a> (" length ")</p><p>To display video captions, start video and click <strong>CC</strong> in the video frame. To expand the video, use direct link above to open video in YouTube.</p>
")
  )


(defn htmlout [bmi-data param value width height min max length]
  (sab/html

    [:textarea {:cols      max
                :rows      min
                :value     (fluff value width height length)
                :style     {:width "100%"}
                :on-change (fn [e] (swap! bmi-data assoc param (.-target.value e))
                             (when (not= param :bmi)
                               (swap! bmi-data assoc :bmi nil)
                               )
                             )}]
    ))


(defn htmloutvisual [bmi-data param value width height min max length]
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
                                    (swap! bmi-data assoc :bmi nil)
                                    )
                                  )}]
     [:p {:style {:font-size ".8em"}} "If video doesn't appear, follow this direct link: "
      [:a {:href   value
           :title  "Video"
           :target "_blank"
           } value] " (" length ")"
      ]
     [:p {:style {:font-size ".8em"}} "To display video captions, start video and click " [:strong "CC"] " in the video
     frame. To expand the video, use direct link above to open video in YouTube."]
     ]
    ))

(defn height-ratio [w h]
  (/ h (gcd w h))
  )

(defn width-ratio [w h]
  (/ w (gcd w h))
  )

(defn bmi-component [bmi-data]
  (let [{:keys [width height bmi yurl length]} (calc-bmi @bmi-data)
        [color diagnose] (cond
                           ;(and (> bmi 0) (< bmi 1)) ["green" (str "approx ratio: 16:9. exact ratio:")]
                           (and (> bmi .562) (< bmi .563)) ["green" (str "approx ratio: 16:9. exact ratio: " (width-ratio width height)  " by " (height-ratio width height) ".")]
                           (and (> bmi .74) (< bmi .76)) ["inherit" (str "approx ratio: 4:3. exact ratio: " (width-ratio width height) " by " (height-ratio width height) ".")]
                           ; (< bmi 30) ["orange" "overweight"]
                           :else ["red" (str "non-standard ratio " (width-ratio width height)  " by " (height-ratio width height) ".") ])]
    (sab/html
      [:div
       [:h3 "Parameters"]
       [:div
        [:span (str "url: " yurl)]
        (slider bmi-data :yurl yurl 0 100)]
       [:div
        [:span (str "time: " length)]
        (slider bmi-data :length length 0 100)]
       [:div
        [:span (str "width: " (int width) "px")]
        (slider bmi-data :width width 30 150)]
       [:div
        [:span (str "height: " (int height) "px")]
        (slider bmi-data :height height 100 220)]
       [:div
        [:span (str "ratio: " (cljs.pprint/cl-format nil "~,3f" bmi) " ")]
        [:span {:style {:color color}} diagnose]
        (slider bmi-data :bmi bmi 10 50)]
       [:div
        [:span (str "html")]
        (htmlout bmi-data :yurl yurl width height 10 50 length)
        ]
       [:div
        [:span (str "preview")]
        (htmloutvisual bmi-data :yurl yurl width height 10 50 length)
        ]
       ])))

(defcard YouTube
         ;"see [devcards](https://github.com/bhauman/devcards) for deets"
         (fn [data-atom _] (bmi-component data-atom))
         {:height 360 :width 640 :yurl "https://youtube.com/watch?v=2FpW1ctrDHE" :length "3m 45s"}
         {:inspect-data true
          :frame        true
          :history      true
          :heading      true
          })



(defn main []
  ;; conditionally start the app based on wether the #main-app-area
  ;; node is on the page
  (if-let [node (.getElementById js/document "main-app-area")]
    (js/React.render (sab/html [:div ""]) node)
    ))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html

;; api to youtube to return duration... be sure to supply a key
;; https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id=lm8oxC24QZc&fields=items%2FcontentDetails%2Fduration&key=
;; {"items" :
;;  [{"contentDetails" :
;;     {"duration" : "PT3M57S" } } ] }
