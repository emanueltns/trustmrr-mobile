-- Auth, tiers and chat schema (design doc docs/superpowers/specs/2026-09-28-founderrooms-design.md).
-- Column shapes must match the Exposed table objects exactly:
--   users    -> auth/UsersTable.kt
--   tiers    -> tiers/TiersTable.kt (id is VARCHAR, not auto-increment; Task 1 fix)
--   messages -> chat/MessagesTable.kt

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    verified_mrr    DOUBLE PRECISION NULL,
    refresh_version INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL
);

CREATE TABLE tiers (
    id          VARCHAR(32) PRIMARY KEY,
    emoji       VARCHAR(16) NOT NULL,
    label       VARCHAR(32) NOT NULL,
    mrr_floor   DOUBLE PRECISION NOT NULL,
    mrr_ceiling DOUBLE PRECISION NULL,
    sort_order  INT NOT NULL
);

-- Locked seed values, design doc section 7. Do not alter.
INSERT INTO tiers (id, emoji, label, mrr_floor, mrr_ceiling, sort_order) VALUES
    ('public', '🌱', '$0',         0,       0,        0),
    ('1',      '🌿', '$1-$1K',      1,       1000,     1),
    ('2',      '🌳', '$1K-$10K',    1000,    10000,    2),
    ('3',      '🚀', '$10K-$100K',  10000,   100000,   3),
    ('4',      '💎', '$100K-$1M',   100000,  1000000,  4),
    ('5',      '👑', '$1M+',        1000000, NULL,     5);

-- No users or messages seeded here: founder accounts are seeded in Kotlin by
-- FounderSeeder.kt at startup (it needs jbcrypt, which SQL cannot call), and
-- v1 ships with no seed message history.
CREATE TABLE messages (
    id               BIGSERIAL PRIMARY KEY,
    tier_id          VARCHAR(32) NOT NULL REFERENCES tiers(id),
    author_id        BIGINT NOT NULL REFERENCES users(id),
    author_name      VARCHAR(255) NOT NULL,
    author_mrr_badge VARCHAR(32) NULL,
    body             TEXT NOT NULL,
    link_url         TEXT NULL,
    link_title       VARCHAR(255) NULL,
    quoted_author    VARCHAR(255) NULL,
    quoted_excerpt   TEXT NULL,
    created_at       TIMESTAMP NOT NULL
);
