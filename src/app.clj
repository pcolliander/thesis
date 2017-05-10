(def annotationSymbols [ "ADJ", "ADP", "ADV", "AUX", "CONJ","DET", "NOUN", "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "VERB", "X" ])

(import 
  (java.util UUID)
  (marmot.morph.cmd Trainer)
  (marmot.morph.cmd Annotator)
)

(require '[clojure.string :as str])
(use 'clojure.java.io)

(def annotatedCounter (agent 0))
(def finishedCounter (agent 0))

;; Main refs (are updated in transactions)
(def annotatedSentences (ref {}))
(def corroboratedSentences (ref {}))

;; -----------------
;; Utility Functions
;; -----------------
(defn pm [] 
  ;; (prn "sentencesCounter " @sentencesCounter)
  (prn "annotatedCounter " @annotatedCounter)
  (prn "finished: " @finishedCounter)
  ;; (prn "sentences: " @sentences)
  ;; (prn "annotatedSentences " @annotatedSentences)
  ;; (prn "corroboratedSentences: " @corroboratedSentences)
)

;; ------
;; Upvote
;; ------
(defn upvote [id] 
  (dosync
    (if (contains? @annotatedSentences id)
      (alter annotatedSentences update-in [id :counter] inc)
      "false"
    )
  )
  
  (dosync 
    (if (contains? @annotatedSentences id)
      (do
        (if (> (get-in @annotatedSentences [id :counter]) 3)
          (do 
            (alter corroboratedSentences conj {id (get @annotatedSentences id)})
            (alter annotatedSentences dissoc annotatedSentences id)
            (send annotatedCounter dec)
            (send finishedCounter inc))
        ;; else? "counter not big enough"
        )
      )
      ;; else? "false 2"
    )
  )
)

;; ----------------------
;; Add annotatedSentences
;; ----------------------
(defn addSentence [sentence]
  (dosync 
    (alter annotatedSentences conj {(keyword (str (UUID/randomUUID))) {:counter 0, :sentence sentence}})
    (send annotatedCounter inc)
  )
)

;; -----------------
;; Get functions (sentences in different states)
;; -----------------
;; (defn getAnnotated []
;;   (@annotatedSentences)
;; )
;;
;; (defn getCorroborated []
;;   (@corroboratedSentences)
;; )
;;
;; (defn getSentences []
;;   (@sentences)
;; )

;; -----------
;; TODO 
;; -----------

;; -----------------------------------------------------------------------------------------
;; Read in texts from annotated source (the first time the app starts I guess)
;; -----------------------------------------------------------------------------------------

(def sentence)
(require '[clojure.string :as str])
(defn readTxtFile []
  (with-open [rdr (reader "src/en-ud-train.conllu")]
    (doseq [line (line-seq rdr)]
      ;; (addSentence line)

      (while (.contains line "#") (set! sentence line))

      (prn str/split line "\\t")

    )
  )
  (prn "finished reading txt file.")
)

;; -----------
;; Upvote Simulation
;; -----------

(defn upvoteSimulationTask []
  (prn "starting upvoting.")

  (while (> @annotatedCounter 0) 
    (doseq [k (keys @annotatedSentences)] (upvote k)) ;(prn @annotatedCounter))
  )
)

;; (ereadTxtFile)
;; (upvoteSimulationTask)
;; (pm) 

;; (upvoteSimulation)

;; -----------
;; Output Textfile
;; -----------
(defn outputTxtFile []
  (vals corroboratedSentences)
  ;; (spit "clojure.txt" "Hello Clojure")
)

;; (outputTxtFile)

;; -----------
;; Annotate with the trained Model
;; -----------
(defn trainModel []
  (Trainer/main (into-array String ["-train-file","form-index=1,tag-index=4,en-ud-train.conll", "-tag-morph", "false", "-model-file", "fromClojure.marmot"]))
)

;; ---------
;; Run Model
;; ---------
(defn runModel []
  (Annotator/main (into-array String ["--model-file", "en.marmot", "--test-file", "form-index=1,trainedModel.conll",  "--pred-file", "taggedFile" ]))
)

;; ----
;; Main
;; ----


;; (dotimes [i 10] (.start (Thread. (fn [] (upvote i)))))
;; (dotimes [i 0] (upvote 1))
;; (await a)



