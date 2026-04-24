package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a single quiz event returned by the validator API.
 * Each event captures one participant's score in one round.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {

    private String roundId;
    private String participant;
    private int score;

    public Event() {}

    public Event(String roundId, String participant, int score) {
        this.roundId = roundId;
        this.participant = participant;
        this.score = score;
    }

    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }

    public String getParticipant() { return participant; }
    public void setParticipant(String participant) { this.participant = participant; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    /**
     * Composite deduplication key: roundId + "|" + participant.
     * Two events with the same key are duplicates and must not be
     * counted twice.
     */
    public String deduplicationKey() {
        return roundId + "|" + participant;
    }

    @Override
    public String toString() {
        return String.format("Event{round=%s, participant=%s, score=%d}", roundId, participant, score);
    }
}
