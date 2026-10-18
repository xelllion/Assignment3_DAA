package mst;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Prim {
    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.isDirected()) throw new IllegalArgumentException("MST needs an undirected graph");
        boolean[] visited = new boolean[graph.vertexCount()];
        List<Edge> chosen = new ArrayList<>();
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight, b.weight);
        });
        long total = 0;
        int components = 0;
        // Restart in every component, so disconnected graphs produce a forest.
        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) continue;
            components++;
            visited[start] = true;
            addEdges(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.poll();
                metrics.queuePolls++;
                if (visited[edge.to]) continue;
                visited[edge.to] = true;
                chosen.add(edge);
                total += edge.weight;
                addEdges(graph, edge.to, visited, queue, metrics);
            }
        }
        return new MstResult(chosen, total, components);
    }

    private static void addEdges(Graph graph, int vertex, boolean[] visited,
                                 PriorityQueue<Edge> queue, Metrics metrics) {
        for (Edge edge : graph.neighbors(vertex)) {
            metrics.edgeExaminations++;
            if (!visited[edge.to]) {
                queue.offer(edge);
                metrics.queueOffers++;
            }
        }
    }
}
