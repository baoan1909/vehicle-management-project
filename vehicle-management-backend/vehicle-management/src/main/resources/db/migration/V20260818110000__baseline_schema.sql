-- Consolidated schema baseline generated from the verified PostgreSQL 17 migration result.
-- Contains schema objects only; canonical reference data is seeded by later migrations.
--
-- PostgreSQL database dump
--

-- Dumped from database version 17.11
-- Dumped by pg_dump version 17.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: access_control; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA access_control;

--
-- Name: audit; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA audit;

--
-- Name: billing; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA billing;

--
-- Name: catalog; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA catalog;

--
-- Name: hardware; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA hardware;

--
-- Name: iam; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA iam;

--
-- Name: notification; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA notification;

--
-- Name: operations; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA operations;

--
-- Name: parking; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA parking;

--
-- Name: people; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA people;

--
-- Name: citext; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;

--
-- Name: EXTENSION citext; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION citext IS 'data type for case-insensitive character strings';

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';

--
-- Name: set_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: cards; Type: TABLE; Schema: access_control; Owner: -
--

CREATE TABLE access_control.cards (
    card_id uuid DEFAULT gen_random_uuid() NOT NULL,
    card_number character varying(50) NOT NULL,
    uid character varying(100) NOT NULL,
    card_type_id uuid NOT NULL,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    issued_at timestamp with time zone,
    blocked_at timestamp with time zone,
    blocked_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    status_before_blocked character varying(20),
    blocked_by uuid,
    retired_at timestamp with time zone,
    retired_by uuid,
    retired_reason character varying(500),
    recovered_at timestamp with time zone,
    recovered_by uuid,
    recovery_note character varying(500),
    CONSTRAINT ck_cards_status CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'RESERVED'::character varying, 'ASSIGNED'::character varying, 'IN_USE'::character varying, 'LOST'::character varying, 'BLOCKED'::character varying, 'RETIRED'::character varying])::text[])))
);

--
-- Name: lost_card_reports; Type: TABLE; Schema: access_control; Owner: -
--

CREATE TABLE access_control.lost_card_reports (
    lost_card_report_id uuid DEFAULT gen_random_uuid() NOT NULL,
    card_id uuid NOT NULL,
    customer_id uuid,
    parking_session_id uuid,
    notification_time timestamp with time zone NOT NULL,
    time_of_lost timestamp with time zone NOT NULL,
    ticket_price numeric(12,2) DEFAULT 0 NOT NULL,
    lost_card_fee numeric(12,2) DEFAULT 0 NOT NULL,
    reporter_name character varying(150),
    reporter_phone character varying(20),
    identify_card character varying(20),
    registration_license character varying(50),
    note text,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    resolved_by uuid,
    resolved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    subscription_id uuid,
    context character varying(50) NOT NULL,
    cancelled_at timestamp with time zone,
    cancelled_by uuid,
    cancel_reason character varying(500),
    CONSTRAINT ck_lost_card_reports_context CHECK (((context)::text = ANY ((ARRAY['VISITOR_IN_PARKING'::character varying, 'REGISTERED_IN_PARKING'::character varying, 'REGISTERED_OUTSIDE'::character varying])::text[]))),
    CONSTRAINT ck_lost_card_reports_status CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'RESOLVED'::character varying, 'CANCELLED'::character varying])::text[])))
);

--
-- Name: registered_card_number_seq; Type: SEQUENCE; Schema: access_control; Owner: -
--

CREATE SEQUENCE access_control.registered_card_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: subscriptions; Type: TABLE; Schema: access_control; Owner: -
--

CREATE TABLE access_control.subscriptions (
    subscription_id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid NOT NULL,
    customer_vehicle_id uuid NOT NULL,
    card_id uuid,
    ticket_type_id uuid NOT NULL,
    price_rule_id uuid,
    effective_from date NOT NULL,
    effective_to date NOT NULL,
    price numeric(12,2) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    approved_by uuid,
    approved_at timestamp with time zone,
    card_receipt_date date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    requested_effective_from date NOT NULL,
    rejection_reason character varying(500),
    rejected_by uuid,
    rejected_at timestamp with time zone,
    CONSTRAINT ck_subscriptions_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PENDING_PAYMENT'::character varying, 'PENDING_CARD'::character varying, 'ACTIVE'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying, 'REJECTED'::character varying])::text[])))
);

--
-- Name: visitor_card_number_seq; Type: SEQUENCE; Schema: access_control; Owner: -
--

CREATE SEQUENCE access_control.visitor_card_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: audit_logs; Type: TABLE; Schema: audit; Owner: -
--

CREATE TABLE audit.audit_logs (
    audit_log_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_account_id uuid,
    action character varying(100) NOT NULL,
    target_schema character varying(50),
    target_table character varying(80),
    target_id uuid,
    old_data jsonb,
    new_data jsonb,
    ip_address character varying(50),
    user_agent text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid
);

--
-- Name: invoices; Type: TABLE; Schema: billing; Owner: -
--

CREATE TABLE billing.invoices (
    invoice_id uuid DEFAULT gen_random_uuid() NOT NULL,
    invoice_no character varying(50) NOT NULL,
    customer_id uuid,
    parking_session_id uuid,
    subscription_id uuid,
    lost_card_report_id uuid,
    amount numeric(12,2) NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0 NOT NULL,
    final_amount numeric(12,2) NOT NULL,
    status character varying(20) DEFAULT 'UNPAID'::character varying NOT NULL,
    issued_at timestamp with time zone DEFAULT now() NOT NULL,
    paid_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_invoices_status CHECK (((status)::text = ANY ((ARRAY['UNPAID'::character varying, 'PAID'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying])::text[])))
);

--
-- Name: payments; Type: TABLE; Schema: billing; Owner: -
--

CREATE TABLE billing.payments (
    payment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    invoice_id uuid NOT NULL,
    payment_method character varying(30) NOT NULL,
    amount numeric(12,2) NOT NULL,
    transaction_ref character varying(100),
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    paid_at timestamp with time zone,
    received_by uuid,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone,
    expires_at timestamp with time zone,
    provider_transaction_no character varying(100),
    provider_response_code character varying(20),
    provider_transaction_status character varying(20),
    bank_code character varying(20),
    card_type character varying(30),
    failure_reason character varying(255),
    CONSTRAINT ck_payments_method CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'QR'::character varying, 'BANK_TRANSFER'::character varying, 'MOMO'::character varying, 'VNPAY'::character varying])::text[]))),
    CONSTRAINT ck_payments_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);

--
-- Name: card_types; Type: TABLE; Schema: catalog; Owner: -
--

CREATE TABLE catalog.card_types (
    card_type_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    is_return_required boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    is_active boolean DEFAULT true NOT NULL
);

--
-- Name: holiday_calendar; Type: TABLE; Schema: catalog; Owner: -
--

CREATE TABLE catalog.holiday_calendar (
    holiday_id uuid DEFAULT gen_random_uuid() NOT NULL,
    holiday_date date NOT NULL,
    name character varying(150) NOT NULL,
    price_multiplier numeric(5,2) DEFAULT 1 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid
);

--
-- Name: price_plans; Type: TABLE; Schema: catalog; Owner: -
--

CREATE TABLE catalog.price_plans (
    price_plan_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    applies_to character varying(20) NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_price_plans_applies_to CHECK (((applies_to)::text = ANY ((ARRAY['VISITOR'::character varying, 'CUSTOMER'::character varying, 'ALL'::character varying])::text[])))
);

--
-- Name: price_rules; Type: TABLE; Schema: catalog; Owner: -
--

CREATE TABLE catalog.price_rules (
    price_rule_id uuid DEFAULT gen_random_uuid() NOT NULL,
    price_plan_id uuid NOT NULL,
    vehicle_type_id uuid NOT NULL,
    ticket_type_id uuid,
    rule_name character varying(150) NOT NULL,
    time_from time without time zone,
    time_to time without time zone,
    base_price numeric(12,2) NOT NULL,
    unit character varying(30) DEFAULT 'TURN'::character varying NOT NULL,
    lost_card_fee numeric(12,2) DEFAULT 0 NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT ck_price_rules_unit CHECK (((unit)::text = ANY ((ARRAY['TURN'::character varying, 'DAY'::character varying, 'MONTH'::character varying])::text[])))
);

--
-- Name: ticket_types; Type: TABLE; Schema: catalog; Owner: -
--

CREATE TABLE catalog.ticket_types (
    ticket_type_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    duration_days integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT ck_ticket_types_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

--
-- Name: vehicle_types; Type: TABLE; Schema: catalog; Owner: -
--

CREATE TABLE catalog.vehicle_types (
    vehicle_type_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid
);

--
-- Name: devices; Type: TABLE; Schema: hardware; Owner: -
--

CREATE TABLE hardware.devices (
    device_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parking_lot_id uuid NOT NULL,
    lane_id uuid,
    device_code character varying(50) NOT NULL,
    device_type character varying(30) NOT NULL,
    name character varying(150) NOT NULL,
    ip_address character varying(50),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    config jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_devices_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'OFFLINE'::character varying, 'MAINTENANCE'::character varying, 'RETIRED'::character varying])::text[]))),
    CONSTRAINT ck_devices_type CHECK (((device_type)::text = ANY ((ARRAY['CAMERA'::character varying, 'KIOSK'::character varying, 'CARD_READER'::character varying, 'BARRIER'::character varying])::text[])))
);

--
-- Name: account_status_history; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.account_status_history (
    account_status_history_id uuid DEFAULT gen_random_uuid() NOT NULL,
    account_id uuid NOT NULL,
    old_status character varying(20),
    new_status character varying(20) NOT NULL,
    reason text,
    changed_at timestamp with time zone DEFAULT now() NOT NULL,
    changed_by uuid,
    CONSTRAINT ck_account_status_history_new_status CHECK (((new_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'LOCKED'::character varying, 'DISABLED'::character varying, 'PENDING'::character varying])::text[])))
);

--
-- Name: accounts; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.accounts (
    account_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_profile_id uuid,
    username character varying(100) NOT NULL,
    email public.citext NOT NULL,
    role_id uuid NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    last_login_at timestamp with time zone,
    failed_login_count integer DEFAULT 0 NOT NULL,
    locked_until timestamp with time zone,
    password_changed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    keycloak_user_id character varying(255),
    CONSTRAINT ck_accounts_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'LOCKED'::character varying, 'DISABLED'::character varying, 'PENDING'::character varying])::text[])))
);

--
-- Name: permission_actions; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.permission_actions (
    action_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_permission_actions_code_upper CHECK (((code)::text = upper((code)::text)))
);

--
-- Name: permission_modules; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.permission_modules (
    module_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_permission_modules_code_upper CHECK (((code)::text = upper((code)::text)))
);

--
-- Name: permission_scopes; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.permission_scopes (
    scope_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_permission_scopes_code_upper CHECK (((code)::text = upper((code)::text)))
);

