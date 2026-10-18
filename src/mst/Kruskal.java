package mst;

import dsu.DisjointSet;
import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;

public class Kruskal {
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) throw new IllegalArgumentException("MST needs an undirected graph");
        List<Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight, b.weight);
        });
        DisjointSet sets = new DisjointSet(graph.vertexCount(), metrics);
        List<Edge> chosen = new ArrayList<>();
        long total = 0;
        for (Edge edge : sorted) {
            if (chosen.size() == graph.vertexCount() - 1) break;
            metrics.edgeExaminations++;
            // union returns false when the edge would create a cycle.
            if (sets.union(edge.from, edge.to)) {
                chosen.add(edge);
                total += edge.weight;
            }
        }
        return new MstResult(chosen, total, sets.componentCount());
    }
}
