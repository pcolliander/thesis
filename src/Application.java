import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.concurrent.Future;
import java.util.*;
import java.io.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.UUID;

import marmot.morph.cmd.Trainer;
import marmot.morph.cmd.Annotator;;

enum CorruptionMethod {
  NAIVE, CLEVER
}

class Application {
  public static void main(String[] args) throws IOException {
    int nthreads = 5;
    nlp app = new nlp();

    int corruptFeedback;
    long startTime;
    long endTime;
    while (true) {
      System.out.println();
      System.out.println("webSim = do all");
      System.out.println("nlpNaive = run naive correption of tags and compare results");
      System.out.println("nlpClever = run clever correption of tags and compare results");
      System.out.println("o = output txt file.");
      System.out.println("r = read input and map to dict");
      System.out.println("u = upvote simulation task");
      System.out.println("train = train model.");
      System.out.println("run = run model.");
      Scanner reader = new Scanner(System.in);
      System.out.print("Enter a command: ");
      String n = reader.nextLine();

      switch (n) {
        case "webSim" : app.fullSimulation(args[0], nthreads, 0);
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
        case "o": app.outputTxtFile(0);
                  break;
        case "train": 
                  startTime = System.currentTimeMillis();
                  app.trainModel(0);
                  endTime = System.currentTimeMillis();

                  System.out.println("Total execution time: " + (endTime - startTime) );
                  break;
        case "run": 
                  startTime = System.currentTimeMillis();
                  app.runModel(0);
                  endTime = System.currentTimeMillis();

                  System.out.println("Total execution time: " + (endTime - startTime) );
                  break;

        case "tc": 
                  corruptFeedback = 50;
                  app.corruptOutputModelAndStoreResult(args[0], corruptFeedback, CorruptionMethod.CLEVER);

                  break;
        case "nlpNaive":
                  System.out.println("reading, corrupting tags (Naive), training model, tagging and checking accuracy");
                  corruptFeedback = 0;

                  for (int i = 0; i <= 10; i++) {
                    corruptFeedback = i;
                    app.corruptOutputModelAndStoreResult(args[0], corruptFeedback, CorruptionMethod.NAIVE);
                  }

                  for (int i = 20; i <= 100; i += 10) {
                    corruptFeedback = i;
                    app.corruptOutputModelAndStoreResult(args[0], corruptFeedback, CorruptionMethod.NAIVE);
                  }
                  System.out.println("done");
                  break;

        case "nlpClever":
                  System.out.println("reading, corrupting tags (Clever), training model, tagging and checking accuracy");
                  corruptFeedback = 0;

                  for (int i = 0; i <= 10; i++) {
                    corruptFeedback = i;
                    app.corruptOutputModelAndStoreResult(args[0], corruptFeedback, CorruptionMethod.CLEVER);
                  }

                  for (int i = 20; i <= 100; i += 10) {
                    corruptFeedback = i;
                    app.corruptOutputModelAndStoreResult(args[0], corruptFeedback, CorruptionMethod.CLEVER);
                  }

                  break;
      }
    }
  }
}

