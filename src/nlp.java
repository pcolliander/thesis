import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

class nlp {
  public static void main(String[] args) { 

    SentencesDictionary annotatedSentences;
    SentencesDictionary corroboratedSentences;

    // ArrayList<Text> texts = new ArrayList<>();
    //
    // Text t = new Text();
    // System.out.println(t.getUpvotes());
    //
    // t.upvote();
    // System.out.println(t.getUpvotes());
  }
  
  // void editText(int id, String text) { }
  // void getTexts() { }

  void addAnnotated(String sentence) {
    AnnotatedSentences.AddSentence(sentence);
  }

  void upvote(int id) { 
    TaggedSentence sentence = annotatedSentences.get(id);

    if (sentence == null) return;

    annotatedSentences.upvoteSentence(id);

    synchronized(this) {

      if (sentence.getUpvotes() > 3) {
        annotatedSentences.remove(id);
        annotatedSentences.decrementCounter();

        corroboratedSentences.addSentence(id, sentence);
        corroboratedSentences.incrementCounter();
      }
    }

  }
}

class SentencesDictionary {
  private HashMap<int, TaggedSentence> sentences;
  public SentencesCounter counter;

  public AnnotatedSentences() { 
    HashMap<int, TaggedSentence> annotatedSentences = new HashMap<int, TaggedSentence>();
    counter = new SentencesCounter("Annotated Sentences");
  }

  public synchronized addSentence(String sentence) {
    // logic to add sentence
    // ...
    
    incrementCounter();
  }

  public void upvoteSentence(int id) {
    if (getSentence(id) == null) return;

    getSentence(id).upvote();
  }

  private TaggedSentence getSentence(int id) {
    return sentences.get(id);
  }

  public void incrementCounter() {
    counter.increment();
  }

  public void decrementCounter() {
    counter.decrement();
  }
}


// use to initialise the counters.
class SentencesCounter {
  String name;
  private AtomicInteger counter = new AtomicInteger();

  public SentencesCounter(String name) {
    this.name = name;
    counter = 0;
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
  private AtomicInteger counter = new AtomicInteger();

  public int getUpvotes() {
    return counter.get();
  }

  public int upvote() {
    return counter.incrementAndGet();
  }

  private void annotate() {

  }
}
