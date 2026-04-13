# Fulfill Order By Units Implementation Plan

## Goal

Allow a supplying facility to fulfill an order line in dispensing units, while preserving the
current pack-based behavior for existing clients, CSV imports, draft shipments, shipments, proof of
delivery, and stock event posting.

Example:

- Order line requests `1` pack of Paracetamol.
- `netContent = 1000`.
- Facility may ship `500` dispensing units instead of `1` full pack.

The implementation must preserve current behavior unless the new unit-based mode is explicitly
used.

## Current State

The fulfillment service stores numeric quantities as plain integers, but several paths assume those
integers are pack counts:

- Order export computes dispensing units as `orderedQuantity * netContent`.
- Shipment and Proof of Delivery stock events multiply exported quantity by `netContent`.
- Shipment and POD DTOs expose raw quantity fields without declaring what the quantity means.

Relevant code:

- [src/main/java/org/openlmis/fulfillment/web/util/StockEventBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/util/StockEventBuilder.java:245)
- [src/main/java/org/openlmis/fulfillment/web/util/OrderExportHelper.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/util/OrderExportHelper.java:162)
- [src/main/java/org/openlmis/fulfillment/service/ExporterBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/service/ExporterBuilder.java:91)
- [src/main/java/org/openlmis/fulfillment/domain/ProofOfDelivery.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/domain/ProofOfDelivery.java:118)
- [src/main/java/org/openlmis/fulfillment/domain/ProofOfDeliveryLineItem.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/domain/ProofOfDeliveryLineItem.java:115)

## Recommended Design

### Quantity Semantics

Keep order quantities pack-based. Add explicit quantity semantics to shipment and shipment-draft
line items.

Proposed enum:

- `PACKS`
- `DISPENSING_UNITS`

Interpretation:

- `OrderLineItem.orderedQuantity` remains packs.
- `ShipmentLineItem.quantityShipped` is interpreted according to `quantityType`.
- `ShipmentDraftLineItem.quantityShipped` is interpreted according to `quantityType`.
- `ProofOfDeliveryLineItem.quantityAccepted` and `quantityRejected` must use the same semantic as
  the related shipment line.

This preserves backward compatibility because existing records and old clients will default to
`PACKS`.

## Scope

### In Scope

- Shipment drafts
- Shipments
- Proof of delivery validation and stock event generation
- Shipment CSV import
- DTOs and REST API payloads
- Database migration and backfill
- Unit and integration tests

### Out of Scope For First Increment

- Changing order entry to request units
- Splitting a single shipment line into both packs and units
- Changing stock-management service contracts
- UI redesign beyond labeling and field support required for the new backend contract

## Concrete Backend Changes

### 1. New Enum

Add:

- `src/main/java/org/openlmis/fulfillment/domain/ShipmentQuantityType.java`

Contents:

- `PACKS`
- `DISPENSING_UNITS`

Use `@Enumerated(EnumType.STRING)` in entities.

### 2. Shipment Domain

Update:

- [src/main/java/org/openlmis/fulfillment/domain/ShipmentLineItem.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/domain/ShipmentLineItem.java:42)

Add field:

- `private ShipmentQuantityType quantityType;`

Changes required:

- include field in constructors
- default null imports to `PACKS`
- export `quantityType`
- preserve it in `copy()`
- expose getter

Update interfaces:

- `ShipmentLineItem.Exporter`
- `ShipmentLineItem.Importer`

New methods:

- `void setQuantityType(ShipmentQuantityType quantityType);`
- `ShipmentQuantityType getQuantityType();`

### 3. Shipment Draft Domain

Update:

- [src/main/java/org/openlmis/fulfillment/domain/ShipmentDraftLineItem.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/domain/ShipmentDraftLineItem.java:34)

Add field:

- `private ShipmentQuantityType quantityType;`

Changes required:

- include field in constructors
- default null imports to `PACKS`
- export `quantityType`
- preserve it in `copy()`
- carry it through `updateFrom`

### 4. Shipment DTOs

Update:

- [src/main/java/org/openlmis/fulfillment/web/shipment/ShipmentLineItemDto.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/shipment/ShipmentLineItemDto.java:42)

Add field:

- `private ShipmentQuantityType quantityType;`

Behavior:

- incoming null defaults to `PACKS` in domain construction, not in DTO
- serialized responses always include `quantityType`

This same DTO is reused by shipment drafts, so this also updates:

