package shaleva.run_app_proj.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import shaleva.run_app_proj.datamodels.Candidate;
import shaleva.run_app_proj.datamodels.SparseGraph;
import shaleva.run_app_proj.datamodels.Particle;
import shaleva.run_app_proj.datamodels.Waypoint;

@Service
public class DPSOAlgorithmService {
    private SparseGraph graph;
    Map<Candidate, Map<Candidate, Double>> distanceMap;
    private double T_MAX;
    private int iterations;
    private List<Particle> swarm;
    private Random rand;
    private List<Candidate> globalBest;
    private double globalBestFitness;
    private int swarmSize;

    public final double W = 0.2; // random Candidate choice
    public final double C1 = 0.4; // pBest influence
    public final double C2 = 0.4; // gBest influence

    public List<Waypoint> getSolution(double tmax, int iterations, int swarmSize, List<Candidate> candidates,
            Map<Candidate, Map<Candidate, Double>> distanceMap) {
        this.graph = new SparseGraph(distanceMap, candidates);
        this.T_MAX = tmax;
        this.iterations = iterations;
        this.rand = new Random();
        this.swarm = new ArrayList<>();
        this.globalBest = new ArrayList<>();
        this.globalBestFitness = Double.NEGATIVE_INFINITY;
        this.swarmSize = swarmSize;

        Particle solution = solve();
        return getCandidatesAsWaypoints(solution.getPosition());
    }

    public Particle solve() {
        initializeSwarm();

        for (int i = 0; i < iterations; i++) {
            // מעבר על כל חלקיק
            for (Particle particle : swarm) {
                List<Candidate> newPos = localSearchOptimize(particle); // משיכה ל-pBest/gBest
                newPos = projectToFeasible(newPos); // חותך מסלול עד שהוא ישים

                newPos = twoOptOperation(newPos);
                newPos = projectToFeasible(newPos);

                double fitness = evaluateFitness(newPos); // עכשיו אפשר לחשב reward
                particle.setPosition(newPos);
                particle.updatePersonalBest(fitness);
                updateGlobalBest(particle);
            }

            // עבור כל חלקיק:

            // 1. חישוב פיטנס והשוואתו לשיא האישי של החלקיק
            // 2. בדיקה אם יש שיא גלובלי חדש
            // 3. ביצוע שיפור על המסלולים של החלקיקים באמצעות הרכיבים ההסתברותיים
            // ובדיקת תקינות המסלול

        }

        return new Particle(globalBest, evaluateFitness(globalBest));
    }

    // searching for a segment through the route, that his reverse version
    // decreasing the
    // cost of the route, and if it does, the reversed segment is applied to the
    // route
    private List<Candidate> twoOptOperation(List<Candidate> route) {

        if (route.size() < 4)
            return new ArrayList<>(route);

        List<Candidate> improved = new ArrayList<>(route);
        boolean improvement = true;

        while (improvement) {
            improvement = false;

            for (int i = 0; i < improved.size() - 3; i++) {
                for (int j = i + 2; j < improved.size() - 1; j++) {

                    double before = graph.getCost(improved.get(i), improved.get(i + 1)) +
                            graph.getCost(improved.get(j), improved.get(j + 1));

                    double after = graph.getCost(improved.get(i), improved.get(j)) +
                            graph.getCost(improved.get(i + 1), improved.get(j + 1));

                    if (after < before) {
                        improved = reverseSegmentCopy(improved, i + 1, j);
                        improvement = true;
                    }
                }
            }
        }

        return improved;
    }

    // if the solution is better than the current best known solution in the entire
    // population
    private void updateGlobalBest(Particle particle) {
        if (particle.getFitness() > globalBestFitness) {
            globalBest = new ArrayList<>(particle.getPosition());
            globalBestFitness = particle.getFitness();
        }
    }

    // initializes the solution population randomly
    private void initializeSwarm() {
        for (int i = 0; i < swarmSize; i++) {
            List<Candidate> toImprove = new ArrayList<>(graph.getCandidates());
            Collections.shuffle(toImprove, rand);

            toImprove = projectToFeasible(toImprove);
            Particle p = new Particle(toImprove, evaluateFitness(toImprove));

            swarm.add(p);

            updateGlobalBest(p);
        }
    }

    // applies the route changes according to the random value
    public List<Candidate> localSearchOptimize(Particle p) {

        List<Candidate> current = new ArrayList<>(p.getPosition());
        List<Candidate> pBest = p.getPersonalBest();
        List<Candidate> gBest = new ArrayList<>(globalBest);

        // כמה מהלכים מקומיים לבצע בכל איטרציה
        int moves = Math.max(1, current.size() / 4);

        for (int m = 0; m < moves; m++) {
            double r = rand.nextDouble();

            if (r < W) {

                swapOperation(current);
            } else if (r < W + C1 && pBest != null && !pBest.isEmpty()) {
                insertOperation(current, pBest);
            } else if (gBest != null && !gBest.isEmpty()) {
                insertOperation(current, gBest);
            }
        }

        return current;
    }

