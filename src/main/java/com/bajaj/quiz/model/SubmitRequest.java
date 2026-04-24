package com.bajaj.quiz.model;

import java.util.List;

/**
 * Payload for POST /quiz/submit.
 */
public class SubmitRequest {

    private String regNo;
    private List<LeaderboardEntry> leaderboard;

    public SubmitRequest() {}

    public SubmitRequest(String regNo, List<LeaderboardEntry> leaderboard) {
        this.regNo = regNo;
        this.leaderboard = leaderboard;
    }

    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }

    public List<LeaderboardEntry> getLeaderboard() { return leaderboard; }
    public void setLeaderboard(List<LeaderboardEntry> leaderboard) { this.leaderboard = leaderboard; }
}
