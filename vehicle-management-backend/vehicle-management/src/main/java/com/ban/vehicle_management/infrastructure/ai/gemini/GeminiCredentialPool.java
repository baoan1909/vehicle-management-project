package com.ban.vehicle_management.infrastructure.ai.gemini;

import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiProviderAttempt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

/** Credential secrets stay in memory. Availability is scoped to the actual quota group. */
@Component
public class GeminiCredentialPool {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GeminiCredentialPool.class);
    public static final String GENERATION = "generateContent";
    public static final String EMBEDDING = "embedContent";
    private static final Pattern INDEXED_KEY = Pattern.compile("GEMINI_API_KEY_(\\d+)");
    private final GeminiProperties properties;
    private final ConfigurableEnvironment environment;
    private final DistributedCachePortOut cache;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Availability> local = new ConcurrentHashMap<>();
    private final Map<String, Selection> sticky = new ConcurrentHashMap<>();
    private final Map<String, Semaphore> permits = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> nextRequest = new ConcurrentHashMap<>();
    private final Map<String, Diagnostic> diagnostics = new ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentLinkedDeque<FailureEvent> failureEvents = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private volatile List<Credential> credentials;

    @Autowired
    public GeminiCredentialPool(GeminiProperties properties, ConfigurableEnvironment environment,
            DistributedCachePortOut cache) {
        this(properties, environment, cache, Clock.systemUTC());
    }

    GeminiCredentialPool(GeminiProperties properties, ConfigurableEnvironment environment,
            DistributedCachePortOut cache, Clock clock) {
        this.properties = properties;
        this.environment = environment;
        this.cache = cache;
        this.clock = clock;
    }

    public record Credential(String alias, String variable, String project, String secret) {
        @Override public String toString() { return alias; }
    }
    // Cache wire format follows the existing circuit records: ISO text works without a JavaTime module.
    public record Availability(String until, String reason) { }
    private record Selection(String alias, String model) { }
    public record Diagnostic(String variable, String status, String operation, String model,
            long calls, long successes) { }
    public record FailureEvent(String variable, String kind, String operation, String model, String occurredAt) { }
    public enum Failure { AUTH, DAILY_QUOTA, RATE_LIMIT, MODEL, TRANSIENT, REQUEST }

    public List<Credential> credentials() {
        if (credentials != null) return credentials;
        synchronized (this) {
            if (credentials != null) return credentials;
            List<Credential> result = new ArrayList<>();
            Set<String> secrets = new HashSet<>();
            add(result, secrets, "primary", "GEMINI_API_KEY", properties.getApiKey(), properties.getProjectId());
            if (environment != null) {
                SortedSet<Integer> indexes = new TreeSet<>();
                for (PropertySource<?> source : environment.getPropertySources()) {
                    if (source instanceof EnumerablePropertySource<?> enumerable) {
                        for (String name : enumerable.getPropertyNames()) {
                            var matcher = INDEXED_KEY.matcher(name);
                            if (matcher.matches()) {
                                try { indexes.add(Integer.parseInt(matcher.group(1))); }
                                catch (NumberFormatException ignored) { /* Invalid configuration name. */ }
                            }
                        }
                    }
                }
                for (int index : indexes) add(result, secrets, "backup-" + index,
                        "GEMINI_API_KEY_" + index, environment.getProperty("GEMINI_API_KEY_" + index),
                        environment.getProperty("GEMINI_PROJECT_ID_" + index));
            }
            credentials = List.copyOf(result);
            return credentials;
        }
    }

    private void add(List<Credential> result, Set<String> secrets, String alias, String variable,
            String secret, String project) {
        String key = secret == null ? "" : secret.strip();
        if (key.isEmpty() || key.equals("\\") || key.matches("(?i)(KEY_.*|YOUR_.*|CHANGE_?ME.*|<.*>)")) {
            diagnostics.put(alias, new Diagnostic(variable, key.isEmpty() ? "MISSING" : "PLACEHOLDER",
                    null, null, 0, 0));
            return;
        }
        if (!secrets.add(key)) {
            diagnostics.put(alias, new Diagnostic(variable, "DUPLICATE", null, null, 0, 0));
            return;
        }
        // Unknown projects share one conservative quota group; do not assume independence.
        String group = project == null || project.isBlank() || project.matches("project-[a-e]")
                ? "unknown" : project.strip();
        result.add(new Credential(alias, variable, group, key));
        diagnostics.put(alias, new Diagnostic(variable, "UNTESTED", null, null, 0, 0));
    }

    public Instant deadline() { return clock.instant().plus(properties.getCallBudget()); }

    public List<Credential> ordered(String scope) {
        List<Credential> all = credentials();
        Selection selected = sticky.get(scope);
        if (selected == null) return all;
        int start = 0;
        for (int i = 0; i < all.size(); i++) if (all.get(i).alias().equals(selected.alias())) start = i;
        List<Credential> ordered = new ArrayList<>(all.subList(start, all.size()));
        ordered.addAll(all.subList(0, start));
        return ordered;
    }

    public List<AiProviderAttempt> generationAttempts(List<AiModelConfiguration> models) {
        if (models.isEmpty()) return List.of();
        String scope = GENERATION + ":" + models.getFirst().getUseCase();
        Selection selected = sticky.get(scope);
        Instant deadline = deadline();
        List<AiProviderAttempt> result = new ArrayList<>();
        for (Credential credential : ordered(scope)) {
            if (blocked(authKey(credential))) continue;
            List<AiModelConfiguration> ordered = new ArrayList<>(models);
            if (selected != null && credential.alias().equals(selected.alias())) {
                ordered.sort(Comparator.comparing(model -> !model.getModelId().equals(selected.model())));
            }
            for (AiModelConfiguration model : ordered) {
                if (blocked(quotaKey(credential, GENERATION, model.getModelId()))) continue;
                if (result.size() >= Math.max(1, properties.getMaxAttempts())) return List.copyOf(result);
                result.add(new AiProviderAttempt(model, credential.alias(), deadline));
            }
        }
        return List.copyOf(result);
    }

    public Optional<Credential> credential(String alias) {
        return credentials().stream().filter(item -> item.alias().equals(alias)).findFirst();
    }

    private String authKey(Credential credential) { return "gemini:availability:v1:auth:" + digest(credential.secret()); }
    private String quotaKey(Credential credential, String operation, String model) {
        return "gemini:availability:v1:quota:" + digest(credential.project() + ":" + operation + ":" + model);
    }

    public Lease acquire(Credential credential, String operation, String model, Instant deadline)
            throws InterruptedException {
        String group = quotaKey(credential, operation, model);
        if (blocked(authKey(credential)) || blocked(group) || !clock.instant().isBefore(deadline)) return null;
        Semaphore permit = permits.computeIfAbsent(group, ignored -> new Semaphore(1));
        long remaining = Math.max(0, Duration.between(clock.instant(), deadline).toMillis());
        if (!permit.tryAcquire(remaining, TimeUnit.MILLISECONDS)) return null;
        try {
            if (blocked(authKey(credential)) || blocked(group)) { permit.release(); return null; }
            AtomicLong next = nextRequest.computeIfAbsent(group, ignored -> new AtomicLong());
            long delay = Math.max(0, next.get() - clock.millis());
            if (delay >= Math.max(0, Duration.between(clock.instant(), deadline).toMillis())) {
                permit.release(); return null;
            }
            if (delay > 0) Thread.sleep(delay);
            Duration interval = EMBEDDING.equals(operation) ? properties.getEmbeddingMinInterval()
                    : GENERATION.equals(operation) ? properties.getGenerationMinInterval() : Duration.ZERO;
            next.set(clock.millis() + Math.max(0, interval.toMillis()));
            return new Lease(permit);
        } catch (InterruptedException | RuntimeException exception) {
            permit.release(); throw exception;
        }
    }

    public static final class Lease implements AutoCloseable {
        private final Semaphore permit;
        private Lease(Semaphore permit) { this.permit = permit; }
        @Override public void close() { permit.release(); }
    }

    public Duration timeout(Instant deadline, Duration configured) {
        Duration remaining = Duration.between(clock.instant(), deadline);
        if (remaining.isNegative() || remaining.isZero()) return Duration.ofMillis(1);
        return remaining.compareTo(configured) < 0 ? remaining : configured;
    }

    public void sent(Credential credential, String operation, String model) {
        diagnostics.compute(credential.alias(), (alias, old) -> new Diagnostic(credential.variable(),
                "ATTEMPTED", operation, sanitize(model), old == null ? 1 : old.calls() + 1,
                old == null ? 0 : old.successes()));
    }

    public void success(Credential credential, String operation, String model, String scope) {
        Selection selected = new Selection(credential.alias(), model);
        Selection previous = sticky.put(scope, selected);
        if ((previous == null && !"primary".equals(credential.alias())) || (previous != null && !previous.equals(selected))) {
            LOG.info("Gemini selection changed scope={} credential={} model={}", scope, credential.variable(), sanitize(model));
        }
        diagnostics.compute(credential.alias(), (alias, old) -> new Diagnostic(credential.variable(),
                "OK", operation, sanitize(model), old == null ? 1 : old.calls(), old == null ? 1 : old.successes() + 1));
    }

    public Failure failure(Credential credential, String operation, String model,
            int status, String body, String retryAfter) {
        JsonNode error;
        try { error = json.readTree(body == null || body.isBlank() ? "{}" : body).path("error"); }
        catch (Exception ignored) { error = json.createObjectNode(); }
        String reasons = error.path("details").toString().toLowerCase(Locale.ROOT);
        String message = error.path("message").asText("").toLowerCase(Locale.ROOT).strip();
        boolean legacyAuthMessage = (status == 400 || status == 403) && (
                message.startsWith("api key not valid") || message.startsWith("api key expired")
                || message.startsWith("your api key was reported as leaked")
                || message.startsWith("your api key has been reported as leaked"));
        Failure kind;
        if (status == 401 || legacyAuthMessage || reasons.contains("api_key_invalid") || reasons.contains("api_key_expired")
                || reasons.contains("api_key_service_blocked") || reasons.contains("api_key_http_referrer_blocked")) {
            kind = Failure.AUTH;
        } else if (status == 429) {
            kind = reasons.contains("perday") || reasons.contains("per_day") || reasons.contains("daily")
                    ? Failure.DAILY_QUOTA : Failure.RATE_LIMIT;
        } else if (status == 403 || status == 404) kind = Failure.MODEL;
        else if (status == 408 || status >= 500 || status == 0) kind = Failure.TRANSIENT;
        else kind = Failure.REQUEST;
        Instant until = switch (kind) {
            case DAILY_QUOTA -> clock.instant().atZone(ZoneId.of("America/Los_Angeles"))
                    .toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant();
            case AUTH -> clock.instant().plus(Duration.ofDays(1));
            case MODEL -> clock.instant().plus(Duration.ofMinutes(5));
            case RATE_LIMIT, TRANSIENT -> clock.instant().plus(retryDelay(error, retryAfter));
            default -> null;
        };
        if (until != null) put(kind == Failure.AUTH ? authKey(credential)
                : quotaKey(credential, operation, model), new Availability(until.toString(), kind.name()));
        diagnostics.compute(credential.alias(), (alias, old) -> new Diagnostic(credential.variable(), kind.name(),
                operation, sanitize(model), old == null ? 0 : old.calls(), old == null ? 0 : old.successes()));
        failureEvents.addLast(new FailureEvent(credential.variable(), kind.name(), operation, sanitize(model), clock.instant().toString()));
        while (failureEvents.size() > 200) failureEvents.pollFirst();
        LOG.warn("Gemini request unavailable credential={} category={} operation={} model={} retryAt={}",
                credential.variable(), kind, operation, sanitize(model), until);
        return kind;
    }

    private Duration retryDelay(JsonNode error, String header) {
        long seconds = 30;
        try { seconds = Math.max(seconds, Long.parseLong(header)); }
        catch (Exception ignored) {
            try { seconds = Math.max(seconds, Duration.between(clock.instant(),
                    ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toSeconds()); }
            catch (Exception ignoredDate) { /* Missing or invalid header. */ }
        }
        for (JsonNode detail : error.path("details")) {
            String delay = detail.path("retryDelay").asText("");
            try { if (delay.endsWith("s")) seconds = Math.max(seconds,
                    (long) Math.ceil(Double.parseDouble(delay.substring(0, delay.length() - 1)))); }
            catch (NumberFormatException ignored) { /* Malformed retry hint. */ }
        }
        return Duration.ofSeconds(Math.min(86400, seconds));
    }

    private boolean blocked(String key) {
        Availability state = local.get(key);
        if (state != null && clock.instant().isBefore(until(state))) return true;
        if (cache != null) {
            try {
                state = cache.get(key, Availability.class).orElse(null);
                if (state != null && clock.instant().isBefore(until(state))) {
                    local.put(key, state); return true;
                }
            } catch (RuntimeException ignored) { /* Local state protects this instance. */ }
        }
        return false;
    }

    private void put(String key, Availability state) {
        local.put(key, state);
        if (cache != null) {
            try { cache.put(key, state, Duration.between(clock.instant(), until(state))); }
            catch (RuntimeException ignored) { /* Never leak an exception that could contain a credential. */ }
        }
    }

    private static Instant until(Availability state) {
        try { return Instant.parse(state.until()); }
        catch (RuntimeException ignored) { return Instant.EPOCH; }
    }

    public List<Diagnostic> diagnostics() {
        credentials();
        return diagnostics.values().stream().sorted(Comparator.comparing(Diagnostic::variable)).toList();
    }

    public List<FailureEvent> failureEvents() { return List.copyOf(failureEvents); }

    public String sanitize(String body) {
        if (body == null) return null;
        String safe = body;
        for (Credential credential : credentials()) safe = safe.replace(credential.secret(), "[REDACTED]");
        return safe.replaceAll("AIza[0-9A-Za-z_-]{20,}", "[REDACTED]");
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA256_UNAVAILABLE"); }
    }
}
