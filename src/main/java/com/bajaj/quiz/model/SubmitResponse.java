package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from POST /quiz/submit indicating whether the
 * submitted leaderboard was correct.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitResponse {

    private boolean isCorrect;
    private boolean isIdempotent;
    private int submittedTotal;
    private int expectedTotal;
    private String message;

    public SubmitResponse() {}

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public boolean isIdempotent() { return isIdempotent; }
    public void setIdempotent(boolean idempotent) { isIdempotent = idempotent; }

    public int getSubmittedTotal() { return submittedTotal; }
    public void setSubmittedTotal(int submittedTotal) { this.submittedTotal = submittedTotal; }

    public int getExpectedTotal() { return expectedTotal; }
    public void setExpectedTotal(int expectedTotal) { this.expectedTotal = expectedTotal; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
