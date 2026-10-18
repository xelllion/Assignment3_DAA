package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GraphGenerator {
    private static void checkSize(int vertices, int edges, boolean directed, int minimum) {
        long maximum = (long) vertices * (vertices - 1);
        if (!directed) maximum /= 2;
        if (vertices < 1 || edges < minimum || edges > maximum) {
            throw new IllegalArgumentException("Impossible graph size");
        }
    }

    private static void addUnique(Graph graph, Set<Long> used, int from, int to, Random random) {
        if (from == to) return;
        int a = from;
        int b = to;
        if (!graph.isDirected() && a > b) { int temp = a; a = b; b = temp; }
        long key = (long) a * graph.vertexCount() + b;
        if (used.add(key)) graph.addEdge(from, to, 1 + random.nextInt(100));
    }

    public static Graph connected(int vertices, int edges, long seed) {
        checkSize(vertices, edges, false, vertices - 1);
        Graph graph = new Graph(vertices, false);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        // Every new vertex joins the earlier vertices, which guarantees connectivity.
        for (int v = 1; v < vertices; v++) addUnique(graph, used, v, random.nextInt(v), random);
        while (graph.edgeCount() < edges) {
            addUnique(graph, used, random.nextInt(vertices), random.nextInt(vertices), random);
        }
        return graph;
    }

    public static Graph dag(int vertices, int edges, long seed) {
        checkSize(vertices, edges, false, 0);
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        int[] order = new int[vertices];
        for (int i = 0; i < vertices; i++) order[i] = i;
        for (int i = vertices - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = order[i]; order[i] = order[j]; order[j] = temp;
        }
        while (graph.edgeCount() < edges) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (a > b) { int temp = a; a = b; b = temp; }
            if (a != b) addUnique(graph, used, order[a], order[b], random);
        }
        return graph;
    }

    public static Graph cyclic(int vertices, int edges, long seed) {
        checkSize(vertices, edges, true, 3);
        if (vertices < 3) throw new IllegalArgumentException("A three-vertex cycle needs three vertices");
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        addUnique(graph, used, 0, 1, random);
        addUnique(graph, used, 1, 2, random);
        addUnique(graph, used, 2, 0, random);
        while (graph.edgeCount() < edges) {
            addUnique(graph, used, random.nextInt(vertices), random.nextInt(vertices), random);
        }
        return graph;
    }
}
