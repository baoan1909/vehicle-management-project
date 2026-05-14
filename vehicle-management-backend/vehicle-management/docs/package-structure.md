# Package Structure

This repository now uses a schema-first package layout aligned with `src/main/resources/db/vehicle_management.sql`.

## Main package direction

- Group by business schema first
- Group by feature second
- Keep Clean Architecture boundaries inside each feature

## Main source tree

```text
src/main/java/com/ban/vehicle_management
+-- controller
|   +-- <schema>/<feature>
|   `-- dto/<schema>/<feature>/{request,response}
+-- application
|   `-- <schema>/<feature>/{port/in,port/out,service,service/impl}
+-- domain
|   `-- <schema>/<feature>/{model,policy,service}
+-- infrastructure
|   +-- mapper/<schema>/<feature>
|   +-- persistence/<schema>/<feature>
|   `-- security
`-- shared
```

## Schema to feature map

- `iam`: `account`, `role`, `permission`
- `people`: `userprofile`, `customer`, `employee`, `customervehicle`
- `catalog`: `vehicletype`, `tickettype`, `cardtype`, `priceplan`, `pricerule`, `holidaycalendar`
- `accesscontrol`: `card`, `subscription`, `lostcardreport`
- `parking`: `parkinglot`, `zone`, `parkingspace`, `lane`, `parkingsession`, `parkingevent`
- `billing`: `invoice`, `payment`
- `operations`: `shift`, `approvalrequest`, `supportticket`
- `hardware`: `device`
- `notification`: `notification`
- `audit`: `auditlog`

## Notes

- Legacy skeleton folders such as `authorization`, `cardswipe`, `parkingfee`, `report`, `ticket`, and `user` were removed because they do not match the current PostgreSQL schema vocabulary.
- Supporting tables such as `iam.refresh_tokens`, `iam.login_attempts`, `iam.account_status_history`, `iam.role_permissions`, and `operations.shift_assignments` should live inside the nearest owning feature instead of becoming separate top-level modules.
- Test directories under `src/test/java/com/ban/vehicle_management` follow the same schema-first grouping.
