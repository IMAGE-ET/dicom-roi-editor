;
; Copyright (c) Dilvan A. Moreira 2016. All rights reserved.
;
;  This file is part of ePAD2.
;
;  ePAD2 is free software: you can redistribute it and/or modify
;  it under the terms of the GNU General Public License as published by
;  the Free Software Foundation, either version 3 of the License, or
;  (at your option) any later version.
;
;  ePAD2 is distributed in the hope that it will be useful,
;  but WITHOUT ANY WARRANTY; without even the implied warranty of
;  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;  GNU General Public License for more details.
;
;  You should have received a copy of the GNU General Public License
;  along with ePAD2.  If not, see <http://www.gnu.org/licenses/>.
;

(ns roiEditor.core
  (:require [reagent.core :refer [render]]
            [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [roiEditor.base :refer [dispatch-canvas-event]]
            [roiEditor.tools]
            [ajax.core :refer [GET]]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

;; -- View Components ---------------------------------------------------------

;(def base-URL "1.2.826.0.1.3680043.8.420.29267207592271555902603369361594637742/series/1.2.826.0.1.3680043.8.420.13029244630897359628709378005929429184/images/");

;(def base-URL-server "http://localhost:8080")
;(def base-URL-server "http://localhost:9080")
(def base-URL-server "http://localhost:3000")


(defn toolbar-cmp [mode disp-fn]
  (let [button (fn [icon tooltip selected event]
                 [:div {:title tooltip}
                  [ui/flat-button {:icon             icon
                                   :background-color (if selected "#d0d060" (color :background-color))
                                   :style            {:margin    "12px 0px"
                                                      :min-width "35px"}
                                   :on-touch-tap     (fn [] (disp-fn event))}]])]
      [ui/toolbar
       [ui/toolbar-group ;{:first-child true}
        [ui/toolbar-title {:text "Tools"}]
        (button (ic/action-search) "zoom" (= mode :zoom) [:change-mode :zoom])
        (button (ic/maps-my-location) "scroll" (= mode :scroll) [:change-mode :scroll])
        (button (ic/image-brightness-6) "windowing" (= mode :gradient) [:change-mode :gradient])
        (button (ic/action-pan-tool) "move" (= mode :move) [:change-mode :move])
        (button (ic/action-polymer) "3D Edit" (= mode :3D) [:change-mode :3D])]
       [ui/toolbar-group ;{:last-child true}
        ;[ui/toolbar-separator]
        (button (ic/communication-call-made) "move +1" false [:inc])
        (button (ic/communication-call-received) "move -1" false [:dec])]]))

(defn toolbar []
  (let [mode (subscribe [:change-mode])
        disp-fn #(dispatch %1)]
    (fn [] [:div (toolbar-cmp @mode disp-fn)])))

(defn dicom-editor []
  (let [views (subscribe [:canvas-event])
        editor-id "editor0"]
    (fn []
      ;(js/alert (js/JSON.stringify (clj->js (@views :pngs))))
      [:div
       [:dicom-roi-editor
                {;:is "dicom-roi-editor"
                 :id editor-id
                 :prefs  (js/JSON.stringify (clj->js (@views :prefs)))
                 :series (js/JSON.stringify (clj->js (@views :series)))
                 :pngs   (js/JSON.stringify (clj->js (@views :pngs)))
                 :baseurl base-URL-server
                 :activeplane (@views :active-plane)
                 :windowingcenter (@views :windowing-center)
                 :windowingwidth (@views :windowing-width)
                 :axial    (js/JSON.stringify (clj->js (@views :axial)))
                 :sagittal (js/JSON.stringify (clj->js (@views :sagittal)))
                 :frontal  (js/JSON.stringify (clj->js (@views :frontal)))
                 :sphere   (js/JSON.stringify (clj->js (@views :sphere)))
                 :on-mouse-down   (dispatch-canvas-event editor-id)
                 :on-mouse-up     (dispatch-canvas-event editor-id)
                 :on-mouse-move   (dispatch-canvas-event editor-id)
                 :on-mouse-out    (dispatch-canvas-event editor-id)
                 :on-double-click (dispatch-canvas-event editor-id)
                 :on-key-down     (dispatch-canvas-event editor-id)
                 :on-wheel        (dispatch-canvas-event editor-id)}]])))

(defn get-patients []
  (GET (str base-URL-server "/allSeries") {:handler #(dispatch [:read-patients [%]])})) ;:keywords? true}))

(defn get-series [seriesId]
  (GET (str base-URL-server "/objects") {:params {:serieId seriesId}
                                         :handler #(dispatch [:open-series [%]])}))

(defn menu-dicom []
  (let [patients (subscribe [:read-patients])]
    (fn []
      [ui/menu {:key "1"}
        (for [serie @patients]
          [ui/menu-item {:key (str (serie "series_iuid"))
                         :primary-text (str (serie "series_iuid"))
                         :on-touch-tap #(get-series (str (serie "series_iuid")))}])])))

(defn make-tabs [views]
  (map (fn [x] [ui/tab {:key (first x) :label (first x) :value "1"}
                [:div {:class "editor-holder"}
                  [toolbar]
                  [dicom-editor]]])
       views))

(defn patients-tabs []
  (let [db (subscribe [:open-series])]
    (fn []
      [ui/tabs
         (conj (make-tabs (:views @db))
           [ui/tab {:label "Menu" :value "0"}
            [:div
             [menu-dicom]]])])))

(defn simple-example []
  [ui/mui-theme-provider {:mui-theme (get-mui-theme
                                       {:palette {:text-color (color :blue800)}})}
   [:div
    [ui/app-bar {:title                 "ePAD2"
                 :icon-class-name-right "muidocs-icon-navigation-expand-more"}]
    [ui/paper
     [:div
      [patients-tabs]]]]]);[dicom-editor editor-id]



;; -- Entry Point -------------------------------------------------------------


(defn dev-setup []
  ;(when config/debug?
    (enable-console-print!)
    (println "dev mode"))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [simple-example]
                  (.getElementById js/document "app")))

(defn ^:export run []
  (dispatch-sync [:initialize])
  (get-patients)
  (dev-setup)
  (mount-root))