--
-- Name: permissions; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.permissions (
    permission_id uuid DEFAULT gen_random_uuid() NOT NULL,
    permission_code character varying(100) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    module_id uuid NOT NULL,
    action_id uuid NOT NULL,
    scope_id uuid NOT NULL,
    CONSTRAINT ck_permissions_code_upper CHECK (((permission_code)::text = upper((permission_code)::text)))
);

--
-- Name: role_permissions; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.role_permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    is_active boolean DEFAULT true NOT NULL,
    is_system boolean DEFAULT false NOT NULL
);

--
-- Name: roles; Type: TABLE; Schema: iam; Owner: -
--

CREATE TABLE iam.roles (
    role_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    is_system boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_roles_code_upper CHECK (((code)::text = upper((code)::text)))
);

--
-- Name: broadcast_announcements; Type: TABLE; Schema: notification; Owner: -
--

CREATE TABLE notification.broadcast_announcements (
    broadcast_id uuid DEFAULT gen_random_uuid() NOT NULL,
    notification_type character varying(80) NOT NULL,
    title character varying(200) NOT NULL,
    message text NOT NULL,
    audience_type character varying(30) NOT NULL,
    role_codes jsonb,
    start_at timestamp with time zone NOT NULL,
    end_at timestamp with time zone,
    enabled boolean DEFAULT true NOT NULL,
    redirect_url character varying(1000),
    status character varying(30) NOT NULL,
    published_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    related_schema character varying(50),
    related_table character varying(80),
    related_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    display_order integer DEFAULT 100 NOT NULL,
    CONSTRAINT ck_broadcast_announcements_audience_type CHECK (((audience_type)::text = ANY ((ARRAY['ALL_ACTIVE_ACCOUNTS'::character varying, 'ROLE_CODES'::character varying])::text[]))),
    CONSTRAINT ck_broadcast_announcements_display_order CHECK ((display_order >= 1)),
    CONSTRAINT ck_broadcast_announcements_period CHECK (((end_at IS NULL) OR (end_at >= start_at))),
    CONSTRAINT ck_broadcast_announcements_role_audience CHECK ((((audience_type)::text <> 'ROLE_CODES'::text) OR
CASE
    WHEN (jsonb_typeof(role_codes) = 'array'::text) THEN (jsonb_array_length(role_codes) > 0)
    ELSE false
END)),
    CONSTRAINT ck_broadcast_announcements_role_codes_json CHECK (((role_codes IS NULL) OR (jsonb_typeof(role_codes) = 'array'::text))),
    CONSTRAINT ck_broadcast_announcements_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT ck_broadcast_announcements_type CHECK (((notification_type)::text = ANY ((ARRAY['SYSTEM_NOTICE'::character varying, 'SUBSCRIPTION_REQUESTED'::character varying, 'SUBSCRIPTION_APPROVED'::character varying, 'SUBSCRIPTION_REJECTED'::character varying, 'SUBSCRIPTION_EXPIRING_SOON'::character varying, 'SUBSCRIPTION_EXPIRED'::character varying, 'SUBSCRIPTION_CANCELLED'::character varying, 'SUBSCRIPTION_PAYMENT_COMPLETED'::character varying, 'INVOICE_CREATED'::character varying, 'PAYMENT_SUCCEEDED'::character varying, 'PAYMENT_FAILED'::character varying, 'SUPPORT_TICKET_CREATED'::character varying, 'SUPPORT_TICKET_ASSIGNED'::character varying, 'SUPPORT_TICKET_IN_PROGRESS'::character varying, 'SUPPORT_TICKET_RESPONDED'::character varying, 'SUPPORT_TICKET_REOPENED'::character varying, 'SUPPORT_TICKET_CLOSED'::character varying, 'SHIFT_ASSIGNED'::character varying, 'SHIFT_CHANGED'::character varying, 'SHIFT_CANCELLED'::character varying, 'DEVICE_OFFLINE'::character varying, 'DEVICE_MAINTENANCE'::character varying, 'LANE_MAINTENANCE'::character varying, 'PARKING_LOT_MAINTENANCE'::character varying, 'PRICE_PLAN_CHANGED'::character varying, 'PRICE_RULE_CHANGED'::character varying, 'TICKET_TYPE_CHANGED'::character varying, 'ACCOUNT_REGISTERED'::character varying, 'ACCOUNT_PROVISIONED'::character varying, 'ACCOUNT_STATUS_CHANGED'::character varying, 'ACCOUNT_PROFILE_SUBMITTED'::character varying, 'CUSTOMER_ONBOARDING_APPROVED'::character varying, 'CUSTOMER_ONBOARDING_REJECTED'::character varying, 'CUSTOMER_ONBOARDING_RESUBMITTED'::character varying, 'INTERNAL_EMPLOYEE_APPROVED'::character varying, 'INTERNAL_EMPLOYEE_REJECTED'::character varying, 'INTERNAL_EMPLOYEE_RESUBMITTED'::character varying, 'SYSTEM_ADMIN_APPROVED'::character varying, 'SYSTEM_ADMIN_REJECTED'::character varying, 'SYSTEM_ADMIN_RESUBMITTED'::character varying])::text[])))
);

--
-- Name: notifications; Type: TABLE; Schema: notification; Owner: -
--

CREATE TABLE notification.notifications (
    notification_id uuid DEFAULT gen_random_uuid() NOT NULL,
    account_id uuid,
    channel character varying(20) NOT NULL,
    title character varying(200) NOT NULL,
    message text NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    sent_at timestamp with time zone,
    read_at timestamp with time zone,
    related_schema character varying(50),
    related_table character varying(80),
    related_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    realtime_delivered_at timestamp with time zone,
    notification_type character varying(80) DEFAULT 'SYSTEM_NOTICE'::character varying NOT NULL,
    broadcast_id uuid,
    redirect_url character varying(1000),
    CONSTRAINT ck_notifications_channel CHECK (((channel)::text = ANY ((ARRAY['WEB'::character varying, 'EMAIL'::character varying, 'PUSH'::character varying, 'SMS'::character varying])::text[]))),
    CONSTRAINT ck_notifications_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'READ'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT ck_notifications_type CHECK (((notification_type)::text = ANY ((ARRAY['SYSTEM_NOTICE'::character varying, 'SUBSCRIPTION_REQUESTED'::character varying, 'SUBSCRIPTION_APPROVED'::character varying, 'SUBSCRIPTION_REJECTED'::character varying, 'SUBSCRIPTION_EXPIRING_SOON'::character varying, 'SUBSCRIPTION_EXPIRED'::character varying, 'SUBSCRIPTION_CANCELLED'::character varying, 'SUBSCRIPTION_PAYMENT_COMPLETED'::character varying, 'INVOICE_CREATED'::character varying, 'PAYMENT_SUCCEEDED'::character varying, 'PAYMENT_FAILED'::character varying, 'SUPPORT_TICKET_CREATED'::character varying, 'SUPPORT_TICKET_ASSIGNED'::character varying, 'SUPPORT_TICKET_IN_PROGRESS'::character varying, 'SUPPORT_TICKET_RESPONDED'::character varying, 'SUPPORT_TICKET_REOPENED'::character varying, 'SUPPORT_TICKET_CLOSED'::character varying, 'SHIFT_ASSIGNED'::character varying, 'SHIFT_CHANGED'::character varying, 'SHIFT_CANCELLED'::character varying, 'DEVICE_OFFLINE'::character varying, 'DEVICE_MAINTENANCE'::character varying, 'LANE_MAINTENANCE'::character varying, 'PARKING_LOT_MAINTENANCE'::character varying, 'PRICE_PLAN_CHANGED'::character varying, 'PRICE_RULE_CHANGED'::character varying, 'TICKET_TYPE_CHANGED'::character varying, 'ACCOUNT_REGISTERED'::character varying, 'ACCOUNT_PROVISIONED'::character varying, 'ACCOUNT_STATUS_CHANGED'::character varying, 'ACCOUNT_PROFILE_SUBMITTED'::character varying, 'CUSTOMER_ONBOARDING_APPROVED'::character varying, 'CUSTOMER_ONBOARDING_REJECTED'::character varying, 'CUSTOMER_ONBOARDING_RESUBMITTED'::character varying, 'INTERNAL_EMPLOYEE_APPROVED'::character varying, 'INTERNAL_EMPLOYEE_REJECTED'::character varying, 'INTERNAL_EMPLOYEE_RESUBMITTED'::character varying, 'SYSTEM_ADMIN_APPROVED'::character varying, 'SYSTEM_ADMIN_REJECTED'::character varying, 'SYSTEM_ADMIN_RESUBMITTED'::character varying])::text[])))
);

--
-- Name: approval_requests; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.approval_requests (
    approval_request_id uuid DEFAULT gen_random_uuid() NOT NULL,
    request_type character varying(50) NOT NULL,
    target_schema character varying(50) NOT NULL,
    target_table character varying(80) NOT NULL,
    target_id uuid NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    requested_by uuid,
    approved_by uuid,
    approved_at timestamp with time zone,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_approval_requests_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);

--
-- Name: chat_conversation_members; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.chat_conversation_members (
    conversation_member_id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    account_id uuid NOT NULL,
    member_role character varying(30) DEFAULT 'MEMBER'::character varying NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    last_read_message_id uuid,
    muted_until timestamp with time zone,
    joined_at timestamp with time zone DEFAULT now() NOT NULL,
    left_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_chat_members_role CHECK (((member_role)::text = ANY ((ARRAY['OWNER'::character varying, 'MEMBER'::character varying, 'ASSIGNEE'::character varying, 'OBSERVER'::character varying, 'CUSTOMER'::character varying])::text[]))),
    CONSTRAINT ck_chat_members_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'LEFT'::character varying, 'REMOVED'::character varying, 'BLOCKED'::character varying])::text[])))
);

--
-- Name: chat_conversations; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.chat_conversations (
    conversation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_type character varying(30) NOT NULL,
    title character varying(200),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    customer_id uuid,
    support_ticket_id uuid,
    owner_account_id uuid,
    assigned_to uuid,
    related_schema character varying(50),
    related_table character varying(80),
    related_id uuid,
    last_message_id uuid,
    last_message_at timestamp with time zone,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_chat_conversations_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'ARCHIVED'::character varying, 'CLOSED'::character varying])::text[]))),
    CONSTRAINT ck_chat_conversations_type CHECK (((conversation_type)::text = ANY ((ARRAY['INTERNAL_DIRECT'::character varying, 'INTERNAL_GROUP'::character varying, 'CUSTOMER_DIRECT'::character varying, 'SUPPORT_TICKET'::character varying, 'PARKING_SESSION'::character varying, 'BILLING'::character varying, 'LOST_CARD'::character varying, 'SYSTEM_DIRECT'::character varying])::text[])))
);

