import java.util.*;

public class UkkonenAlgorithm {
  private static final int ALPHABET_SIZE = 4; // For DNA: A, C, G, T

  /*
   * Node class represents both internal nodes and leaves in the suffix tree.
   * It's a crucial component in Ukkonen's algorithm, allowing for efficient
   * tree construction and traversal.
   */
  static class Node {
    Node[] children;     // Array of child nodes, one for each possible DNA base
    int start;           // Start index of the substring this edge represents
    Integer end;         // End index of the substring (null for leaves)
    Node suffixLink;     // Suffix link to another node (crucial for Ukkonen's algorithm)

    Node(int start, Integer end) {
      this.children = new Node[ALPHABET_SIZE];
      this.start = start;
      this.end = end;
      this.suffixLink = null;
    }

    // Calculate the length of the edge leading to this node
    int edgeLength(int position) {
      return end == null ? position - start + 1 : end - start;
    }
  }

  /*
   * SuffixTree class implements Ukkonen's algorithm for linear-time suffix tree construction.
   * This data structure allows for efficient pattern matching in strings, particularly useful
   * for DNA sequence analysis.
   */
  static class SuffixTree {
    byte[] text;           // The input text (DNA sequence) stored as bytes for efficiency
    Node root;             // Root node of the suffix tree
    Node activeNode;       // Part of the "active point" in Ukkonen's algorithm
    int activeEdge = -1;   // Part of the "active point" in Ukkonen's algorithm
    int activeLength = 0;  // Part of the "active point" in Ukkonen's algorithm
    int remainingSuffixCount = 0;  // Number of suffixes yet to be added in current phase
    int leafEnd = -1;      // End index for leaf nodes
    int position = -1;     // Current position in the text being processed
    List<Node> nodes;      // List of all nodes, useful for operations on the entire tree

    /*
     * Constructor: Initializes the suffix tree and builds it using Ukkonen's algorithm.
     * Time Complexity: O(n) where n is the length of the input string.
     * Space Complexity: O(n) in the worst case.
     */
    SuffixTree(String input) {
      // Convert input string to byte array for memory efficiency
      text = new byte[input.length() + 1];
      for (int i = 0; i < input.length(); i++) {
        text[i] = charToByte(input.charAt(i));
      }
      text[input.length()] = 0; // Terminator character

      nodes = new ArrayList<>();
      root = new Node(-1, -1);
      nodes.add(root);
      activeNode = root;

      // Build the suffix tree
      for (int i = 0; i < text.length; i++) {
        extendSuffixTree(i);
      }
    }

    // Convert DNA character to byte representation
    private byte charToByte(char c) {
      switch (c) {
        case 'A': return 0;
        case 'C': return 1;
        case 'G': return 2;
        case 'T': return 3;
        default: throw new IllegalArgumentException("Invalid DNA base: " + c);
      }
    }

    // Convert byte representation back to DNA character
    private char byteToChar(byte b) {
      switch (b) {
        case 0: return 'A';
        case 1: return 'C';
        case 2: return 'G';
        case 3: return 'T';
        default: return '$'; // Terminator character
      }
    }

    /*
     * Core method of Ukkonen's algorithm: extends the suffix tree by one character.
     * This method implements the heart of the linear-time construction process.
     * Time Complexity: Amortized O(1) per character, leading to O(n) overall.
     */
    void extendSuffixTree(int pos) {
      leafEnd = pos;
      remainingSuffixCount++;
      int lastNewNodeIndex = -1;

      while (remainingSuffixCount > 0) {
        if (activeLength == 0) activeEdge = pos;

        if (activeNode.children[text[activeEdge]] == null) {
          // Create a new leaf node
          activeNode.children[text[activeEdge]] = new Node(pos, null);
          nodes.add(activeNode.children[text[activeEdge]]);
          if (lastNewNodeIndex != -1) {
            nodes.get(lastNewNodeIndex).suffixLink = activeNode;
            lastNewNodeIndex = -1;
          }
        } else {
          Node next = activeNode.children[text[activeEdge]];
          if (walkDown(next)) continue;

          if (text[next.start + activeLength] == text[pos]) {
            activeLength++;
            if (lastNewNodeIndex != -1) {
              nodes.get(lastNewNodeIndex).suffixLink = activeNode;
            }
            break;
          }

          // Split the edge
          Node split = new Node(next.start, next.start + activeLength);
          nodes.add(split);
          activeNode.children[text[activeEdge]] = split;
          split.children[text[pos]] = new Node(pos, null);
          nodes.add(split.children[text[pos]]);
          next.start += activeLength;
          split.children[text[next.start]] = next;

          if (lastNewNodeIndex != -1) {
            nodes.get(lastNewNodeIndex).suffixLink = split;
          }
          lastNewNodeIndex = nodes.indexOf(split);
        }

        remainingSuffixCount--;
        if (activeNode == root && activeLength > 0) {
          activeLength--;
          activeEdge = pos - remainingSuffixCount + 1;
        } else if (activeNode != root) {
          activeNode = activeNode.suffixLink != null ? activeNode.suffixLink : root;
        }
      }
    }

    /*
     * Helper method for the "walk down" procedure in Ukkonen's algorithm.
     * This method is crucial for maintaining the correct active point during tree extension.
     */
    boolean walkDown(Node node) {
      int edgeLength = edgeLength(node);
      if (activeLength >= edgeLength) {
        activeEdge += edgeLength;
        activeLength -= edgeLength;
        activeNode = node;
        return true;
      }
      return false;
    }

