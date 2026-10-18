package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Graph {
    private final boolean directed;
    private final List<List<Edge>> adjacency = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    public Graph(int vertices, boolean directed) {
        if (vertices < 1) throw new IllegalArgumentException("At least one vertex is required");
        this.directed = directed;
        for (int i = 0; i < vertices; i++) adjacency.add(new ArrayList<>());
    }

    public int vertexCount() { return adjacency.size(); }
    public int edgeCount() { return edges.size(); }
    public boolean isDirected() { return directed; }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }

    public List<Edge> neighbors(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    public void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount()) {
            throw new IllegalArgumentException("Invalid vertex: " + vertex);
        }
    }

    public void addEdge(int from, int to, int weight) {
        checkVertex(from);
        checkVertex(to);
        Edge edge = new Edge(from, to, weight);
        edges.add(edge); // One logical edge, even in an undirected graph.
        adjacency.get(from).add(edge);
        if (!directed) adjacency.get(to).add(new Edge(to, from, weight));
    }

    public Graph reversed() {
        Graph result = new Graph(vertexCount(), directed);
        for (Edge edge : edges) result.addEdge(edge.to, edge.from, edge.weight);
        return result;
    }
}
