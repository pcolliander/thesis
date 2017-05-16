import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.concurrent.Future;
import java.util.*;
import java.io.*;

import java.util.UUID;

import marmot.morph.cmd.Trainer;
import marmot.morph.cmd.Annotator;;

class Application {
  public static void main(String[] args) throws IOException {
    int corruptFeedbackPercentage = 50;
    int nthreads = 50;
    nlp app = new nlp(corruptFeedbackPercentage);

    long startTime;
    long endTime;
    while (true) {
      System.out.println();
      System.out.println("a = do all");
      System.out.println("r = read input and map to dict");
      System.out.println("u = upvote simulation task");
      System.out.println("o = output txt file.");
      System.out.println("train = train model.");
      System.out.println("run = run model.");
      Scanner reader = new Scanner(System.in);  // Reading from System.in
      System.out.print("Enter a command: ");
      String n = reader.nextLine(); // Scans the next token of the input as an int.

      switch (n) {
        case "a" : app.fullSimulation(args[0], nthreads);
               break;
        case "r": app.readTxtFile(args[0]);
               break;
        case "u": 
          startTime = System.currentTimeMillis();
          app.upvoteSimulationMultipleUsers(nthreads);
          endTime = System.currentTimeMillis();
          System.out.println("Total execution time: " + (endTime - startTime) );
          app.printme();
               break;
        case "o": app.outputTxtFile();
               break;
        case "train": 
          startTime = System.currentTimeMillis();

          app.trainModel();

          endTime = System.currentTimeMillis();
          System.out.println("Total execution time: " + (endTime - startTime) );
          break;
        case "run": 
          startTime = System.currentTimeMillis();

          app.runModel();

          endTime = System.currentTimeMillis();
          System.out.println("Total execution time: " + (endTime - startTime) );
          break;
        case "u-train":
          System.out.println("upvoting and training at the same time");
          break;
      }
    }
  }
}

class nlp {
  String[] allTags = { "ADJ", "ADP", "ADV", "AUX", "CONJ","DET", "NOUN", "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "VERB", "X"};
  SentencesCollection annotatedSentences;
  SentencesCollection corroboratedSentences;

  int percentageOfCorruptFeedback = 0;

  boolean tagsetWritten = false;
  boolean hasEnoughCorroboratedSentences = false;
  boolean modelTrained = false;

  final Runnable upvoteSimulationTask = new Runnable() {
    public void run() { upvoteSimulation(); }
  };

  // final Callable<Boolean> trainingModelSimulationTask = new Callable<Boolean>() {
  //   public Boolean call() { 
  //     Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll", "-tag-morph",  "false", "-model-file", "en.marmot" });  
  //     return true;
  //   }
  // };

  final Runnable annotatingModelSimulationTask = new Runnable() {
    public void run() { Annotator.main(new String[] { "--model-file", "en.marmot", "--test-file", "form-index=1,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll",  "--pred-file", percentageOfCorruptFeedback + "corruptTaggedFile" }); }
  };


  public nlp(int percentageppercentageOfCorruptFeedback) {
    this.percentageOfCorruptFeedback = percentageppercentageOfCorruptFeedback;
    annotatedSentences = new SentencesCollection();
    corroboratedSentences = new SentencesCollection();
  }

  public void upvoteSimulationMultipleUsers(int nthreads) {
    Executor executor = Executors.newFixedThreadPool(nthreads);
    while (annotatedSentences.size() > 0) {
     executor.execute(upvoteSimulationTask);
    }
    System.out.println("finished upvoting");
  }

  public void trainModel() {
    Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll", "-tag-morph",  "false", "-model-file", "en.marmot" });  
  }

  public void runModel() {
    Annotator.main(new String[] { "--model-file", "en.marmot", "--test-file", "form-index=1,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll",  "--pred-file", percentageOfCorruptFeedback + "corruptTaggedFile" }); 
  }
  
  public void fullSimulation(String file, int nthreads) throws IOException {

    Executor executor = Executors.newFixedThreadPool(nthreads);
    readTxtFile(file);

    while (annotatedSentences.size() > 0) {
     executor.execute(upvoteSimulationTask);
    }

    while (!hasEnoughCorroboratedSentences) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }

    System.out.println("done");

    outputTxtFile();

    while (!tagsetWritten) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }

    Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll", "-tag-morph",  "false", "-model-file", "en.marmot" });  
    modelTrained = true;

