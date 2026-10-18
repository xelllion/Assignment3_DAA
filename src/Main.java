import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import graph.Graph;
import graph.Metrics;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;

public class Main {
    public static void main(String[] args) {
        System.out.println("SMART CITY: districts 0=Center, 1=University, 2=Hospital, 3=Station, 4=Park");
        Graph city = new Graph(5, false);
        city.addEdge(0, 1, 4); city.addEdge(0, 2, 2); city.addEdge(1, 2, 1);
        city.addEdge(1, 3, 5); city.addEdge(2, 3, 8); city.addEdge(3, 4, 2);
        city.addEdge(2, 4, 10);
        printMst("Kruskal", Kruskal.run(city, new Metrics()));
        printMst("Prim", Prim.run(city, new Metrics()));

        Graph roads = new Graph(6, true);
        roads.addEdge(0, 1, 4); roads.addEdge(0, 2, 1); roads.addEdge(2, 1, 2);
        roads.addEdge(1, 3, 1); roads.addEdge(3, 2, 1); roads.addEdge(3, 4, 3);
        System.out.println("\nOne-way roads: SCCs = " + Kosaraju.run(roads, new Metrics()));
        System.out.println("Road network contains a cycle: " + TopologicalSort.run(roads, new Metrics()).hasCycle);
        Graph dependencies = new Graph(5, true);
        dependencies.addEdge(0, 1, 1); dependencies.addEdge(0, 2, 1);
        dependencies.addEdge(1, 3, 1); dependencies.addEdge(2, 3, 1); dependencies.addEdge(3, 4, 1);
        System.out.println("Service installation order: " + TopologicalSort.run(dependencies, new Metrics()).order);
        Dijkstra.Result paths = Dijkstra.run(roads, 0, new Metrics());
        System.out.println("\nRoutes from district 0 (district 5 is isolated):");
        for (int v = 0; v < roads.vertexCount(); v++) {
            if (paths.distance[v] == Dijkstra.INF) System.out.println(v + ": unreachable");
            else System.out.println(v + ": distance=" + paths.distance[v] + ", path=" + paths.pathTo(v));
        }
        Graph disconnected = new Graph(3, false);
        disconnected.addEdge(0, 1, 7);
        printMst("Disconnected example", Kruskal.run(disconnected, new Metrics()));
    }

    private static void printMst(String name, MstResult result) {
        System.out.println("\n" + name + ": " + (result.isConnected() ? "MST" : "No full MST; minimum spanning forest")
                + ", weight=" + result.totalWeight + ", components=" + result.components);
        System.out.println(result.edges);
    }
}
