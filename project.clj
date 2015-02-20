(defproject threed "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "target/generated/clj" "target/generated/cljx"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2816" :scope "provided"]
                 [com.taoensso/encore "1.21.0"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.2"]
                 [compojure "1.3.1"]
                 [com.cemerick/piggieback "0.1.5"]


                 [compojure "1.2.0"]
                 [enlive "1.1.5"]
                 [sablono "0.3.4"]
                 [environ "1.0.0"]

                 [com.taoensso/carmine "2.9.0"]
                 [ring-middleware-format "0.4.0"]
                 [ring/ring-json "0.3.1"]

                 [fogus/ring-edn "0.2.0"]
                 [om-sync "0.1.1"]
                 [http-kit "2.1.19"]

                 [figwheel "0.1.6-SNAPSHOT"]

                 [leiningen "2.5.0"]
                 [om "0.8.0-rc1"]
                 [ring "1.3.1"]
                 [weasel "0.6.0-SNAPSHOT"]

                 ]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-environ "1.0.0"]
            [com.keminglabs/cljx "0.4.0" :exclusions [org.clojure/clojure]]]

  :min-lein-version "2.5.0"

  :uberjar-name "threed.jar"

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "target/generated/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :source-map    "resources/public/js/out.js.map"
                                        ;; :preamble      ["react/react.min.js"]
                                        ;; :externs       ["react/externs/react.js"]
                                        :preamble      ["public/js/threejs/three.min.js"]
                                        :externs       ["public/js/threejs/three.js"]
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}]}

  :profiles { ;; :dev {:repl-options {:init-ns threed.server
             ;;                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl
             ;;                                         cljx.repl-middleware/wrap-cljx]}

             ;;       :plugins [[lein-figwheel "0.1.4-SNAPSHOT"]
             ;;                 ]

             ;;       :figwheel {:http-server-root "public"
             ;;                  :port 3449
             ;;                  :css-dirs ["resources/public/css"]}

             ;;       :env {:is-dev true}

             ;;       :hooks [cljx.hooks]

             ;;       :cljx {:builds [{:source-paths ["src/cljx"]
             ;;                        :output-path "target/generated/clj"
             ;;                        :rules :clj}
             ;;                       {:source-paths ["src/cljx"]
             ;;                        :output-path "target/generated/cljs"
             ;;                        :rules :cljs}]}

             ;;       :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]}}}}

             :production {:hooks [cljx.hooks leiningen.cljsbuild]
                          :env {:production true}
                          :omit-source true
                          :aot :all
                          :cljsbuild {:builds {:app
                                               {:source-paths ["env/prod/cljs"]
                                                :compiler
                                                {
                                                 :closure-warnings {:externs-validation :off
                                                                    :non-standard-jsdoc :off}
                                                 :optimizations :none
                                                 :pretty-print false}}}}}

             :uberjar {:hooks [cljx.hooks leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {
                                              :closure-warnings {:externs-validation :off
                                                                 :non-standard-jsdoc :off}
                                              :optimizations :advanced
                                              :pretty-print false}}}}}})
