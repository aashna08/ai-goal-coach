package com.example.goalcoach.engine;

import com.example.goalcoach.model.GoalCoachResponse;
import com.example.goalcoach.service.GoalCoachStub;

public final class StubGoalCoachEngine implements GoalCoachEngine {
    private final GoalCoachStub stub = new GoalCoachStub();

    @Override
    public GoalCoachResponse refine(String rawGoal) {
        return stub.refine(rawGoal);
    }
}
