-- The "one review per author per POS" rule was previously enforced only by an application-level
-- check-then-act, which concurrent requests (and updates that change the POS or author) could bypass.
-- The database constraint is the authoritative guard; the FK columns are required by the domain model.
--
-- Precondition: the table already satisfies the invariant. A database written by the pre-fix code
-- could contain duplicate (pos_id, author_id) pairs (via review updates that changed the POS or
-- author) or NULL references; such rows must be cleaned up manually before this migration runs,
-- since no automatic dedup can decide which review to keep.
ALTER TABLE reviews ALTER COLUMN pos_id SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN author_id SET NOT NULL;
-- explicitly named so the application can map a violation to the offending fields
ALTER TABLE reviews ADD CONSTRAINT uq_reviews_pos_author UNIQUE (pos_id, author_id);
