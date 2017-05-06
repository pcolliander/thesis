import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.*;
import java.io.*;

import java.net.*;

import marmot.morph.cmd.Trainer;
import marmot.morph.cmd.Annotator;;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Executors;

class ThreadTest implements Runnable {

  public void run() {
    System.out.println("hello from thread");
  }

}

class nlp {
  private static final long MEGABYTE = 1024L * 1024L;
  static SentencesCollection annotatedSentences;
  static SentencesCollection corroboratedSentences;

   static final Runnable upvoteSimulationTask = new Runnable() {
      public void run() { upvoteSimulation(); }
   };

   static final Runnable trainingModelSimulationTask = new Runnable() {
     public void run() { Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,en-ud-train.conll", "-tag-morph",  "false", "-model-file", "en.marmot" }); }
   };

   static final Runnable annotatingModelSimulationTask = new Runnable() {
     public void run() { Annotator.main(new String[] { "--model-file", "en.marmot", "--test-file", "form-index=1,en-ud-test.conll",  "--pred-file", "test" }); }
   };

  public static void main(String[] args) throws IOException {

    readTxtFile(args[0]);
    Executor e = Executors.newCachedThreadPool();
    System.out.println("e" + e);

    memoryUsage("Before anything" );
    while (annotatedSentences.size() > 0) {
     e.execute(upvoteSimulationTask);
    }
    System.out.println("e" + e);

    memoryUsage("upvote simulation");

    for (int i=0; i < 10; i++) {
      e.execute(trainingModelSimulationTask);
    }
  }

  public void outputTxtFile() {
    for(TaggedSentence sentence  : corroboratedSentences.getSentences().values()) {
      System.out.println(sentence);
    }

  }

  static void upvoteSimulation() {
    Set<Integer> keys = annotatedSentences.getKeys();
    int i = 0;
      for (int key : keys) {
        if (i > 10) break;
        upvote(key);
        i++;
      }
  }

  synchronized static void upvote(int id) { 
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
    return bytes / MEGABYTE;
  }
}

class SentencesCollection {
  private ConcurrentHashMap<Integer, TaggedSentence> sentences;
  private AtomicInteger counter;

  public SentencesCollection() { 
    this.sentences = new ConcurrentHashMap<Integer, TaggedSentence>();
    this.counter = new AtomicInteger();
  }

  public synchronized void addSentence(TaggedSentence sentence) {
    int id = (int) (Math.random() * 1000000000);

    sentences.put(id, sentence);
    incrementCounter();
  }

  public ConcurrentHashMap<Integer, TaggedSentence> getSentences() {
    return sentences;
  }

  public Set<Integer> getKeys() {
    return sentences.keySet();
  }

  public void upvoteSentence(int id) {
    if (getSentence(id) == null) return;

    getSentence(id).upvote();
  }

  public TaggedSentence getSentence(int id) {
    return sentences.get(id);
  }

  public void removeSentence(int id) {
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

  public int upvote() {
    return counter.incrementAndGet();
  }

  public String toString() {
    return "\noriginal sentence: " + sentence + 
      "\nupvote count: " + 
      counter.get() + 
      "\ntags:" + tags + 
      "\nwords:" + words;
  }
}

