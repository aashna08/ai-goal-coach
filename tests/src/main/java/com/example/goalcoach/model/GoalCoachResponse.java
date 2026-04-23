package com.example.goalcoach.model;

import java.util.List;

public class GoalCoachResponse {
    private String refined_goal;
    private List<String> key_results;
    private int confidence_score;

    // Jackson needs a default constructor
    public GoalCoachResponse() {}

    public GoalCoachResponse(String refinedGoal, List<String> keyResults, int confidenceScore) {
        this.refined_goal = refinedGoal;
        this.key_results = keyResults;
        this.confidence_score = confidenceScore;
    }

    public String getRefined_goal() {
        return refined_goal;
    }

    public void setRefined_goal(String refined_goal) {
        this.refined_goal = refined_goal;
    }

    public List<String> getKey_results() {
        return key_results;
    }

    public void setKey_results(List<String> key_results) {
        this.key_results = key_results;
    }

    public int getConfidence_score() {
        return confidence_score;
    }

    public void setConfidence_score(int confidence_score) {
        this.confidence_score = confidence_score;
    }
}

