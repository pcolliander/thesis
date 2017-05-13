(import 
  (java.util UUID)
  (java.util.concurrent Executors)
  (marmot.morph.cmd Trainer)
  (marmot.morph.cmd Annotator)

)


(use 'clojure.java.io)
(require '[clojure.string :as str])



(def annotationSymbols [ "ADJ", "ADP", "ADV", "AUX", "CONJ","DET", "NOUN", "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "VERB", "X" ])

(def annotatedCounter (agent 0))
(def finishedCounter (agent 0))

(def annotatedSentences (ref {}))
(def corroboratedSentences (ref {}))

;; -----------------
;; Utility Functions
;; -----------------
(defn pm [] 
  (prn "annotatedCounter " @annotatedCounter)
  (prn "corrboratedCounter " @finishedCounter)
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
(defn addSentence [sentence words tags]
  (dosync 
    (alter annotatedSentences conj {(keyword (str (UUID/randomUUID))) {:counter 0, :sentence sentence :words words :tags tags}})
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

(def sentence "")
(def words [])
(def tags [])
(require '[clojure.string :as str])
(defn readTxtFile []
  (with-open [rdr (reader "en-ud-train.conllu")]
    (doseq [line (line-seq rdr)]
      (if (.contains line "#") (def sentence line))

      (if (str/blank? line) (do 
        (addSentence sentence words tags)
        (def words []) (def tags [])
        )
      )

      (if (not (str/blank? line)) 
        (if (not (.contains line "#") )
          (do 
            (def sent (str/split line #"\t"))
            (def words (conj words (sent 1)))
            (def tags (conj tags (sent 3)))
          )
        )
      )
    )
    (prn "finished reading txt file.")
  )
)

;; -----------
;; Upvote Simulation
;; -----------

(defn upvoteSimulationTask []
  ;; (prn "starting upvoting.")

  ;; (while (> @annotatedCounter 0) 
    (doseq [k (keys @annotatedSentences)] (upvote k))
  ;; )
)

;; -----------
;; Output Textfile
;; -----------
(defn outputTxtFile []
  (spit "trainedModel.conll" "")
  (doseq [[key val] @corroboratedSentences] 
    (println) 
    (println) 
    
    (def i 0)
    (doseq [x (map vector (get val :words) (get val :tags))] 
      (spit "trainedModel.conll" (str i \tab (nth x 0) \tab (nth x 1) \newline) :append true)
      (def i (+ i 1))
    )
    (spit "trainedModel.conll" \newline :append true)
  )
)
;; -----------
;; Annotate with the trained Model
;; -----------
(defn trainModel []
  (prn "training model")
  (Trainer/main (into-array String ["-train-file","form-index=1,tag-index=2,trainedModel.conll", "-tag-morph", "false", "-model-file", "fromClojure.marmot"]))
  (prn "model trained")
)

;; (trainModel)
;; ---------
;; Run Model
;; ---------
(defn runModel []
  (prn "running model")
  (Annotator/main (into-array String ["--model-file", "fromClojure.marmot", "--test-file", "form-index=1,en-ud-test.conll",  "--pred-file", "taggedFile" ]))
  (prn "model run")
)

(defn upvoteSimulationMultipleUsers [nthreads]
  (def pool (Executors/newFixedThreadPool nthreads))

  (while (> @annotatedCounter 0) 
    (.execute pool upvoteSimulationTask) )
  (print pool)
  (.shutdown pool)
  (pm)
)

(defn runProgram [] 
  (println)
  (prn "a = do all")
  (prn "r = read input and map to dict")
  (prn "u = upvote simulation task")
  (prn "o = output txt file.")
  (prn "train  = train model.")
  (prn "run  = run model.")

  (let [input (read-line)]
    (case input
      "a" (do 
            (readTxtFile)
            (upvoteSimulationMultipleUsers 50)
            (outputTxtFile)
            (trainModel)
            (runModel)
            (runProgram)
          )

      "r" ((readTxtFile) (runProgram))
      "u" ((upvoteSimulationMultipleUsers 50) (runProgram))
      "o" ((outputTxtFile) (runProgram))
      "train" ((trainModel) (runProgram))
      "run" ((runModel) (runProgram))
      "default"
    )
  )
)
           
(runProgram)

