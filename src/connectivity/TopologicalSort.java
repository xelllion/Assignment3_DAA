package connectivity;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class TopologicalSort {
    public static class Result {
        public final List<Integer> order;
        public final boolean hasCycle;

        private Result(List<Integer> order, boolean hasCycle) {
            this.order = List.copyOf(order);
            this.hasCycle = hasCycle;
        }
    }

    public static Result run(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) throw new IllegalArgumentException("Topological sort needs a directed graph");
        int[] indegree = new int[graph.vertexCount()];
        for (Edge edge : graph.edges()) {
            metrics.edgeExaminations++;
            indegree[edge.to]++;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < indegree.length; v++) {
            if (indegree[v] == 0) { queue.offer(v); metrics.queueOffers++; }
        }
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            metrics.queuePolls++;
            order.add(vertex);
            for (Edge edge : graph.neighbors(vertex)) {
                metrics.edgeExaminations++;
                indegree[edge.to]--;
                if (indegree[edge.to] == 0) { queue.offer(edge.to); metrics.queueOffers++; }
            }
        }
        boolean cycle = order.size() != graph.vertexCount();
        // Do not present a partial order as a valid solution.
        return new Result(cycle ? List.of() : order, cycle);
    }
}
