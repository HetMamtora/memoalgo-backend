-- ============================================================
-- MemoAlgo — V1 Initial Schema
-- Managed by Flyway. Never edit this file after it has run.
-- To change the schema, create V2__description.sql instead.
-- ============================================================


-- ── USERS ────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_active_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username)
);


-- ── TOPICS ───────────────────────────────────────────────────
-- Self-referential: parent_topic_id enables topic hierarchies.
-- e.g. Trees → Binary Trees → BST
-- Query subtrees with a recursive CTE (covered in Day 7).
CREATE TABLE topics (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    parent_topic_id UUID         REFERENCES topics(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_topics_name UNIQUE (name)
);

-- Seed: default DSA topics every user starts with
INSERT INTO topics (name, description) VALUES
    ('Arrays & Strings',        'Array manipulation, string algorithms, sliding window, two pointers'),
    ('Linked Lists',             'Singly/doubly linked lists, fast & slow pointers, reversal'),
    ('Stacks & Queues',         'Stack-based problems, monotonic stacks, queue simulations'),
    ('Trees',                    'Binary trees, BST, DFS, BFS, tree construction'),
    ('Graphs',                   'Graph traversal, topological sort, union find, shortest path'),
    ('Dynamic Programming',      'Memoization, tabulation, 1D/2D DP, classic DP patterns'),
    ('Backtracking',             'Permutations, combinations, subsets, constraint satisfaction'),
    ('Binary Search',            'Search on sorted arrays, search on answer space'),
    ('Heaps & Priority Queues',  'Min-heap, max-heap, top-K problems, heap sort'),
    ('Greedy',                   'Greedy algorithms, interval scheduling, activity selection'),
    ('Bit Manipulation',         'Bitwise operations, XOR tricks, bit masks'),
    ('Math & Number Theory',     'Prime numbers, GCD, modular arithmetic, combinatorics'),
    ('Tries',                    'Prefix trees, autocomplete, word search, IP routing'),
    ('Sliding Window',           'Variable and fixed size sliding window patterns'),
    ('Two Pointers',             'Opposite ends, same direction, fast & slow pointer');


-- ── PROBLEMS ─────────────────────────────────────────────────
CREATE TABLE problems (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    topic_id    UUID                   REFERENCES topics(id)  ON DELETE SET NULL,
    title       VARCHAR(255)  NOT NULL,
    url         VARCHAR(500),
    difficulty  VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    notes       TEXT,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_problems_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);


-- ── REVIEWS ──────────────────────────────────────────────────
-- One row per (problem, user) pair.
-- This is the live SM-2 state — always reflects the current schedule.
-- SM-2 fields:
--   ease_factor      : starts at 2.5, adjusted per review (min 1.3)
--   interval_days    : days until next review (1 → 6 → growing)
--   repetition_count : how many times reviewed without "Again"
--   next_review_date : the date this problem surfaces in the queue
CREATE TABLE reviews (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id        UUID          NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    user_id           UUID          NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    ease_factor       DECIMAL(4, 2) NOT NULL DEFAULT 2.50,
    interval_days     INTEGER       NOT NULL DEFAULT 1,
    repetition_count  INTEGER       NOT NULL DEFAULT 0,
    next_review_date  DATE          NOT NULL DEFAULT CURRENT_DATE,
    last_reviewed_at  TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_reviews_problem_user UNIQUE (problem_id, user_id),
    CONSTRAINT chk_reviews_ease_factor CHECK (ease_factor >= 1.30),
    CONSTRAINT chk_reviews_interval    CHECK (interval_days >= 1),
    CONSTRAINT chk_reviews_repetition  CHECK (repetition_count >= 0)
);


-- ── REVIEW_HISTORY ───────────────────────────────────────────
-- Append-only log. One row written every time a user rates a problem.
-- Used for: streak calculation, retention rate, topic charts, heatmap.
-- quality: 0=Blackout, 1=Wrong, 2=Wrong+hint, 3=Correct+hard, 4=Correct, 5=Perfect
-- In the UI we map: Again=1, Hard=3, Good=4, Easy=5
CREATE TABLE review_history (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id           UUID          NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id             UUID          NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    quality             INTEGER       NOT NULL,
    ease_factor_before  DECIMAL(4, 2) NOT NULL,
    ease_factor_after   DECIMAL(4, 2) NOT NULL,
    interval_before     INTEGER       NOT NULL,
    interval_after      INTEGER       NOT NULL,
    reviewed_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_review_history_quality CHECK (quality BETWEEN 0 AND 5)
);


-- ── TAGS ─────────────────────────────────────────────────────
-- User-scoped: each user owns their own tag namespace.
-- UNIQUE on (user_id, name) — two users can share tag name "revisit".
CREATE TABLE tags (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tags_user_name UNIQUE (user_id, name)
);


-- ── PROBLEM_TAGS ─────────────────────────────────────────────
-- Junction table for the many-to-many between problems and tags.
-- Composite PK prevents duplicate (problem, tag) pairs.
CREATE TABLE problem_tags (
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags(id)     ON DELETE CASCADE,

    CONSTRAINT pk_problem_tags PRIMARY KEY (problem_id, tag_id)
);


-- ── INDEXES ──────────────────────────────────────────────────
-- Add indexes only where we have real query patterns.
-- Over-indexing slows down writes — don't index everything.

-- problems: most filtered by user, then topic, then difficulty
CREATE INDEX idx_problems_user_id    ON problems(user_id);
CREATE INDEX idx_problems_topic_id   ON problems(topic_id);
CREATE INDEX idx_problems_difficulty ON problems(difficulty);

-- reviews: THE hot query — "get all problems due today for this user"
-- Composite index covers both the user_id filter AND the date range scan
CREATE INDEX idx_reviews_user_next_date ON reviews(user_id, next_review_date);
CREATE INDEX idx_reviews_problem_id     ON reviews(problem_id);

-- review_history: drives stats queries — filter by user, order/group by date
CREATE INDEX idx_review_history_user_id     ON review_history(user_id);
CREATE INDEX idx_review_history_reviewed_at ON review_history(reviewed_at);
CREATE INDEX idx_review_history_review_id   ON review_history(review_id);

-- tags + junction
CREATE INDEX idx_tags_user_id        ON tags(user_id);
CREATE INDEX idx_problem_tags_tag_id ON problem_tags(tag_id);
