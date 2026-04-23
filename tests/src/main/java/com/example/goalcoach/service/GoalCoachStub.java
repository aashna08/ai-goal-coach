package com.example.goalcoach.service;

import com.example.goalcoach.model.GoalCoachResponse;
import com.example.goalcoach.safety.GoalInputSafety;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GoalCoachStub {
    public GoalCoachResponse refine(String rawGoal) {
        Optional<GoalCoachResponse> early = GoalInputSafety.earlyExitForUnsafeOrNonsense(rawGoal);
        if (early.isPresent()) return early.get();

        String goal = rawGoal == null ? "" : rawGoal.trim();

        int confidence = Math.min(10, Math.max(1, 7 + (goal.length() >= 20 ? 2 : 0)));
        String refined = smartify(goal);
        List<String> krs = keyResultsFor(goal);

        return new GoalCoachResponse(refined, krs, confidence);
    }

    private static String smartify(String goal) {
        // Minimal SMART-ification: enforce timeframe + measurable outcome.
        LocalDate end = LocalDate.now().plusDays(60);
        String cleaned = goal.replaceAll("\\s+", " ").trim();
        if (cleaned.toLowerCase(Locale.ROOT).startsWith("i want to ")) {
            cleaned = cleaned.substring("i want to ".length());
            cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
        }
        return cleaned + " by " + end + " with measurable weekly progress tracking.";
    }

    private static List<String> keyResultsFor(String goal) {
        String lower = goal.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        if (lower.contains("sales")) {
            out.add("Increase qualified pipeline by 20% within 8 weeks.");
            out.add("Complete 15 customer discovery calls per week for 6 weeks.");
            out.add("Improve close rate by 5 percentage points by end date.");
            out.add("Shadow 3 top-performing reps and document 10 tactics to adopt.");
            return out;
        }

        out.add("Define a baseline metric and target (e.g., +15%) within 1 week.");
        out.add("Execute a weekly plan with at least 3 focused sessions per week for 8 weeks.");
        out.add("Track progress weekly and adjust tactics based on results.");
        return out;
    }
}

