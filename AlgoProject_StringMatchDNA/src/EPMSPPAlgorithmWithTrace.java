import java.util.*;

public class EPMSPPAlgorithmWithTrace {
  private static final int ALPHABET_SIZE = 16; // 4^2 for DNA pairs
  private static int comparisons = 0;

  public static void main(String[] args) {
    DNAGenerator generator = new DNAGenerator();
    String sequence = generator.generateSequence(1000); // Shorter for tracing

    List<String> patterns = new ArrayList<>();
    patterns.add(sequence.substring(100, 106)); // 6-mer
    patterns.add(sequence.substring(200, 208)); // 8-mer
    patterns.add(generator.generatePattern(7)); // Random 7-mer

    System.out.println("\nSequence length: " + sequence.length());
    for (String pattern : patterns) {
      System.out.println("\nPattern: " + pattern);
      comparisons = 0; // Reset comparisons counter
      List<Integer> matches = findMatches(sequence, pattern);
      System.out.println("Matches found at positions: " + matches);
      System.out.println("Number of matches: " + matches.size());
      System.out.println("Total comparisons made: " + comparisons);

      printRandomMatchContext(sequence, pattern, matches);
    }
  }

  public static List<Integer> findMatches(String sequence, String pattern) {
    List<Integer> matches = new ArrayList<>();
    if (pattern.length() > sequence.length()) return matches;

    System.out.println("\nCreating index tables...");
    int[][] stab = createIndexTable(sequence);
    int[][] ptab = createIndexTable(pattern);

    printIndexTable("Sequence", stab);
    printIndexTable("Pattern", ptab);

    System.out.println("\nFinding least frequent pair...");
    int leastFrequentPair = findLeastFrequentPair(ptab, stab);
    int leastFrequentPairPos = ptab[leastFrequentPair][0];
    System.out.println("Least frequent pair index: " + leastFrequentPair +
            ", position in pattern: " + leastFrequentPairPos);

    System.out.println("\nSearching for matches...");
    for (int i = 0; i < stab[leastFrequentPair].length && stab[leastFrequentPair][i] != -1; i++) {
      int potentialMatchPos = stab[leastFrequentPair][i] - leastFrequentPairPos;
      // System.out.println("Checking potential match at position: " + potentialMatchPos);

      if (potentialMatchPos < 0) {
        // System.out.println("Position out of bounds, skipping...");
        continue;
      }

      boolean match = true;
      for (int j = 0; j < pattern.length() - 1; j++) {
        comparisons++;
        int pairIndex = computePairIndex(pattern.charAt(j), pattern.charAt(j + 1));
        int seqPairIndex = computePairIndex(sequence.charAt(potentialMatchPos + j),
                sequence.charAt(potentialMatchPos + j + 1));
        //System.out.println("Comparing pair at position " + j + ": " +
        //        "Pattern[" + pairIndex + "] vs Sequence[" + seqPairIndex + "]");
        if (pairIndex != seqPairIndex) {
          // System.out.println("Mismatch found, breaking...");
          match = false;
          break;
        }
      }

      if (match) {
        // System.out.println("Match found at position: " + potentialMatchPos);
        matches.add(potentialMatchPos);
      }
    }

    return matches;
  }

  private static int[][] createIndexTable(String str) {
    int[][] table = new int[ALPHABET_SIZE][str.length()];
    for (int[] row : table) Arrays.fill(row, -1);

    int[] counts = new int[ALPHABET_SIZE];

    for (int i = 0; i < str.length() - 1; i++) {
      int index = computePairIndex(str.charAt(i), str.charAt(i + 1));
      table[index][counts[index]++] = i;
    }

    return table;
  }

  private static int computePairIndex(char c1, char c2) {
    return ((charToIndex(c1) & 3) << 2) | (charToIndex(c2) & 3);
  }

  private static int charToIndex(char c) {
    switch (c) {
      case 'A': return 0;
      case 'C': return 1;
      case 'G': return 2;
      case 'T': return 3;
      default: throw new IllegalArgumentException("\nInvalid DNA base: " + c);
    }
  }

  private static int findLeastFrequentPair(int[][] ptab, int[][] stab) {
    int leastFrequent = -1;
    int minCount = Integer.MAX_VALUE;

    for (int i = 0; i < ALPHABET_SIZE; i++) {
      int pCount = 0;
      int sCount = 0;
      while (pCount < ptab[i].length && ptab[i][pCount] != -1) pCount++;
      while (sCount < stab[i].length && stab[i][sCount] != -1) sCount++;

      System.out.println("Pair index " + i + ": Pattern count = " + pCount +
              ", Sequence count = " + sCount);

      if (pCount > 0 && sCount < minCount) {
        minCount = sCount;
        leastFrequent = i;
      }
    }

    return leastFrequent;
  }

  private static void printIndexTable(String label, int[][] table) {
    System.out.println(label + " Index Table:");
    for (int i = 0; i < ALPHABET_SIZE; i++) {
      System.out.print("Index " + i + ": ");
      for (int j = 0; j < table[i].length && table[i][j] != -1; j++) {
        System.out.print(table[i][j] + " ");
      }
      System.out.println();
    }
  }

  private static void printRandomMatchContext(String sequence, String pattern, List<Integer> matches) {
    if (matches.size() < 2) {
      System.out.println("Not enough matches to show context.");
      return;
    }

    List<Integer> selectedPositions = selectRandomPositions(matches, 2);
    for (int position : selectedPositions) {
      String context = extractContext(sequence, position, pattern.length(), 10);
      System.out.println("Context at position " + position + ": " + context);
    }
  }

  private static List<Integer> selectRandomPositions(List<Integer> positions, int count) {
    if (positions.size() <= count) {
      return new ArrayList<>(positions);
    }

    List<Integer> copy = new ArrayList<>(positions);
    Collections.shuffle(copy);
    return copy.subList(0, count);
  }

  private static String extractContext(String sequence, int position, int patternLength, int contextSize) {
    int start = Math.max(0, position - contextSize);
    int end = Math.min(sequence.length(), position + patternLength + contextSize);

    StringBuilder sb = new StringBuilder();
    if (start > 0) sb.append("...");
    sb.append(sequence, start, position);
    sb.append("[");
    sb.append(sequence, position, position + patternLength);
    sb.append("]");
    sb.append(sequence, position + patternLength, end);
    if (end < sequence.length()) sb.append("...");

    return sb.toString();
  }

  private static class DNAGenerator {
    private static final String DNA_BASES = "ACGT";
    private Random random;

    public DNAGenerator() {
      this.random = new Random();
    }

    public String generateSequence(int length) {
      StringBuilder sequence = new StringBuilder(length);
      for (int i = 0; i < length; i++) {
        sequence.append(DNA_BASES.charAt(random.nextInt(DNA_BASES.length())));
      }
      return sequence.toString();
    }

    public String generatePattern(int length) {
      return generateSequence(length);
    }
  }
}