-- Optimistic-locking version column for reviews. It guards the approval read-modify-write against
-- concurrent approvals. Hibernate manages the value; the default covers any rows inserted outside JPA.
ALTER TABLE reviews ADD COLUMN version bigint NOT NULL DEFAULT 0;
