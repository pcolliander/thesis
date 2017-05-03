import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.*;
import java.io.*;

import java.net.*;

import marmot.morph.cmd.Trainer;
import marmot.morph.cmd.Annotator;;

class ThreadTest implements Runnable {

  public void run() {
    System.out.println("hello from thread");
  }

}

class nlp {
  private static final long MEGABYTE = 1024L * 1024L;
  static SentencesCollection annotatedSentences;
  static SentencesCollection corroboratedSentences;
  static int threadCount = 1;

  public static void main(String[] args) throws IOException {

    for (int i=0; i < threadCount; i++) {
      new Thread(new Runnable() {
        public void run() { upvoteSimulation(); }
      }).start();
    }
  

    while(corroboratedSentences.size() < 100) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }
     // (new Thread(new ThreadTest())).start();

    // readTxtFile(args[0]);
    //
    // annotatedSentences.size();
    // corroboratedSentences.size();
    //
    // upvoteSimulation();
    // // annotatedSentences.print(); 
    // // corroboratedSentences.print(); 
    // memoryUsage();
    // annotatedSentences.size();
    // corroboratedSentences.size();
    //
    // Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,en-ud-train.conll", "-tag-morph", "false", "-model-file", "en.marmot" });
    //
    // memoryUsage();
    // annotatedSentences.size();
    // corroboratedSentences.size();
    //
    // Annotator.main(new String[] { "--model-file", "en.marmot", "--test-file", "form-index=1,en-ud-test.conll",  "--pred-file", "test" });
    //
    // memoryUsage();
    // annotatedSentences.size();
    // corroboratedSentences.size();
  }

  static void readTxtFile(String file) throws IOException {
    annotatedSentences = new SentencesCollection();
    corroboratedSentences = new SentencesCollection();

		BufferedReader reader = new BufferedReader (new FileReader (file));

		String str;
    String sentence = "";
    ArrayList<String> words = new ArrayList<String>();
    ArrayList<String> tags = new ArrayList<String>();
    // int i = 0;

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

  static void memoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memory = runtime.totalMemory() - runtime.freeMemory();
    System.out.println();
    System.out.println("Used memory is bytes: " + memory);
    System.out.println("Used memory is megabytes: " + bytesToMegabytes(memory));
  }

  // should probably run this in paralell ? As right now all the upvotes are done sequentially.
  static void upvoteSimulation() {
    Set<Integer> keys = annotatedSentences.getKeys();

    while (keys.size() > 0) {
      for (int key : keys) {
        upvote(key);
      }

      keys = annotatedSentences.getKeys();
    }
  }

  synchronized static void upvote(int id) { 
    TaggedSentence sentence = annotatedSentences.getSentence(id);
    if (sentence == null) return;

    annotatedSentences.upvoteSentence(id);

    if (sentence.getUpvotes() > 3000) {
      annotatedSentences.removeSentence(id);
      corroboratedSentences.addSentence(sentence);
    }
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

