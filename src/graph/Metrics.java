package graph;

// A new object is used for every run, so counts never carry over.
public class Metrics {
    public long edgeExaminations;
    public long comparisons;
    public long queueOffers;
    public long queuePolls;
    public long findCalls;
    public long unionCalls;
    public long parentTraversals;

    public String csv() {
        return edgeExaminations + "," + comparisons + "," + queueOffers + ","
                + queuePolls + "," + findCalls + "," + unionCalls + "," + parentTraversals;
    }

    public static String header() {
        return "edge_examinations,comparisons,queue_offers,queue_polls,find_calls,union_calls,parent_traversals";
    }
}
