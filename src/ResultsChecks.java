import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

// Checks the saved evidence without rerunning or replacing the submitted benchmarks.
public class ResultsChecks {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args.length == 0 ? "results/tables" : args[0]);
        List<String> raw = Files.readAllLines(directory.resolve("raw_runs.csv"), StandardCharsets.UTF_8);
        List<String> summary = Files.readAllLines(directory.resolve("summary.csv"), StandardCharsets.UTF_8);
        require(raw.size() == 91 && summary.size() == 19, "Expected 90 raw runs and 18 summaries");
        Map<String, List<String[]>> groups = new HashMap<>();
        for (int i = 1; i < raw.size(); i++) {
            String[] row = raw.get(i).split(",");
            require(row.length == 14, "Raw CSV column count");
            groups.computeIfAbsent(key(row), unused -> new ArrayList<>()).add(row);
        }
        Set<String> expectedKeys = new HashSet<>();
        String[] algorithms = {"Kruskal", "Prim", "Kosaraju", "Topo-DAG", "Topo-Cyclic", "Dijkstra"};
        for (String algorithm : algorithms) {
            String type = algorithm.equals("Kruskal") || algorithm.equals("Prim") ? "connected"
                    : algorithm.equals("Topo-DAG") ? "dag" : "cyclic";
            for (int[] size : new int[][]{{10, 30}, {100, 500}, {1000, 5000}}) {
                expectedKeys.add(algorithm + "," + type + "," + size[0] + "," + size[1] + ",42");
            }
        }
        require(groups.keySet().equals(expectedKeys), "Exact configurations and seed");
        Set<String> summaries = new HashSet<>();
        for (int i = 1; i < summary.size(); i++) {
            String[] row = summary.get(i).split(",");
            require(row.length == 16 && row[5].equals("5"), "Summary columns and repetition count");
            require(summaries.add(key(row)), "Unique summary configuration");
            List<String[]> runs = groups.get(key(row));
            require(runs != null && runs.size() == 5, "Five runs in every group");
            long total = 0, min = Long.MAX_VALUE, max = 0;
            Set<Integer> numbers = new HashSet<>();
            for (String[] run : runs) {
                numbers.add(Integer.parseInt(run[5]));
                long ns = Long.parseLong(run[6]);
                require(ns > 0, "Positive elapsed time");
                total += ns; min = Math.min(min, ns); max = Math.max(max, ns);
                for (int column = 7; column < 14; column++) {
                    require(Long.parseLong(run[column]) >= 0, "Non-negative operation count");
                    require(run[column].equals(row[column + 2]), "Counts repeat and match summary");
                }
            }
            require(numbers.equals(Set.of(1, 2, 3, 4, 5)), "Unique run numbers 1 through 5");
            require(Math.abs(total / 5_000_000.0 - Double.parseDouble(row[6])) <= 0.00000051, "Correct mean");
            require(Math.abs(min / 1_000_000.0 - Double.parseDouble(row[7])) <= 0.00000051, "Correct minimum");
            require(Math.abs(max / 1_000_000.0 - Double.parseDouble(row[8])) <= 0.00000051, "Correct maximum");
        }
        require(summaries.equals(expectedKeys), "All summary configurations present");
        if (args.length == 0) {
            for (String name : List.of("time_vs_vertices.png", "time_vs_edges.png")) {
                BufferedImage plot = ImageIO.read(Path.of("results/plots", name).toFile());
                require(plot != null && plot.getWidth() == 1400 && plot.getHeight() == 820, "Readable full-size PNG");
            }
        }
        System.out.println("PASS: 90 raw measurements, 18 correct summaries, all operation counts and plot files");
    }

    private static String key(String[] row) {
        return String.join(",", row[0], row[1], row[2], row[3], row[4]);
    }
}