--
-- Name: chat_message_attachments; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.chat_message_attachments (
    attachment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    message_id uuid NOT NULL,
    bucket character varying(20) NOT NULL,
    object_key character varying(255) NOT NULL,
    original_filename character varying(255),
    content_type character varying(100),
    size_bytes bigint,
    checksum_sha256 character varying(64),
    attachment_type character varying(30) DEFAULT 'IMAGE'::character varying NOT NULL,
    width integer,
    height integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_chat_attachments_bucket CHECK (((bucket)::text = ANY ((ARRAY['PUBLIC'::character varying, 'PRIVATE'::character varying])::text[]))),
    CONSTRAINT ck_chat_attachments_size_non_negative CHECK (((size_bytes IS NULL) OR (size_bytes >= 0))),
    CONSTRAINT ck_chat_attachments_type CHECK (((attachment_type)::text = ANY ((ARRAY['IMAGE'::character varying, 'DOCUMENT'::character varying, 'AUDIO'::character varying, 'PARKING_EVIDENCE'::character varying, 'PAYMENT_PROOF'::character varying])::text[])))
);

--
-- Name: chat_messages; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.chat_messages (
    message_id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    sender_account_id uuid,
    message_type character varying(30) NOT NULL,
    content text,
    reply_to_message_id uuid,
    related_schema character varying(50),
    related_table character varying(80),
    related_id uuid,
    metadata jsonb,
    deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    edited_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_chat_messages_type CHECK (((message_type)::text = ANY ((ARRAY['TEXT'::character varying, 'IMAGE'::character varying, 'FILE'::character varying, 'SYSTEM'::character varying, 'CONTEXT_CARD'::character varying, 'ACTION_CARD'::character varying, 'SUPPORT_REQUEST'::character varying])::text[])))
);

--
-- Name: employee_roster_rules; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.employee_roster_rules (
    roster_rule_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parking_lot_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    preferred_shift_type character varying(20),
    preferred_gate_id uuid,
    weekly_day_off character varying(10) NOT NULL,
    assignment_mode character varying(20) NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_roster_rules_assignment_mode CHECK (((assignment_mode)::text = ANY ((ARRAY['FIXED'::character varying, 'RELIEF'::character varying])::text[]))),
    CONSTRAINT ck_roster_rules_effective_period CHECK (((effective_to IS NULL) OR (effective_to >= effective_from))),
    CONSTRAINT ck_roster_rules_mode_fields CHECK (((((assignment_mode)::text = 'FIXED'::text) AND (preferred_shift_type IS NOT NULL) AND (preferred_gate_id IS NOT NULL)) OR (((assignment_mode)::text = 'RELIEF'::text) AND (preferred_shift_type IS NULL) AND (preferred_gate_id IS NULL)))),
    CONSTRAINT ck_roster_rules_shift_type CHECK (((preferred_shift_type IS NULL) OR ((preferred_shift_type)::text = ANY ((ARRAY['MORNING'::character varying, 'AFTERNOON'::character varying, 'NIGHT'::character varying])::text[])))),
    CONSTRAINT ck_roster_rules_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT ck_roster_rules_weekly_day_off CHECK (((weekly_day_off)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[])))
);

--
-- Name: shift_assignments; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.shift_assignments (
    shift_assignment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    shift_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    gate_id uuid,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_shift_assignments_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SCHEDULED'::character varying, 'ACTIVE'::character varying, 'REMOVED'::character varying])::text[])))
);

--
-- Name: shift_templates; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.shift_templates (
    shift_template_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parking_lot_id uuid NOT NULL,
    shift_type character varying(20) NOT NULL,
    name character varying(100) NOT NULL,
    start_local_time time without time zone NOT NULL,
    end_local_time time without time zone NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_shift_templates_duration CHECK ((
CASE
    WHEN (end_local_time > start_local_time) THEN (end_local_time - start_local_time)
    ELSE ((end_local_time - start_local_time) + '24:00:00'::interval)
END = '08:00:00'::interval)),
    CONSTRAINT ck_shift_templates_name_not_blank CHECK ((btrim((name)::text) <> ''::text)),
    CONSTRAINT ck_shift_templates_shift_type CHECK (((shift_type)::text = ANY ((ARRAY['MORNING'::character varying, 'AFTERNOON'::character varying, 'NIGHT'::character varying])::text[]))),
    CONSTRAINT ck_shift_templates_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT ck_shift_templates_time_not_equal CHECK ((start_local_time <> end_local_time))
);

--
-- Name: shifts; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.shifts (
    shift_id uuid DEFAULT gen_random_uuid() NOT NULL,
    shift_code character varying(50) NOT NULL,
    parking_lot_id uuid NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    opening_cash numeric(12,2),
    closing_cash numeric(12,2),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    shift_template_id uuid,
    shift_date date NOT NULL,
    shift_type character varying(20) NOT NULL,
    approved_at timestamp with time zone,
    approved_by uuid,
    opened_at timestamp with time zone,
    opened_by uuid,
    closed_at timestamp with time zone,
    closed_by uuid,
    cancelled_at timestamp with time zone,
    cancelled_by uuid,
    cancellation_reason text,
    note text,
    CONSTRAINT ck_shifts_closing_cash CHECK (((closing_cash IS NULL) OR (closing_cash >= (0)::numeric))),
    CONSTRAINT ck_shifts_opening_cash CHECK (((opening_cash IS NULL) OR (opening_cash >= (0)::numeric))),
    CONSTRAINT ck_shifts_shift_type CHECK (((shift_type)::text = ANY ((ARRAY['MORNING'::character varying, 'AFTERNOON'::character varying, 'NIGHT'::character varying])::text[]))),
    CONSTRAINT ck_shifts_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SCHEDULED'::character varying, 'OPEN'::character varying, 'CLOSED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT ck_shifts_time_order CHECK ((end_time > start_time))
);

--
-- Name: support_ticket_categories; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.support_ticket_categories (
    category_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    priority character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_support_ticket_categories_priority CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'NORMAL'::character varying, 'HIGH'::character varying, 'URGENT'::character varying])::text[]))),
    CONSTRAINT ck_support_ticket_categories_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

--
-- Name: support_tickets; Type: TABLE; Schema: operations; Owner: -
--

CREATE TABLE operations.support_tickets (
    support_ticket_id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid,
    title character varying(200) NOT NULL,
    content text NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    assigned_to uuid,
    resolved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    category_id uuid NOT NULL,
    resolution_note text,
    closed_at timestamp with time zone,
    closed_by uuid,
    reopen_count integer DEFAULT 0 NOT NULL,
    last_reopened_at timestamp with time zone,
    CONSTRAINT ck_support_tickets_reopen_count_non_negative CHECK ((reopen_count >= 0)),
    CONSTRAINT ck_support_tickets_status CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying, 'CLOSED'::character varying])::text[])))
);

--
-- Name: gates; Type: TABLE; Schema: parking; Owner: -
--

CREATE TABLE parking.gates (
    gate_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    zone_id uuid NOT NULL,
    CONSTRAINT ck_gates_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'MAINTENANCE'::character varying, 'CLOSED'::character varying])::text[])))
);

--
-- Name: lanes; Type: TABLE; Schema: parking; Owner: -
--

CREATE TABLE parking.lanes (
    lane_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    direction character varying(10) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    gate_id uuid NOT NULL,
    CONSTRAINT ck_lanes_direction CHECK (((direction)::text = ANY ((ARRAY['IN'::character varying, 'OUT'::character varying])::text[]))),
    CONSTRAINT ck_lanes_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'MAINTENANCE'::character varying, 'CLOSED'::character varying])::text[])))
);

--
-- Name: parking_events; Type: TABLE; Schema: parking; Owner: -
--

CREATE TABLE parking.parking_events (
    parking_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parking_session_id uuid NOT NULL,
    lane_id uuid NOT NULL,
    event_type character varying(20) NOT NULL,
    event_time timestamp with time zone NOT NULL,
    license_plate_detected character varying(20),
    actor_account_id uuid,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    license_plate_image_path character varying(255),
    person_image_path character varying(255),
    CONSTRAINT ck_parking_events_type CHECK (((event_type)::text = ANY ((ARRAY['CHECK_IN'::character varying, 'CHECK_OUT_PENDING'::character varying, 'CHECK_OUT'::character varying, 'MANUAL_REVIEW'::character varying, 'BARRIER_OPEN'::character varying])::text[])))
);

--
-- Name: parking_lots; Type: TABLE; Schema: parking; Owner: -
--

CREATE TABLE parking.parking_lots (
    parking_lot_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    address text,
    total_capacity integer DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_parking_lots_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'MAINTENANCE'::character varying, 'CLOSED'::character varying])::text[]))),
    CONSTRAINT ck_parking_lots_total_capacity_non_negative CHECK ((total_capacity >= 0))
);

--
-- Name: parking_sessions; Type: TABLE; Schema: parking; Owner: -
--

CREATE TABLE parking.parking_sessions (
    parking_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    card_id uuid NOT NULL,
    customer_id uuid,
    customer_vehicle_id uuid,
    vehicle_type_id uuid NOT NULL,
    license_plate_in character varying(20) NOT NULL,
    license_plate_out character varying(20),
    check_in_time timestamp with time zone NOT NULL,
    check_out_time timestamp with time zone,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    total_price numeric(12,2),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    zone_id uuid,
    CONSTRAINT ck_parking_sessions_status CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'CLOSED'::character varying, 'LOST_CARD'::character varying, 'CANCELLED'::character varying])::text[])))
);

--
-- Name: zones; Type: TABLE; Schema: parking; Owner: -
--

CREATE TABLE parking.zones (
    zone_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parking_lot_id uuid NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    vehicle_type_id uuid,
    capacity integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT ck_zones_capacity_non_negative CHECK ((capacity >= 0)),
    CONSTRAINT ck_zones_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'MAINTENANCE'::character varying, 'CLOSED'::character varying])::text[])))
);

--
-- Name: customer_vehicles; Type: TABLE; Schema: people; Owner: -
--

CREATE TABLE people.customer_vehicles (
    customer_vehicle_id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid NOT NULL,
    vehicle_type_id uuid NOT NULL,
    license_plate character varying(20) NOT NULL,
    brand character varying(80),
    color character varying(50),
    is_default boolean DEFAULT false NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_customer_vehicles_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'BLOCKED'::character varying])::text[])))
);

--
-- Name: customers; Type: TABLE; Schema: people; Owner: -
--

CREATE TABLE people.customers (
    customer_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_profile_id uuid NOT NULL,
    customer_code character varying(50) NOT NULL,
    customer_type character varying(20) DEFAULT 'REGISTERED'::character varying NOT NULL,
    approval_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    approved_by uuid,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT ck_customers_approval_status CHECK (((approval_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'SUSPENDED'::character varying])::text[]))),
    CONSTRAINT ck_customers_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT ck_customers_type CHECK (((customer_type)::text = ANY ((ARRAY['REGISTERED'::character varying, 'VIP'::character varying])::text[])))
);

--
-- Name: employees; Type: TABLE; Schema: people; Owner: -
--

