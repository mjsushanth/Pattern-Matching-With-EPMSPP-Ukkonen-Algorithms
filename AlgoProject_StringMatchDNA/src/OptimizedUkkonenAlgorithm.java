import java.util.*;

public class OptimizedUkkonenAlgorithm {
  private static final int ALPHABET_SIZE = 4; // For DNA: A, C, G, T

  static class Node {
    Node[] children;
    int start;
    int end;
    int suffixLink;

    Node(int start, int end) {
      this.children = new Node[ALPHABET_SIZE];
      this.start = start;
      this.end = end;
      this.suffixLink = -1;
    }

    @Override
    public String toString() {
      return "Node{start=" + start + ", end=" + end + ", suffixLink=" + suffixLink + "}";
    }
  }

  static class SuffixTree {
    byte[] text;
    Node root;
    Node activeNode;
    int activeEdge;
    int activeLength;
    int remainingSuffixCount;
    int leafEnd;
    int size;
    List<Node> nodes;

    SuffixTree(String input) {
      System.out.println("Constructing SuffixTree for input of length: " + input.length());
      text = new byte[input.length() + 1];
      for (int i = 0; i < input.length(); i++) {
        text[i] = charToByte(input.charAt(i));
      }
      text[input.length()] = 0; // terminator

      nodes = new ArrayList<>();
      root = new Node(-1, -1);
      nodes.add(root);
      activeNode = root;
      activeEdge = -1;
      activeLength = 0;
      remainingSuffixCount = 0;
      leafEnd = -1;
      size = input.length();

      for (int i = 0; i < text.length; i++) {
        // System.out.println("Processing character " + i + " of " + text.length);
        extendSuffixTree(i);
      }
      System.out.println("SuffixTree construction completed.");
    }

    private byte charToByte(char c) {
      switch (c) {
        case 'A': return 0;
        case 'C': return 1;
        case 'G': return 2;
        case 'T': return 3;
        default: throw new IllegalArgumentException("Invalid DNA base: " + c);
      }
    }

    private char byteToChar(byte b) {
      switch (b) {
        case 0: return 'A';
        case 1: return 'C';
        case 2: return 'G';
        case 3: return 'T';
        default: return '$';
      }
    }

    void extendSuffixTree(int pos) {
      // System.out.println("Extending SuffixTree for position: " + pos);
      leafEnd = pos;
      remainingSuffixCount++;
      int lastNewNodeIndex = -1;

      while (remainingSuffixCount > 0) {
        // System.out.println("  Remaining suffix count: " + remainingSuffixCount);
        if (activeLength == 0) {
          activeEdge = pos;
        }
        // System.out.println("  ActiveNode: " + activeNode + ", ActiveEdge: " + activeEdge + ", ActiveLength: " + activeLength);

        if (activeNode.children[text[activeEdge]] == null) {
          // System.out.println("  Creating new leaf node");
          activeNode.children[text[activeEdge]] = new Node(pos, size);
          nodes.add(activeNode.children[text[activeEdge]]);
          if (lastNewNodeIndex != -1) {
            // System.out.println("  Setting suffix link for last new node");
            nodes.get(lastNewNodeIndex).suffixLink = nodes.indexOf(activeNode);
            lastNewNodeIndex = -1;
          }
        } else {
          Node next = activeNode.children[text[activeEdge]];
          // System.out.println("  Existing node found: " + next);
          if (walkDown(next)) {
            // System.out.println("  Walked down, continuing");
            continue;
          }
          if (text[next.start + activeLength] == text[pos]) {
            // System.out.println("  Characters match, incrementing activeLength");
            activeLength++;
            if (lastNewNodeIndex != -1) {
              // System.out.println("  Setting suffix link for last new node");
              nodes.get(lastNewNodeIndex).suffixLink = nodes.indexOf(activeNode);
            }
            break;
          }
          // System.out.println("  Splitting edge");
          Node split = new Node(next.start, next.start + activeLength);
          nodes.add(split);
          activeNode.children[text[activeEdge]] = split;
          split.children[text[pos]] = new Node(pos, size);
          nodes.add(split.children[text[pos]]);
          next.start += activeLength;
          split.children[text[next.start]] = next;

          if (lastNewNodeIndex != -1) {
            // System.out.println("  Setting suffix link for last new node");
            nodes.get(lastNewNodeIndex).suffixLink = nodes.indexOf(split);
          }
          lastNewNodeIndex = nodes.indexOf(split);
        }

        remainingSuffixCount--;
        if (activeNode == root && activeLength > 0) {
          // System.out.println("  At root, decrementing activeLength");
          activeLength--;
          activeEdge = pos - remainingSuffixCount + 1;
        } else if (activeNode != root) {
          // System.out.println("  Following suffix link");
          activeNode = (activeNode.suffixLink != -1) ? nodes.get(activeNode.suffixLink) : root;
        }
      }
      // System.out.println("Finished extending for position: " + pos);
    }

