CREATE INDEX IF NOT EXISTS idx_recommendation_events_created_at
    ON recommendation_events (created_at DESC);