CREATE TABLE people.employees (
    employee_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_profile_id uuid NOT NULL,
    employee_code character varying(50) NOT NULL,
    job_title character varying(100),
    hired_at date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_employees_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);

--
-- Name: user_profile_avatars; Type: TABLE; Schema: people; Owner: -
--

CREATE TABLE people.user_profile_avatars (
    avatar_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_profile_id uuid NOT NULL,
    object_key character varying(255) NOT NULL,
    original_filename character varying(255),
    content_type character varying(100),
    size_bytes bigint,
    checksum_sha256 character varying(64),
    bucket character varying(20) NOT NULL,
    status character varying(30) NOT NULL,
    is_current boolean DEFAULT false NOT NULL,
    uploaded_by_account_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_user_profile_avatars_bucket CHECK (((bucket)::text = ANY ((ARRAY['PUBLIC'::character varying, 'PRIVATE'::character varying])::text[]))),
    CONSTRAINT ck_user_profile_avatars_current_active CHECK (((is_current = false) OR ((status)::text = 'ACTIVE'::text))),
    CONSTRAINT ck_user_profile_avatars_size_non_negative CHECK (((size_bytes IS NULL) OR (size_bytes >= 0))),
    CONSTRAINT ck_user_profile_avatars_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'REPLACED'::character varying, 'DELETED'::character varying])::text[])))
);

--
-- Name: user_profiles; Type: TABLE; Schema: people; Owner: -
--

CREATE TABLE people.user_profiles (
    user_profile_id uuid DEFAULT gen_random_uuid() NOT NULL,
    full_name character varying(150) NOT NULL,
    date_of_birth date,
    gender character varying(20),
    phone_number character varying(20),
    address text,
    identify_card character varying(20),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT ck_user_profiles_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);

--
-- Name: cards cards_card_number_key; Type: CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.cards
    ADD CONSTRAINT cards_card_number_key UNIQUE (card_number);

--
-- Name: cards cards_pkey; Type: CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.cards
    ADD CONSTRAINT cards_pkey PRIMARY KEY (card_id);

--
-- Name: cards cards_uid_key; Type: CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.cards
    ADD CONSTRAINT cards_uid_key UNIQUE (uid);

--
-- Name: lost_card_reports lost_card_reports_pkey; Type: CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.lost_card_reports
    ADD CONSTRAINT lost_card_reports_pkey PRIMARY KEY (lost_card_report_id);

--
-- Name: subscriptions subscriptions_pkey; Type: CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (subscription_id);

--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: audit; Owner: -
--

ALTER TABLE ONLY audit.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (audit_log_id);

--
-- Name: invoices invoices_invoice_no_key; Type: CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT invoices_invoice_no_key UNIQUE (invoice_no);

--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (invoice_id);

--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (payment_id);

--
-- Name: card_types card_types_code_key; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.card_types
    ADD CONSTRAINT card_types_code_key UNIQUE (code);

--
-- Name: card_types card_types_pkey; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.card_types
    ADD CONSTRAINT card_types_pkey PRIMARY KEY (card_type_id);

--
-- Name: holiday_calendar holiday_calendar_holiday_date_key; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.holiday_calendar
    ADD CONSTRAINT holiday_calendar_holiday_date_key UNIQUE (holiday_date);

--
-- Name: holiday_calendar holiday_calendar_pkey; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.holiday_calendar
    ADD CONSTRAINT holiday_calendar_pkey PRIMARY KEY (holiday_id);

--
-- Name: price_plans price_plans_code_key; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.price_plans
    ADD CONSTRAINT price_plans_code_key UNIQUE (code);

--
-- Name: price_plans price_plans_pkey; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.price_plans
    ADD CONSTRAINT price_plans_pkey PRIMARY KEY (price_plan_id);

--
-- Name: price_rules price_rules_pkey; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.price_rules
    ADD CONSTRAINT price_rules_pkey PRIMARY KEY (price_rule_id);

--
-- Name: ticket_types ticket_types_pkey; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.ticket_types
    ADD CONSTRAINT ticket_types_pkey PRIMARY KEY (ticket_type_id);

--
-- Name: vehicle_types vehicle_types_code_key; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.vehicle_types
    ADD CONSTRAINT vehicle_types_code_key UNIQUE (code);

--
-- Name: vehicle_types vehicle_types_pkey; Type: CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.vehicle_types
    ADD CONSTRAINT vehicle_types_pkey PRIMARY KEY (vehicle_type_id);

--
-- Name: devices devices_device_code_key; Type: CONSTRAINT; Schema: hardware; Owner: -
--

ALTER TABLE ONLY hardware.devices
    ADD CONSTRAINT devices_device_code_key UNIQUE (device_code);

--
-- Name: devices devices_pkey; Type: CONSTRAINT; Schema: hardware; Owner: -
--

ALTER TABLE ONLY hardware.devices
    ADD CONSTRAINT devices_pkey PRIMARY KEY (device_id);

--
-- Name: account_status_history account_status_history_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.account_status_history
    ADD CONSTRAINT account_status_history_pkey PRIMARY KEY (account_status_history_id);

--
-- Name: accounts accounts_email_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT accounts_email_key UNIQUE (email);

--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (account_id);

--
-- Name: accounts accounts_user_profile_id_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT accounts_user_profile_id_key UNIQUE (user_profile_id);

--
-- Name: accounts accounts_username_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT accounts_username_key UNIQUE (username);

--
-- Name: permission_actions permission_actions_code_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permission_actions
    ADD CONSTRAINT permission_actions_code_key UNIQUE (code);

--
-- Name: permission_actions permission_actions_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permission_actions
    ADD CONSTRAINT permission_actions_pkey PRIMARY KEY (action_id);

--
-- Name: permission_modules permission_modules_code_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permission_modules
    ADD CONSTRAINT permission_modules_code_key UNIQUE (code);

--
-- Name: permission_modules permission_modules_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permission_modules
    ADD CONSTRAINT permission_modules_pkey PRIMARY KEY (module_id);

--
-- Name: permission_scopes permission_scopes_code_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permission_scopes
    ADD CONSTRAINT permission_scopes_code_key UNIQUE (code);

--
-- Name: permission_scopes permission_scopes_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permission_scopes
    ADD CONSTRAINT permission_scopes_pkey PRIMARY KEY (scope_id);

--
-- Name: permissions permissions_permission_code_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT permissions_permission_code_key UNIQUE (permission_code);

--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (permission_id);

--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (id);

--
-- Name: roles roles_code_key; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.roles
    ADD CONSTRAINT roles_code_key UNIQUE (code);

--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);

--
-- Name: accounts uq_accounts_keycloak_user_id; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT uq_accounts_keycloak_user_id UNIQUE (keycloak_user_id);

--
-- Name: permissions uq_permissions_module_action_scope; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT uq_permissions_module_action_scope UNIQUE (module_id, action_id, scope_id);

--
-- Name: role_permissions uq_role_permissions; Type: CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.role_permissions
    ADD CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id);

--
-- Name: broadcast_announcements broadcast_announcements_pkey; Type: CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.broadcast_announcements
    ADD CONSTRAINT broadcast_announcements_pkey PRIMARY KEY (broadcast_id);

--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (notification_id);

--
-- Name: approval_requests approval_requests_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.approval_requests
    ADD CONSTRAINT approval_requests_pkey PRIMARY KEY (approval_request_id);

--
-- Name: chat_conversation_members chat_conversation_members_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversation_members
    ADD CONSTRAINT chat_conversation_members_pkey PRIMARY KEY (conversation_member_id);

--
-- Name: chat_conversations chat_conversations_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversations
    ADD CONSTRAINT chat_conversations_pkey PRIMARY KEY (conversation_id);

--
-- Name: chat_message_attachments chat_message_attachments_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_message_attachments
    ADD CONSTRAINT chat_message_attachments_pkey PRIMARY KEY (attachment_id);

--
-- Name: chat_messages chat_messages_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (message_id);

--
-- Name: employee_roster_rules employee_roster_rules_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.employee_roster_rules
    ADD CONSTRAINT employee_roster_rules_pkey PRIMARY KEY (roster_rule_id);

--
-- Name: shift_assignments shift_assignments_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_assignments
    ADD CONSTRAINT shift_assignments_pkey PRIMARY KEY (shift_assignment_id);

--
-- Name: shift_templates shift_templates_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_templates
    ADD CONSTRAINT shift_templates_pkey PRIMARY KEY (shift_template_id);

--
-- Name: shifts shifts_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT shifts_pkey PRIMARY KEY (shift_id);

--
-- Name: shifts shifts_shift_code_key; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT shifts_shift_code_key UNIQUE (shift_code);

--
-- Name: support_ticket_categories support_ticket_categories_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.support_ticket_categories
    ADD CONSTRAINT support_ticket_categories_pkey PRIMARY KEY (category_id);

--
-- Name: support_tickets support_tickets_pkey; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.support_tickets
    ADD CONSTRAINT support_tickets_pkey PRIMARY KEY (support_ticket_id);

--
-- Name: chat_conversation_members uq_chat_members_conversation_account; Type: CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversation_members
    ADD CONSTRAINT uq_chat_members_conversation_account UNIQUE (conversation_id, account_id);

--
-- Name: gates gates_pkey; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.gates
    ADD CONSTRAINT gates_pkey PRIMARY KEY (gate_id);

--
-- Name: lanes lanes_pkey; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.lanes
    ADD CONSTRAINT lanes_pkey PRIMARY KEY (lane_id);

--
-- Name: parking_events parking_events_pkey; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_events
    ADD CONSTRAINT parking_events_pkey PRIMARY KEY (parking_event_id);

--
-- Name: parking_lots parking_lots_code_key; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_lots
    ADD CONSTRAINT parking_lots_code_key UNIQUE (code);

--
-- Name: parking_lots parking_lots_pkey; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_lots
    ADD CONSTRAINT parking_lots_pkey PRIMARY KEY (parking_lot_id);

--
-- Name: parking_sessions parking_sessions_pkey; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_sessions
    ADD CONSTRAINT parking_sessions_pkey PRIMARY KEY (parking_session_id);

--
-- Name: gates uq_gates_zone_code; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.gates
    ADD CONSTRAINT uq_gates_zone_code UNIQUE (zone_id, code);

--
-- Name: lanes uq_lanes_gate_code; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.lanes
    ADD CONSTRAINT uq_lanes_gate_code UNIQUE (gate_id, code);

--
-- Name: zones uq_zones_lot_code; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.zones
    ADD CONSTRAINT uq_zones_lot_code UNIQUE (parking_lot_id, code);

--
-- Name: zones zones_pkey; Type: CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.zones
    ADD CONSTRAINT zones_pkey PRIMARY KEY (zone_id);

--
-- Name: customer_vehicles customer_vehicles_license_plate_key; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customer_vehicles
    ADD CONSTRAINT customer_vehicles_license_plate_key UNIQUE (license_plate);

