# Quiz Leaderboard System — Bajaj Finserv Health Internship

A production-grade **Spring Boot** backend application that consumes a quiz validator API, intelligently deduplicates distributed event data, aggregates per-participant scores, and submits a verified leaderboard.

---

## 🧠 Problem Statement

A quiz system delivers participant scores across multiple polling rounds. Due to the distributed nature of the system, **duplicate events** can appear across different API polls. The challenge is to:

1. Poll the validator API **10 times** (with mandatory 5-second delays)
2. Collect and **deduplicate** events using composite keys
3. **Aggregate** scores per participant
4. Generate a **sorted leaderboard**
5. **Submit** the leaderboard and validate correctness

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    QuizRunner (CLI)                      │
│              CommandLineRunner — entry point             │
├─────────────────────────────────────────────────────────┤
│                     QuizService                          │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │  Polling  │→│ Deduplication│→│ Aggregation + Sort │  │
│  │ (10 reqs) │  │ (Set<Key>)  │  │  (Map<P, Score>)  │  │
│  └──────────┘  └──────────────┘  └───────────────────┘  │
├─────────────────────────────────────────────────────────┤
│              REST Client (RestTemplate)                  │
│         ← GET /quiz/messages | POST /quiz/submit →      │
└─────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Composite deduplication key** `roundId\|participant` | Uniquely identifies an event — same key across polls = duplicate |
| **`HashSet<String>` for O(1) lookup** | Constant-time dedup regardless of event volume |
| **Stream-based aggregation** | Clean, functional-style score summation and sorting |
| **`CommandLineRunner`** | Application runs as a CLI pipeline — no web server needed |
| **Configurable via `application.properties`** | Easy to change regNo, poll count, delays without code changes |

---

## 📁 Project Structure

```
src/main/java/com/bajaj/quiz/
├── QuizLeaderboardApplication.java   # Spring Boot entry point
├── config/
│   └── AppConfig.java                # RestTemplate with timeouts
├── model/
│   ├── Event.java                    # Quiz event (roundId, participant, score)
│   ├── PollResponse.java             # GET /quiz/messages response
│   ├── LeaderboardEntry.java         # Aggregated participant score
│   ├── SubmitRequest.java            # POST /quiz/submit request body
│   └── SubmitResponse.java           # POST /quiz/submit response body
├── service/
│   └── QuizService.java              # Core logic — poll, dedup, aggregate, submit
└── runner/
    └── QuizRunner.java               # CLI runner — orchestrates the pipeline
```

---

## 🔑 Deduplication Strategy

In distributed systems, **at-least-once delivery** is common — the same event may arrive in multiple polls. Naive aggregation would inflate scores:

```
❌ WRONG                           ✅ CORRECT
Poll 1 → Alice R1 +10             Poll 1 → Alice R1 +10  (new key → count)
Poll 3 → Alice R1 +10             Poll 3 → Alice R1 +10  (seen key → skip)
Total = 20                        Total = 10
```

### Implementation

```java
Set<String> seen = new HashSet<>();
for (Event e : allEvents) {
    String key = e.getRoundId() + "|" + e.getParticipant();
    if (seen.add(key)) {   // returns false if already present
        uniqueEvents.add(e);
    }
}
```

The `Set.add()` method returns `false` when the element already exists — providing an atomic check-and-insert in a single operation.

---

## 🚀 How to Run

### Prerequisites

- **Java 17+** (`java -version`)
- **Maven 3.6+** (`mvn -version`)

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/bajaj-finserv.git
   cd bajaj-finserv
   ```

2. **Set your registration number** in `src/main/resources/application.properties`:
   ```properties
   quiz.regNo=YOUR_REG_NUMBER_HERE
   ```

3. **Build & Run**
   ```bash
   mvn clean package -DskipTests
   java -jar target/quiz-leaderboard-1.0.0.jar
   ```

   Or run directly with Maven:
   ```bash
   mvn spring-boot:run
   ```

### Expected Output

```
╔═══════════════════════════════════════════════════════════╗
║          QUIZ LEADERBOARD SYSTEM — Bajaj Finserv         ║
╚═══════════════════════════════════════════════════════════╝

[Polling 10 times with 5-second intervals...]

┌─────────────────────────────────────────────────────────┐
│              🏆  FINAL LEADERBOARD  🏆                   │
├──────┬───────────────────────┬──────────────────────────┤
│ Rank │ Participant           │ Total Score              │
├──────┼───────────────────────┼──────────────────────────┤
│ 1    │ Alice                 │                      120 │
│ 2    │ Bob                   │                      100 │
└──────┴───────────────────────┴──────────────────────────┘

╔═══════════════════════════════════════════════════════════╗
║  ✅  SUBMISSION RESULT: CORRECT!                         ║
╚═══════════════════════════════════════════════════════════╝
```

---

## ⚙️ Configuration

All settings are externalized in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `quiz.regNo` | — | Your registration number (required) |
| `quiz.api.base-url` | `https://devapigw.vidalhealthtpa.com/srm-quiz-task` | Validator API base URL |
| `quiz.api.poll-count` | `10` | Number of polls to execute |
| `quiz.api.poll-delay-ms` | `5000` | Delay between polls (milliseconds) |

---

## 🛡️ Error Handling

- **Network failures**: Each poll is wrapped in try-catch — individual failures don't crash the pipeline
- **Empty responses**: Gracefully handled with null-checks on event lists
- **Thread interruption**: Clean interruption handling during mandatory sleep periods
- **Timeout protection**: RestTemplate configured with 10s connect and 30s read timeouts

---

## 📊 Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Runtime |
| Spring Boot | 3.2.5 | Application framework |
| RestTemplate | — | HTTP client for API communication |
| Jackson | — | JSON serialization/deserialization |
| Maven | — | Build tool |
| SLF4J + Logback | — | Structured logging |

---

## 📝 License

This project was created as part of the Bajaj Finserv Health internship selection process.
