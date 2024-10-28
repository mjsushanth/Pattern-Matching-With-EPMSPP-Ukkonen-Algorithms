import java.util.*;

/*
 * EPMSPPAlgorithm class implements the Exact Multiple Pattern Matching Algorithm
 * using DNA Sequence and Pattern Pair (EPMSPP). This algorithm is designed to efficiently search
 * for DNA patterns in large sequences.
 */

public class EPMSPPAlgorithm {
  private static final int ALPHABET_SIZE = 16; // 4^2 for DNA pairs


  /*
   * Main method to find matches of a pattern in a sequence; Implements the Exact Multiple
   * Pattern Matching Algorithm using DNA Sequence and Pattern Pair (EPMSPP).
   * This algorithm is designed to efficiently search for DNA patterns in large sequences
   */
  public static List<Integer> findMatches(String sequence, String pattern) {
    List<Integer> matches = new ArrayList<>();

    // Return empty list if pattern is longer than sequence.
    if (pattern.length() > sequence.length()) return matches;

    // Create index tables for both sequence and pattern
    // These tables store the positions of each DNA pair, reducing unnecessary comparisons.
    int[][] stab = createIndexTable(sequence);
    int[][] ptab = createIndexTable(pattern);

    // Find the least frequent pair in the pattern
    // This is a key optimization of EPMSPP, as it minimizes the number of potential match positions
    int leastFrequentPair = findLeastFrequentPair(ptab, stab);
    int leastFrequentPairPos = ptab[leastFrequentPair][0];

    // Iterate through potential match positions
    // We only check positions where the least frequent pair occurs, greatly reducing comparisons
    for (int i = 0; i < stab[leastFrequentPair].length && stab[leastFrequentPair][i] != -1; i++) {
      int potentialMatchPos = stab[leastFrequentPair][i] - leastFrequentPairPos;

      // change1: if (potentialMatchPos < 0) continue;
      if (potentialMatchPos < 0 || potentialMatchPos + pattern.length() > sequence.length()) continue;

      // Check if the potential match is a full match.
      // We compare pairs of characters, which is more efficient than character-by-character comparison
      boolean match = true;

      for (int j = 0; j < pattern.length() - 1; j++) {
        int pairIndex = computePairIndex(pattern.charAt(j), pattern.charAt(j + 1));
        int seqPairIndex = computePairIndex(sequence.charAt(potentialMatchPos + j),
                sequence.charAt(potentialMatchPos + j + 1));
        if (pairIndex != seqPairIndex) { match = false; break; }
      }


      if (match) {  matches.add(potentialMatchPos); }
    }

    return matches;
  }



  /*
   * Creates an index table for a given string
   * This is a key component of the EPMSPP algorithm, reducing the search space
   * The table stores positions of each DNA pair in the string
   */
  private static int[][] createIndexTable(String str) {
    int[][] table = new int[ALPHABET_SIZE][str.length()];
    for (int[] row : table) Arrays.fill(row, -1);

    int[] counts = new int[ALPHABET_SIZE];

    // Populate the table with positions of each DNA pair
    // This allows for quick lookup of pair positions later
    for (int i = 0; i < str.length() - 1; i++) {
      int index = computePairIndex(str.charAt(i), str.charAt(i + 1));
      table[index][counts[index]++] = i;
    }
    return table;
  }



  /*
   * Computes the index for a pair of DNA bases
   * Uses bit manipulation for efficiency, as suggested in the research paper

   * For example:
   * For the pair 'AC': A (00) << 2 gives 0000, C (01) gives 01, Res: 0001 (1 in decimal)
   * For the pair 'GT': G (10) << 2 gives 1000, T (11) gives 11, Res: 1011 (11 in decimal)   */
  private static int computePairIndex(char c1, char c2) {

    // charToIndex(c1) & 3: This ensures we only get the last 2 bits (3 in binary is 11).
    // (charToIndex(c1) & 3) << 2: This shifts the bits to the left by 2 positions.
    // OR operation |: Combines the bits of the two characters to form a unique index.
    return ((charToIndex(c1) & 3) << 2) | (charToIndex(c2) & 3);
  }


  /*
   * Converts a DNA base character to its corresponding index
   * This mapping is crucial for the pair indexing system
   */
  private static int charToIndex(char c) {
    switch (c) {
      case 'A': return 0;
      case 'C': return 1;
      case 'G': return 2;
      case 'T': return 3;
      default: throw new IllegalArgumentException("Invalid DNA base: " + c);
    }
  }

  /*
   * Finds the least frequent pair in the pattern
   * This is a key optimization in EPMSPP, significantly reducing the number of comparisons
   */
  private static int findLeastFrequentPair(int[][] ptab, int[][] stab) {
    int leastFrequent = -1;
    int minCount = Integer.MAX_VALUE;

    for (int i = 0; i < ALPHABET_SIZE; i++) {
      int pCount = 0;
      int sCount = 0;
      while (pCount < ptab[i].length && ptab[i][pCount] != -1) pCount++;
      while (sCount < stab[i].length && stab[i][sCount] != -1) sCount++;

      if (pCount > 0 && sCount < minCount) {
        minCount = sCount;
        leastFrequent = i;
      }
    }

    return leastFrequent;
  }