    while (!modelTrained) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }
    // executor.execute(annotatingModelSimulationTask);
    System.out.println("annotatingModelSimlation started");
    Annotator.main(new String[] { "--model-file", "en.marmot", "--test-file", "form-index=1,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll",  "--pred-file", percentageOfCorruptFeedback + "corruptTaggedFile" }); 
  }

  public void corruptTags() {
    HashSet<Integer> rand_nums;

    for(TaggedSentence sentence : annotatedSentences.getSentences()) {
      rand_nums = new HashSet<>();
      int X = ( percentageOfCorruptFeedback * sentence.getWordCount()) / 100;
      for(int i = 0; i < sentence.getSize(); i++) {
        rand_nums.add(i);
      }

      int currentIndex = 0;
      for (int index : rand_nums) {
        if (currentIndex == X) break;
        sentence.corruptTag(index, getDifferentTag(sentence.getTag(index)));
          currentIndex++;
      }
    }
  }

   String getDifferentTag(String tag) {
    int randomNum = ThreadLocalRandom.current().nextInt(0, allTags.length);
    String newTag = allTags[randomNum];

    while (newTag == tag) {
      randomNum = ThreadLocalRandom.current().nextInt(0, allTags.length);
      newTag = allTags[randomNum];
    }

    return newTag;
  }

   public void outputTxtFile() throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(percentageOfCorruptFeedback+"corruptTrainedModel.conll"));

    int i = 1;
    ArrayList<String> tags;
    for(TaggedSentence sentence  : corroboratedSentences.getSentences()) {
      i = 1;
      tags = sentence.getTags();
      for(String word : sentence.getWords()) {
        writer.write(i + "\t" + word + "\t" + "_" + "\t" + "_" + "\t" + tags.get(i-1) + "\t" + "+\n");
        i++;
      }
      if (i == 1) continue;
      writer.write("\n");
    }
      writer.flush();
      tagsetWritten = true;
  }

  void upvoteSimulation() {
    Set<UUID> keys = annotatedSentences.getKeys();
    int i = 0;
    // String string = annotatedSentences.toString();
    // string = null;
    for (UUID key : keys) {
      if (i < 10) {
        // synchronized(this) {
          upvote(key);
        // }
        i++;
      }
    }
  }

  synchronized void upvote(UUID id) { 
    TaggedSentence sentence = annotatedSentences.getSentence(id);
    if (sentence == null) return;

    annotatedSentences.upvoteSentence(id);

    if (sentence.getUpvotes() > 5) {
      annotatedSentences.removeSentence(id);
      corroboratedSentences.addSentence(sentence);
    }
  }

   void readTxtFile(String file) throws IOException {
     System.out.println("reading text file");
        
		BufferedReader reader = new BufferedReader (new FileReader (file));

		String str;
    String sentence = "";
    ArrayList<String> words = new ArrayList<String>();
    ArrayList<String> tags = new ArrayList<String>();

		while((str = reader.readLine() ) != null ) {
      if (str.contains("#")) {
        sentence = str;
      }

      if (str.length() == 0) {
        annotatedSentences.addSentence(new TaggedSentence(sentence, words, tags));
        words = new ArrayList<String>();
        tags = new ArrayList<String>();
      }
      while (!str.contains("#") && str.length() > 0) {
        String[] sent = str.split("\\t");
        words.add(sent[1]);
        tags.add(sent[3]);
        break;
      }
    }
    System.out.println("finished reading file");
  }

  void memoryUsage(String cause) {
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memory = runtime.totalMemory() - runtime.freeMemory();
    System.out.println();
    System.out.println("Runned after " + cause);
    System.out.println("Used memory is bytes: " + memory);
    System.out.println("Used memory is megabytes: " + bytesToMegabytes(memory));
  }


  void printme() {
    System.out.println();
    System.out.println("annotatedSentencesCounter: " + annotatedSentences.getCounterValue());
    System.out.println("corroboratedSentencesCounter: " + corroboratedSentences.getCounterValue());
    // System.out.println("annotatedSentences: ");
    // annotatedSentences.print();
    //
    // System.out.println("corroboratedSentences: " + corroboratedSentences.getCounterValue());
    // corroboratedSentences.print();
  }

  long bytesToMegabytes(long bytes) {
    final long MEGABYTE = 1024L * 1024L;
    return bytes / MEGABYTE;
  }
}

class SentencesCollection {
  private ConcurrentHashMap<UUID, TaggedSentence> sentences;
  private AtomicInteger counter;

  public SentencesCollection() { 
    this.sentences = new ConcurrentHashMap<UUID, TaggedSentence>();
    this.counter = new AtomicInteger();
  }

  public void addSentence(TaggedSentence sentence) {
    sentences.put(UUID.randomUUID(), sentence);
    incrementCounter();
  }

  public Collection<TaggedSentence> getSentences() {
    return sentences.values();
  }

  public Set<UUID> getKeys() {
    return sentences.keySet();
  }

  public void upvoteSentence(UUID id) {
    if (getSentence(id) == null) return;
    getSentence(id).upvote();
  }

  public TaggedSentence getSentence(UUID id) {
    return sentences.get(id);
  }

  public void removeSentence(UUID id) {
    sentences.remove(id);
    decrementCounter();
  }

  public int incrementCounter() {
    return counter.incrementAndGet();
  }

  public int decrementCounter() {
    return counter.decrementAndGet();
  }

  public int getCounterValue() {
    return counter.get();
  }

  public void print() {
    for (TaggedSentence sentence : this.sentences.values()) {
      System.out.println(sentence);
    }
  }

  public int size() {
    return sentences.size();
  }
}

class TaggedSentence {
  private AtomicInteger counter;
  private String sentence;
  private ArrayList<String> words;
  private ArrayList<String> tags;

  public TaggedSentence(String originalSentence, ArrayList<String> words, ArrayList<String> tags) {
    this.sentence = originalSentence;  
    this.words = words;
    this.tags = tags;

    counter = new AtomicInteger();
  }

  public int getUpvotes() {
    return counter.get();
  }

  public int getSize() {
    return words.size();
  }

  public int upvote() {
    return counter.incrementAndGet();
  }

  public ArrayList<String> getWords() {
    return words;
  }

  public ArrayList<String> getTags() {
    return tags;
  }

  public String getTag(int index) {
    return tags.get(index);
  }

  int getWordCount() {
    return words.size();
  }

  synchronized public void corruptTag(int index, String corruptTag) {
    tags.set(index, corruptTag);
  }

  public String toString() {
    return "\noriginal sentence: " + sentence + 
      "\nupvote count: " + 
      counter.get() + 
      "\ntags:" + tags + 
      "\nwords:" + words;
  }
}

