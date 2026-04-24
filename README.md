# Quiz Leaderboard System

A production-grade Spring Boot backend application that consumes a quiz validator API, intelligently deduplicates distributed event data, aggregates per-participant scores, and submits a verified leaderboard.

---

## Problem Statement

A quiz system delivers participant scores across multiple polling rounds. Due to the distributed nature of the system, duplicate events can appear across different API polls. The challenge is to:

1. Poll the validator API 10 times (with mandatory 5-second delays)
2. Collect and deduplicate events using composite keys
3. Aggregate scores per participant
4. Generate a sorted leaderboard
5. Submit the leaderboard and validate correctness

---

## Architecture

```
+---------------------------------------------------------+
|                    QuizRunner (CLI)                     |
|              CommandLineRunner - entry point            |
+---------------------------------------------------------+
|                     QuizService                         |
|  +----------+  +--------------+  +-------------------+  |
|  |  Polling  |->| Deduplication|->| Aggregation + Sort|  |
|  | (10 reqs) |  | (Set<Key>)   |  |  (Map<P, Score>)  |  |
|  +----------+  +--------------+  +-------------------+  |
+---------------------------------------------------------+
|              REST Client (RestTemplate)                 |
|         <- GET /quiz/messages | POST /quiz/submit ->    |
+---------------------------------------------------------+
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Composite deduplication key** `roundId|participant` | Uniquely identifies an event - same key across polls = duplicate |
| **HashSet for O(1) lookup** | Constant-time deduplication regardless of event volume |
| **Stream-based aggregation** | Clean, functional-style score summation and sorting |
| **CommandLineRunner** | Application runs as a CLI pipeline |
| **Configurable via properties** | Easy to adjust parameters without code changes |

---

## Project Structure

```
src/main/java/com/bajaj/quiz/
├── QuizLeaderboardApplication.java   # Spring Boot entry point
├── config/
│   └── AppConfig.java                # RestTemplate with timeouts
├── model/
│   ├── Event.java                    # Quiz event model
│   ├── PollResponse.java             # GET response model
│   ├── LeaderboardEntry.java         # Aggregated score model
│   ├── SubmitRequest.java            # POST request model
│   └── SubmitResponse.java           # POST response model
├── service/
│   └── QuizService.java              # Core business logic
└── runner/
    └── QuizRunner.java               # Execution pipeline
```

---

## Deduplication Strategy

In distributed systems, at-least-once delivery is common - the same event may arrive in multiple polls. Naive aggregation would inflate scores. This system uses a Set-based approach to ensure each unique event is processed exactly once.

```java
Set<String> seen = new HashSet<>();
for (Event e : allEvents) {
    String key = e.getRoundId() + "|" + e.getParticipant();
    if (seen.add(key)) {
        uniqueEvents.add(e);
    }
}
```

The `Set.add()` method returns `false` when the element already exists, providing an efficient check-and-insert operation.

---

## How to Run

### Prerequisites

- Java 17+
- Maven 3.6+ (or use the included mvnw)

### Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd <project-directory>
   ```

2. **Configure your registration number** in `src/main/resources/application.properties`:
   ```properties
   quiz.regNo=YOUR_REG_NUMBER
   ```

3. **Build & Run**
   ```bash
   chmod +x mvnw
   ./mvnw clean package -DskipTests
   java -jar target/quiz-leaderboard-1.0.0.jar
   ```

### Expected Output

```
+-----------------------------------------------------------+
|              QUIZ LEADERBOARD SYSTEM                      |
+-----------------------------------------------------------+
|  Registration : RA2311008020034                           |
+-----------------------------------------------------------+

[Polling 10 times with 5-second intervals...]

+------+---------------------+------------------+
| Rank | Participant         | Total Score      |
+------+---------------------+------------------+
| 1    | Diana               |              470 |
| 2    | Ethan               |              455 |
| 3    | Fiona               |              440 |
+------+---------------------+------------------+
| GRAND TOTAL                 |             1365 |
+-----------------------------+------------------+

+-----------------------------------------------------------+
|  SUBMISSION RESULT: CORRECT                               |
+-----------------------------------------------------------+
|  Idempotent  : true                                       |
|  Submitted   : 1365                                       |
|  ...                                                      |
+-----------------------------------------------------------+
```

---

## Configuration

Settings are managed via `application.properties`:

| Property | Description |
|----------|-------------|
| `quiz.regNo` | Participant registration number |
| `quiz.api.base-url` | Validator API base URL |
| `quiz.api.poll-count` | Number of polls (default: 10) |
| `quiz.api.poll-delay-ms` | Polling interval (default: 5000ms) |

---

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Maven (Build Tool)
- RestTemplate (HTTP Client)
- Jackson (JSON Processing)
- SLF4J + Logback (Logging)
