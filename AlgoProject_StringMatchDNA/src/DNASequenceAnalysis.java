import java.util.*;


public class DNASequenceAnalysis {
  private static final String DNA_BASES = "ACGT";
  private static final Random random = new Random();
  private static final int CHUNK_SIZE = 10_000_000; // 10 million characters/chunk

  public static void main(String[] args) {
    long sequenceLength = 100_000_000;
    int patternLength = 10;
    int numberOfPatterns = 3;
    int numberOfTests = 3;

    for (int test = 1; test <= numberOfTests; test++) {
      // Generates the sequence
      long startTime = System.nanoTime();
      DNASequenceStream dnaStream = new DNASequenceStream(sequenceLength);
      long endTime = System.nanoTime();
      double sequenceGenerationTime = (endTime - startTime) / 1e9;
      // Generates random patterns
      String[] patterns = generateRandomPatterns(patternLength, numberOfPatterns);

      // KMP Algorithm
      long kmpStartTime = System.nanoTime();
      Map<String, Double> kmpResults = runKMPTests(dnaStream, patterns);
      long kmpEndTime = System.nanoTime();
      double kmpTotalExecutionTime = (kmpEndTime - kmpStartTime) / 1e9;
      kmpResults.put("TotalExecutionTime", kmpTotalExecutionTime);

      // Boyer-Moore Algorithm
      long bmStartTime = System.nanoTime();
      Map<String, Double> bmResults = runBoyerMooreTests(dnaStream, patterns);
      long bmEndTime = System.nanoTime();
      double bmTotalExecutionTime = (bmEndTime - bmStartTime) / 1e9;
      bmResults.put("TotalExecutionTime", bmTotalExecutionTime);

      // Aho-Corasick Algorithm
      long acStartTime = System.nanoTime();
      Map<String, Double> acResults = runAhoCorasickTests(dnaStream, patterns);
      long acEndTime = System.nanoTime();
      double acTotalExecutionTime = (acEndTime - acStartTime) / 1e9;
      acResults.put("TotalExecutionTime", acTotalExecutionTime);

      printResultTable(test, sequenceLength, sequenceGenerationTime, patterns, kmpResults, bmResults, acResults);
    }
  }

  private static Map<String, Double> runKMPTests(DNASequenceStream dnaStream, String[] patterns) {
    Map<String, Double> results = new HashMap<>();
    double totalMatchingTime = 0;
    double totalMatches = 0;

    for (int i = 0; i < patterns.length; i++) {
      long startTime = System.nanoTime();
      long matches = searchPatternStreamKMP(dnaStream, patterns[i]);
      long endTime = System.nanoTime();
      double matchingTime = (endTime - startTime) / 1e9;

      results.put("Pattern" + (i + 1) + "Time", matchingTime);
      results.put("Pattern" + (i + 1) + "Matches", (double) matches);
      results.put("Pattern" + (i + 1) + "Speed", dnaStream.length / matchingTime / 1e6);

      totalMatchingTime += matchingTime;
      totalMatches += matches;
    }

    results.put("TotalPatternMatchingTime", totalMatchingTime);
    results.put("AverageMatchingTimePerPattern", totalMatchingTime / patterns.length);
    results.put("AverageMatchingSpeed", (dnaStream.length * patterns.length) / totalMatchingTime / 1e6);

    return results;
  }

  private static Map<String, Double> runBoyerMooreTests(DNASequenceStream dnaStream, String[] patterns) {
    Map<String, Double> results = new HashMap<>();
    double totalMatchingTime = 0;
    double totalMatches = 0;

    for (int i = 0; i < patterns.length; i++) {
      long startTime = System.nanoTime();
      long matches = searchPatternStreamBM(dnaStream, patterns[i]);
      long endTime = System.nanoTime();
      double matchingTime = (endTime - startTime) / 1e9;

      results.put("Pattern" + (i + 1) + "Time", matchingTime);
      results.put("Pattern" + (i + 1) + "Matches", (double) matches);
      results.put("Pattern" + (i + 1) + "Speed", dnaStream.length / matchingTime / 1e6);

      totalMatchingTime += matchingTime;
      totalMatches += matches;
    }

    results.put("TotalPatternMatchingTime", totalMatchingTime);
    results.put("AverageMatchingTimePerPattern", totalMatchingTime / patterns.length);
    results.put("AverageMatchingSpeed", (dnaStream.length * patterns.length) / totalMatchingTime / 1e6);

    return results;
  }