    boolean walkDown(Node node) {
      int edgeLength = edgeLength(node);
      if (activeLength >= edgeLength) {
        activeEdge += edgeLength;
        activeLength -= edgeLength;
        activeNode = node;
        // System.out.println("  Walked down to node: " + node);
        return true;
      }
      return false;
    }

    int edgeLength(Node node) {
      return Math.min(node.end, leafEnd + 1) - node.start;
    }

    List<Integer> findPattern(String pattern) {
      System.out.println("Searching for pattern: " + pattern);
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
              // System.out.println("  Pattern fully matched, collecting leaf positions");
              collectLeafPositions(node, positions);
              break;
            }
          } else if (i == pattern.length()) {
            // System.out.println("  Pattern fully matched, collecting leaf positions");
            collectLeafPositions(node, positions);
            break;
          } else {
            // System.out.println("  Mismatch found, stopping search");
            break;
          }
        } else {
          // System.out.println("  No matching child found, stopping search");
          break;
        }
      }
      System.out.println("Search completed, found " + positions.size() + " matches");
      return positions;
    }

    void collectLeafPositions(Node node, List<Integer> positions) {
      if (isLeaf(node)) {
        positions.add(node.start);
      } else {
        for (Node child : node.children) {
          if (child != null) {
            collectLeafPositions(child, positions);
          }
        }
      }
    }

    boolean isLeaf(Node node) {
      return node.end == size;
    }
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



  public static void main(String[] args) {
    DNAGenerator generator = new DNAGenerator();
    long startTime, endTime, totalStartTime, totalEndTime;

    // Test with different sequence lengths
    int[] sequenceLengths = {1000000}; // 1 million

    for (int length : sequenceLengths) {
      System.out.println("\n--- Testing with sequence length: " + length + " ---");

      totalStartTime = System.nanoTime();

      startTime = System.nanoTime();
      String sequence = generator.generateSequence(length);
      endTime = System.nanoTime();
      System.out.println("Sequence generation time: " + String.format("%.6f", (endTime - startTime) / 1e9) + " seconds");

      List<String> patterns = new ArrayList<>();
      patterns.add(sequence.substring(1000, 1010)); // 10-character substring
      patterns.add(generator.generatePattern(15));  // Random 15-character pattern

      try {
        startTime = System.nanoTime();
        SuffixTree tree = new SuffixTree(sequence);
        endTime = System.nanoTime();
        System.out.println("Suffix tree construction time: " + String.format("%.6f", (endTime - startTime) / 1e9) + " seconds");

        double totalMatchingTime = 0;

        for (String pattern : patterns) {
          System.out.println("\nSearching for pattern: " + pattern);
          startTime = System.nanoTime();
          List<Integer> matches = tree.findPattern(pattern);
          endTime = System.nanoTime();

          double matchingTime = (endTime - startTime) / 1e9;
          totalMatchingTime += matchingTime;

          System.out.println("Matching time: " + String.format("%.6f", matchingTime) + " seconds");
          System.out.println("Number of matches: " + matches.size());

          // Print context for up to 2 matches
          printRandomMatchContext(sequence, pattern, matches);
        }

        System.out.println("\nPattern Matching Statistics:");
        System.out.println("Total pattern matching time: " + String.format("%.6f", totalMatchingTime) + " seconds");
        System.out.println("Average matching time per pattern: " + String.format("%.6f", totalMatchingTime / patterns.size()) + " seconds");

        totalEndTime = System.nanoTime();
        double totalExecutionTime = (totalEndTime - totalStartTime) / 1e9;
        System.out.println("\nTotal Execution Statistics:");
        System.out.println("Total time taken (including sequence generation and tree construction): " + String.format("%.6f", totalExecutionTime) + " seconds");

      } catch (Exception e) {
        System.out.println("An error occurred: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }



}