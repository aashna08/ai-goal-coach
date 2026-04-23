package com.example.goalcoach.engine;

import com.example.goalcoach.model.GoalCoachResponse;

public interface GoalCoachEngine {
    GoalCoachResponse refine(String rawGoal);
}