--
-- Name: customer_vehicles customer_vehicles_pkey; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customer_vehicles
    ADD CONSTRAINT customer_vehicles_pkey PRIMARY KEY (customer_vehicle_id);

--
-- Name: customers customers_customer_code_key; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customers
    ADD CONSTRAINT customers_customer_code_key UNIQUE (customer_code);

--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (customer_id);

--
-- Name: customers customers_user_profile_id_key; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customers
    ADD CONSTRAINT customers_user_profile_id_key UNIQUE (user_profile_id);

--
-- Name: employees employees_employee_code_key; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.employees
    ADD CONSTRAINT employees_employee_code_key UNIQUE (employee_code);

--
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (employee_id);

--
-- Name: employees employees_user_profile_id_key; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.employees
    ADD CONSTRAINT employees_user_profile_id_key UNIQUE (user_profile_id);

--
-- Name: user_profile_avatars user_profile_avatars_pkey; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profile_avatars
    ADD CONSTRAINT user_profile_avatars_pkey PRIMARY KEY (avatar_id);

--
-- Name: user_profiles user_profiles_identify_card_key; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profiles
    ADD CONSTRAINT user_profiles_identify_card_key UNIQUE (identify_card);

--
-- Name: user_profiles user_profiles_phone_number_key; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profiles
    ADD CONSTRAINT user_profiles_phone_number_key UNIQUE (phone_number);

--
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (user_profile_id);

--
-- Name: idx_cards_status; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_cards_status ON access_control.cards USING btree (status);

--
-- Name: idx_cards_status_before_blocked; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_cards_status_before_blocked ON access_control.cards USING btree (status, status_before_blocked);

--
-- Name: idx_lost_card_reports_card_status; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_lost_card_reports_card_status ON access_control.lost_card_reports USING btree (card_id, status);

--
-- Name: idx_lost_card_reports_context_status; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_lost_card_reports_context_status ON access_control.lost_card_reports USING btree (context, status);

--
-- Name: idx_lost_card_reports_subscription; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_lost_card_reports_subscription ON access_control.lost_card_reports USING btree (subscription_id);

--
-- Name: idx_subscriptions_card_status; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_subscriptions_card_status ON access_control.subscriptions USING btree (card_id, status);

--
-- Name: idx_subscriptions_customer_id; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_subscriptions_customer_id ON access_control.subscriptions USING btree (customer_id);

--
-- Name: idx_subscriptions_customer_status; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_subscriptions_customer_status ON access_control.subscriptions USING btree (customer_id, status);

--
-- Name: idx_subscriptions_customer_vehicle_status_period; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_subscriptions_customer_vehicle_status_period ON access_control.subscriptions USING btree (customer_vehicle_id, status, effective_from, effective_to);

--
-- Name: idx_subscriptions_effective_period; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_subscriptions_effective_period ON access_control.subscriptions USING btree (effective_from, effective_to);

--
-- Name: idx_subscriptions_requested_effective; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_subscriptions_requested_effective ON access_control.subscriptions USING btree (requested_effective_from, status);

--
-- Name: idx_subscriptions_vehicle_status; Type: INDEX; Schema: access_control; Owner: -
--

CREATE INDEX idx_subscriptions_vehicle_status ON access_control.subscriptions USING btree (customer_vehicle_id, status);

--
-- Name: ux_lost_card_reports_open_parking_session; Type: INDEX; Schema: access_control; Owner: -
--

CREATE UNIQUE INDEX ux_lost_card_reports_open_parking_session ON access_control.lost_card_reports USING btree (parking_session_id) WHERE ((parking_session_id IS NOT NULL) AND ((status)::text = 'OPEN'::text));

--
-- Name: idx_audit_logs_target; Type: INDEX; Schema: audit; Owner: -
--

CREATE INDEX idx_audit_logs_target ON audit.audit_logs USING btree (target_schema, target_table, target_id);

--
-- Name: idx_invoices_status; Type: INDEX; Schema: billing; Owner: -
--

CREATE INDEX idx_invoices_status ON billing.invoices USING btree (status);

--
-- Name: idx_payments_invoice_id; Type: INDEX; Schema: billing; Owner: -
--

CREATE INDEX idx_payments_invoice_id ON billing.payments USING btree (invoice_id);

--
-- Name: uq_payments_transaction_ref; Type: INDEX; Schema: billing; Owner: -
--

CREATE UNIQUE INDEX uq_payments_transaction_ref ON billing.payments USING btree (transaction_ref) WHERE (transaction_ref IS NOT NULL);

--
-- Name: uq_ticket_types_active_code; Type: INDEX; Schema: catalog; Owner: -
--

CREATE UNIQUE INDEX uq_ticket_types_active_code ON catalog.ticket_types USING btree (code) WHERE ((status)::text = 'ACTIVE'::text);

--
-- Name: idx_accounts_role_id; Type: INDEX; Schema: iam; Owner: -
--

CREATE INDEX idx_accounts_role_id ON iam.accounts USING btree (role_id);

--
-- Name: idx_accounts_status; Type: INDEX; Schema: iam; Owner: -
--

CREATE INDEX idx_accounts_status ON iam.accounts USING btree (status);

--
-- Name: idx_role_permissions_role_id; Type: INDEX; Schema: iam; Owner: -
--

CREATE INDEX idx_role_permissions_role_id ON iam.role_permissions USING btree (role_id);

--
-- Name: idx_broadcast_announcements_active_window; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_broadcast_announcements_active_window ON notification.broadcast_announcements USING btree (enabled, start_at, end_at);

--
-- Name: idx_broadcast_announcements_related; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_broadcast_announcements_related ON notification.broadcast_announcements USING btree (related_schema, related_table, related_id);

--
-- Name: idx_broadcast_announcements_status_created; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_broadcast_announcements_status_created ON notification.broadcast_announcements USING btree (status, created_at DESC);

--
-- Name: idx_broadcast_announcements_ticker_order; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_broadcast_announcements_ticker_order ON notification.broadcast_announcements USING btree (status, enabled, display_order, start_at, published_at);

--
-- Name: idx_notifications_account_id; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notifications_account_id ON notification.notifications USING btree (account_id);

--
-- Name: idx_notifications_account_type_created; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notifications_account_type_created ON notification.notifications USING btree (account_id, notification_type, created_at DESC);

--
-- Name: idx_notifications_broadcast_id; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notifications_broadcast_id ON notification.notifications USING btree (broadcast_id);

--
-- Name: idx_notifications_realtime_pending; Type: INDEX; Schema: notification; Owner: -
--

CREATE INDEX idx_notifications_realtime_pending ON notification.notifications USING btree (account_id, created_at) WHERE (((channel)::text = 'WEB'::text) AND ((status)::text = 'SENT'::text) AND (read_at IS NULL) AND (realtime_delivered_at IS NULL));

--
-- Name: idx_approval_requests_request_type_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_approval_requests_request_type_status ON operations.approval_requests USING btree (request_type, status);

--
-- Name: idx_approval_requests_target_lookup; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_approval_requests_target_lookup ON operations.approval_requests USING btree (target_schema, target_table, target_id);

--
-- Name: idx_chat_attachments_message; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_attachments_message ON operations.chat_message_attachments USING btree (message_id);

--
-- Name: idx_chat_attachments_object_key; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_attachments_object_key ON operations.chat_message_attachments USING btree (object_key);

--
-- Name: idx_chat_conversations_assigned_to_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_conversations_assigned_to_status ON operations.chat_conversations USING btree (assigned_to, status);

--
-- Name: idx_chat_conversations_customer; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_conversations_customer ON operations.chat_conversations USING btree (customer_id);

--
-- Name: idx_chat_conversations_last_message_at; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_conversations_last_message_at ON operations.chat_conversations USING btree (last_message_at DESC, conversation_id DESC);

--
-- Name: idx_chat_conversations_related; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_conversations_related ON operations.chat_conversations USING btree (related_schema, related_table, related_id);

--
-- Name: idx_chat_conversations_support_ticket; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_conversations_support_ticket ON operations.chat_conversations USING btree (support_ticket_id);

--
-- Name: idx_chat_members_account_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_members_account_status ON operations.chat_conversation_members USING btree (account_id, status);

--
-- Name: idx_chat_members_conversation_account_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_members_conversation_account_status ON operations.chat_conversation_members USING btree (conversation_id, account_id, status);

--
-- Name: idx_chat_members_conversation_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_members_conversation_status ON operations.chat_conversation_members USING btree (conversation_id, status);

--
-- Name: idx_chat_messages_conversation_created_at; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_messages_conversation_created_at ON operations.chat_messages USING btree (conversation_id, created_at DESC, message_id DESC);

--
-- Name: idx_chat_messages_related; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_messages_related ON operations.chat_messages USING btree (related_schema, related_table, related_id);

--
-- Name: idx_chat_messages_sender_created_at; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_chat_messages_sender_created_at ON operations.chat_messages USING btree (sender_account_id, created_at DESC);

--
-- Name: idx_roster_rules_effective_period; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_roster_rules_effective_period ON operations.employee_roster_rules USING btree (effective_from, effective_to);

--
-- Name: idx_roster_rules_employee_period; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_roster_rules_employee_period ON operations.employee_roster_rules USING btree (employee_id, effective_from, effective_to);

--
-- Name: idx_roster_rules_fixed_position; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_roster_rules_fixed_position ON operations.employee_roster_rules USING btree (parking_lot_id, preferred_shift_type, preferred_gate_id, status);

--
-- Name: idx_roster_rules_lot_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_roster_rules_lot_status ON operations.employee_roster_rules USING btree (parking_lot_id, status);

--
-- Name: idx_shift_assignments_employee_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_shift_assignments_employee_status ON operations.shift_assignments USING btree (employee_id, status);

--
-- Name: idx_shift_assignments_gate_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_shift_assignments_gate_status ON operations.shift_assignments USING btree (gate_id, status);

--
-- Name: idx_shift_templates_lot_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_shift_templates_lot_status ON operations.shift_templates USING btree (parking_lot_id, status);

--
-- Name: idx_shift_templates_type_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_shift_templates_type_status ON operations.shift_templates USING btree (shift_type, status);

--
-- Name: idx_shifts_lot_date_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_shifts_lot_date_status ON operations.shifts USING btree (parking_lot_id, shift_date, status);

--
-- Name: idx_shifts_template; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_shifts_template ON operations.shifts USING btree (shift_template_id);

--
-- Name: idx_shifts_time_range; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_shifts_time_range ON operations.shifts USING btree (start_time, end_time);

--
-- Name: idx_support_ticket_categories_code; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_support_ticket_categories_code ON operations.support_ticket_categories USING btree (code);

--
-- Name: idx_support_ticket_categories_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_support_ticket_categories_status ON operations.support_ticket_categories USING btree (status);

--
-- Name: idx_support_tickets_assigned_to; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_support_tickets_assigned_to ON operations.support_tickets USING btree (assigned_to);

