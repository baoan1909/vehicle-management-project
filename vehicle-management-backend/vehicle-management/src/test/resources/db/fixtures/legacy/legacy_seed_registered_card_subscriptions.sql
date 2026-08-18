BEGIN;

DO $$
DECLARE
    registered_card_type_id UUID;
    monthly_ticket_type_id UUID;
    seed_record RECORD;
    seed_card_id UUID;
    seed_customer_id UUID;
    seed_vehicle_id UUID;
    seed_subscription_id UUID;
    seed_vehicle_type_id UUID;
    seed_price_rule_id UUID;
    seed_price NUMERIC(12, 2);
BEGIN
    SELECT card_type_id
    INTO registered_card_type_id
    FROM catalog.card_types
    WHERE UPPER(code) = 'REGISTERED'
    LIMIT 1;

    IF registered_card_type_id IS NULL THEN
        registered_card_type_id := '00000000-0000-0000-0000-0000000c0002';

        INSERT INTO catalog.card_types (
            card_type_id,
            code,
            name,
            description,
            is_return_required,
            is_active,
            created_at,
            updated_at
        )
        VALUES (
            registered_card_type_id,
            'REGISTERED',
            'Registered card',
            'Seed registered card type for monthly subscriptions.',
            FALSE,
            TRUE,
            now(),
            now()
        );
    ELSE
        UPDATE catalog.card_types
        SET is_active = TRUE,
            updated_at = now()
        WHERE card_type_id = registered_card_type_id;
    END IF;

    SELECT ticket_type_id
    INTO monthly_ticket_type_id
    FROM catalog.ticket_types
    WHERE status = 'ACTIVE'
    ORDER BY
        CASE
            WHEN UPPER(code) IN ('MONTHLY', 'MONTH', 'THANG', 'MONTHLY_PASS') THEN 0
            WHEN duration_days >= 28 THEN 1
            ELSE 2
        END,
        duration_days NULLS LAST,
        code
    LIMIT 1;

    IF monthly_ticket_type_id IS NULL THEN
        monthly_ticket_type_id := '00000000-0000-0000-0000-0000000d0001';

        INSERT INTO catalog.ticket_types (
            ticket_type_id,
            code,
            name,
            description,
            duration_days,
            status,
            created_at,
            updated_at
        )
        VALUES (
            monthly_ticket_type_id,
            'MONTHLY_SEED',
            'Monthly seed ticket',
            'Seed monthly ticket type for registered card test data.',
            30,
            'ACTIVE',
            now(),
            now()
        );
    END IF;

    FOR seed_record IN
        SELECT *
        FROM (
            VALUES
                (
                    '00000000-0000-0000-0000-000000020002'::UUID,
                    '00000000-0000-0000-0000-000000030002'::UUID,
                    '00000000-0000-0000-0000-000000040002'::UUID,
                    '00000000-0000-0000-0000-000000050002'::UUID,
                    '00000000-0000-0000-0000-000000060002'::UUID,
                    'C002',
                    'RFID-REGISTERED-002',
                    'Seed Registered Customer 002',
                    'CUS-REGISTERED-002',
                    '51H-888.02',
                    'Toyota',
                    'White',
                    'CAR'
                ),
                (
                    '00000000-0000-0000-0000-000000020003'::UUID,
                    '00000000-0000-0000-0000-000000030003'::UUID,
                    '00000000-0000-0000-0000-000000040003'::UUID,
                    '00000000-0000-0000-0000-000000050003'::UUID,
                    '00000000-0000-0000-0000-000000060003'::UUID,
                    'C003',
                    'RFID-REGISTERED-003',
                    'Seed Registered Customer 003',
                    'CUS-REGISTERED-003',
                    '59A-123.03',
                    'Honda',
                    'Black',
                    'CAR'
                ),
                (
                    '00000000-0000-0000-0000-000000020004'::UUID,
                    '00000000-0000-0000-0000-000000030004'::UUID,
                    '00000000-0000-0000-0000-000000040004'::UUID,
                    '00000000-0000-0000-0000-000000050004'::UUID,
                    '00000000-0000-0000-0000-000000060004'::UUID,
                    'C004',
                    'RFID-REGISTERED-004',
                    'Seed Registered Customer 004',
                    'CUS-REGISTERED-004',
                    '60B-456.04',
                    'Yamaha',
                    'Blue',
                    'MOTORBIKE'
                )
        ) AS seed(
            profile_id,
            customer_id,
            vehicle_id,
            subscription_id,
            card_id,
            card_number,
            uid,
            full_name,
            customer_code,
            license_plate,
            brand,
            color,
            preferred_vehicle_code
        )
    LOOP
        SELECT vehicle_type_id
        INTO seed_vehicle_type_id
        FROM catalog.vehicle_types
        WHERE is_active IS TRUE
        ORDER BY
            CASE
                WHEN UPPER(code) = UPPER(seed_record.preferred_vehicle_code) THEN 0
                WHEN seed_record.preferred_vehicle_code = 'CAR'
                    AND (
                        UPPER(code) IN ('CAR', 'AUTO', 'OTO', 'OTOCON')
                        OR LOWER(name) LIKE '%car%'
                        OR LOWER(name) LIKE '%oto%'
                    )
                    THEN 1
                WHEN seed_record.preferred_vehicle_code = 'MOTORBIKE'
                    AND (
                        UPPER(code) IN ('MOTORBIKE', 'MOTOR', 'BIKE', 'XE_MAY')
                        OR LOWER(name) LIKE '%motor%'
                        OR LOWER(name) LIKE '%bike%'
                    )
                    THEN 1
                ELSE 2
            END,
            code
        LIMIT 1;

        IF seed_vehicle_type_id IS NULL THEN
            seed_vehicle_type_id := '00000000-0000-0000-0000-0000000e0001';

            INSERT INTO catalog.vehicle_types (
                vehicle_type_id,
                code,
                name,
                description,
                is_active,
                created_at,
                updated_at
            )
            VALUES (
                seed_vehicle_type_id,
                'CAR_SEED',
                'Seed car',
                'Seed vehicle type for registered card test data.',
                TRUE,
                now(),
                now()
            )
            ON CONFLICT (code) DO UPDATE
            SET is_active = TRUE,
                updated_at = now();

            SELECT vehicle_type_id
            INTO seed_vehicle_type_id
            FROM catalog.vehicle_types
            WHERE code = 'CAR_SEED'
            LIMIT 1;
        END IF;

        SELECT card_id
        INTO seed_card_id
        FROM access_control.cards
        WHERE uid = seed_record.uid
           OR card_number = seed_record.card_number
        ORDER BY CASE WHEN uid = seed_record.uid THEN 0 ELSE 1 END
        LIMIT 1;

        IF seed_card_id IS NULL THEN
            seed_card_id := seed_record.card_id;

            INSERT INTO access_control.cards (
                card_id,
                card_number,
                uid,
                card_type_id,
                status,
                issued_at,
                created_at,
                updated_at
            )
            VALUES (
                seed_card_id,
                seed_record.card_number,
                seed_record.uid,
                registered_card_type_id,
                'ASSIGNED',
                now(),
                now(),
                now()
            );
        ELSE
            UPDATE access_control.cards
            SET card_type_id = registered_card_type_id,
                status = 'ASSIGNED',
                issued_at = COALESCE(issued_at, now()),
                updated_at = now()
            WHERE card_id = seed_card_id;
        END IF;

        INSERT INTO people.user_profiles (
            user_profile_id,
            full_name,
            status,
            created_at,
            updated_at
        )
        VALUES (
            seed_record.profile_id,
            seed_record.full_name,
            'ACTIVE',
            now(),
            now()
        )
        ON CONFLICT (user_profile_id) DO UPDATE
        SET full_name = EXCLUDED.full_name,
            status = 'ACTIVE',
            updated_at = now();

        SELECT customer_id
        INTO seed_customer_id
        FROM people.customers
        WHERE customer_code = seed_record.customer_code
           OR user_profile_id = seed_record.profile_id
        LIMIT 1;

        IF seed_customer_id IS NULL THEN
            seed_customer_id := seed_record.customer_id;

            INSERT INTO people.customers (
                customer_id,
                user_profile_id,
                customer_code,
                customer_type,
                status,
                approval_status,
                approved_at,
                created_at,
                updated_at
            )
            VALUES (
                seed_customer_id,
                seed_record.profile_id,
                seed_record.customer_code,
                'REGISTERED',
                'ACTIVE',
                'APPROVED',
                now(),
                now(),
                now()
            );
        ELSE
            UPDATE people.customers
            SET user_profile_id = seed_record.profile_id,
                customer_type = 'REGISTERED',
                status = 'ACTIVE',
                approval_status = 'APPROVED',
                approved_at = COALESCE(approved_at, now()),
                updated_at = now()
            WHERE customer_id = seed_customer_id;
        END IF;

        SELECT customer_vehicle_id
        INTO seed_vehicle_id
        FROM people.customer_vehicles
        WHERE license_plate = seed_record.license_plate
           OR customer_vehicle_id = seed_record.vehicle_id
        LIMIT 1;

        IF seed_vehicle_id IS NULL THEN
            seed_vehicle_id := seed_record.vehicle_id;

            INSERT INTO people.customer_vehicles (
                customer_vehicle_id,
                customer_id,
                vehicle_type_id,
                license_plate,
                brand,
                color,
                is_default,
                status,
                created_at,
                updated_at
            )
            VALUES (
                seed_vehicle_id,
                seed_customer_id,
                seed_vehicle_type_id,
                seed_record.license_plate,
                seed_record.brand,
                seed_record.color,
                TRUE,
                'ACTIVE',
                now(),
                now()
            );
        ELSE
            UPDATE people.customer_vehicles
            SET customer_id = seed_customer_id,
                vehicle_type_id = seed_vehicle_type_id,
                brand = seed_record.brand,
                color = seed_record.color,
                is_default = TRUE,
                status = 'ACTIVE',
                updated_at = now()
            WHERE customer_vehicle_id = seed_vehicle_id;
        END IF;

        SELECT price_rule_id,
               base_price
        INTO seed_price_rule_id,
             seed_price
        FROM catalog.price_rules
        WHERE vehicle_type_id = seed_vehicle_type_id
          AND ticket_type_id = monthly_ticket_type_id
          AND is_active IS TRUE
        ORDER BY priority, base_price
        LIMIT 1;

        seed_price := COALESCE(seed_price, 0);

        SELECT subscription_id
        INTO seed_subscription_id
        FROM access_control.subscriptions
        WHERE card_id = seed_card_id
          AND status = 'ACTIVE'
          AND CURRENT_DATE BETWEEN effective_from AND effective_to
        LIMIT 1;

        IF seed_subscription_id IS NULL THEN
            SELECT subscription_id
            INTO seed_subscription_id
            FROM access_control.subscriptions
            WHERE subscription_id = seed_record.subscription_id
            LIMIT 1;
        END IF;

        IF seed_subscription_id IS NULL THEN
            seed_subscription_id := seed_record.subscription_id;

            INSERT INTO access_control.subscriptions (
                subscription_id,
                customer_id,
                customer_vehicle_id,
                card_id,
                ticket_type_id,
                price_rule_id,
                requested_effective_from,
                effective_from,
                effective_to,
                price,
                status,
                approved_at,
                card_receipt_date,
                created_at,
                updated_at
            )
            VALUES (
                seed_subscription_id,
                seed_customer_id,
                seed_vehicle_id,
                seed_card_id,
                monthly_ticket_type_id,
                seed_price_rule_id,
                CURRENT_DATE,
                CURRENT_DATE - 1,
                CURRENT_DATE + 365,
                seed_price,
                'ACTIVE',
                now(),
                CURRENT_DATE,
                now(),
                now()
            );
        ELSE
            UPDATE access_control.subscriptions
            SET customer_id = seed_customer_id,
                customer_vehicle_id = seed_vehicle_id,
                card_id = seed_card_id,
                ticket_type_id = monthly_ticket_type_id,
                price_rule_id = seed_price_rule_id,
                requested_effective_from = CURRENT_DATE,
                effective_from = CURRENT_DATE - 1,
                effective_to = CURRENT_DATE + 365,
                price = seed_price,
                status = 'ACTIVE',
                approved_at = COALESCE(approved_at, now()),
                card_receipt_date = COALESCE(card_receipt_date, CURRENT_DATE),
                updated_at = now()
            WHERE subscription_id = seed_subscription_id;
        END IF;
    END LOOP;
END $$;

COMMIT;
