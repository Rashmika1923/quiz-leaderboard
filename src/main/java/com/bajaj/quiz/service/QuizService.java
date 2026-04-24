package com.bajaj.quiz.service;

import com.bajaj.quiz.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service that orchestrates the entire quiz workflow:
 *
 * <ol>
 *   <li>Polls the validator API 10 times (with mandatory 5-second gaps)</li>
 *   <li>Deduplicates events using a composite key (roundId | participant)</li>
 *   <li>Aggregates per-participant scores</li>
 *   <li>Builds and submits the final leaderboard</li>
 * </ol>
 *
 * <h3>Deduplication Strategy</h3>
 * <p>
 * In distributed systems, the same event can be delivered more than once.
 * Naively summing all received events would inflate scores. We maintain a
 * {@code HashSet<String>} of seen composite keys. An event is processed
 * only if its key has <em>never</em> been seen before.
 * </p>
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final RestTemplate restTemplate;

    @Value("${quiz.regNo}")
    private String regNo;

    @Value("${quiz.api.base-url}")
    private String baseUrl;

    @Value("${quiz.api.poll-count}")
    private int pollCount;

    @Value("${quiz.api.poll-delay-ms}")
    private long pollDelayMs;

    public QuizService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // -----------------------------------------------------------
    //  1. POLL ALL MESSAGES
    // -----------------------------------------------------------

    /**
     * Calls GET /quiz/messages for poll indices 0 through 9,
     * respecting the mandatory 5-second delay between each call.
     *
     * @return list of all PollResponse objects received
     */
    public List<PollResponse> pollAllMessages() {
        List<PollResponse> responses = new ArrayList<>();

        for (int poll = 0; poll < pollCount; poll++) {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/quiz/messages")
                    .queryParam("regNo", regNo)
                    .queryParam("poll", poll)
                    .toUriString();

            log.info("Polling [{}/{}] -> {}", poll + 1, pollCount, url);

            try {
                ResponseEntity<PollResponse> response =
                        restTemplate.getForEntity(url, PollResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    PollResponse body = response.getBody();
                    int eventCount = body.getEvents() != null ? body.getEvents().size() : 0;
                    log.info("Poll {} received -- setId={}, events={}", poll, body.getSetId(), eventCount);
                    responses.add(body);
                } else {
                    log.warn("Poll {} returned status {}", poll, response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("Poll {} failed: {}", poll, e.getMessage());
            }

            // Mandatory delay between polls (skip after the last one)
            if (poll < pollCount - 1) {
                log.info("Waiting {} ms before next poll...", pollDelayMs);
                sleep(pollDelayMs);
            }
        }

        log.info("Polling complete -- {} responses collected", responses.size());
        return responses;
    }

    // ───────────────────────────────────────────────────────────
    //  2. DEDUPLICATE EVENTS
    // ───────────────────────────────────────────────────────────

    /**
     * Flattens all events from every poll response and removes
     * duplicates using the composite key {@code roundId|participant}.
     *
     * @param responses raw poll responses
     * @return deduplicated list of events
     */
    public List<Event> deduplicateEvents(List<PollResponse> responses) {
        Set<String> seen = new HashSet<>();
        List<Event> unique = new ArrayList<>();
        int totalRaw = 0;

        for (PollResponse pr : responses) {
            if (pr.getEvents() == null) continue;
            for (Event e : pr.getEvents()) {
                totalRaw++;
                String key = e.deduplicationKey();
                if (seen.add(key)) {        // returns true only if the key was NOT already present
                    unique.add(e);
                } else {
                    log.debug("Duplicate ignored: {}", key);
                }
            }
        }

        int dupes = totalRaw - unique.size();
        log.info("Deduplication: {} raw events -> {} unique, {} duplicates removed",
                totalRaw, unique.size(), dupes);
        return unique;
    }

    // ───────────────────────────────────────────────────────────
    //  3. BUILD LEADERBOARD
    // ───────────────────────────────────────────────────────────

    /**
     * Aggregates unique events into a per-participant leaderboard,
     * sorted in descending order of totalScore.
     *
     * @param uniqueEvents deduplicated events
     * @return sorted leaderboard
     */
    public List<LeaderboardEntry> buildLeaderboard(List<Event> uniqueEvents) {
        // Sum scores per participant
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        for (Event e : uniqueEvents) {
            scoreMap.merge(e.getParticipant(), e.getScore(), Integer::sum);
        }

        // Convert to sorted list (descending by totalScore)
        List<LeaderboardEntry> leaderboard = scoreMap.entrySet().stream()
                .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed())
                .collect(Collectors.toList());

        log.info("Leaderboard built with {} participants", leaderboard.size());
        return leaderboard;
    }

    // ───────────────────────────────────────────────────────────
    //  4. SUBMIT LEADERBOARD
    // ───────────────────────────────────────────────────────────

    /**
     * POSTs the final leaderboard to the validator.
     *
     * @param leaderboard sorted leaderboard entries
     * @return the validator's response
     */
    public SubmitResponse submitLeaderboard(List<LeaderboardEntry> leaderboard) {
        String url = baseUrl + "/quiz/submit";

        SubmitRequest request = new SubmitRequest(regNo, leaderboard);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SubmitRequest> entity = new HttpEntity<>(request, headers);

        log.info("Submitting leaderboard to {} ...", url);

        ResponseEntity<SubmitResponse> response =
                restTemplate.postForEntity(url, entity, SubmitResponse.class);

        SubmitResponse body = response.getBody();
        if (body != null) {
            log.info("Submission response -- correct={}, idempotent={}, submitted={}, expected={}, message={}",
                    body.isCorrect(), body.isIdempotent(),
                    body.getSubmittedTotal(), body.getExpectedTotal(), body.getMessage());
        }
        return body;
    }

    // ───────────────────────────────────────────────────────────
    //  HELPERS
    // ───────────────────────────────────────────────────────────

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted");
        }
    }
}
