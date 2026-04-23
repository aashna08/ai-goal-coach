package com.example.goalcoach.safety;

import com.example.goalcoach.model.GoalCoachResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Shared server-side guardrails for goal inputs (used by both deterministic stub and external LLM adapters).
 */
public final class GoalInputSafety {
    private static final Set<String> UNSAFE_MARKERS = new HashSet<>(Arrays.asList(
            "drop table", "truncate", "delete from", "insert into", "update ",
            "--", "/*", "*/", ";--", " or 1=1", "' or '1'='1",
            "kill yourself", "self-harm", "suicide", "bomb", "make a bomb"
    ));

    private static final Set<String> PROFANITY_MARKERS = new HashSet<>(Arrays.asList(
            "fuck", "shit", "bitch", "asshole", "cunt"
    ));

    private GoalInputSafety() {}

    /**
     * If present, the caller should return this response immediately (do not call a model).
     */
    public static Optional<GoalCoachResponse> earlyExitForUnsafeOrNonsense(String rawGoal) {
        String goal = rawGoal == null ? "" : rawGoal.trim();
        if (goal.isEmpty()) {
            return Optional.of(new GoalCoachResponse("", new ArrayList<>(), 1));
        }

        String normalized = goal.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, UNSAFE_MARKERS) || containsAny(normalized, PROFANITY_MARKERS)) {
            return Optional.of(new GoalCoachResponse("", new ArrayList<>(), 1));
        }

        if (isMostlyGibberish(goal)) {
            return Optional.of(new GoalCoachResponse("", new ArrayList<>(), 2));
        }

        return Optional.empty();
    }

    private static boolean containsAny(String haystack, Set<String> needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }

    private static boolean isMostlyGibberish(String s) {
        String trimmed = s.trim();
        if (trimmed.length() < 6) return true;
        int letters = 0;
        int vowels = 0;
        int nonLetters = 0;
        for (char c : trimmed.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                char lc = Character.toLowerCase(c);
                if (lc == 'a' || lc == 'e' || lc == 'i' || lc == 'o' || lc == 'u') vowels++;
            } else if (!Character.isWhitespace(c)) {
                nonLetters++;
            }
        }
        if (letters == 0) return true;
        double vowelRatio = (double) vowels / Math.max(1, letters);
        double nonLetterRatio = (double) nonLetters / Math.max(1, trimmed.length());
        return vowelRatio < 0.18 || nonLetterRatio > 0.35;
    }
}