--
-- Name: idx_support_tickets_category; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_support_tickets_category ON operations.support_tickets USING btree (category_id);

--
-- Name: idx_support_tickets_customer; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_support_tickets_customer ON operations.support_tickets USING btree (customer_id);

--
-- Name: idx_support_tickets_status; Type: INDEX; Schema: operations; Owner: -
--

CREATE INDEX idx_support_tickets_status ON operations.support_tickets USING btree (status);

--
-- Name: uq_shift_assignments_active_employee; Type: INDEX; Schema: operations; Owner: -
--

CREATE UNIQUE INDEX uq_shift_assignments_active_employee ON operations.shift_assignments USING btree (shift_id, employee_id) WHERE ((status)::text = 'ACTIVE'::text);

--
-- Name: uq_shift_assignments_active_gate; Type: INDEX; Schema: operations; Owner: -
--

CREATE UNIQUE INDEX uq_shift_assignments_active_gate ON operations.shift_assignments USING btree (shift_id, gate_id) WHERE ((status)::text = 'ACTIVE'::text);

--
-- Name: uq_shift_templates_active_lot_type; Type: INDEX; Schema: operations; Owner: -
--

CREATE UNIQUE INDEX uq_shift_templates_active_lot_type ON operations.shift_templates USING btree (parking_lot_id, shift_type) WHERE ((status)::text = 'ACTIVE'::text);

--
-- Name: uq_shifts_lot_date_type; Type: INDEX; Schema: operations; Owner: -
--

CREATE UNIQUE INDEX uq_shifts_lot_date_type ON operations.shifts USING btree (parking_lot_id, shift_date, shift_type);

--
-- Name: uq_support_ticket_categories_active_code; Type: INDEX; Schema: operations; Owner: -
--

CREATE UNIQUE INDEX uq_support_ticket_categories_active_code ON operations.support_ticket_categories USING btree (code) WHERE ((status)::text = 'ACTIVE'::text);

--
-- Name: idx_gates_zone_status; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_gates_zone_status ON parking.gates USING btree (zone_id, status);

--
-- Name: idx_lanes_direction_status; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_lanes_direction_status ON parking.lanes USING btree (direction, status);

--
-- Name: idx_lanes_gate; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_lanes_gate ON parking.lanes USING btree (gate_id);

--
-- Name: idx_parking_events_session_id; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_parking_events_session_id ON parking.parking_events USING btree (parking_session_id);

--
-- Name: idx_parking_sessions_card_id; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_parking_sessions_card_id ON parking.parking_sessions USING btree (card_id);

--
-- Name: idx_parking_sessions_card_status; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_parking_sessions_card_status ON parking.parking_sessions USING btree (card_id, status);

--
-- Name: idx_parking_sessions_check_in_time; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_parking_sessions_check_in_time ON parking.parking_sessions USING btree (check_in_time);

--
-- Name: idx_parking_sessions_license_plate_in_status; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_parking_sessions_license_plate_in_status ON parking.parking_sessions USING btree (license_plate_in, status);

--
-- Name: idx_parking_sessions_status; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_parking_sessions_status ON parking.parking_sessions USING btree (status);

--
-- Name: idx_parking_sessions_zone_status; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_parking_sessions_zone_status ON parking.parking_sessions USING btree (zone_id, status);

--
-- Name: idx_zones_lot_vehicle_type; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_zones_lot_vehicle_type ON parking.zones USING btree (parking_lot_id, vehicle_type_id);

--
-- Name: idx_zones_status; Type: INDEX; Schema: parking; Owner: -
--

CREATE INDEX idx_zones_status ON parking.zones USING btree (status);

--
-- Name: uq_parking_events_pending_checkout; Type: INDEX; Schema: parking; Owner: -
--

CREATE UNIQUE INDEX uq_parking_events_pending_checkout ON parking.parking_events USING btree (parking_session_id) WHERE ((event_type)::text = 'CHECK_OUT_PENDING'::text);

--
-- Name: idx_customer_vehicles_customer_id; Type: INDEX; Schema: people; Owner: -
--

CREATE INDEX idx_customer_vehicles_customer_id ON people.customer_vehicles USING btree (customer_id);

--
-- Name: idx_user_profile_avatars_object_key; Type: INDEX; Schema: people; Owner: -
--

CREATE INDEX idx_user_profile_avatars_object_key ON people.user_profile_avatars USING btree (object_key);

--
-- Name: idx_user_profile_avatars_profile; Type: INDEX; Schema: people; Owner: -
--

CREATE INDEX idx_user_profile_avatars_profile ON people.user_profile_avatars USING btree (user_profile_id);

--
-- Name: idx_user_profile_avatars_uploaded_by; Type: INDEX; Schema: people; Owner: -
--

CREATE INDEX idx_user_profile_avatars_uploaded_by ON people.user_profile_avatars USING btree (uploaded_by_account_id);

--
-- Name: uq_user_profile_current_avatar; Type: INDEX; Schema: people; Owner: -
--

CREATE UNIQUE INDEX uq_user_profile_current_avatar ON people.user_profile_avatars USING btree (user_profile_id) WHERE (is_current = true);

--
-- Name: cards trg_cards_set_updated_at; Type: TRIGGER; Schema: access_control; Owner: -
--

CREATE TRIGGER trg_cards_set_updated_at BEFORE UPDATE ON access_control.cards FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: lost_card_reports trg_lost_card_reports_set_updated_at; Type: TRIGGER; Schema: access_control; Owner: -
--

CREATE TRIGGER trg_lost_card_reports_set_updated_at BEFORE UPDATE ON access_control.lost_card_reports FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: subscriptions trg_subscriptions_set_updated_at; Type: TRIGGER; Schema: access_control; Owner: -
--

CREATE TRIGGER trg_subscriptions_set_updated_at BEFORE UPDATE ON access_control.subscriptions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: audit_logs trg_audit_logs_set_updated_at; Type: TRIGGER; Schema: audit; Owner: -
--

CREATE TRIGGER trg_audit_logs_set_updated_at BEFORE UPDATE ON audit.audit_logs FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: invoices trg_invoices_set_updated_at; Type: TRIGGER; Schema: billing; Owner: -
--

CREATE TRIGGER trg_invoices_set_updated_at BEFORE UPDATE ON billing.invoices FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: card_types trg_card_types_set_updated_at; Type: TRIGGER; Schema: catalog; Owner: -
--

CREATE TRIGGER trg_card_types_set_updated_at BEFORE UPDATE ON catalog.card_types FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: holiday_calendar trg_holiday_calendar_set_updated_at; Type: TRIGGER; Schema: catalog; Owner: -
--

CREATE TRIGGER trg_holiday_calendar_set_updated_at BEFORE UPDATE ON catalog.holiday_calendar FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: price_plans trg_price_plans_set_updated_at; Type: TRIGGER; Schema: catalog; Owner: -
--

CREATE TRIGGER trg_price_plans_set_updated_at BEFORE UPDATE ON catalog.price_plans FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: price_rules trg_price_rules_set_updated_at; Type: TRIGGER; Schema: catalog; Owner: -
--

CREATE TRIGGER trg_price_rules_set_updated_at BEFORE UPDATE ON catalog.price_rules FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: ticket_types trg_ticket_types_set_updated_at; Type: TRIGGER; Schema: catalog; Owner: -
--

CREATE TRIGGER trg_ticket_types_set_updated_at BEFORE UPDATE ON catalog.ticket_types FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: vehicle_types trg_vehicle_types_set_updated_at; Type: TRIGGER; Schema: catalog; Owner: -
--

CREATE TRIGGER trg_vehicle_types_set_updated_at BEFORE UPDATE ON catalog.vehicle_types FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: devices trg_devices_set_updated_at; Type: TRIGGER; Schema: hardware; Owner: -
--

CREATE TRIGGER trg_devices_set_updated_at BEFORE UPDATE ON hardware.devices FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: accounts trg_accounts_set_updated_at; Type: TRIGGER; Schema: iam; Owner: -
--

CREATE TRIGGER trg_accounts_set_updated_at BEFORE UPDATE ON iam.accounts FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: permissions trg_permissions_set_updated_at; Type: TRIGGER; Schema: iam; Owner: -
--

CREATE TRIGGER trg_permissions_set_updated_at BEFORE UPDATE ON iam.permissions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: roles trg_roles_set_updated_at; Type: TRIGGER; Schema: iam; Owner: -
--

CREATE TRIGGER trg_roles_set_updated_at BEFORE UPDATE ON iam.roles FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: notifications trg_notifications_set_updated_at; Type: TRIGGER; Schema: notification; Owner: -
--

CREATE TRIGGER trg_notifications_set_updated_at BEFORE UPDATE ON notification.notifications FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: approval_requests trg_approval_requests_set_updated_at; Type: TRIGGER; Schema: operations; Owner: -
--

CREATE TRIGGER trg_approval_requests_set_updated_at BEFORE UPDATE ON operations.approval_requests FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: chat_conversation_members trg_chat_conversation_members_set_updated_at; Type: TRIGGER; Schema: operations; Owner: -
--

CREATE TRIGGER trg_chat_conversation_members_set_updated_at BEFORE UPDATE ON operations.chat_conversation_members FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: chat_conversations trg_chat_conversations_set_updated_at; Type: TRIGGER; Schema: operations; Owner: -
--

CREATE TRIGGER trg_chat_conversations_set_updated_at BEFORE UPDATE ON operations.chat_conversations FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: chat_message_attachments trg_chat_message_attachments_set_updated_at; Type: TRIGGER; Schema: operations; Owner: -
--

CREATE TRIGGER trg_chat_message_attachments_set_updated_at BEFORE UPDATE ON operations.chat_message_attachments FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: chat_messages trg_chat_messages_set_updated_at; Type: TRIGGER; Schema: operations; Owner: -
--

CREATE TRIGGER trg_chat_messages_set_updated_at BEFORE UPDATE ON operations.chat_messages FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: shifts trg_shifts_set_updated_at; Type: TRIGGER; Schema: operations; Owner: -
--

CREATE TRIGGER trg_shifts_set_updated_at BEFORE UPDATE ON operations.shifts FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: support_tickets trg_support_tickets_set_updated_at; Type: TRIGGER; Schema: operations; Owner: -
--

CREATE TRIGGER trg_support_tickets_set_updated_at BEFORE UPDATE ON operations.support_tickets FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: lanes trg_lanes_set_updated_at; Type: TRIGGER; Schema: parking; Owner: -
--

CREATE TRIGGER trg_lanes_set_updated_at BEFORE UPDATE ON parking.lanes FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: parking_events trg_parking_events_set_updated_at; Type: TRIGGER; Schema: parking; Owner: -
--

CREATE TRIGGER trg_parking_events_set_updated_at BEFORE UPDATE ON parking.parking_events FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: parking_lots trg_parking_lots_set_updated_at; Type: TRIGGER; Schema: parking; Owner: -
--