- [src/main/java/org/openlmis/fulfillment/web/shipmentdraft/ShipmentDraftDto.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/shipmentdraft/ShipmentDraftDto.java:34)
- [src/main/java/org/openlmis/fulfillment/web/shipmentdraft/ShipmentDraftDtoBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/shipmentdraft/ShipmentDraftDtoBuilder.java:37)

### 5. Stock Event Conversion Logic

Update:

- [src/main/java/org/openlmis/fulfillment/web/util/StockEventBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/util/StockEventBuilder.java:245)

Replace the implicit conversion rule with an explicit one:

- if `quantityType == PACKS`, convert `quantity * netContent`
- if `quantityType == DISPENSING_UNITS`, use quantity as-is

Recommended refactor:

- rename `convertQuantityToDispensingUnits` to `normalizeQuantityForStockEvent`
- pass `ShipmentQuantityType`
- avoid looking up `orderables` again inside the conversion method

Suggested method shape:

```java
private void normalizeQuantityForStockEvent(StockEventLineItemDto dto,
    OrderableDto orderableDto, ShipmentQuantityType quantityType) {
  if (quantityType == ShipmentQuantityType.PACKS) {
    dto.setQuantity(Math.toIntExact(dto.getQuantity() * orderableDto.getNetContent()));
  }
}
```

For Proof of Delivery:

- derive quantity type from the related shipment line matched by orderable and lot
- use the shipment line's `quantityType` when building the stock event line

### 6. Proof Of Delivery Validation

Update:

- [src/main/java/org/openlmis/fulfillment/domain/ProofOfDelivery.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/domain/ProofOfDelivery.java:118)
- [src/main/java/org/openlmis/fulfillment/domain/ProofOfDeliveryLineItem.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/domain/ProofOfDeliveryLineItem.java:115)

The current validation is numerically fine as long as both shipment and POD use the same semantic.
No quantity conversion is needed in validation itself.

Changes required:

- keep `quantityAccepted + quantityRejected == quantityShipped`
- include `quantityType` in exported POD DTO line items so UI/API consumers know the expected unit

Recommended addition:

- add `ShipmentQuantityType quantityType` to `ProofOfDeliveryLineItemDto`

Update:

- [src/main/java/org/openlmis/fulfillment/web/util/ProofOfDeliveryLineItemDto.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/util/ProofOfDeliveryLineItemDto.java:39)

This field is informative for clients and does not need to be persisted in the POD table for the
first increment because the canonical value lives on the linked shipment line.

### 7. Order DTO Behavior

Do not change ordering semantics in this increment.

Keep:

- [src/main/java/org/openlmis/fulfillment/web/util/OrderLineItemDto.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/util/OrderLineItemDto.java:37)
- [src/main/java/org/openlmis/fulfillment/web/util/OrderExportHelper.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/util/OrderExportHelper.java:176)
- [src/main/java/org/openlmis/fulfillment/service/ExporterBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/service/ExporterBuilder.java:103)

Behavior:

- `orderedQuantity` remains packs
- `totalDispensingUnits` remains `orderedQuantity * netContent`

Optional response enhancement:

- add a read-only `fulfillmentQuantityTypeSupported` flag at order or line-item level later if the
  UI needs capability discovery, but not required for the backend implementation

### 8. CSV Import

Update:

- [src/main/java/org/openlmis/fulfillment/service/shipment/ShipmentLineItemBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/service/shipment/ShipmentLineItemBuilder.java:45)
- [src/main/java/org/openlmis/fulfillment/util/FileColumnKeyPath.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/util/FileColumnKeyPath.java:29)
- [src/main/java/org/openlmis/fulfillment/web/validator/FileTemplateValidator.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/validator/FileTemplateValidator.java:42)

Recommended approach:

- add optional template column `quantityType`
- accepted values: `PACKS`, `DISPENSING_UNITS`
- if missing, default to `PACKS`

This keeps existing shipment import files valid.

Alternative:

- add `quantityDispensedUnits` column instead of `quantityType`

Recommendation:

- prefer `quantityType` over a second quantity field because it keeps one quantity column and one
  semantic flag across REST and CSV

### 9. Validation Rules

Existing validators do not block this feature.

Keep:

- [src/main/java/org/openlmis/fulfillment/web/validator/OrderValidator.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/main/java/org/openlmis/fulfillment/web/validator/OrderValidator.java:36)

Add shipment-level validation in controller or domain helper:

