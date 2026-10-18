package shortestpath;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class Dijkstra {
    public static final long INF = Long.MAX_VALUE;

    private static class Entry {
        final int vertex;
        final long distance;
        Entry(int vertex, long distance) { this.vertex = vertex; this.distance = distance; }
    }

    public static class Result {
        public final long[] distance;
        public final int[] parent;

        private Result(long[] distance, int[] parent) {
            this.distance = distance;
            this.parent = parent;
        }

        public List<Integer> pathTo(int target) {
            if (target < 0 || target >= distance.length) throw new IllegalArgumentException("Invalid target");
            List<Integer> path = new ArrayList<>();
            if (distance[target] == INF) return path;
            for (int v = target; v != -1; v = parent[v]) path.add(v);
            Collections.reverse(path);
            return path;
        }
    }

    public static Result run(Graph graph, int source, Metrics metrics) {
        graph.checkVertex(source);
        // Validate all edges, including edges in unreachable components.
        for (Edge edge : graph.edges()) {
            metrics.edgeExaminations++;
            if (edge.weight < 0) throw new IllegalArgumentException("Dijkstra requires non-negative weights");
        }
        long[] distance = new long[graph.vertexCount()];
        int[] parent = new int[graph.vertexCount()];
        Arrays.fill(distance, INF);
        Arrays.fill(parent, -1);
        distance[source] = 0;
        PriorityQueue<Entry> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Long.compare(a.distance, b.distance);
        });
        queue.offer(new Entry(source, 0));
        metrics.queueOffers++;
        while (!queue.isEmpty()) {
            Entry current = queue.poll();
            metrics.queuePolls++;
            if (current.distance != distance[current.vertex]) continue;
            for (Edge edge : graph.neighbors(current.vertex)) {
                metrics.edgeExaminations++;
                long candidate = current.distance + edge.weight;
                if (candidate < distance[edge.to]) {
                    distance[edge.to] = candidate;
                    parent[edge.to] = current.vertex;
                    queue.offer(new Entry(edge.to, candidate));
                    metrics.queueOffers++;
                }
            }
        }
        return new Result(distance, parent);
    }
}
