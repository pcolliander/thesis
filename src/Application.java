import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.*;
import java.io.*;

import java.util.UUID;

import marmot.morph.cmd.Trainer;
import marmot.morph.cmd.Annotator;;


class Application {

  public static void main(String[] args) throws IOException {

    Executor executor = Executors.newCachedThreadPool();
    nlp app = new nlp(50, executor);
    app.simulation(args[0]);
  }
}

class nlp {
  String[] allTags = { "ADJ", "ADP", "ADV", "AUX", "CONJ","DET", "NOUN", "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "VERB", "X"};
  SentencesCollection annotatedSentences;
  SentencesCollection corroboratedSentences;

  int percentageOfCorruptFeedback = 0;
  Executor executor;

  boolean tagsetWritten = false;
  boolean modelTrained = false;
  // Creates a thread pool that creates new threads as needed, but will reuse previously constructed threads when they are available.

  final Runnable upvoteSimulationTask = new Runnable() {
    public void run() { upvoteSimulation(); }
  };

  final Runnable trainingModelSimulationTask = new Runnable() {
    public void run() { 
      Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll", "-tag-morph",  "false", "-model-file", "en.marmot" });  
      modelTrained = true; 
    }
  };

  final Runnable annotatingModelSimulationTask = new Runnable() {
    public void run() { Annotator.main(new String[] { "--model-file", "en.marmot", "--test-file", "form-index=1,"+percentageOfCorruptFeedback+"corruptTrainedModel.conll",  "--pred-file", percentageOfCorruptFeedback + "corruptTaggedFile" }); }
  };


  public nlp(int percentageppercentageOfCorruptFeedback, Executor e) {
    this.percentageOfCorruptFeedback = percentageppercentageOfCorruptFeedback;
    executor = e;
  }

  synchronized public void simulation(String file) throws IOException {

    readTxtFile(file);
    // printme();

    int i = 0;
    while (annotatedSentences.size() > 0 && i < 50) {
     executor.execute(upvoteSimulationTask);
     i++;
    }
    System.out.println("e" + executor);
    // concurrencySimulation();
    // System.out.println("concurrenySimulation finished");
    // corruptTags(50);
    outputTxtFile();

    while (!tagsetWritten) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }

    executor.execute(trainingModelSimulationTask);
    System.out.println("trainingModelSimulation started");

    while (!modelTrained) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }
    executor.execute(annotatingModelSimulationTask);
    System.out.println("annotatingModelSimlation started");
  }

  public void concurrencySimulation() {
    System.out.println("e" + executor);

    // memoryUsage("Before anything" );

    // memoryUsage("upvote simulation");

    // for (int i=0; i < 1; i++) {
      executor.execute(trainingModelSimulationTask);
    // }
  }

  void corruptTags(int percentage) {
    HashSet<Integer> rand_nums;

    for(TaggedSentence sentence : annotatedSentences.getSentences()) {
      rand_nums = new HashSet<>();
      int X = (percentage * sentence.getWordCount()) / 100;
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
    for(TaggedSentence sentence  : annotatedSentences.getSentences()) {
      i = 1;
      tags = sentence.getTags();
      for(String word : sentence.getWords()) {
        writer.write(i + "\t" + word + "\t" + "_" + "\t" + "_" + "\t" + tags.get(i-1) + "\n");
        i++;
      }
      writer.write("\n");
    }
      writer.flush();
      tagsetWritten = true;
  }

   void upvoteSimulation() {
    Set<UUID> keys = annotatedSentences.getKeys();
    int i = 0;

    for (UUID key : keys) {
      if (i > 10) break;
      upvote(key);
      i++;
    }
  }

  synchronized void upvote(UUID id) { 
    TaggedSentence sentence = annotatedSentences.getSentence(id);
    if (sentence == null) return;

    annotatedSentences.upvoteSentence(id);

    if (sentence.getUpvotes() > 50) {
      annotatedSentences.removeSentence(id);
      corroboratedSentences.addSentence(sentence);
    }
  }

   void readTxtFile(String file) throws IOException {
    annotatedSentences = new SentencesCollection();
    corroboratedSentences = new SentencesCollection();

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
    System.out.println("annotatedSentences: ");
    annotatedSentences.print();

    System.out.println("corroboratedSentences: " + corroboratedSentences.getCounterValue());
    corroboratedSentences.print();
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

  public synchronized void addSentence(TaggedSentence sentence) {
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