  private static Map<String, Double> runAhoCorasickTests(DNASequenceStream dnaStream, String[] patterns) {
    Map<String, Double> results = new HashMap<>();
    AhoCorasickNode root = buildAhoCorasickTrie(patterns);
    long startTime = System.nanoTime();
    long matches = searchPatternsStreamAC(dnaStream, patterns);
    long endTime = System.nanoTime();
    double matchingTime = (endTime - startTime) / 1e9;

    results.put("TotalMatches", (double) matches);
    results.put("TotalPatternMatchingTime", matchingTime);
    results.put("Speed", dnaStream.length / matchingTime / 1e6);
    results.put("TrieSize", (double) countTrieNodes(root));

    return results;
  }

  private static void printResultTable(int testNumber, long sequenceLength, double sequenceGenerationTime,
                                       String[] patterns, Map<String, Double> kmpResults,
                                       Map<String, Double> bmResults, Map<String, Double> acResults) {
    System.out.println("Test " + testNumber + " Results");
    System.out.println("Sequence Length: " + sequenceLength);
    System.out.printf("Sequence Generation Time: %.6f s%n", sequenceGenerationTime);
    System.out.println();

    printAlgorithmResults("KMP Algorithm", patterns, kmpResults);
    printAlgorithmResults("Boyer-Moore Algorithm", patterns, bmResults);
    printAlgorithmResults("Aho-Corasick Algorithm", patterns, acResults);
  }

  private static void printAlgorithmResults(String algorithmName, String[] patterns, Map<String, Double> results) {
    System.out.println(algorithmName + " Results:");
    if (algorithmName.equals("Aho-Corasick Algorithm")) {
      // Displays the patterns and trie structure
      System.out.println("Searching for multiple patterns: " + Arrays.toString(patterns));
      System.out.println("Aho-Corasick Trie Structure:");
      AhoCorasickNode root = buildAhoCorasickTrie(patterns);
      printTrie(root, "", "");
      System.out.println();

      // Displays the performance results
      System.out.printf("Total Matches: %.0f%n", results.get("TotalMatches"));
      System.out.printf("Total Pattern Matching Time: %.6f s%n", results.get("TotalPatternMatchingTime"));
      System.out.printf("Matching Speed: %.2f Mbp/s%n", results.get("Speed"));
      System.out.printf("Trie size: %.0f%n", results.get("TrieSize"));
    } else {
      for (int i = 0; i < patterns.length; i++) {
        System.out.println("Pattern " + (i + 1) + ": " + patterns[i]);
        System.out.printf("Matches: %.0f%n", results.get("Pattern" + (i + 1) + "Matches"));
        System.out.printf("Matching Time: %.6f s%n", results.get("Pattern" + (i + 1) + "Time"));
        System.out.printf("Matching Speed: %.2f Mbp/s%n", results.get("Pattern" + (i + 1) + "Speed"));
        System.out.println();
      }
      System.out.printf("Total Pattern Matching Time: %.6f s%n", results.get("TotalPatternMatchingTime"));
      System.out.printf("Average Matching Time per Pattern: %.6f s%n", results.get("AverageMatchingTimePerPattern"));
      System.out.printf("Average Matching Speed: %.2f Mbp/s%n", results.get("AverageMatchingSpeed"));
    }
    System.out.printf("Total Execution Time: %.6f s%n", results.get("TotalExecutionTime"));
    System.out.println();
  }

  // KMP Algorithm
  public static List<Integer> kmpSearch(String text, String pattern) {
    List<Integer> matches = new ArrayList<>();
    int[] lps = computeLPSArray(pattern);
    int i = 0, j = 0;
    while (i < text.length()) {
      if (pattern.charAt(j) == text.charAt(i)) {
        j++;
        i++;
      }
      if (j == pattern.length()) {
        matches.add(i - j);
        j = lps[j - 1];
      } else if (i < text.length() && pattern.charAt(j) != text.charAt(i)) {
        if (j != 0) {
          j = lps[j - 1];
        } else {
          i++;
        }
      }
    }
    return matches;
  }

