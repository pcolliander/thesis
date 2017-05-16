(import 
  (java.util UUID)
  (java.util.concurrent Executors)
  (marmot.morph.cmd Trainer)
  (marmot.morph.cmd Annotator)
)

(use 'clojure.java.io)
(require '[clojure.string :as str])

(def annotatedCounter (agent 0))
(def finishedCounter (agent 0))

(def annotatedSentences (ref {}))
(def corroboratedSentences (ref {}))

(def retryCounter (atom 0))
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
    )
  )
  
  (dosync 
    (if (contains? @annotatedSentences id)
      (do
        (if (> (get-in @annotatedSentences [id :counter]) 5)
          (do 
            (swap! retryCounter inc)
            (alter corroboratedSentences conj {id (get @annotatedSentences id)})
            (alter annotatedSentences dissoc annotatedSentences id)
            (send annotatedCounter dec)
            (send finishedCounter inc))
        )
      )
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

;; -----------------------------------------------------------------------------------------
;; Read in texts from annotated source
;; -----------------------------------------------------------------------------------------

(def sentence "")
(def words [])
(def tags [])
(require '[clojure.string :as str])
(defn readTxtFile []
  (with-open [rdr (reader "en-ud-train.conllu")]
    (doseq [line (line-seq rdr)]
      (if (.contains line "#") (def sentence line))

      (if (str/blank? line) 
        (do 
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

;; read from file
;; upvote 10

(defn serialize [m sep] (apply str (concat (interpose sep (vals m)) ["\n"])))

(defn upvoteSimulationTask []
  (def i 0)
  (doseq [k (keys @annotatedSentences)] 
    (if (< i 10) 
      (do
        (upvote k)
        (def i (+ i 1))
      )
    )
  )
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
  (time (Trainer/main (into-array String ["-train-file","form-index=1,tag-index=2,trainedModel.conll", "-tag-morph", "false", "-model-file", "fromClojure.marmot"])))
  (prn "model trained")
)

;; (trainModel)
;; ---------
;; Run Model
;; ---------
(defn runModel []
  (prn "running model")
  (time (Annotator/main (into-array String ["--model-file", "fromClojure.marmot", "--test-file", "form-index=1,en-ud-test.conll",  "--pred-file", "taggedFile" ])))
  (prn "model run")
)

(defn upvoteSimulationMultipleUsers [nthreads]
  (def pool (Executors/newFixedThreadPool nthreads))

  (time (while (> @annotatedCounter 0) 
    (.execute pool upvoteSimulationTask) ))
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
      "u" (time ((upvoteSimulationMultipleUsers 50) (prn retryCounter) (runProgram)))
      "o" ((outputTxtFile) (runProgram))
      "train" ((trainModel) (runProgram))
      "run" ((runModel) (runProgram))
      "default"
    )
  )
)
           
(runProgram)

