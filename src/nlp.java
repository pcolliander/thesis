import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.*;

class nlp {
  static SentencesCollection annotatedSentences;
  static SentencesCollection corroboratedSentences;

  public static void main(String[] args) { 
    annotatedSentences = new SentencesCollection();
    corroboratedSentences = new SentencesCollection();

    String[] sentences = {"my name is bob", "my name is bobba", "my name is bobba fett", "my name is guy", "my name is guy richie", "my name is anders", "my name is lena" }; 

    simulation(sentences);
    printme();
  }

  static void simulation(String[] sentences) {
    addAnnotatedTextSimulation(sentences);
    upvoteSimulation();
  }

  static void addAnnotatedTextSimulation(String[] sentences) {
    for (String sentence : sentences) {
      String[] words = sentence.split(" ");
      String[] tags = sentence.split(" ");
      annotatedSentences.addSentence(new TaggedSentence(sentence, words, tags));
    }
  }

  static void upvoteSimulation() {
    Set<Integer> keys = annotatedSentences.getKeys();

    while (keys.size() > 0) {
      for (int key : keys) {
        upvote(key);
      }

      keys = annotatedSentences.getKeys();
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

  synchronized static void upvote(int id) { 
    TaggedSentence sentence = annotatedSentences.getSentence(id);
    if (sentence == null) return;

    annotatedSentences.upvoteSentence(id);

    if (sentence.getUpvotes() > 3) {
      annotatedSentences.removeSentence(id);
      corroboratedSentences.addSentence(sentence);
    }
  }
}

class SentencesCollection {
  private ConcurrentHashMap<Integer, TaggedSentence> sentences;
  public SentencesCounter counter;

  public SentencesCollection() { 
    this.sentences = new ConcurrentHashMap<Integer, TaggedSentence>();
    this.counter = new SentencesCounter("Annotated Sentences");
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

  public void incrementCounter() {
    counter.increment();
  }

  public void decrementCounter() {
    counter.decrement();
  }

  public int getCounterValue() {
    return counter.getValue();
  }

  public void print() {
    for (TaggedSentence sentence : this.sentences.values()) {
      System.out.println(sentence);
    }
  }
}

// use to initialise the counters.
class SentencesCounter {
  String name;
  private AtomicInteger counter;

  public SentencesCounter(String name) {
    this.name = name;
    counter = new AtomicInteger();
  }

  public int getValue() {
    return counter.get();
  }

  public int increment() {
    return counter.incrementAndGet();
  }

  public int decrement() {
    return counter.decrementAndGet();
  }
}

class TaggedSentence {
  private AtomicInteger counter;
  private String sentence;
  private String[] words;
  private String[] tags;

  public TaggedSentence(String originalSentence, String[] words, String[] tags) {
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
      "\ntags:" + Arrays.toString(tags)  + 
      "\nwords:" + Arrays.toString(words);
  }
}

