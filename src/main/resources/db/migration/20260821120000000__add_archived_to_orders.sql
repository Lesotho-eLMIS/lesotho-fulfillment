-- Adds a soft-delete flag to orders. Archived orders are retained in the
-- database but excluded from all order searches.
ALTER TABLE orders ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