    // Calculate the length of an edge
    int edgeLength(Node node) {
      return node.end == null ? leafEnd - node.start + 1 : node.end - node.start;
    }

    /*
     * Pattern matching method: finds all occurrences of a pattern in the text.
     * This showcases the power of suffix trees for efficient string matching.
     * Time Complexity: O(m + k), where m is pattern length and k is number of occurrences.
     */
    List<Integer> findPattern(String pattern) {
      List<Integer> positions = new ArrayList<>();
      Node node = root;
      int i = 0;
      while (i < pattern.length()) {
        byte c = charToByte(pattern.charAt(i));
        if (node.children[c] != null) {
          node = node.children[c];
          int j = 0;
          while (j < edgeLength(node) && i < pattern.length() &&
                  text[node.start + j] == charToByte(pattern.charAt(i))) {
            i++;
            j++;
          }
          if (j == edgeLength(node)) {
            if (i == pattern.length()) {
              collectLeafPositions(node, positions);
              break;
            }
          } else if (i == pattern.length()) {
            collectLeafPositions(node, positions);
            break;
          } else {
            break;
          }
        } else {
          break;
        }
      }
      return positions;
    }

    /*
     * Recursive method to collect all leaf positions under a given node.
     * This is used to find all occurrences of a pattern in the text.
     */
    void collectLeafPositions(Node node, List<Integer> positions) {
      if (isLeaf(node)) {
        positions.add(text.length - node.start - 1);
      } else {
        for (Node child : node.children) {
          if (child != null) {
            collectLeafPositions(child, positions);
          }
        }
      }
    }

    // Check if a node is a leaf node
    boolean isLeaf(Node node) {
      return node.end == null;
    }
  }

  // DNA sequence generator class (unchanged)
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

  // Utility methods for printing match contexts (unchanged)
  private static void printRandomMatchContext(String sequence, String pattern, List<Integer> matches) {
    if (matches.isEmpty()) {
      System.out.println("No matches found to show context.");
      return;
    }

    List<Integer> selectedPositions = selectRandomPositions(matches, Math.min(2, matches.size()));
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
    sb.append(sequence, position, Math.min(position + patternLength, sequence.length()));
    sb.append("]");
    sb.append(sequence, Math.min(position + patternLength, sequence.length()), end);
    if (end < sequence.length()) sb.append("...");

    return sb.toString();
  }

  /*
   * Main method to demonstrate the Ukkonen's algorithm for DNA sequence matching.
   * It generates a DNA sequence, constructs a suffix tree, and performs pattern matching.
   */
  public static void main(String[] args) {
    DNAGenerator generator = new DNAGenerator();
    long startTime, endTime, totalStartTime, totalEndTime;

    // Generate a DNA sequence
    startTime = System.nanoTime();
    String sequence = generator.generateSequence(1000000); // 1 million bases
    endTime = System.nanoTime();
    System.out.println("Sequence generation time: " + (endTime - startTime) / 1e9 + " seconds");
    System.out.println("Sequence length: " + sequence.length());

    // Define patterns to search for
    List<String> patterns = new ArrayList<>();
    patterns.add(sequence.substring(3000, 3010)); // 10-character pattern from the sequence
    patterns.add(generator.generatePattern(12));  // Random 12-character pattern
    patterns.add(generator.generatePattern(14));  // Random 14-character pattern

    totalStartTime = System.nanoTime();

    // Construct the suffix tree
    startTime = System.nanoTime();
    SuffixTree tree = new SuffixTree(sequence);
    endTime = System.nanoTime();
    System.out.println("Suffix tree construction time: " + (endTime - startTime) / 1e9 + " seconds");

    double totalMatchingTime = 0;

    // Perform pattern matching for each pattern
    for (String pattern : patterns) {
      System.out.println("\nPattern: " + pattern);

      startTime = System.nanoTime();
      List<Integer> matches = tree.findPattern(pattern);
      endTime = System.nanoTime();

      double matchingTime = (endTime - startTime) / 1e9;
      totalMatchingTime += matchingTime;

      System.out.println("Matching time: " + String.format("%.6f", matchingTime) + " seconds");
      System.out.println("Matches found at positions: " + matches);
      System.out.println("Number of matches: " + matches.size());

      double matchingSpeed = sequence.length() / (matchingTime * 1e6); // Million bases per second
      System.out.println("Matching speed: " + String.format("%.2f", matchingSpeed) + " Mbp/s");

      printRandomMatchContext(sequence, pattern, matches);
    }

    totalEndTime = System.nanoTime();
    double totalTime = (totalEndTime - totalStartTime) / 1e9;

    // Print overall statistics
    System.out.println("\nTotal Statistics:");
    System.out.println("Total time taken (including tree construction): " + String.format("%.6f", totalTime) + " seconds");
    System.out.println("Total matching time: " + String.format("%.6f", totalMatchingTime) + " seconds");
    System.out.println("Average matching time per pattern: " + String.format("%.6f", totalMatchingTime / patterns.size()) + " seconds");
    System.out.println("Average matching speed: " + String.format("%.2f", (sequence.length() * patterns.size()) / (totalMatchingTime * 1e6)) + " Mbp/s");
  }
}