ALTER TABLE fulfillment.shipment_line_items
  ADD COLUMN quantityType VARCHAR(50);

ALTER TABLE fulfillment.shipment_draft_line_items
  ADD COLUMN quantityType VARCHAR(50);

UPDATE fulfillment.shipment_line_items
SET quantityType = 'PACKS'
WHERE quantityType IS NULL;

UPDATE fulfillment.shipment_draft_line_items
SET quantityType = 'PACKS'
WHERE quantityType IS NULL;

ALTER TABLE fulfillment.shipment_line_items
  ALTER COLUMN quantityType SET NOT NULL;

ALTER TABLE fulfillment.shipment_draft_line_items
  ALTER COLUMN quantityType SET NOT NULL;