- `quantityShipped >= 0`
- `quantityType != null` after defaulting
- if `quantityType == DISPENSING_UNITS`, `orderable.netContent > 0`

Optional business validation for later:

- prohibit shipping more dispensing units than `orderedQuantity * netContent`
- allow over-shipment only if current business rules already allow it

Recommendation for first increment:

- do not introduce new max-quantity restrictions unless the ministry explicitly requests them
- preserve current permissive behavior

## Database Migration Plan

### New Columns

Add new nullable columns first, then backfill, then set `NOT NULL`.

Tables:

- `fulfillment.shipment_line_items`
- `fulfillment.shipment_draft_line_items`

New column:

- `quantityType VARCHAR(50)`

Migration sequence:

1. add nullable column to both tables
2. backfill all existing rows to `PACKS`
3. alter column to `NOT NULL`

Do not change:

- `order_line_items`
- `proof_of_delivery_line_items`

Reason:

- order remains pack-based
- POD can derive quantity semantics from its related shipment line

### Migration File

Add a new Flyway migration under:

- `src/main/resources/db/migration/`

Follow the existing timestamp naming convention. Example placeholder:

- `20260413120000000__add_quantity_type_to_shipment_line_items.sql`

Expected SQL:

```sql
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
```

If the project prefers snake_case DB column names, use `quantitytype` only if matching existing
conventions; otherwise use explicit `@Column(name = "quantityType")` in Java.

Recommendation:

- keep DB column as `quantityType` to align with the existing camel-case schema style in this repo

## API Contract Changes

### Shipment Line Item Response

Current:

```json
{
  "orderable": { "...": "..." },
  "quantityShipped": 1
}
```

New:

```json
{
  "orderable": { "...": "..." },
  "quantityShipped": 500,
  "quantityType": "DISPENSING_UNITS"
}
```

Backward compatible behavior:

- if `quantityType` is omitted on create/update/import, default to `PACKS`
- old payloads continue to work

### Proof Of Delivery Line Item Response

Recommended response addition:

```json
{
  "orderable": { "...": "..." },
  "quantityAccepted": 450,
  "quantityRejected": 50,
  "quantityType": "DISPENSING_UNITS"
}
```

This avoids UI ambiguity.

## Test Plan

### Unit Tests To Update

#### Stock Event

Update:

- [src/test/java/org/openlmis/fulfillment/web/util/StockEventBuilderTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/web/util/StockEventBuilderTest.java:1)

Add cases:

- shipment line with `PACKS` multiplies by `netContent`
- shipment line with `DISPENSING_UNITS` does not multiply
- POD line tied to shipment with `PACKS` multiplies
- POD line tied to shipment with `DISPENSING_UNITS` does not multiply

#### Domain

Update:

- [src/test/java/org/openlmis/fulfillment/domain/ShipmentLineItemTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/domain/ShipmentLineItemTest.java:1)
- [src/test/java/org/openlmis/fulfillment/domain/ShipmentDraftLineItemTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/domain/ShipmentDraftLineItemTest.java:1)
- [src/test/java/org/openlmis/fulfillment/domain/ShipmentTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/domain/ShipmentTest.java:1)
- [src/test/java/org/openlmis/fulfillment/domain/ShipmentDraftTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/domain/ShipmentDraftTest.java:1)
- [src/test/java/org/openlmis/fulfillment/domain/ProofOfDeliveryTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/domain/ProofOfDeliveryTest.java:1)
- [src/test/java/org/openlmis/fulfillment/domain/ProofOfDeliveryLineItemTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/domain/ProofOfDeliveryLineItemTest.java:1)

Add assertions:

- default importer quantity type is `PACKS`
- export preserves `quantityType`
- `copy()` preserves `quantityType`
- POD confirm still succeeds when shipment and POD values match in units

#### DTO

Update:

- [src/test/java/org/openlmis/fulfillment/web/shipment/ShipmentLineItemDtoTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/web/shipment/ShipmentLineItemDtoTest.java:1)
- [src/test/java/org/openlmis/fulfillment/web/shipmentdraft/ShipmentDraftDtoTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/web/shipmentdraft/ShipmentDraftDtoTest.java:1)
- [src/test/java/org/openlmis/fulfillment/web/util/ProofOfDeliveryLineItemDtoTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/web/util/ProofOfDeliveryLineItemDtoTest.java:1)

Add assertions:

