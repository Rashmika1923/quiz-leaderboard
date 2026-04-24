package com.bajaj.quiz.runner;

import com.bajaj.quiz.model.*;
import com.bajaj.quiz.service.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs the quiz pipeline automatically when the application starts.
 * Executes all 4 stages in sequence, prints the leaderboard to the
 * console, and exits.
 */
@Component
public class QuizRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QuizRunner.class);

    private final QuizService quizService;

    @Value("${quiz.regNo}")
    private String regNo;

    public QuizRunner(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    public void run(String... args) {
        printBanner();

        // ── Stage 1: Poll ──────────────────────────────────────
        log.info("═══════════════════════════════════════════════════");
        log.info("  STAGE 1 / 4 — Polling Validator API (10 polls)");
        log.info("═══════════════════════════════════════════════════");
        List<PollResponse> responses = quizService.pollAllMessages();

        if (responses.isEmpty()) {
            log.error("❌ No responses received. Check your regNo and network.");
            return;
        }

        // ── Stage 2: Deduplicate ───────────────────────────────
        log.info("");
        log.info("═══════════════════════════════════════════════════");
        log.info("  STAGE 2 / 4 — Deduplicating Events");
        log.info("═══════════════════════════════════════════════════");
        List<Event> uniqueEvents = quizService.deduplicateEvents(responses);

        // ── Stage 3: Build Leaderboard ─────────────────────────
        log.info("");
        log.info("═══════════════════════════════════════════════════");
        log.info("  STAGE 3 / 4 — Building Leaderboard");
        log.info("═══════════════════════════════════════════════════");
        List<LeaderboardEntry> leaderboard = quizService.buildLeaderboard(uniqueEvents);

        printLeaderboard(leaderboard);

        // ── Stage 4: Submit ────────────────────────────────────
        log.info("");
        log.info("═══════════════════════════════════════════════════");
        log.info("  STAGE 4 / 4 — Submitting Leaderboard");
        log.info("═══════════════════════════════════════════════════");
        SubmitResponse result = quizService.submitLeaderboard(leaderboard);

        printResult(result);
    }

    // ───────────────────────────────────────────────────────────
    //  CONSOLE OUTPUT FORMATTERS
    // ───────────────────────────────────────────────────────────

    private void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║          QUIZ LEADERBOARD SYSTEM — Bajaj Finserv         ║");
        System.out.println("║                  Internship Assignment                    ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║  Registration : " + padRight(regNo, 40) + "║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printLeaderboard(List<LeaderboardEntry> leaderboard) {
        int totalScore = leaderboard.stream()
                .mapToInt(LeaderboardEntry::getTotalScore)
                .sum();

        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────┐");
        System.out.println("│              🏆  FINAL LEADERBOARD  🏆          │");
        System.out.println("├──────┬───────────────────────┬──────────────────┤");
        System.out.println("│ Rank │ Participant           │ Total Score      │");
        System.out.println("├──────┼───────────────────────┼──────────────────┤");

        int rank = 1;
        for (LeaderboardEntry entry : leaderboard) {
            System.out.printf("│ %-4d │ %-21s │ %16d │%n",
                    rank++, entry.getParticipant(), entry.getTotalScore());
        }

        System.out.println("├──────┴───────────────────────┼──────────────────┤");
        System.out.printf("│ GRAND TOTAL                   │ %16d │%n", totalScore);
        System.out.println("└───────────────────────────────┴──────────────────┘");
        System.out.println();

        log.info("Grand Total Score (all participants): {}", totalScore);
    }

    private void printResult(SubmitResponse result) {
        System.out.println();
        if (result == null) {
            System.out.println("❌ No response from the validator. Submission may have failed.");
            return;
        }

        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        if (result.isCorrect()) {
            System.out.println("║  ✅  SUBMISSION RESULT: CORRECT!                         ║");
        } else {
            System.out.println("║  ❌  SUBMISSION RESULT: INCORRECT                         ║");
        }
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║  Idempotent  : " + padRight(String.valueOf(result.isIdempotent()), 40) + "║");
        System.out.println("║  Submitted   : " + padRight(String.valueOf(result.getSubmittedTotal()), 40) + "║");
        System.out.println("║  Expected    : " + padRight(String.valueOf(result.getExpectedTotal()), 40) + "║");
        System.out.println("║  Message     : " + padRight(result.getMessage(), 40) + "║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private String padRight(String s, int n) {
        if (s == null) s = "N/A";
        return String.format("%-" + n + "s", s);
    }
}
