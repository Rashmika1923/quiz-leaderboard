package com.bajaj.quiz.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Maps the JSON payload returned by GET /quiz/messages.
 *
 * <pre>
 * {
 *   "regNo": "...",
 *   "setId": "SET_1",
 *   "pollIndex": 0,
 *   "events": [ ... ]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollResponse {

    private String regNo;
    private String setId;
    private int pollIndex;
    private List<Event> events;

    public PollResponse() {}

    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }

    public String getSetId() { return setId; }
    public void setSetId(String setId) { this.setId = setId; }

    public int getPollIndex() { return pollIndex; }
    public void setPollIndex(int pollIndex) { this.pollIndex = pollIndex; }

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }
}