CREATE TRIGGER trg_parking_lots_set_updated_at BEFORE UPDATE ON parking.parking_lots FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: parking_sessions trg_parking_sessions_set_updated_at; Type: TRIGGER; Schema: parking; Owner: -
--

CREATE TRIGGER trg_parking_sessions_set_updated_at BEFORE UPDATE ON parking.parking_sessions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: zones trg_zones_set_updated_at; Type: TRIGGER; Schema: parking; Owner: -
--

CREATE TRIGGER trg_zones_set_updated_at BEFORE UPDATE ON parking.zones FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: customer_vehicles trg_customer_vehicles_set_updated_at; Type: TRIGGER; Schema: people; Owner: -
--

CREATE TRIGGER trg_customer_vehicles_set_updated_at BEFORE UPDATE ON people.customer_vehicles FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: customers trg_customers_set_updated_at; Type: TRIGGER; Schema: people; Owner: -
--

CREATE TRIGGER trg_customers_set_updated_at BEFORE UPDATE ON people.customers FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: employees trg_employees_set_updated_at; Type: TRIGGER; Schema: people; Owner: -
--

CREATE TRIGGER trg_employees_set_updated_at BEFORE UPDATE ON people.employees FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: user_profile_avatars trg_user_profile_avatars_set_updated_at; Type: TRIGGER; Schema: people; Owner: -
--

CREATE TRIGGER trg_user_profile_avatars_set_updated_at BEFORE UPDATE ON people.user_profile_avatars FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: user_profiles trg_user_profiles_set_updated_at; Type: TRIGGER; Schema: people; Owner: -
--

CREATE TRIGGER trg_user_profiles_set_updated_at BEFORE UPDATE ON people.user_profiles FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

