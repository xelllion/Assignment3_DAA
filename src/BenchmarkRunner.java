import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import graph.Graph;
import graph.GraphGenerator;
import graph.Metrics;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public class BenchmarkRunner {
    private static final int WARMUPS = 5;
    private static final int REPETITIONS = 5;
    private static final long SEED = 42;
    private static final String[] ALGORITHMS = {"Kruskal", "Prim", "Kosaraju", "Topo-DAG", "Topo-Cyclic", "Dijkstra"};

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "results/tables" : args[0]);
        Files.createDirectories(output);
        List<String> raw = new ArrayList<>();
        List<String> summary = new ArrayList<>();
        List<String> fingerprints = new ArrayList<>();
        raw.add("algorithm,graph_type,vertices,edges,seed,run,time_ns," + Metrics.header());
        summary.add("algorithm,graph_type,vertices,edges,seed,runs,average_ms,min_ms,max_ms," + Metrics.header());
        fingerprints.add("graph_type,vertices,edges,seed,sha256");
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            Graph undirected = GraphGenerator.connected(size[0], size[1], SEED);
            Graph dag = GraphGenerator.dag(size[0], size[1], SEED);
            Graph cyclic = GraphGenerator.cyclic(size[0], size[1], SEED);
            fingerprints.add(fingerprint("connected", undirected));
            fingerprints.add(fingerprint("dag", dag));
            fingerprints.add(fingerprint("cyclic", cyclic));
            long expectedMstWeight = Kruskal.run(undirected, new Metrics()).totalWeight;
            for (String algorithm : ALGORITHMS) {
                Graph graph = algorithm.equals("Kruskal") || algorithm.equals("Prim") ? undirected
                        : algorithm.equals("Topo-DAG") ? dag : cyclic;
                String type = !graph.isDirected() ? "connected" : graph == dag ? "dag" : "cyclic";
                for (int warmup = 0; warmup < WARMUPS; warmup++) run(algorithm, graph, new Metrics());
                long total = 0;
                long min = Long.MAX_VALUE;
                long max = 0;
                String operationCounts = null;
                List<Object> results = new ArrayList<>();
                for (int repetition = 1; repetition <= REPETITIONS; repetition++) {
                    Metrics metrics = new Metrics();
                    long start = System.nanoTime();
                    Object result = run(algorithm, graph, metrics);
                    long elapsed = System.nanoTime() - start;
                    // Everything below is outside the measured interval.
                    results.add(result);
                    total += elapsed;
                    min = Math.min(min, elapsed);
                    max = Math.max(max, elapsed);
                    raw.add(algorithm + "," + type + "," + size[0] + "," + size[1] + "," + SEED
                            + "," + repetition + "," + elapsed + "," + metrics.csv());
                    if (operationCounts != null && !operationCounts.equals(metrics.csv())) {
                        throw new IllegalStateException("Operation counts changed between identical runs");
                    }
                    operationCounts = metrics.csv();
                }
                for (Object result : results) validate(algorithm, graph, result, expectedMstWeight);
                summary.add(String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%.6f,%.6f,%.6f,%s",
                        algorithm, type, size[0], size[1], SEED, REPETITIONS,
                        total / (REPETITIONS * 1_000_000.0), min / 1_000_000.0, max / 1_000_000.0, operationCounts));
                System.out.printf(Locale.ROOT, "%s V=%d E=%d: %.6f ms (5 runs, checked)%n",
                        algorithm, size[0], size[1], total / (REPETITIONS * 1_000_000.0));
            }
        }
        Files.write(output.resolve("raw_runs.csv"), raw, StandardCharsets.UTF_8);
        Files.write(output.resolve("summary.csv"), summary, StandardCharsets.UTF_8);
        Files.write(output.resolve("graph_fingerprints.csv"), fingerprints, StandardCharsets.UTF_8);
        writeEnvironment(output);
        System.out.println("Saved 90 measured runs and 18 summary rows to " + output);
    }

    private static Object run(String algorithm, Graph graph, Metrics metrics) {
        switch (algorithm) {
            case "Kruskal": return Kruskal.run(graph, metrics);
            case "Prim": return Prim.run(graph, metrics);
            case "Kosaraju": return Kosaraju.run(graph, metrics);
            case "Topo-DAG":
            case "Topo-Cyclic": return TopologicalSort.run(graph, metrics);
            case "Dijkstra": return Dijkstra.run(graph, 0, metrics);
            default: throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
    }

    @SuppressWarnings("unchecked")
    private static void validate(String algorithm, Graph graph, Object result, long expectedMstWeight) {
        switch (algorithm) {
            case "Kruskal":
            case "Prim":
                MstResult mst = (MstResult) result;
                AlgorithmTests.validateForest(graph, mst);
                AlgorithmTests.check(mst.isConnected() && mst.totalWeight == expectedMstWeight, "Benchmark MST agreement");
                break;
            case "Kosaraju": AlgorithmTests.validateScc(graph, (List<List<Integer>>) result); break;
            case "Topo-DAG": AlgorithmTests.validateTopological(graph, (TopologicalSort.Result) result); break;
            case "Topo-Cyclic":
                TopologicalSort.Result order = (TopologicalSort.Result) result;
                AlgorithmTests.check(order.hasCycle && order.order.isEmpty(), "Benchmark cyclic input rejected");
                break;
            case "Dijkstra": AlgorithmTests.validatePaths(graph, 0, (Dijkstra.Result) result); break;
            default: throw new IllegalArgumentException("Unknown algorithm");
        }
    }

    private static String fingerprint(String type, Graph graph) throws Exception {
        byte[] bytes = graph.edges().toString().getBytes(StandardCharsets.UTF_8);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        return type + "," + graph.vertexCount() + "," + graph.edgeCount() + "," + SEED + "," + hash;
    }

    private static void writeEnvironment(Path output) throws IOException {
        List<String> rows = new ArrayList<>();
        rows.add("property,value");
        rows.add("recorded_at," + OffsetDateTime.now());
        for (String property : List.of("java.version", "java.vendor", "java.vm.name", "os.name", "os.version", "os.arch")) {
            rows.add(property + "," + System.getProperty(property).replace(',', ';'));
        }
        rows.add("available_processors," + Runtime.getRuntime().availableProcessors());
        rows.add("max_heap_bytes," + Runtime.getRuntime().maxMemory());
        rows.add("warmups_per_configuration," + WARMUPS);
        rows.add("measured_runs_per_configuration," + REPETITIONS);
        rows.add("seed," + SEED);
        Files.write(output.resolve("environment.csv"), rows, StandardCharsets.UTF_8);
    }
}