  private static int[] computeLPSArray(String pattern) {
    int[] lps = new int[pattern.length()];
    int len = 0;
    int i = 1;

    while (i < pattern.length()) {
      if (pattern.charAt(i) == pattern.charAt(len)) {
        len++;
        lps[i] = len;
        i++;
      } else {
        if (len != 0) {
          len = lps[len - 1];
        } else {
          lps[i] = 0;
          i++;
        }
      }
    }
    return lps;
  }

  // Optimized Boyer-Moore Algorithm with strong good suffix rule
  public static List<Integer> boyerMooreSearch(String text, String pattern) {
    List<Integer> matches = new ArrayList<>();
    int[] badChar = buildBadCharTable(pattern);
    int[] goodSuffix = buildGoodSuffixTable(pattern);

    int n = text.length();
    int m = pattern.length();
    int i = 0;

    while (i <= n - m) {
      int j = m - 1;

      while (j >= 0 && pattern.charAt(j) == text.charAt(i + j)) {
        j--;
      }

      if (j < 0) {
        matches.add(i);
        i += (i + m < n) ? m - badChar[text.charAt(i + m)] : 1;
      } else {
        int badCharShift = j - badChar[text.charAt(i + j)];
        int goodSuffixShift = 0;

        if (j < m - 1) {
          goodSuffixShift = goodSuffix[j + 1];
        }

        i += Math.max(badCharShift, goodSuffixShift);
      }
    }

    return matches;
  }

  private static int[] buildBadCharTable(String pattern) {
    int[] badChar = new int[256];
    Arrays.fill(badChar, -1);

    for (int i = 0; i < pattern.length(); i++) {
      badChar[pattern.charAt(i)] = i;
    }

    return badChar;
  }

  private static int[] buildGoodSuffixTable(String pattern) {
    int m = pattern.length();
    int[] goodSuffix = new int[m];
    int[] suffixes = computeSuffixes(pattern);
    Arrays.fill(goodSuffix, m);
    for (int i = m - 1, j = 0; i >= 0; i--) {
      if (suffixes[i] == i + 1) {
        for (; j < m - 1 - i; j++) {
          if (goodSuffix[j] == m) {
            goodSuffix[j] = m - 1 - i;
          }
        }
      }
    }
    for (int i = 0; i < m - 1; i++) {
      goodSuffix[m - 1 - suffixes[i]] = m - 1 - i;
    }
    return goodSuffix;
  }

  private static int[] computeSuffixes(String pattern) {
    int m = pattern.length();
    int[] suffixes = new int[m];
    suffixes[m - 1] = m;
    int g = m - 1;
    int f = 0;
    for (int i = m - 2; i >= 0; --i) {
      if (i > g && suffixes[i + m - 1 - f] < i - g) {
        suffixes[i] = suffixes[i + m - 1 - f];
      } else {
        if (i < g) {
          g = i;
        }
        f = i;
        while (g >= 0 && pattern.charAt(g) == pattern.charAt(g + m - 1 - f)) {
          --g;
        }
        suffixes[i] = f - g;
      }
    }
    return suffixes;
  }

  // Aho-Corasick Algorithm
  public static List<Integer> ahoCorasickSearch(String text, String[] patterns) {
    List<Integer> matches = new ArrayList<>();
    AhoCorasickNode root = buildAhoCorasickTrie(patterns);
    AhoCorasickNode state = root;

    for (int i = 0; i < text.length(); i++) {
      while (state != root && !state.children.containsKey(text.charAt(i)))
        state = state.fail;

      if (state.children.containsKey(text.charAt(i))) {
        state = state.children.get(text.charAt(i));
        if (state.outputs != null) {
          for (String found : state.outputs) {
            matches.add(i - found.length() + 1);
          }
        }
      }
    }
    return matches;
  }

