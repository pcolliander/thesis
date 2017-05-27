(import 
  (java.util UUID)
  (java.util.concurrent Executors)
  (marmot.morph.cmd Trainer)
  (marmot.morph.cmd Annotator)
)

(declare outputTxtFile, trainModel, upvote, upvoteSimulationMultipleUsers, upvoteSimulationTask, pm, readTxtFile, runModel)

(use 'clojure.java.io)
(require '[clojure.string :as str])


(set! *warn-on-reflection* true)
(def transactionsCounter (atom 0))
(def annotatedCounter (agent 0))
(def finishedCounter (agent 0))

;; (add-watch finishedCounter nil 
;;  (fn [key atom old-state new-state]
;;    (if (= new-state 12543)
;;      (do 
;;        (outputTxtFile)
;;        (trainModel)
;;      )
;;    )
;;  )
;; )

(def annotatedSentences (ref {}))
(def corroboratedSentences (ref {}))


(defn pm [] 
  (prn "annotatedCounter " @annotatedCounter)
  (prn "corrboratedCounter " @finishedCounter)
  ;; (prn "annotatedSentences " @annotatedSentences)
  ;; (prn "corroboratedSentences: " @corroboratedSentences)
)

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
            (swap! transactionsCounter inc)
            (alter corroboratedSentences conj {id (get @annotatedSentences id)})
            (alter annotatedSentences dissoc annotatedSentences id)
            (send annotatedCounter dec)
            (send finishedCounter inc))
        )
      )
    )
  )
)

(defn addSentence [sentence words tags]
  (dosync 
    (alter annotatedSentences conj {(keyword (str (UUID/randomUUID))) {:counter 0, :sentence sentence :words words :tags tags}})
    (send annotatedCounter inc)
  )
)

;; -----------------------------------------------------------------------------------------
;; Read in texts from annotated source
;; -----------------------------------------------------------------------------------------

(def sentence "")
(def words [])
(def tags [])
(require '[clojure.string :as str])
(defn readTxtFile []
  (with-open [rdr (reader "en-ud-train.conllu")]
    (doseq [^String line (line-seq rdr)]
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

(defn trainModel []
  (prn "training model")
  (time (Trainer/main (into-array String ["-train-file","form-index=1,tag-index=2,trainedModel.conll", "-tag-morph", "false", "-model-file", "fromClojure.marmot"])))
  (prn "model trained")
)

(defn runModel []
  (prn "running model")
  (time (Annotator/main (into-array String ["--model-file", "fromClojure.marmot", "--test-file", "form-index=1,en-ud-test.conll",  "--pred-file", "taggedFile" ])))
  (prn "model run")
)

(defn upvoteSimulationMultipleUsers [nthreads]
  (def pool (^Executors.newFixedThreadPool Executors/newFixedThreadPool nthreads))

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

      "r" (do(time(readTxtFile)) (runProgram))
      "u" (time ((upvoteSimulationMultipleUsers 40) (prn transactionsCounter) (runProgram)))
      "o" ((outputTxtFile) (runProgram))
      "train" ((trainModel) (runProgram))
      "run" ((runModel) (runProgram))
      "default"
    )
  )
)
           
(runProgram)

