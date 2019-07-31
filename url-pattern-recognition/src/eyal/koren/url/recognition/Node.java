package eyal.koren.url.recognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A tree implementation for the problem of recognizing high-variance parts within hierarchical structures and substituting them with a
 * wildcard. The root Node would represent the forest of separate hierarchies, and the first level children are the roots of the tree
 * hierarchies.
 *
 * Paths are analyzed trough the following algorithm:
 *  1.  traversing the tree, incrementing counters of all nodes along the path, creating missing nodes
 *  2.  after reaching leaf node, traversing back, collapsing levels (the groups of sibling nodes) with variance that crosses a threshold:
 *      If node is insignificant (checking against a threshold)- merge its contents into the wildcard node.
 *      Merging includes:
 *      a.  account merged node's occurrences to the wildcard node's occurrences
 *      b.  account merged node's children occurrences to the wildcard node's children occurrences
 *      c.  add child nodes from the merged node to the wildcard nodes if such do not already exist
 *      d.  recursively merge child nodes from the merged node into corresponding existing child nodes of the wildcard node
 *
 * Analysis should be done by a single thread. Implementation of multi-threaded environment should ensure that, eg by using a queue for
 * multiple writers and a single reader.
 */
@SuppressWarnings("WeakerAccess")
public class Node {

    /*************************************************************************************************************************************
     *                                                ALGORITHM CONFIGURATION
     ************************************************************************************************************************************/
    public static final String WILDCARD = "*";
    public static final String SEPARATOR = "/";
    private static final float MIN_NODE_SIGNIFICANCE = 0.1f;
    private static final float MAX_LEVEL_VARIANCE = 0.05f;
    /************************************************************************************************************************************/

    private int numOccurrences;
    private int childrenOccurrences;

    // If occurrences within a level spread evenly across nodes, then the max num of nodes would be (1 / Configuration.MAX_LEVEL_VARIANCE),
    // so we initialize with a load factor of 0.5, and make sure to maintain this load factor
    private Map<String, Node> children = new HashMap<>((int) (2 / MAX_LEVEL_VARIANCE), 0.5f);

    private Node wildcardNode;

    /*************************************************************************************************************************************
     *                                                        ALGORITHM
     ************************************************************************************************************************************/

    public void analyze(String path) {
        // todo: consider other methods that do not enforce allocation, eg a Trie, also probably better not do recursively
        analyzeRecursively(path.split(SEPARATOR), 0);
    }

    private void analyzeRecursively(String[] pathParts, int index) {
        numOccurrences++;
        if (index == pathParts.length) {
            return;
        }
        childrenOccurrences++;
        Node subTree = getOrCreateChild(pathParts[index]);
        subTree.analyzeRecursively(pathParts, index + 1);
        collapseChildren();
    }

    /**
     * If a child node corresponding the given key exists - return it.
     * Otherwise, if a wildcard child node already exists - return it.
     * Otherwise, create a new child node and return it.
     *
     * NOTE: THIS MEANS THAT THE COLLAPSE PROCESS IS IRREVERSIBLE AND ALL KEYS ENCOUNTERED AFTER IT'S CREATION WILL BE CONSIDERED AS
     * MATCHING THE WILDCARD.
     *
     * @param key child node key
     * @return the node corresponding the given key
     */
    private Node getOrCreateChild(String key) {
        Node child = children.getOrDefault(key, wildcardNode);
        if (child == null) {
            child = new Node();
            children.put(key, child);
        }
        return child;
    }

    /**
     * Collapses the children of this node if variance crosses the threshold.
     * Collapse only those child nodes that are insignificant enough.
     */
    private void collapseChildren() {
        if ((float) children.size() / childrenOccurrences < MAX_LEVEL_VARIANCE) {
            return;
        }

        boolean wildcardNodeCreated = false;
        // todo - avoid iterator allocation
        Iterator<Node> entryIterator = children.values().iterator();
        while (entryIterator.hasNext()) {
            Node childNode = entryIterator.next();
            if ((float) childNode.numOccurrences / childrenOccurrences < MIN_NODE_SIGNIFICANCE && childNode != wildcardNode) {
                // collapse this child into the wildcard node if not significant enough, unless it is the existing wildcard child
                entryIterator.remove();
                if (wildcardNode == null) {
                    wildcardNode = new Node();
                    wildcardNodeCreated = true;
                }
                wildcardNode.merge(childNode);
            }
        }

        if (wildcardNodeCreated) {
            this.children.put(WILDCARD, wildcardNode);
        }
    }

    /**
     * Merges another node's data with this node. Besides incrementing the occurrences counters, this requires a recursive merge of
     * children (so equivalent children of this node and the other node are merged as well, and same for their children).
     * @param other node to be merged into this node
     */
    private void merge(Node other) {
        numOccurrences += other.numOccurrences;
        childrenOccurrences += other.childrenOccurrences;
        other.children.forEach((pathPart, otherChild) -> {
            Node thisChild = this.children.get(pathPart);
            if (thisChild == null) {
                this.children.put(pathPart, otherChild);
            } else {
                thisChild.merge(otherChild);
            }
        });
    }


    /*************************************************************************************************************************************
     *                                                         UTILITIES
     ************************************************************************************************************************************/

    public void addToStructure(String path) {
        addRecursively(path.split(SEPARATOR), 0);
    }

    private void addRecursively(String[] pathParts, int index) {
        if (index == pathParts.length) {
            return;
        }
        Node subTree = getOrCreateChild(pathParts[index]);
        subTree.addRecursively(pathParts, index + 1);
    }

    public List<String> getAllPaths() {
        ArrayList<String> paths = new ArrayList<>();
        getAllPathRecursively("", paths);
        return paths;
    }

    private void getAllPathRecursively(final String path, List<String> paths) {
        if (children.isEmpty()) {
            paths.add(path);
        } else {
            children.forEach((pathPart, child) -> {
                String separator = (path.isEmpty()) ? "" : SEPARATOR;
                child.getAllPathRecursively(path + separator + pathPart, paths);
            });
        }
    }

    public boolean equalStructure(Node other) {
        return equalStructureRecursion(other, "");
    }

    private boolean equalStructureRecursion(Node other, String path) {
        boolean structureIsEqual = true;
        for (Map.Entry<String, Node> entry : other.children.entrySet()) {
            String pathPart = entry.getKey();
            Node otherChild = entry.getValue();
            String fullPath = path + pathPart;
            Node thisChild = children.get(pathPart);
            if (thisChild == null) {
                System.out.println("Cannot find path " + fullPath);
                structureIsEqual = false;
            } else {
                structureIsEqual &= thisChild.equalStructureRecursion(otherChild, fullPath + SEPARATOR);
            }
        }
        return structureIsEqual;
    }

    public void print() {
        printRecursively("   ", childrenOccurrences);
    }

    private void printRecursively(String space, int levelOccurrences) {
        children.forEach((pathPart, node) -> {
            System.out.println(space + pathPart + " (" + node.numOccurrences + SEPARATOR + levelOccurrences + ")");
            node.printRecursively(space + "   ", node.childrenOccurrences);
        });
    }
}
