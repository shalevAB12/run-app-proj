package shaleva.run_app_proj.services;

import java.util.ArrayList;
import java.util.Collections;
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

    public static final double W = 0.2; // random Candidate choice
    public static final double C1 = 0.4; // pBest influence
    public static final double C2 = 0.4; // gBest influence

    public List<Waypoint> getSolution(double tmax, int iterations, int swarmSize, List<Candidate> candidates,
            Map<Candidate, Map<Candidate, Double>> distanceMap, boolean isCircular, Candidate startNode) {

        DPSORunner runner = new DPSORunner(tmax, iterations, swarmSize, candidates, distanceMap, isCircular, startNode);
        Particle solution = runner.solve();

        return runner.getCandidatesAsWaypoints(solution.getPosition());
    }

    private static class DPSORunner {
        private final SparseGraph graph;
        private final double T_MAX;
        private final int iterations;
        private final int swarmSize;

        private final List<Particle> swarm;
        private final Random rand;

        private List<Candidate> globalBest;
        private double globalBestFitness;

        private boolean isCircular;
        private Candidate startNode;

        public DPSORunner(double tMax, int iterations, int swarmSize, List<Candidate> candidates,
                Map<Candidate, Map<Candidate, Double>> distanceMap, boolean isCircular, Candidate startNode) {
            this.graph = new SparseGraph(distanceMap, candidates);
            this.T_MAX = tMax;
            this.iterations = iterations;
            this.swarmSize = swarmSize;
            this.rand = new Random();
            this.swarm = new ArrayList<>();
            this.globalBest = new ArrayList<>();
            this.globalBestFitness = Double.NEGATIVE_INFINITY;

            this.isCircular = isCircular;
            this.startNode = startNode;
        }

        public Particle solve() {
            initializeSwarm();

            for (int i = 0; i < iterations; i++) {
                for (Particle particle : swarm) {
                    List<Candidate> newPos = localSearchOptimize(particle);
                    newPos = projectToFeasible(newPos);
                    newPos = twoOptOperation(newPos);
                    newPos = projectToFeasible(newPos);

                    double fitness = evaluateFitness(newPos);
                    particle.setPosition(newPos);
                    particle.updatePersonalBest(fitness);
                    updateGlobalBest(particle);
                }
            }
            return new Particle(globalBest, evaluateFitness(globalBest));
        }

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

        private void updateGlobalBest(Particle particle) {
            if (particle.getFitness() > globalBestFitness) {
                globalBest = new ArrayList<>(particle.getPosition());
                globalBestFitness = particle.getFitness();
            }
        }

        private void initializeSwarm() {
            for (int i = 0; i < swarmSize; i++) {
                List<Candidate> toImprove = new ArrayList<>(graph.getCandidates());

                // הגנה על נקודת ההתחלה: מסירים אותה, מערבבים, ומחזירים בדיוק לאינדקס 0
                toImprove.removeIf(c -> c.getId().equals(startNode.getId()));
                Collections.shuffle(toImprove, rand);
                toImprove.add(0, startNode);

                toImprove = projectToFeasible(toImprove);
                Particle p = new Particle(toImprove, evaluateFitness(toImprove));
                swarm.add(p);
                updateGlobalBest(p);
            }
        }

        public List<Candidate> localSearchOptimize(Particle p) {
            List<Candidate> current = new ArrayList<>(p.getPosition());
            List<Candidate> pBest = p.getPersonalBest();
            List<Candidate> gBest = new ArrayList<>(globalBest);

            // הגנה על המעגל: הסרת נקודת הסיום הזמנית כדי שפעולות החיתוך לא יהרסו אותה
            if (isCircular) {
                if (current.size() > 1 && current.get(current.size() - 1).getId().equals(startNode.getId())) {
                    current.remove(current.size() - 1);
                }
                if (pBest != null && pBest.size() > 1
                        && pBest.get(pBest.size() - 1).getId().equals(startNode.getId())) {
                    pBest = new ArrayList<>(pBest.subList(0, pBest.size() - 1));
                }
                if (gBest != null && gBest.size() > 1
                        && gBest.get(gBest.size() - 1).getId().equals(startNode.getId())) {
                    gBest = new ArrayList<>(gBest.subList(0, gBest.size() - 1));
                }
            }

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

        public double evaluateFitness(List<Candidate> toImprove) {
            double totalReward = 0.0;
            for (Candidate candidate : toImprove) {
                totalReward += candidate.getReward();
            }
            return totalReward;
        }

        public List<Candidate> projectToFeasible(List<Candidate> toImprove) {
            if (toImprove == null || toImprove.isEmpty())
                return toImprove;

            List<Candidate> result = new ArrayList<>();
            result.add(toImprove.get(0));
            double cost = 0.0;

            for (int i = 1; i < toImprove.size(); i++) {
                Candidate currentValid = result.get(result.size() - 1);
                Candidate nextAttempt = toImprove.get(i);
                double edgeCost = graph.getCost(currentValid, nextAttempt);

                if (edgeCost == Double.POSITIVE_INFINITY)
                    continue;

                if (isCircular) {
                    // מנסים לבדוק אם גוגל נתן לנו קשת ישירה חזרה הביתה
                    double returnCost = graph.getCost(nextAttempt, startNode);

                    // בדיקת התקציב: עלות עד כה + עלות הוספת הנקודה + עלות החזרה הביתה
                    if (cost + edgeCost + returnCost > T_MAX) {
                        break; // אי אפשר להוסיף את הנקודה הזו, חייבים להתחיל לחזור הביתה
                    }
                } else {
                    // לוגיקה מקורית למסלול קווי (Point-to-Point)
                    if (cost + edgeCost > T_MAX) {
                        break;
                    }
                }

                result.add(nextAttempt);
                cost += edgeCost;
            }

            if (isCircular && result.size() > 1) {
                Candidate lastNode = result.get(result.size() - 1);
                if (!lastNode.getId().equals(startNode.getId())) {
                    result.add(startNode);
                }
            }

            return result;
        }

        private void swapOperation(List<Candidate> toImprove) {
            if (toImprove.size() < 3)
                return;
            int i = 1 + rand.nextInt(toImprove.size() - 1);
            int j;
            do {
                j = 1 + rand.nextInt(toImprove.size() - 1);
            } while (j == i);
            Collections.swap(toImprove, i, j);
        }

        private void insertOperation(List<Candidate> toImprove, List<Candidate> model) {
            if (toImprove.size() < 3 || model.size() < 3)
                return;
            int from = 1 + rand.nextInt(model.size() - 1);
            int to = from + rand.nextInt(model.size() - from);
            List<Candidate> segment = new ArrayList<>(model.subList(from, to + 1));
            toImprove.removeAll(segment);
            if (toImprove.isEmpty())
                return;
            int insertPos = 1 + rand.nextInt(toImprove.size());
            toImprove.addAll(insertPos, segment);
        }

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

            // ריצה על כל הזוגות במסלול
            for (int i = 0; i < candidates.size() - 1; i++) {
                Candidate candidate = candidates.get(i);
                Candidate nextCan = candidates.get(i + 1);

                // שליפת המרחק האמיתי מהגרף - כעת מובטח שהוא לא יהיה אינסוף!
                double distToNext = graph.getCost(candidate, nextCan);

                waypoints.add(
                        new Waypoint(candidate.getLat(), candidate.getLng(), candidate.getId(), candidate.getReward(),
                                distToNext));
            }

            // הוספת נקודת הסיום הסופית (במסלול מעגלי זו תהיה נקודת ה-START בפעם השנייה)
            Candidate lastCandidate = candidates.get(candidates.size() - 1);
            waypoints.add(new Waypoint(lastCandidate.getLat(), lastCandidate.getLng(), lastCandidate.getId(),
                    lastCandidate.getReward(), 0.0));

            return waypoints;
        }
    }
}