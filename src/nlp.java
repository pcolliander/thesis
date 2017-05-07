import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.*;
import java.io.*;

import java.util.UUID;

import marmot.morph.cmd.Trainer;
import marmot.morph.cmd.Annotator;;

class nlp {
  static SentencesCollection annotatedSentences;
  static SentencesCollection corroboratedSentences;

  static String[] allTags = { "ADJ", "ADP", "ADV", "AUX", "CONJ","DET", "NOUN", "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "VERB", "X"};

   static final Runnable upvoteSimulationTask = new Runnable() {
      public void run() { upvoteSimulation(); }
   };

   static final Runnable trainingModelSimulationTask = new Runnable() {
     public void run() { Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,outputfilepontus.conll", "-tag-morph",  "false", "-model-file", "en.marmot" }); }
   };

   static final Runnable annotatingModelSimulationTask = new Runnable() {
     public void run() { Annotator.main(new String[] { "--model-file", "en.marmot", "--test-file", "form-index=1,outputfilespontus.conll",  "--pred-file", "taggingFromTrainedModelWithBadFeedback" }); 
      // memoryUsage("after annotation simulation");
     }
   };

  static Executor e = Executors.newCachedThreadPool();
  public static void main(String[] args) throws IOException {
    readTxtFile(args[0]);

    // concurrencySimulation();

    corruptTags(50);
    outputTxtFile();
    // annotateAndCompare();
    // e.execute(annotatingModelSimulationTask);
  }

  static void corruptTags(int percentage) {

    HashSet<Integer> rand_nums;

    for(TaggedSentence sentence : annotatedSentences.getSentences()) {

      rand_nums = new HashSet<>();
      int X = (percentage * sentence.getWordCount()) / 100;
      for(int i = 0; i < sentence.getSize(); i++) {
        rand_nums.add(i);
      }

      int x = 0;
      for (int index : rand_nums) {
        if (x == X) break;
        sentence.corruptTag(index, getDifferentTag(sentence.getTag(index)));
          x++;
      }

    }
  }

  static public String getDifferentTag(String tag) {

    int randomNum = ThreadLocalRandom.current().nextInt(0, allTags.length);

    String newTag = allTags[randomNum];

    while (newTag == tag) {
      randomNum = ThreadLocalRandom.current().nextInt(0, allTags.length);
      tag = allTags[randomNum];
    }

    return newTag;
  }

  // static public void annotateAndCompare() {
    // e.execute(annotatingModelSimulationTask);
  // }

  static public void concurrencySimulation() {
    System.out.println("e" + e);

    // memoryUsage("Before anything" );
    while (annotatedSentences.size() > 0) {
     e.execute(upvoteSimulationTask);
    }
    System.out.println("e" + e);

    // memoryUsage("upvote simulation");

    // for (int i=0; i < 1; i++) {
      e.execute(trainingModelSimulationTask);
    // }
  }


  static public void outputTxtFile() throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter("outputfilepontusCorrupt.conll"));

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
  }

  static void upvoteSimulation() {
    Set<UUID> keys = annotatedSentences.getKeys();
    int i = 0;
      for (UUID key : keys) {
        if (i > 10) break;
        upvote(key);
        i++;
      }
  }

  synchronized static void upvote(UUID id) { 
    TaggedSentence sentence = annotatedSentences.getSentence(id);
    if (sentence == null) return;

    annotatedSentences.upvoteSentence(id);

    if (sentence.getUpvotes() > 50) {
      annotatedSentences.removeSentence(id);
      corroboratedSentences.addSentence(sentence);
    }
  }

  static void readTxtFile(String file) throws IOException {
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

  static void memoryUsage(String cause) {
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memory = runtime.totalMemory() - runtime.freeMemory();
    System.out.println();
    System.out.println("Runned after " + cause);
    System.out.println("Used memory is bytes: " + memory);
    System.out.println("Used memory is megabytes: " + bytesToMegabytes(memory));
  }


  static void printme() {
    System.out.println();
    System.out.println("annotatedSentencesCounter: " + annotatedSentences.getCounterValue());
    System.out.println("corroboratedSentencesCounter: " + corroboratedSentences.getCounterValue());
    System.out.println("annotatedSentences: ");
    annotatedSentences.print();

    System.out.println("corroboratedSentences: " + corroboratedSentences.getCounterValue());
    corroboratedSentences.print();
  }

  static long bytesToMegabytes(long bytes) {
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

