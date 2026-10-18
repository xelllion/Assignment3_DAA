package connectivity;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class Kosaraju {
    public static List<List<Integer>> run(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) throw new IllegalArgumentException("SCC expects a directed graph");
        int n = graph.vertexCount();
        boolean[] visited = new boolean[n];
        int[] nextEdge = new int[n];
        List<Integer> finishOrder = new ArrayList<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        // Iterative DFS avoids a recursion limit on long chains.
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            stack.push(start);
            visited[start] = true;
            while (!stack.isEmpty()) {
                int vertex = stack.peek();
                List<Edge> neighbors = graph.neighbors(vertex);
                if (nextEdge[vertex] < neighbors.size()) {
                    Edge edge = neighbors.get(nextEdge[vertex]++);
                    metrics.edgeExaminations++;
                    if (!visited[edge.to]) {
                        visited[edge.to] = true;
                        stack.push(edge.to);
                    }
                } else {
                    finishOrder.add(stack.pop());
                }
            }
        }
        Graph reversed = graph.reversed();
        metrics.edgeExaminations += graph.edgeCount(); // Transpose construction.
        visited = new boolean[n];
        List<List<Integer>> components = new ArrayList<>();
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            int start = finishOrder.get(i);
            if (visited[start]) continue;
            List<Integer> component = new ArrayList<>();
            visited[start] = true;
            stack.push(start);
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                component.add(vertex);
                for (Edge edge : reversed.neighbors(vertex)) {
                    metrics.edgeExaminations++;
                    if (!visited[edge.to]) {
                        visited[edge.to] = true;
                        stack.push(edge.to);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }
}
