package com.bajaj.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Quiz Leaderboard System — Bajaj Finserv Health Internship Assignment.
 *
 * <p>This application polls a remote quiz validator API, deduplicates
 * event data using composite keys (roundId + participant), aggregates
 * per-participant scores, and submits a final leaderboard.</p>
 *
 * @author Rashmi
 */
@SpringBootApplication
public class QuizLeaderboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizLeaderboardApplication.class, args);
    }
}