class nlp {
  String[] allTags = { "ADJ", "ADP", "ADV", "AUX", "CONJ","DET", "NOUN", "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "VERB", "X"};
  SentencesCollection annotatedSentences;
  SentencesCollection corroboratedSentences;

  int corruptedTagsCleverCounter = 0;
  int totalTagsInTrainFileCounter = 0;

  boolean tagsetWritten = false;
  boolean hasEnoughCorroboratedSentences = false;
  boolean modelTrained = false;

  Date testDate;

  final Runnable upvoteSimulationTask = new Runnable() {
    public void run() { upvoteSimulation(); }
  };

  public nlp() {
    annotatedSentences = new SentencesCollection();
    corroboratedSentences = new SentencesCollection();

    testDate = new Date();
  }

  public void corruptOutputModelAndStoreResult(String file, int corruptFeedback, CorruptionMethod corruptionMethod) throws IOException {
    System.out.println("percentage of corruptFeedback " + corruptFeedback);

    annotatedSentences = new SentencesCollection();
    readTxtFile(file);
    System.out.println("file read");

    corruptedTagsCleverCounter = 0;
    if (corruptionMethod == CorruptionMethod.NAIVE) {
      corruptTagsNaive(corruptFeedback);
    } else {
      corruptTagsClever(corruptFeedback);
    }

    System.out.println("tags corrupted");
    outputTxtFile(corruptFeedback);
    System.out.println("txtFile output");
    trainModel(corruptFeedback);
    System.out.println("model trained");
    runModel(corruptFeedback);
    System.out.println("model run");

    compareAccuracyAndStoreResult(corruptFeedback);
  }

  public void compareAccuracyAndStoreResult(int corruptFeedback) throws IOException {
    BufferedReader readerOriginalTestFile = new BufferedReader (new FileReader ("./en-ud-test.conll"));
    BufferedReader readerCorruptTaggedFile = new BufferedReader (new FileReader ("./taggedFiles/"+corruptFeedback+"corruptTaggedFile"));
    int counter = 0;
    int totalCounter = 0;
    String answerStr, corruptStr;

    while((answerStr = readerOriginalTestFile.readLine() ) != null ) {
      if (answerStr.length() == 0) {
        corruptStr = readerCorruptTaggedFile.readLine();
        continue;
      }
      corruptStr = readerCorruptTaggedFile.readLine();

      String[] correct = answerStr.split("\\t");
      String[] corrupt = corruptStr.split("\\t");

      if (!correct[4].equals(corrupt[5])) counter++; 

      if (correct[4].equals(correct[4])) totalCounter++; 
    }
    // System.out.println("(test file) totalCounter: " + totalCounter);

    FileWriter fw = new FileWriter("resultsOfComparison - " + testDate.toString(), true);
    fw.write(corruptFeedback + ": " + counter + ", tags corrupted: " + corruptedTagsCleverCounter);

    corruptedTagsCleverCounter = 0;
    fw.write(System.getProperty( "line.separator" ));
    fw.close();
  }

  public void upvoteSimulationMultipleUsers(int nthreads) {
    Executor executor = Executors.newFixedThreadPool(nthreads);
    while (annotatedSentences.size() > 0) {
      executor.execute(upvoteSimulationTask);
    }
    System.out.println("finished upvoting");
  }

  public void trainModel(int corruptFeedback) {
    Trainer.main(new String[] { "-train-file", "form-index=1,tag-index=4,"+corruptFeedback+"corruptTrainedModel.conll", "-tag-morph",  "false", "-model-file", "modelEnglish.marmot" });  
  }

  public void runModel(int corruptFeedback) {
    Annotator.main(new String[] { "--model-file", "modelEnglish.marmot", "--test-file", "form-index=1,en-ud-test.conll",  "--pred-file", "./taggedFiles/"+corruptFeedback+"corruptTaggedFile" }); 
  }

  public void corruptTagsNaive(int corruptFeedback) {
    for(TaggedSentence sentence : annotatedSentences.getSentences()) {
      HashSet<Integer> rand_nums = new HashSet<>();

      int X = (corruptFeedback * sentence.getWordCount()) / 100;
      for(int i = 0; i < sentence.getSize(); i++) {
        rand_nums.add(i);
      }
      for (int index = 0; index < X; index++) {
        sentence.corruptTag(index, getDifferentTagNaive(sentence.getTag(index)));
      }
    }
  }

  String getDifferentTagNaive(String tag) {
    int randomNum = ThreadLocalRandom.current().nextInt(0, allTags.length);
    String newTag = allTags[randomNum];

    while (newTag == tag) {
      randomNum = ThreadLocalRandom.current().nextInt(0, allTags.length);
      newTag = allTags[randomNum];
    }

    return newTag;
  }

  public void corruptTagsClever(int corruptFeedback) {
    Pattern pattern = Pattern.compile("ADJ|ADV|AUX|NOUN|NUM|PROPN|VERB");
    for (TaggedSentence sentence : annotatedSentences.getSentences()) {
      HashSet<Integer> matches = new HashSet<Integer>();
      HashSet<Integer> rand_indices = new HashSet<>();
      int currentIndex = 0;

      for (String tag : sentence.getTags()) {
        Matcher m = pattern.matcher(tag);
        if (m.find()) {
          matches.add(currentIndex);
          currentIndex++;
          continue;
        }
        currentIndex++;
      }

      int X = (corruptFeedback * matches.size()) / 100;
      currentIndex = 0;
      for (int i : matches) {
        if (currentIndex == X) break;
        rand_indices.add(i);
        currentIndex++;
      }
      for (int index : rand_indices) {
        sentence.corruptTag(index, getDifferentTagClever(sentence.getTag(index)));
        corruptedTagsCleverCounter++;
      }
    }
  }

  String getDifferentTagClever(String tag) {
    switch (tag) {
      case "ADJ" : return "NUM";
      case "ADV" : return "ADJ";
      case "AUX" : return "VERB"; // id - use verbs
      case "NOUN" : return "X";
      case "NUM" : return "ADJ";
      case "PROPN" : return "NOUN"; // id - noun
      case "VERB" : return "ADJ";

      default: return tag;
    }
  }

  public void outputTxtFile(int corruptFeedback) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(corruptFeedback+"corruptTrainedModel.conll"));

    int i = 1;
    ArrayList<String> tags;
    for(TaggedSentence sentence  : annotatedSentences.getSentences()) {
      i = 1;
      tags = sentence.getTags();
      for(String word : sentence.getWords()) {
        writer.write(i + "\t" + word + "\t" + "_" + "\t" + "_" + "\t" + tags.get(i-1) + "\t" + "+\n");
        i++;
      }
      if (i == 1) continue;
      writer.write("\n");
    }
    writer.close();
    tagsetWritten = true;
  }

  void upvoteSimulation() {
    Set<UUID> keys = annotatedSentences.getKeys();
    int i = 0;
    for (UUID key : keys) {
      if (i < 10) {
        upvote(key);
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
        totalTagsInTrainFileCounter++;
        break;
      }
    }

    System.out.println("totalTagsInTrainFileCounter: " + totalTagsInTrainFileCounter);
    reader.close();
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
  }

  long bytesToMegabytes(long bytes) {
    final long MEGABYTE = 1024L * 1024L;
    return bytes / MEGABYTE;
  }

  public void fullSimulation(String file, int nthreads, int corruptFeedback) throws IOException {
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
    outputTxtFile(corruptFeedback);

    while (!tagsetWritten) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }

    trainModel(corruptFeedback);
    modelTrained = true;

    while (!modelTrained) {
      try {
        wait();
      } catch (InterruptedException e) {}
    }
    System.out.println("annotatingModelSimlation started");
    runModel(corruptFeedback);
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

