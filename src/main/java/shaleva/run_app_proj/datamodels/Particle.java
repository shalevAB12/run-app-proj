package shaleva.run_app_proj.datamodels;

import java.util.ArrayList;
import java.util.List;

public class Particle {

    private List<Candidate> position;       // המסלול הנוכחי
    private List<Candidate> personalBest;   // pBest
    private double fitness;
    private double bestFitness;

    public Particle(List<Candidate> initialRoute, double fitness) {
        this.position = new ArrayList<>(initialRoute);
        this.personalBest = new ArrayList<>(initialRoute);
        this.fitness = fitness;
        this.bestFitness = fitness;
    }

    public List<Candidate> getPosition() {
        return position;
    }

    public List<Candidate> getPersonalBest() {
        return personalBest;
    }

    public double getFitness() {
        return fitness;
    }

    public double getBestFitness() {
        return bestFitness;
    }

    public void setPosition(List<Candidate> newPosition) {
        this.position = new ArrayList<>(newPosition);
    }

    public void updatePersonalBest(double newFitness) {
        if (newFitness > bestFitness) {
            bestFitness = newFitness;
            personalBest = new ArrayList<>(position);
        }
        this.fitness = newFitness;
    }
}