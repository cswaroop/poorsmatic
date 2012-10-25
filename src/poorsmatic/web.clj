(ns poorsmatic.web
  (:use [hiccup core form element middleware]
        [compojure.core :only [GET POST defroutes]]
        [compojure.handler :only [site]]
        [ring.util.response :only [redirect]])
  (:require [poorsmatic.models :as model]
            [poorsmatic.config :as config]
            [immutant.web :as web]
            [immutant.xa :as xa]))

(defn home
  []
  (html
   [:h2 "Poorsmatic"]
   (form-to [:post "/add"]
            (label "term" "Search term: ")
            (text-field "term")
            (submit-button "add"))
   [:ul
    (for [term (model/get-all-terms)]
      [:li
       [:span.term term]
       (form-to [:post (str "/delete/" term)] (submit-button "delete"))
       [:table
        (for [[url title count] (model/find-urls-by-term term)]
          [:tr [:td count] [:td (link-to url (or title url))]])]])]))

(defn add
  [term]
  (xa/transaction
   (model/add-term term)
   (config/notify)))

(defn delete
  [term]
  (xa/transaction
   (model/delete-term term)
   (config/notify)))

(defroutes routes
  (GET "/" [] (home))
  (POST "/add" [term] (add term) (redirect "."))
  (POST "/delete/:term" [term] (delete term) (redirect "..")))
(def app (-> (site routes) (wrap-base-url)))

(defn start [] (web/start #'app :reload true))
(defn stop [] (web/stop))

