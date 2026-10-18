import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import dsu.DisjointSet;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import graph.Metrics;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AlgorithmTests {
    private static int checks;

    public static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    public static void main(String[] args) {
        testDsu();
        testMst();
        testConnectivity();
        testShortestPaths();
        testGeneratorsAndRandomGraphs();
        testValidation();
        System.out.println("PASS: " + checks + " correctness checks (no -ea flag needed)");
    }

    private static void testDsu() {
        Metrics metrics = new Metrics();
        DisjointSet sets = new DisjointSet(8, metrics);
        check(sets.componentCount() == 8, "Initial DSU components");
        for (int step = 1; step < 8; step *= 2) {
            for (int i = 0; i < 8; i += 2 * step) check(sets.union(i, i + step), "Merge distinct sets");
        }
        long before = metrics.parentTraversals;
        sets.find(7);
        long first = metrics.parentTraversals - before;
        before = metrics.parentTraversals;
        sets.find(7);
        check(metrics.parentTraversals - before < first, "Compression shortens a repeated find");
        check(!sets.union(0, 7), "Repeated union does not merge");
        check(sets.componentCount() == 1, "One DSU component");
        for (int v = 0; v < 8; v++) check(sets.find(v) == sets.find(0), "Common representative");
        rejects(() -> sets.find(-1));
        System.out.println("PASS: DSU merging and path compression");
    }

    private static void testMst() {
        Graph small = new Graph(4, false);
        small.addEdge(0, 1, 1); small.addEdge(1, 2, 2); small.addEdge(2, 3, 3);
        small.addEdge(0, 3, 9); small.addEdge(0, 2, 8);
        compareMst(small, 6);
        Graph equal = new Graph(4, false);
        equal.addEdge(0, 1, 1); equal.addEdge(1, 2, 1);
        equal.addEdge(2, 3, 1); equal.addEdge(3, 0, 1); equal.addEdge(0, 2, 1);
        compareMst(equal, 3); // Several different MST edge sets are valid.
        Graph disconnected = new Graph(5, false);
        disconnected.addEdge(0, 1, 3); disconnected.addEdge(2, 3, 2);
        compareMst(disconnected, 5);
        check(!Kruskal.run(disconnected, new Metrics()).isConnected(), "Disconnected MST flag");
        compareMst(new Graph(1, false), 0);
        compareMst(new Graph(4, false), 0);
        Graph parallel = new Graph(3, false);
        parallel.addEdge(0, 0, -100); parallel.addEdge(0, 1, 8);
        parallel.addEdge(0, 1, -2); parallel.addEdge(1, 2, 0);
        compareMst(parallel, -2);
        Graph largeWeights = new Graph(3, false);
        largeWeights.addEdge(0, 1, Integer.MAX_VALUE);
        largeWeights.addEdge(1, 2, Integer.MAX_VALUE);
        compareMst(largeWeights, 2L * Integer.MAX_VALUE);
        for (int seed = 0; seed < 25; seed++) {
            Graph graph = GraphGenerator.connected(5, 7, seed);
            compareMst(graph, bruteForceMst(graph));
        }
        System.out.println("PASS: MST examples, forests, equal weights and exhaustive oracle");
    }

    private static void compareMst(Graph graph, long expected) {
        MstResult a = Kruskal.run(graph, new Metrics());
        MstResult b = Prim.run(graph, new Metrics());
        check(a.totalWeight == expected && b.totalWeight == expected, "Expected MST/forest weight");
        validateForest(graph, a);
        validateForest(graph, b);
    }

    public static void validateForest(Graph graph, MstResult result) {
        Graph forest = new Graph(graph.vertexCount(), false);
        long sum = 0;
        for (Edge edge : result.edges) {
            boolean exists = false;
            for (Edge original : graph.neighbors(edge.from)) {
                if (original.to == edge.to && original.weight == edge.weight) exists = true;
            }
            check(exists, "Forest edge exists in input");
            check(!reachable(forest, edge.from)[edge.to], "Forest contains no cycle");
            forest.addEdge(edge.from, edge.to, edge.weight);
            sum += edge.weight;
        }
        int components = 0;
        boolean[] covered = new boolean[graph.vertexCount()];
        for (int v = 0; v < covered.length; v++) {
            if (covered[v]) continue;
            components++;
            boolean[] original = reachable(graph, v);
            check(Arrays.equals(original, reachable(forest, v)), "Forest spans each input component");
            for (int i = 0; i < covered.length; i++) covered[i] |= original[i];
        }
        check(result.components == components, "Correct component count");
        check(result.edges.size() == graph.vertexCount() - components, "Forest edge count");
        check(sum == result.totalWeight, "Correct reported weight");
    }

    private static long bruteForceMst(Graph graph) {
        long best = Long.MAX_VALUE;
        int m = graph.edgeCount();
        for (int mask = 0; mask < (1 << m); mask++) {
            if (Integer.bitCount(mask) != graph.vertexCount() - 1) continue;
            Graph candidate = new Graph(graph.vertexCount(), false);
            long weight = 0;
            for (int i = 0; i < m; i++) {
                if ((mask & (1 << i)) != 0) {
                    Edge edge = graph.edges().get(i);
                    candidate.addEdge(edge.from, edge.to, edge.weight);
                    weight += edge.weight;
                }
            }
            boolean connected = true;
            for (boolean found : reachable(candidate, 0)) connected &= found;
            if (connected) best = Math.min(best, weight);
        }
        return best;
    }

    private static boolean[] reachable(Graph graph, int source) {
        boolean[] seen = new boolean[graph.vertexCount()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.offer(source); seen[source] = true;
        while (!queue.isEmpty()) {
            for (Edge edge : graph.neighbors(queue.poll())) {
                if (!seen[edge.to]) { seen[edge.to] = true; queue.offer(edge.to); }
            }
        }
        return seen;
    }

    private static void testConnectivity() {
        Graph graph = new Graph(6, true);
        graph.addEdge(0, 1, 1); graph.addEdge(1, 0, 1); graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1); graph.addEdge(3, 2, 1); graph.addEdge(3, 4, 1);
        validateScc(graph, Kosaraju.run(graph, new Metrics()));
        TopologicalSort.Result cycle = TopologicalSort.run(graph, new Metrics());
        check(cycle.hasCycle && cycle.order.isEmpty(), "Cycle has no topological order");
        Graph selfLoop = new Graph(1, true); selfLoop.addEdge(0, 0, 0);
        check(TopologicalSort.run(selfLoop, new Metrics()).hasCycle, "Self-loop is a cycle");
        validateScc(selfLoop, Kosaraju.run(selfLoop, new Metrics()));
        Graph singleton = new Graph(1, true);
        validateTopological(singleton, TopologicalSort.run(singleton, new Metrics()));
        validateScc(singleton, Kosaraju.run(singleton, new Metrics()));
        Graph parallel = new Graph(2, true);
        parallel.addEdge(0, 1, 1); parallel.addEdge(0, 1, 2);
        validateTopological(parallel, TopologicalSort.run(parallel, new Metrics()));
        // Large depth tests the iterative DFS without depending on Java's call stack.
        Graph chain = new Graph(10000, true);
        for (int v = 1; v < chain.vertexCount(); v++) chain.addEdge(v - 1, v, 1);
        check(Kosaraju.run(chain, new Metrics()).size() == 10000, "Deep chain SCCs");
        validateTopological(chain, TopologicalSort.run(chain, new Metrics()));
        System.out.println("PASS: SCCs, DAGs, cycles, parallel edges and deep chain");
    }

    public static void validateTopological(Graph graph, TopologicalSort.Result result) {
        check(!result.hasCycle && result.order.size() == graph.vertexCount(), "Full DAG order");
        int[] position = new int[graph.vertexCount()];
        Arrays.fill(position, -1);
        for (int i = 0; i < result.order.size(); i++) {
            int vertex = result.order.get(i);
            check(vertex >= 0 && vertex < position.length && position[vertex] == -1, "Unique valid vertex in order");
            position[vertex] = i;
        }
        for (Edge edge : graph.edges()) check(position[edge.from] < position[edge.to], "DAG edge points forward");
    }

    public static void validateScc(Graph graph, List<List<Integer>> components) {
        int[] membership = new int[graph.vertexCount()];
        Arrays.fill(membership, -1);
        for (int c = 0; c < components.size(); c++) {
            check(!components.get(c).isEmpty(), "Nonempty SCC");
            for (int vertex : components.get(c)) {
                check(vertex >= 0 && vertex < membership.length && membership[vertex] == -1, "Unique SCC membership");
                membership[vertex] = c;
            }
        }
        boolean[][] reach = new boolean[graph.vertexCount()][];
        for (int v = 0; v < membership.length; v++) {
            check(membership[v] != -1, "Every vertex belongs to an SCC");
            reach[v] = reachable(graph, v);
        }
        for (int a = 0; a < membership.length; a++) {
            for (int b = 0; b < membership.length; b++) {
                check((membership[a] == membership[b]) == (reach[a][b] && reach[b][a]), "SCC equals mutual reachability");
            }
        }
    }

    private static void testShortestPaths() {
        Graph graph = new Graph(6, true);
        graph.addEdge(0, 1, 10); graph.addEdge(0, 2, 1); graph.addEdge(2, 1, 2);
        graph.addEdge(1, 3, 0); graph.addEdge(3, 4, 2); graph.addEdge(4, 2, 0);
        Dijkstra.Result result = Dijkstra.run(graph, 0, new Metrics());
        check(Arrays.equals(result.distance, new long[]{0, 3, 1, 3, 5, Dijkstra.INF}), "Known shortest distances");
        check(result.pathTo(4).equals(List.of(0, 2, 1, 3, 4)), "Known shortest path");
        check(result.pathTo(5).isEmpty(), "No path to unreachable vertex");
        validatePaths(graph, 0, result);
        Graph single = new Graph(1, false);
        validatePaths(single, 0, Dijkstra.run(single, 0, new Metrics()));
        Graph largeWeights = new Graph(3, true);
        largeWeights.addEdge(0, 1, Integer.MAX_VALUE); largeWeights.addEdge(1, 2, Integer.MAX_VALUE);
        check(Dijkstra.run(largeWeights, 0, new Metrics()).distance[2] == 2L * Integer.MAX_VALUE, "Long distances");
        Graph negative = new Graph(3, true); negative.addEdge(1, 2, -1);
        rejects(() -> Dijkstra.run(negative, 0, new Metrics()));
        System.out.println("PASS: shortest distances, paths, zero weights, unreachable and negative edges");
    }

    public static void validatePaths(Graph graph, int source, Dijkstra.Result result) {
        long[] expected = new long[graph.vertexCount()];
        Arrays.fill(expected, Dijkstra.INF); expected[source] = 0;
        // Independent Bellman-Ford oracle, used only for validation.
        for (int pass = 1; pass < graph.vertexCount(); pass++) {
            boolean changed = false;
            for (int v = 0; v < graph.vertexCount(); v++) {
                if (expected[v] == Dijkstra.INF) continue;
                for (Edge edge : graph.neighbors(v)) {
                    if (expected[v] + edge.weight < expected[edge.to]) {
                        expected[edge.to] = expected[v] + edge.weight;
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }
        check(Arrays.equals(expected, result.distance), "Dijkstra agrees with Bellman-Ford");
        for (int target = 0; target < graph.vertexCount(); target++) {
            List<Integer> path = result.pathTo(target);
            if (expected[target] == Dijkstra.INF) { check(path.isEmpty(), "Unreachable path is empty"); continue; }
            check(path.get(0) == source && path.get(path.size() - 1) == target, "Path endpoints");
            check(new HashSet<>(path).size() == path.size(), "Path contains no cycle");
            long sum = 0;
            for (int i = 1; i < path.size(); i++) {
                long best = Long.MAX_VALUE;
                for (Edge edge : graph.neighbors(path.get(i - 1))) {
                    if (edge.to == path.get(i)) best = Math.min(best, edge.weight);
                }
                check(best != Long.MAX_VALUE, "Path edge exists");
                sum += best;
            }
            check(sum == expected[target], "Path weight equals shortest distance");
        }
    }

    private static void testGeneratorsAndRandomGraphs() {
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            Graph[] graphs = {GraphGenerator.connected(size[0], size[1], 42),
                    GraphGenerator.dag(size[0], size[1], 42), GraphGenerator.cyclic(size[0], size[1], 42)};
            Graph[] copies = {GraphGenerator.connected(size[0], size[1], 42),
                    GraphGenerator.dag(size[0], size[1], 42), GraphGenerator.cyclic(size[0], size[1], 42)};
            for (int i = 0; i < graphs.length; i++) {
                Graph graph = graphs[i];
                check(graph.vertexCount() == size[0] && graph.edgeCount() == size[1], "Exact generator size");
                check(graph.edges().toString().equals(copies[i].edges().toString()), "Reproducible graph");
                Set<String> used = new HashSet<>();
                for (Edge edge : graph.edges()) {
                    int a = graph.isDirected() ? edge.from : Math.min(edge.from, edge.to);
                    int b = graph.isDirected() ? edge.to : Math.max(edge.from, edge.to);
                    check(a != b && used.add(a + ":" + b), "No loops or duplicate edges");
                    check(edge.weight >= 1 && edge.weight <= 100, "Weight bounds");
                }
            }
            MstResult a = Kruskal.run(graphs[0], new Metrics());
            MstResult b = Prim.run(graphs[0], new Metrics());
            check(a.isConnected() && b.isConnected() && a.totalWeight == b.totalWeight, "Generated MST agreement");
            validateForest(graphs[0], a); validateForest(graphs[0], b);
            validateTopological(graphs[1], TopologicalSort.run(graphs[1], new Metrics()));
            check(TopologicalSort.run(graphs[2], new Metrics()).hasCycle, "Guaranteed generated cycle");
        }
        Random random = new Random(123);
        for (int test = 0; test < 40; test++) {
            Graph graph = new Graph(8, test % 2 == 0);
            for (int i = 0; i < 15; i++) graph.addEdge(random.nextInt(8), random.nextInt(8), random.nextInt(6));
            for (int source = 0; source < 8; source++) validatePaths(graph, source, Dijkstra.run(graph, source, new Metrics()));
            if (graph.isDirected()) validateScc(graph, Kosaraju.run(graph, new Metrics()));
            Graph dag = GraphGenerator.dag(8, 12, test);
            validateTopological(dag, TopologicalSort.run(dag, new Metrics()));
            validateScc(dag, Kosaraju.run(dag, new Metrics()));
        }
        System.out.println("PASS: exact reproducible generators and independent random-graph oracles");
    }

    private static void testValidation() {
        rejects(() -> new Graph(0, true));
        rejects(() -> new DisjointSet(0, new Metrics()));
        Graph directed = new Graph(2, true);
        Graph undirected = new Graph(2, false);
        rejects(() -> directed.addEdge(0, 2, 1));
        rejects(() -> Kruskal.run(directed, new Metrics()));
        rejects(() -> Prim.run(directed, new Metrics()));
        rejects(() -> Kosaraju.run(undirected, new Metrics()));
        rejects(() -> TopologicalSort.run(undirected, new Metrics()));
        rejects(() -> Dijkstra.run(directed, 2, new Metrics()));
        rejects(() -> GraphGenerator.connected(5, 3, 42));
        rejects(() -> GraphGenerator.dag(5, 11, 42));
        rejects(() -> GraphGenerator.cyclic(2, 2, 42));
        System.out.println("PASS: invalid inputs fail clearly");
    }
}