--
-- Name: cards fk_cards_blocked_by; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.cards
    ADD CONSTRAINT fk_cards_blocked_by FOREIGN KEY (blocked_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: cards fk_cards_card_type; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.cards
    ADD CONSTRAINT fk_cards_card_type FOREIGN KEY (card_type_id) REFERENCES catalog.card_types(card_type_id) ON DELETE RESTRICT;

--
-- Name: cards fk_cards_recovered_by; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.cards
    ADD CONSTRAINT fk_cards_recovered_by FOREIGN KEY (recovered_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: cards fk_cards_retired_by; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.cards
    ADD CONSTRAINT fk_cards_retired_by FOREIGN KEY (retired_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: lost_card_reports fk_lost_card_reports_cancelled_by; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: lost_card_reports fk_lost_card_reports_card; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_card FOREIGN KEY (card_id) REFERENCES access_control.cards(card_id) ON DELETE RESTRICT;

--
-- Name: lost_card_reports fk_lost_card_reports_customer; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL;

--
-- Name: lost_card_reports fk_lost_card_reports_resolved_by; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_resolved_by FOREIGN KEY (resolved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: lost_card_reports fk_lost_card_reports_session; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_session FOREIGN KEY (parking_session_id) REFERENCES parking.parking_sessions(parking_session_id) ON DELETE SET NULL;

--
-- Name: lost_card_reports fk_lost_card_reports_subscription; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_subscription FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE SET NULL;

--
-- Name: subscriptions fk_subscriptions_approved_by; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT fk_subscriptions_approved_by FOREIGN KEY (approved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: subscriptions fk_subscriptions_card; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT fk_subscriptions_card FOREIGN KEY (card_id) REFERENCES access_control.cards(card_id) ON DELETE SET NULL;

--
-- Name: subscriptions fk_subscriptions_customer; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT fk_subscriptions_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE RESTRICT;

--
-- Name: subscriptions fk_subscriptions_price_rule; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT fk_subscriptions_price_rule FOREIGN KEY (price_rule_id) REFERENCES catalog.price_rules(price_rule_id) ON DELETE SET NULL;

--
-- Name: subscriptions fk_subscriptions_rejected_by; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT fk_subscriptions_rejected_by FOREIGN KEY (rejected_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: subscriptions fk_subscriptions_ticket_type; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT fk_subscriptions_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES catalog.ticket_types(ticket_type_id) ON DELETE RESTRICT;

--
-- Name: subscriptions fk_subscriptions_vehicle; Type: FK CONSTRAINT; Schema: access_control; Owner: -
--

ALTER TABLE ONLY access_control.subscriptions
    ADD CONSTRAINT fk_subscriptions_vehicle FOREIGN KEY (customer_vehicle_id) REFERENCES people.customer_vehicles(customer_vehicle_id) ON DELETE RESTRICT;

--
-- Name: audit_logs fk_audit_logs_actor; Type: FK CONSTRAINT; Schema: audit; Owner: -
--

ALTER TABLE ONLY audit.audit_logs
    ADD CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: audit_logs fk_audit_logs_created_by; Type: FK CONSTRAINT; Schema: audit; Owner: -
--

ALTER TABLE ONLY audit.audit_logs
    ADD CONSTRAINT fk_audit_logs_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: audit_logs fk_audit_logs_updated_by; Type: FK CONSTRAINT; Schema: audit; Owner: -
--

ALTER TABLE ONLY audit.audit_logs
    ADD CONSTRAINT fk_audit_logs_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: invoices fk_invoices_created_by; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT fk_invoices_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: invoices fk_invoices_customer; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL;

--
-- Name: invoices fk_invoices_lost_card; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT fk_invoices_lost_card FOREIGN KEY (lost_card_report_id) REFERENCES access_control.lost_card_reports(lost_card_report_id) ON DELETE SET NULL;

--
-- Name: invoices fk_invoices_session; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT fk_invoices_session FOREIGN KEY (parking_session_id) REFERENCES parking.parking_sessions(parking_session_id) ON DELETE SET NULL;

--
-- Name: invoices fk_invoices_subscription; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE SET NULL;

--
-- Name: invoices fk_invoices_updated_by; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.invoices
    ADD CONSTRAINT fk_invoices_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: payments fk_payments_invoice; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.payments
    ADD CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES billing.invoices(invoice_id) ON DELETE CASCADE;

--
-- Name: payments fk_payments_received_by; Type: FK CONSTRAINT; Schema: billing; Owner: -
--

ALTER TABLE ONLY billing.payments
    ADD CONSTRAINT fk_payments_received_by FOREIGN KEY (received_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: holiday_calendar fk_holiday_calendar_created_by; Type: FK CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.holiday_calendar
    ADD CONSTRAINT fk_holiday_calendar_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: holiday_calendar fk_holiday_calendar_updated_by; Type: FK CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.holiday_calendar
    ADD CONSTRAINT fk_holiday_calendar_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: price_rules fk_price_rules_price_plan; Type: FK CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.price_rules
    ADD CONSTRAINT fk_price_rules_price_plan FOREIGN KEY (price_plan_id) REFERENCES catalog.price_plans(price_plan_id) ON DELETE CASCADE;

--
-- Name: price_rules fk_price_rules_ticket_type; Type: FK CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.price_rules
    ADD CONSTRAINT fk_price_rules_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES catalog.ticket_types(ticket_type_id) ON DELETE SET NULL;

--
-- Name: price_rules fk_price_rules_vehicle_type; Type: FK CONSTRAINT; Schema: catalog; Owner: -
--

ALTER TABLE ONLY catalog.price_rules
    ADD CONSTRAINT fk_price_rules_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT;

--
-- Name: devices fk_devices_lane; Type: FK CONSTRAINT; Schema: hardware; Owner: -
--

ALTER TABLE ONLY hardware.devices
    ADD CONSTRAINT fk_devices_lane FOREIGN KEY (lane_id) REFERENCES parking.lanes(lane_id) ON DELETE SET NULL;

--
-- Name: devices fk_devices_parking_lot; Type: FK CONSTRAINT; Schema: hardware; Owner: -
--

ALTER TABLE ONLY hardware.devices
    ADD CONSTRAINT fk_devices_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE;

--
-- Name: account_status_history fk_account_status_history_account; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.account_status_history
    ADD CONSTRAINT fk_account_status_history_account FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE CASCADE;

--
-- Name: account_status_history fk_account_status_history_changed_by; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.account_status_history
    ADD CONSTRAINT fk_account_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: accounts fk_accounts_created_by; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT fk_accounts_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: accounts fk_accounts_role; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT fk_accounts_role FOREIGN KEY (role_id) REFERENCES iam.roles(role_id) ON DELETE RESTRICT;

--
-- Name: accounts fk_accounts_updated_by; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT fk_accounts_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: accounts fk_accounts_user_profile; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.accounts
    ADD CONSTRAINT fk_accounts_user_profile FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT;

--
-- Name: permissions fk_permissions_action; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT fk_permissions_action FOREIGN KEY (action_id) REFERENCES iam.permission_actions(action_id) ON DELETE RESTRICT;

--
-- Name: permissions fk_permissions_created_by; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT fk_permissions_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: permissions fk_permissions_module; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT fk_permissions_module FOREIGN KEY (module_id) REFERENCES iam.permission_modules(module_id) ON DELETE RESTRICT;

--
-- Name: permissions fk_permissions_scope; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT fk_permissions_scope FOREIGN KEY (scope_id) REFERENCES iam.permission_scopes(scope_id) ON DELETE RESTRICT;

--
-- Name: permissions fk_permissions_updated_by; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.permissions
    ADD CONSTRAINT fk_permissions_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: role_permissions fk_role_permissions_permission; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.role_permissions
    ADD CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES iam.permissions(permission_id) ON DELETE CASCADE;

--
-- Name: role_permissions fk_role_permissions_role; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.role_permissions
    ADD CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES iam.roles(role_id) ON DELETE CASCADE;

--
-- Name: roles fk_roles_created_by; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.roles
    ADD CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: roles fk_roles_updated_by; Type: FK CONSTRAINT; Schema: iam; Owner: -
--

ALTER TABLE ONLY iam.roles
    ADD CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: broadcast_announcements fk_broadcast_announcements_created_by; Type: FK CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.broadcast_announcements
    ADD CONSTRAINT fk_broadcast_announcements_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: broadcast_announcements fk_broadcast_announcements_updated_by; Type: FK CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.broadcast_announcements
    ADD CONSTRAINT fk_broadcast_announcements_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: notifications fk_notifications_account; Type: FK CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.notifications
    ADD CONSTRAINT fk_notifications_account FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: notifications fk_notifications_broadcast_announcement; Type: FK CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.notifications
    ADD CONSTRAINT fk_notifications_broadcast_announcement FOREIGN KEY (broadcast_id) REFERENCES notification.broadcast_announcements(broadcast_id) ON DELETE SET NULL;

--
-- Name: notifications fk_notifications_created_by; Type: FK CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.notifications
    ADD CONSTRAINT fk_notifications_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: notifications fk_notifications_updated_by; Type: FK CONSTRAINT; Schema: notification; Owner: -
--

ALTER TABLE ONLY notification.notifications
    ADD CONSTRAINT fk_notifications_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: approval_requests fk_approval_requests_approved_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.approval_requests
    ADD CONSTRAINT fk_approval_requests_approved_by FOREIGN KEY (approved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: approval_requests fk_approval_requests_created_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.approval_requests
    ADD CONSTRAINT fk_approval_requests_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: approval_requests fk_approval_requests_requested_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.approval_requests
    ADD CONSTRAINT fk_approval_requests_requested_by FOREIGN KEY (requested_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: approval_requests fk_approval_requests_updated_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.approval_requests
    ADD CONSTRAINT fk_approval_requests_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: chat_message_attachments fk_chat_attachments_message; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_message_attachments
    ADD CONSTRAINT fk_chat_attachments_message FOREIGN KEY (message_id) REFERENCES operations.chat_messages(message_id) ON DELETE CASCADE;

--
-- Name: chat_conversations fk_chat_conversations_assigned_to; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversations
    ADD CONSTRAINT fk_chat_conversations_assigned_to FOREIGN KEY (assigned_to) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: chat_conversations fk_chat_conversations_customer; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversations
    ADD CONSTRAINT fk_chat_conversations_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL;

--
-- Name: chat_conversations fk_chat_conversations_last_message; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversations
    ADD CONSTRAINT fk_chat_conversations_last_message FOREIGN KEY (last_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;

--
-- Name: chat_conversations fk_chat_conversations_owner_account; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversations
    ADD CONSTRAINT fk_chat_conversations_owner_account FOREIGN KEY (owner_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: chat_conversations fk_chat_conversations_support_ticket; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversations
    ADD CONSTRAINT fk_chat_conversations_support_ticket FOREIGN KEY (support_ticket_id) REFERENCES operations.support_tickets(support_ticket_id) ON DELETE SET NULL;

--
-- Name: chat_conversation_members fk_chat_members_account; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversation_members
    ADD CONSTRAINT fk_chat_members_account FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE CASCADE;

--
-- Name: chat_conversation_members fk_chat_members_conversation; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversation_members
    ADD CONSTRAINT fk_chat_members_conversation FOREIGN KEY (conversation_id) REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE;

--
-- Name: chat_conversation_members fk_chat_members_last_read_message; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_conversation_members
    ADD CONSTRAINT fk_chat_members_last_read_message FOREIGN KEY (last_read_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;

--
-- Name: chat_messages fk_chat_messages_conversation; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_messages
    ADD CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE;

--
-- Name: chat_messages fk_chat_messages_reply_to; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_messages
    ADD CONSTRAINT fk_chat_messages_reply_to FOREIGN KEY (reply_to_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;

--
-- Name: chat_messages fk_chat_messages_sender; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.chat_messages
    ADD CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: employee_roster_rules fk_roster_rules_created_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.employee_roster_rules
    ADD CONSTRAINT fk_roster_rules_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: employee_roster_rules fk_roster_rules_employee; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.employee_roster_rules
    ADD CONSTRAINT fk_roster_rules_employee FOREIGN KEY (employee_id) REFERENCES people.employees(employee_id) ON DELETE RESTRICT;

--
-- Name: employee_roster_rules fk_roster_rules_gate; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.employee_roster_rules
    ADD CONSTRAINT fk_roster_rules_gate FOREIGN KEY (preferred_gate_id) REFERENCES parking.gates(gate_id) ON DELETE RESTRICT;

--
-- Name: employee_roster_rules fk_roster_rules_parking_lot; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.employee_roster_rules
    ADD CONSTRAINT fk_roster_rules_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT;

--
-- Name: employee_roster_rules fk_roster_rules_updated_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.employee_roster_rules
    ADD CONSTRAINT fk_roster_rules_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shift_assignments fk_shift_assignments_created_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_assignments
    ADD CONSTRAINT fk_shift_assignments_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shift_assignments fk_shift_assignments_employee; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_assignments
    ADD CONSTRAINT fk_shift_assignments_employee FOREIGN KEY (employee_id) REFERENCES people.employees(employee_id) ON DELETE RESTRICT;

--
-- Name: shift_assignments fk_shift_assignments_gate; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_assignments
    ADD CONSTRAINT fk_shift_assignments_gate FOREIGN KEY (gate_id) REFERENCES parking.gates(gate_id) ON DELETE RESTRICT;

--
-- Name: shift_assignments fk_shift_assignments_shift; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_assignments
    ADD CONSTRAINT fk_shift_assignments_shift FOREIGN KEY (shift_id) REFERENCES operations.shifts(shift_id) ON DELETE CASCADE;

--
-- Name: shift_assignments fk_shift_assignments_updated_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_assignments
    ADD CONSTRAINT fk_shift_assignments_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shift_templates fk_shift_templates_created_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_templates
    ADD CONSTRAINT fk_shift_templates_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shift_templates fk_shift_templates_parking_lot; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_templates
    ADD CONSTRAINT fk_shift_templates_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT;

--
-- Name: shift_templates fk_shift_templates_updated_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shift_templates
    ADD CONSTRAINT fk_shift_templates_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shifts fk_shifts_approved_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_approved_by FOREIGN KEY (approved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shifts fk_shifts_cancelled_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shifts fk_shifts_closed_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_closed_by FOREIGN KEY (closed_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shifts fk_shifts_created_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shifts fk_shifts_opened_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_opened_by FOREIGN KEY (opened_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: shifts fk_shifts_parking_lot; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT;

--
-- Name: shifts fk_shifts_template; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_template FOREIGN KEY (shift_template_id) REFERENCES operations.shift_templates(shift_template_id) ON DELETE RESTRICT;

--
-- Name: shifts fk_shifts_updated_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.shifts
    ADD CONSTRAINT fk_shifts_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: support_tickets fk_support_tickets_assigned_to; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.support_tickets
    ADD CONSTRAINT fk_support_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: support_tickets fk_support_tickets_category; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.support_tickets
    ADD CONSTRAINT fk_support_tickets_category FOREIGN KEY (category_id) REFERENCES operations.support_ticket_categories(category_id) ON DELETE RESTRICT;

--
-- Name: support_tickets fk_support_tickets_closed_by; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.support_tickets
    ADD CONSTRAINT fk_support_tickets_closed_by FOREIGN KEY (closed_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: support_tickets fk_support_tickets_customer; Type: FK CONSTRAINT; Schema: operations; Owner: -
--

ALTER TABLE ONLY operations.support_tickets
    ADD CONSTRAINT fk_support_tickets_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL;

--
-- Name: gates fk_gates_zone; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.gates
    ADD CONSTRAINT fk_gates_zone FOREIGN KEY (zone_id) REFERENCES parking.zones(zone_id) ON DELETE CASCADE;

--
-- Name: lanes fk_lanes_gate; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.lanes
    ADD CONSTRAINT fk_lanes_gate FOREIGN KEY (gate_id) REFERENCES parking.gates(gate_id) ON DELETE RESTRICT;

--
-- Name: parking_events fk_parking_events_actor; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_events
    ADD CONSTRAINT fk_parking_events_actor FOREIGN KEY (actor_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: parking_events fk_parking_events_created_by; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_events
    ADD CONSTRAINT fk_parking_events_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: parking_events fk_parking_events_lane; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_events
    ADD CONSTRAINT fk_parking_events_lane FOREIGN KEY (lane_id) REFERENCES parking.lanes(lane_id) ON DELETE RESTRICT;

--
-- Name: parking_events fk_parking_events_session; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_events
    ADD CONSTRAINT fk_parking_events_session FOREIGN KEY (parking_session_id) REFERENCES parking.parking_sessions(parking_session_id) ON DELETE CASCADE;

--
-- Name: parking_events fk_parking_events_updated_by; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_events
    ADD CONSTRAINT fk_parking_events_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: parking_sessions fk_parking_sessions_card; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_sessions
    ADD CONSTRAINT fk_parking_sessions_card FOREIGN KEY (card_id) REFERENCES access_control.cards(card_id) ON DELETE RESTRICT;

--
-- Name: parking_sessions fk_parking_sessions_customer; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_sessions
    ADD CONSTRAINT fk_parking_sessions_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL;

--
-- Name: parking_sessions fk_parking_sessions_vehicle; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_sessions
    ADD CONSTRAINT fk_parking_sessions_vehicle FOREIGN KEY (customer_vehicle_id) REFERENCES people.customer_vehicles(customer_vehicle_id) ON DELETE SET NULL;

--
-- Name: parking_sessions fk_parking_sessions_vehicle_type; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_sessions
    ADD CONSTRAINT fk_parking_sessions_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT;

--
-- Name: parking_sessions fk_parking_sessions_zone; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.parking_sessions
    ADD CONSTRAINT fk_parking_sessions_zone FOREIGN KEY (zone_id) REFERENCES parking.zones(zone_id) ON DELETE SET NULL;

--
-- Name: zones fk_zones_parking_lot; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.zones
    ADD CONSTRAINT fk_zones_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE;

--
-- Name: zones fk_zones_vehicle_type; Type: FK CONSTRAINT; Schema: parking; Owner: -
--

ALTER TABLE ONLY parking.zones
    ADD CONSTRAINT fk_zones_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE SET NULL;

--
-- Name: customer_vehicles fk_customer_vehicles_customer; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customer_vehicles
    ADD CONSTRAINT fk_customer_vehicles_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE CASCADE;

--
-- Name: customer_vehicles fk_customer_vehicles_vehicle_type; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customer_vehicles
    ADD CONSTRAINT fk_customer_vehicles_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT;

--
-- Name: customers fk_customers_approved_by; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customers
    ADD CONSTRAINT fk_customers_approved_by FOREIGN KEY (approved_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: customers fk_customers_user_profile; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.customers
    ADD CONSTRAINT fk_customers_user_profile FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT;

--
-- Name: employees fk_employees_user_profile; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.employees
    ADD CONSTRAINT fk_employees_user_profile FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT;

--
-- Name: user_profiles fk_user_profiles_created_by; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profiles
    ADD CONSTRAINT fk_user_profiles_created_by FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: user_profiles fk_user_profiles_updated_by; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profiles
    ADD CONSTRAINT fk_user_profiles_updated_by FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: user_profile_avatars user_profile_avatars_created_by_fkey; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profile_avatars
    ADD CONSTRAINT user_profile_avatars_created_by_fkey FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: user_profile_avatars user_profile_avatars_updated_by_fkey; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profile_avatars
    ADD CONSTRAINT user_profile_avatars_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: user_profile_avatars user_profile_avatars_uploaded_by_account_id_fkey; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profile_avatars
    ADD CONSTRAINT user_profile_avatars_uploaded_by_account_id_fkey FOREIGN KEY (uploaded_by_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

--
-- Name: user_profile_avatars user_profile_avatars_user_profile_id_fkey; Type: FK CONSTRAINT; Schema: people; Owner: -
--

ALTER TABLE ONLY people.user_profile_avatars
    ADD CONSTRAINT user_profile_avatars_user_profile_id_fkey FOREIGN KEY (user_profile_id) REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT;

--
-- PostgreSQL database dump complete
--

