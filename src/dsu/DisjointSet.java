package dsu;

import graph.Metrics;

public class DisjointSet {
    private final int[] parent;
    private final int[] rank;
    private int components;
    private final Metrics metrics;

    public DisjointSet(int size, Metrics metrics) {
        if (size < 1) throw new IllegalArgumentException("DSU size must be positive");
        parent = new int[size];
        rank = new int[size];
        components = size;
        this.metrics = metrics;
        for (int i = 0; i < size; i++) parent[i] = i;
    }

    public int find(int vertex) {
        if (vertex < 0 || vertex >= parent.length) throw new IllegalArgumentException("Invalid DSU vertex");
        metrics.findCalls++;
        int root = vertex;
        while (root != parent[root]) {
            metrics.parentTraversals++;
            root = parent[root];
        }
        // A second walk makes all vertices on the path point to the root.
        while (vertex != root) {
            metrics.parentTraversals++;
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }
        return root;
    }

    public boolean union(int a, int b) {
        metrics.unionCalls++;
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) return false;
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;
        } else {
            parent[rootB] = rootA;
            if (rank[rootA] == rank[rootB]) rank[rootA]++;
        }
        components--;
        return true;
    }

    public int componentCount() { return components; }
}
