BEGIN;

CREATE SEQUENCE IF NOT EXISTS access_control.registered_card_number_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1;

CREATE SEQUENCE IF NOT EXISTS access_control.visitor_card_number_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1;

-- Giữ nguyên mã thẻ cũ và tiếp tục tăng từ số lớn nhất của từng dãy.
SELECT setval(
    'access_control.registered_card_number_seq',
    COALESCE(
        (
            SELECT MAX((substring(upper(card_number) from '^R([0-9]+)$'))::BIGINT)
            FROM access_control.cards
        ),
        0
    ) + 1,
    FALSE
);

SELECT setval(
    'access_control.visitor_card_number_seq',
    COALESCE(
        (
            SELECT MAX((substring(upper(card_number) from '^V([0-9]+)$'))::BIGINT)
            FROM access_control.cards
        ),
        0
    ) + 1,
    FALSE
);

COMMIT;