    // calculate the total reward of the route (fitness)
    public double evaluateFitness(List<Candidate> toImprove) {
        double totalReward = 0.0;
        for (Candidate Candidate : toImprove) {
            totalReward += Candidate.getReward();
        }
        return totalReward;
    }

    public double evaluateTotalCost(List<Candidate> route) {
        double total = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            total += graph.getCost(route.get(i), route.get(i + 1));
        }

        return total;
    }

    // fixes the route to be valid by pruning the violating Candidates from the end
    // of
    // the route
    public List<Candidate> projectToFeasible(List<Candidate> toImprove) {
        if (toImprove == null || toImprove.isEmpty()) {
            return toImprove;
        }

        List<Candidate> result = new ArrayList<>();

        // נקודת ההתחלה (START) תמיד נכנסת ראשונה ובטוחה
        result.add(toImprove.get(0));
        double cost = 0.0;

        for (int i = 1; i < toImprove.size(); i++) {
            // הנקודה האחרונה והתקינה שנמצאת כרגע במסלול שלנו
            Candidate currentValid = result.get(result.size() - 1);

            // הנקודה שהאלגוריתם מנסה להוסיף עכשיו
            Candidate nextAttempt = toImprove.get(i);

            double edgeCost = graph.getCost(currentValid, nextAttempt);

            // 1. ריפוי (Repair): אם אין חיבור חוקי (אינסוף), מדלגים על הנקודה הזו וממשיכים
            // הלאה
            if (edgeCost == Double.POSITIVE_INFINITY) {
                continue;
            }

            // 2. אכיפת מרחק (Prune): אם החיבור חוקי אבל חורג מהקילומטראז', עוצרים הכל
            if (cost + edgeCost > T_MAX) {
                break;
            }

            // 3. הוספה: החיבור גם חוקי וגם בתוך התקציב - מצרפים למסלול
            result.add(nextAttempt);
            cost += edgeCost;
        }

        return result;
    }

    // swaps 2 random Candidates of the route (local search operation)
    private void swapOperation(List<Candidate> toImprove) {
        if (toImprove.size() < 3) // צריך לפחות 2 נקודות חוץ מנקודת ההתחלה
            return;

        // הגבלה לאינדקסים מ-1 ועד הגודל המקסימלי
        int i = 1 + rand.nextInt(toImprove.size() - 1);
        int j;
        do {
            j = 1 + rand.nextInt(toImprove.size() - 1);
        } while (j == i);

        Collections.swap(toImprove, i, j);
    }

    // inserts a random segment (inspiration) from pBest or gBest to the particle's
    // current route
    private void insertOperation(List<Candidate> toImprove, List<Candidate> model) {
        if (toImprove.size() < 3 || model.size() < 3)
            return;

        int from = 1 + rand.nextInt(model.size() - 1);
        int to = from + rand.nextInt(model.size() - from);

        List<Candidate> segment = new ArrayList<>(model.subList(from, to + 1));

        toImprove.removeAll(segment);

        if (toImprove.isEmpty()) {
            return;
        }

        int insertPos = 1 + rand.nextInt(toImprove.size());

        toImprove.addAll(insertPos, segment);
    }

    // reversing the segment
    private List<Candidate> reverseSegmentCopy(List<Candidate> route, int start, int end) {

        List<Candidate> copy = new ArrayList<>(route);

        while (start < end) {
            Collections.swap(copy, start, end);
            start++;
            end--;
        }

        return copy;
    }

    private List<Waypoint> getCandidatesAsWaypoints(List<Candidate> candidates) {
        List<Waypoint> waypoints = new ArrayList<>();
        if (candidates == null || candidates.isEmpty())
            return waypoints;

        // ריצה על כל הזוגות (מייצרת את ה"גשרים" ביניהם)
        for (int i = 0; i < candidates.size() - 1; i++) {
            Candidate candidate = candidates.get(i);
            Candidate nextCan = candidates.get(i + 1);
            waypoints.add(new Waypoint(candidate.getLat(), candidate.getLng(), candidate.getId(), candidate.getReward(),
                    graph.getCost(candidate, nextCan)));
        }

        // תיקון: הוספת נקודת הסיום הסופית של המסלול
        Candidate lastCandidate = candidates.get(candidates.size() - 1);
        waypoints.add(new Waypoint(lastCandidate.getLat(), lastCandidate.getLng(), lastCandidate.getId(),
                lastCandidate.getReward(), 0.0));

        return waypoints;
    }

}