- `quantityType` serializes and deserializes correctly
- omitted `quantityType` remains compatible with old JSON fixtures if applicable

#### CSV Import

Update:

- [src/test/java/org/openlmis/fulfillment/service/shipment/ShipmentLineItemBuilderTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/service/shipment/ShipmentLineItemBuilderTest.java:1)
- [src/test/java/org/openlmis/fulfillment/service/shipment/ShipmentBuilderTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/service/shipment/ShipmentBuilderTest.java:1)

Add cases:

- import with no `quantityType` column defaults line to `PACKS`
- import with `quantityType=PACKS` works
- import with `quantityType=DISPENSING_UNITS` works
- invalid `quantityType` fails with a clear message

### Integration Tests To Update

Update:

- [src/integration-test/java/org/openlmis/fulfillment/web/ShipmentControllerIntegrationTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/integration-test/java/org/openlmis/fulfillment/web/ShipmentControllerIntegrationTest.java:1)
- [src/integration-test/java/org/openlmis/fulfillment/web/ShipmentDraftControllerIntegrationTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/integration-test/java/org/openlmis/fulfillment/web/ShipmentDraftControllerIntegrationTest.java:1)
- [src/integration-test/java/org/openlmis/fulfillment/web/ProofOfDeliveryControllerIntegrationTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/integration-test/java/org/openlmis/fulfillment/web/ProofOfDeliveryControllerIntegrationTest.java:1)
- [src/integration-test/java/org/openlmis/fulfillment/repository/ShipmentRepositoryIntegrationTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/integration-test/java/org/openlmis/fulfillment/repository/ShipmentRepositoryIntegrationTest.java:1)
- [src/integration-test/java/org/openlmis/fulfillment/repository/ShipmentDraftRepositoryIntegrationTest.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/integration-test/java/org/openlmis/fulfillment/repository/ShipmentDraftRepositoryIntegrationTest.java:1)

Add flows:

- create shipment with omitted `quantityType` and verify persisted `PACKS`
- create shipment with `DISPENSING_UNITS` and verify persisted semantics
- fetch shipment and draft and verify `quantityType` is returned
- confirm POD against a unit-based shipment

### Test Data Builders To Update

Update builders so new tests can be written cleanly:

- [src/test/java/org/openlmis/fulfillment/testutils/ShipmentLineItemDataBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/testutils/ShipmentLineItemDataBuilder.java:1)
- [src/test/java/org/openlmis/fulfillment/testutils/ShipmentDraftLineItemDataBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/testutils/ShipmentDraftLineItemDataBuilder.java:1)
- [src/test/java/org/openlmis/fulfillment/web/shipment/ShipmentLineItemDtoDataBuilder.java](/Users/motsokasephali/Code/elmis_github/lesotho-fulfillment/src/test/java/org/openlmis/fulfillment/web/shipment/ShipmentLineItemDtoDataBuilder.java:1)
- `DummyShipmentLineItemDto`
- `DummyShipmentDto`
- `DummyShipmentDraftDto`

Default builder value:

- `quantityType = PACKS`

## Implementation Order

1. Add enum and DB migration.
2. Extend shipment and shipment-draft entities.
3. Extend shipment DTOs and builders.
4. Update stock event quantity normalization.
5. Expose `quantityType` in POD DTOs.
6. Extend CSV template support.
7. Update tests and builders.
8. Run targeted tests, then broader suite.

## Suggested Verification Commands

Run targeted tests first:

```bash
./gradlew test --tests org.openlmis.fulfillment.web.util.StockEventBuilderTest
./gradlew test --tests org.openlmis.fulfillment.service.shipment.ShipmentLineItemBuilderTest
./gradlew test --tests org.openlmis.fulfillment.domain.ProofOfDeliveryTest
```

Then run the full unit and integration suites used by this repo.
  typed manually

## Open Questions For The Ministry

These do not block the first backend increment, but they should be answered before UI rollout:

- Should unit-based fulfillment be allowed for every orderable or only selected products?
- Should users be allowed to ship more units than the ordered packs converted to units?
- Should the printed shipment and POD documents display both packs and units?
- Should CSV templates always include `quantityType`, or is default-to-packs enough?

## Recommendation

Implement this as an explicit quantity-mode extension on shipment lines, not as a silent semantic
change to `quantityShipped`. That is the safest path that preserves all current pack-based behavior
while enabling unit-based fulfillment for Lesotho.
