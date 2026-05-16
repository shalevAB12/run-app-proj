package shaleva.run_app_proj.datamodels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SparseGraph {

    private Map<Candidate, Map<Candidate, Double>> distanceMap = new HashMap<>();
    private List<Candidate> candidates;

    public SparseGraph(Map<Candidate, Map<Candidate, Double>> distanceMap, List<Candidate> candidates) {
        this.distanceMap = distanceMap;
        this.candidates = candidates;
    }

    public double getCost(Candidate a, Candidate b) {
        return distanceMap.getOrDefault(a, Map.of()).getOrDefault(b, Double.POSITIVE_INFINITY);
    }

    public List<Candidate> getCandidates() {
        return this.candidates;
    }


}