  private static AhoCorasickNode buildAhoCorasickTrie(String[] patterns) {
    AhoCorasickNode root = new AhoCorasickNode();
    for (String pattern : patterns) {
      AhoCorasickNode node = root;
      for (char c : pattern.toCharArray()) {
        node.children.putIfAbsent(c, new AhoCorasickNode());
        node = node.children.get(c);
      }
      if (node.outputs == null) {
        node.outputs = new HashSet<>();
      }
      node.outputs.add(pattern);
    }

    Queue<AhoCorasickNode> queue = new LinkedList<>();
    for (Map.Entry<Character, AhoCorasickNode> entry : root.children.entrySet()) {
      entry.getValue().fail = root;
      queue.add(entry.getValue());
    }

    while (!queue.isEmpty()) {
      AhoCorasickNode current = queue.remove();
      for (Map.Entry<Character, AhoCorasickNode> entry : current.children.entrySet()) {
        AhoCorasickNode child = entry.getValue();
        AhoCorasickNode fail = current.fail;
        while (fail != null && !fail.children.containsKey(entry.getKey())) {
          fail = fail.fail;
        }
        if (fail == null) {
          child.fail = root;
        } else {
          child.fail = fail.children.get(entry.getKey());
          if (child.fail.outputs != null) {
            if (child.outputs == null) {
              child.outputs = new HashSet<>();
            }
            child.outputs.addAll(child.fail.outputs);
          }
        }
        queue.add(child);
      }
    }
    return root;
  }

  static class AhoCorasickNode implements Comparable<AhoCorasickNode> {
    TreeMap<Character, AhoCorasickNode> children = new TreeMap<>();
    AhoCorasickNode fail;
    Set<String> outputs;

    AhoCorasickNode() {
      this.fail = null;
      this.outputs = new HashSet<>();
    }

    @Override
    public int compareTo(AhoCorasickNode other) {
      return this.hashCode() - other.hashCode();
    }
  }

  // Stream-based DNA sequence generator
  static class DNASequenceStream {
    private final long length;
    private long position;

    DNASequenceStream(long length) {
      this.length = length;
      this.position = 0;
    }

    public String nextChunk() {
      if (position >= length) return null;
      int chunkSize = (int) Math.min(CHUNK_SIZE, length - position);
      StringBuilder chunk = new StringBuilder(chunkSize);
      for (int i = 0; i < chunkSize; i++) {
        chunk.append(DNA_BASES.charAt(random.nextInt(DNA_BASES.length())));
      }
      position += chunkSize;
      return chunk.toString();
    }

    public void reset() {
      position = 0;
    }
  }

  // Helper Method to generate random DNA patterns
  private static String[] generateRandomPatterns(int length, int count) {
    String[] patterns = new String[count];
    for (int i = 0; i < count; i++) {
      StringBuilder pattern = new StringBuilder(length);
      for (int j = 0; j < length; j++) {
        pattern.append(DNA_BASES.charAt(random.nextInt(DNA_BASES.length())));
      }
      patterns[i] = pattern.toString();
    }
    return patterns;
  }

  private static long searchPatternStreamKMP(DNASequenceStream dnaStream, String pattern) {
    long totalMatches = 0;
    String chunk;
    dnaStream.reset();

    while ((chunk = dnaStream.nextChunk()) != null) {
      List<Integer> matches = kmpSearch(chunk, pattern);
      totalMatches += matches.size();
    }

    return totalMatches;
  }

  private static long searchPatternStreamBM(DNASequenceStream dnaStream, String pattern) {
    long totalMatches = 0;
    String chunk;
    dnaStream.reset();

    while ((chunk = dnaStream.nextChunk()) != null) {
      List<Integer> matches = boyerMooreSearch(chunk, pattern);
      totalMatches += matches.size();
    }

    return totalMatches;
  }

  private static long searchPatternsStreamAC(DNASequenceStream dnaStream, String[] patterns) {
    long totalMatches = 0;
    String chunk;
    dnaStream.reset();
    AhoCorasickNode root = buildAhoCorasickTrie(patterns);

    while ((chunk = dnaStream.nextChunk()) != null) {
      List<Integer> matches = ahoCorasickSearch(chunk, patterns);
      totalMatches += matches.size();
    }

    return totalMatches;
  }

  private static void printTrie(AhoCorasickNode node, String prefix, String childPrefix) {
    System.out.println(prefix + (node.outputs != null ? node.outputs : "[]"));
    for (Map.Entry<Character, AhoCorasickNode> entry : node.children.entrySet()) {
      boolean isLast = entry.getKey().equals(node.children.lastKey());
      System.out.println(childPrefix + (isLast ? "└── " : "├── ") + entry.getKey());
      printTrie(entry.getValue(), childPrefix + (isLast ? "    " : "│   "), childPrefix + (isLast ? "    " : "│   "));
    }
  }

  private static int countTrieNodes(AhoCorasickNode node) {
    int count = 1;
    for (AhoCorasickNode child : node.children.values()) {
      count += countTrieNodes(child);
    }
    return count;
  }

}