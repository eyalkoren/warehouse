package eyal.koren.url.recognition;

import java.util.Random;

@SuppressWarnings("Duplicates")
public class Test {
    private static Random random = new Random();

    public static void main(String[] args) {
        Node baseForest = createSimplePathForestWithOnePathParameter();
        baseForest.print();
        System.out.println("============================================= ANALYSIS =====================================================");
        Node analyzedForest = analyzeForest(baseForest, false);
        for (String path : analyzedForest.getAllPaths()) {
            System.out.println(path);
        }
        System.out.println("============================================================================================================");
        analyzedForest.print();
        System.out.println(analyzedForest.equalStructure(baseForest));
    }

    private static Node createSimplePathForestWithoutPathParameters() {
        Node forest = new Node();
        forest.addToStructure("foo/bar/baz/quo");
        forest.addToStructure("foo/bar/baz/quo");
        forest.addToStructure("foo/bar/baz/quo");
        forest.addToStructure("foo/bar/biz");
        forest.addToStructure("foo/bar/biz/quo");
        forest.addToStructure("foo/bar");
        forest.addToStructure("foo/beer/baz/quo");
        forest.addToStructure("foo/beer/baz/quo");
        forest.addToStructure("foo/beer/biz/quo");
        forest.addToStructure("koo/beer/biz/quo");
        return forest;
    }

    private static Node createSimplePathForestWithOnePathParameter() {
        Node forest = new Node();
        forest.addToStructure("foo/bar/*/quo/roo");
        forest.addToStructure("foo/bar/*/qua/raa");
        forest.addToStructure("foo/bar/baz/quo");
        forest.addToStructure("foo/bar/baz/quo");
        forest.addToStructure("foo/bar/biz");
        forest.addToStructure("foo/bar/biz/quo");
        forest.addToStructure("foo/bar");
        forest.addToStructure("foo/beer/baz/quo");
        forest.addToStructure("foo/beer/baz/*");
        forest.addToStructure("foo/beer/biz/quo");
        forest.addToStructure("koo/beer/biz/quo");
        return forest;
    }

    private static Node createPathForestWithMultiplePathParameter() {
        Node forest = new Node();
        forest.addToStructure("foo/bar/*/quo");
        forest.addToStructure("foo/bar/*/*");
        forest.addToStructure("foo/bar/baz/quo");
        forest.addToStructure("foo/bar/biz");
        forest.addToStructure("foo/bar/biz/quo");
        forest.addToStructure("*/*");
        forest.addToStructure("foo/beer/baz/quo");
        forest.addToStructure("foo/*/baz/*");
        forest.addToStructure("foo/beer/biz/quo");
        forest.addToStructure("foo/beer/*/*/biz/quo");
        forest.addToStructure("koo/beer/biz/quo");
        return forest;
    }

    private static Node analyzeForest(Node baseForest, boolean print) {
        final StringBuilder substitution = new StringBuilder();
        Node analyzed = new Node();
        for (String path : baseForest.getAllPaths()) {
            for (int i=0; i<50; i++) {
                String forAnalysis;
                if (path.contains(Node.WILDCARD)) {
                    substitution.setLength(0);
                    for (int j = 0; j < path.length(); j++) {
                        char ch = path.charAt(j);
                        if (ch == '*') {
                            substitution.append(generateRandomString());
                        } else {
                            substitution.append(ch);
                        }
                    }
                    forAnalysis = substitution.toString();
                } else {
                    forAnalysis = path;
                }
                analyzed.analyze(forAnalysis);
                if (print) {
                    System.out.println(forAnalysis);
                }
            }
        }
        return analyzed;
    }

    private static String generateRandomString() {
        return String.valueOf(random.nextInt(999));
    }
}