  /* -------------------------------------------------------------------------- */
  /* -------------------------------------------------------------------------- */
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

  /* -------------------------------------------------------------------------- */

  // New helper methods: for displaying context around random matches.
  private static void printRandomMatchContext(String sequence
          , String pattern, List<Integer> matches) {
    if (matches.size() < 1) {
      System.out.println("Not enough matches to show context.");
      return;
    }

    List<Integer> selectedPositions = selectRandomPositions(matches, 2);
    for (int position : selectedPositions) {
      String context = extractContext(sequence, position, pattern.length(), 10);
      System.out.println("Context at position " + position + ": " + context);
    }
  }

  // Helper method to select random positions from a list.
  private static List<Integer> selectRandomPositions(List<Integer> positions, int count) {

    if (positions.size() <= count) { return new ArrayList<>(positions); }

    List<Integer> copy = new ArrayList<>(positions);
    Collections.shuffle(copy);
    return copy.subList(0, count);
  }


  // Helper method to extract context around a position in a sequence.
  private static String extractContext(String sequence
          , int position, int patternLength, int contextSize) {
    int start = Math.max(0, position - contextSize);
    int end = Math.min(sequence.length(), position + patternLength + contextSize);

    StringBuilder sb = new StringBuilder();

    // start with ellipsis if we didn't start at the beginning of the sequence.
    if (start > 0) sb.append("...");

    // start and end are the indices of the substring to extract.
    sb.append(sequence, start, position);
    // encase the pattern in square brackets.
    sb.append("["); sb.append(sequence, position, position + patternLength); sb.append("]");
    sb.append(sequence, position + patternLength, end);

    // if we didn't reach the end of the sequence, add ellipsis.
    if (end < sequence.length()) sb.append("...");

    return sb.toString();
  }


  /* -------------------------------------------------------------------------- */


  public static void main(String[] args) {
    DNAGenerator generator = new DNAGenerator();
    long startTime, endTime, totalStartTime, totalEndTime;

    // Time the sequence generation
    startTime = System.nanoTime();
    String sequence = generator.generateSequence(100000000); //  100 million bases

    endTime = System.nanoTime();
    System.out.println("Sequence generation time: " + (endTime - startTime) / 1e9 + " seconds");
    System.out.println("Sequence length: " + sequence.length());

    List<String> patterns = new ArrayList<>();
    patterns.add(sequence.substring(3000, 3010)); // 10-character pattern
    patterns.add(generator.generatePattern(12));  // Random 12-character pattern
    patterns.add(generator.generatePattern(14));  // Random 14-character pattern

    totalStartTime = System.nanoTime();
    double totalMatchingTime = 0;

    for (String pattern : patterns) {
      System.out.println("\nPattern: " + pattern);

      // Time the pattern matching
      startTime = System.nanoTime();
      List<Integer> matches = findMatches(sequence, pattern);
      endTime = System.nanoTime();

      // Calculate and print matching time:
      double matchingTime = (endTime - startTime) / 1e9;
      totalMatchingTime += matchingTime;

      System.out.println("Matching time: " + String.format("%.6f", matchingTime) + " seconds");
      System.out.println("Matches found at positions: " + matches);
      System.out.println("Number of matches: " + matches.size());

      // Calculate and print matching speed
      double matchingSpeed = sequence.length() / (matchingTime * 1e6); // Million bases per second
      System.out.println("Matching speed: " + String.format("%.2f", matchingSpeed) + " Mbp/s");

      // Print context for two random matches
      printRandomMatchContext(sequence, pattern, matches);
    }

    totalEndTime = System.nanoTime();
    double totalTime = (totalEndTime - totalStartTime) / 1e9;

    System.out.println("\nTotal Statistics:");
    System.out.println("Total time taken for all patterns: " + String.format("%.6f", totalTime) + " seconds");
    System.out.println("Sum of individual matching times: " + String.format("%.6f", totalMatchingTime) + " seconds");
    System.out.println("Average matching time per pattern: " + String.format("%.6f", totalMatchingTime / patterns.size()) + " seconds");
    System.out.println("Average matching speed: " + String.format("%.2f", (sequence.length() * patterns.size()) / (totalMatchingTime * 1e6)) + " Mbp/s");
  }


}


/* Notes:
  * Mbp/s stands for "Million base pairs per second.
  * In genomics, a base pair is a unit of DNA (A-T or C-G). For single-stranded DNA or RNA, we often
  just say "bases." This metric tells us how many million DNA bases the algorithm can process in
  one second.
  * By dividing by 1e9, we convert nanoseconds to seconds.
  * */