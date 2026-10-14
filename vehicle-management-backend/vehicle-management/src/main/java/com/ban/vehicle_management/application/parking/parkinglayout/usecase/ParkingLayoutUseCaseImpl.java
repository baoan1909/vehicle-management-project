package com.ban.vehicle_management.application.parking.parkinglayout.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutElementCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutItemCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.PublishPreviewResult;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.application.parking.parkinglayout.port.in.ParkingLayoutPortIn;
import com.ban.vehicle_management.application.parking.parkinglayout.port.out.ParkingLayoutPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.realtime.ParkingRealtimeAfterCommit;
import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.domain.parking.parkinglayout.policy.ParkingLayoutPolicy;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspace.policy.ParkingSpacePolicy;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.parking.zone.policy.ZonePolicy;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLayoutUseCaseImpl implements ParkingLayoutPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingLayoutUseCaseImpl.class);

    private final ParkingLayoutPortOut layoutPortOut;
    private final ParkingSpacePortOut parkingSpacePortOut;
    private final ZonePortOut zonePortOut;
    private final ParkingLevelPortOut parkingLevelPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingSpaceAllocationPortOut allocationPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingRealtimePublisherPortOut parkingRealtimePublisher;
    private final ParkingLayoutPolicy layoutPolicy = new ParkingLayoutPolicy();
    private final ParkingSpacePolicy spacePolicy = new ParkingSpacePolicy();
    private final ZonePolicy zonePolicy = new ZonePolicy();

    public ParkingLayoutUseCaseImpl(
            ParkingLayoutPortOut layoutPortOut,
            ParkingSpacePortOut parkingSpacePortOut,
            ZonePortOut zonePortOut,
            ParkingLevelPortOut parkingLevelPortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingSpaceAllocationPortOut allocationPortOut,
            OrganizationAccessGuard organizationAccessGuard,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            ParkingRealtimePublisherPortOut parkingRealtimePublisher
    ) {
        this.layoutPortOut = layoutPortOut;
        this.parkingSpacePortOut = parkingSpacePortOut;
        this.zonePortOut = zonePortOut;
        this.parkingLevelPortOut = parkingLevelPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.allocationPortOut = allocationPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.parkingRealtimePublisher = parkingRealtimePublisher;
    }

    @Override
    @Transactional
    public ZoneLayoutResult createDraftLayout(UUID zoneId) {
        Zone zone = findConfigurableZone(zoneId);
        return layoutPortOut.findDraftByZoneId(zoneId)
                .map(this::toResult)
                .orElseGet(() -> createDraftFromPublished(zone));
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneLayoutResult getDraftLayout(UUID zoneId) {
        Zone zone = findReadableZone(zoneId);
        return toResult(layoutPortOut.findDraftByZoneId(zone.getZoneId())
                .orElseThrow(() -> new NotFoundException("Draft layout not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneLayoutResult getPublishedLayout(UUID zoneId) {
        Zone zone = findReadableZone(zoneId);
        return toResult(layoutPortOut.findPublishedByZoneId(zone.getZoneId())
                .orElseThrow(() -> new NotFoundException("Published layout not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLayoutVersion> getLayoutVersions(UUID zoneId) {
        return layoutPortOut.findAllByZoneId(findReadableZone(zoneId).getZoneId());
    }

    @Override
    @Transactional
    public ZoneLayoutResult saveLayout(UUID zoneId, Long expectedDraftRevision,
            List<SaveLayoutItemCommand> items, List<SaveLayoutElementCommand> elements) {
        Zone zone = findConfigurableZone(zoneId);
        ParkingLayoutVersion draft = layoutPortOut.findDraftByZoneId(zoneId)
                .orElseThrow(() -> new NotFoundException("Draft layout not found"));
        requireRevision(draft, expectedDraftRevision);

        ParkingLevel level = findLevel(zone);
        Map<String, ParkingSpace> activeByCode = activeSpacesByCode(zoneId);
        List<ParkingSpaceLayoutItem> draftItems = new ArrayList<>();
        for (SaveLayoutItemCommand command : items == null ? List.<SaveLayoutItemCommand>of() : items) {
            ParkingSpaceLayoutItem item = toLayoutItem(command);
            ParkingSpace existing = activeByCode.get(item.getCode());
            item.setParkingSpaceId(existing == null ? null : existing.getParkingSpaceId());
            layoutPolicy.validateLayoutItem(item);
            draftItems.add(item);
        }
        layoutPolicy.validateUniqueCodes(draftItems);
        layoutPolicy.validateWithinCanvas(draftItems, level.getCanvasWidth(), level.getCanvasHeight());
        layoutPolicy.validateNoOverlap(draftItems);

        layoutPortOut.deleteLayoutItemsByVersionId(draft.getLayoutVersionId());
        for (ParkingSpaceLayoutItem item : draftItems) {
            item.setLayoutItemId(UUID.randomUUID());
            item.setLayoutVersionId(draft.getLayoutVersionId());
        }
        List<ParkingSpaceLayoutItem> savedItems = layoutPortOut.saveLayoutItems(draftItems);

        List<ParkingLayoutElement> savedElements;
        if (elements == null) {
            savedElements = layoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId());
        } else {
            layoutPortOut.deleteLayoutElementsByVersionId(draft.getLayoutVersionId());
            savedElements = saveElements(draft.getLayoutVersionId(), elements);
        }

        draft.setLastSavedAt(Instant.now());
        ParkingLayoutVersion savedDraft = layoutPortOut.saveLayoutVersion(draft);
        Metrics.counter("parking.layout.draft.saved").increment();
        LOGGER.info("draft saved zone={} version={} revision={} spaces={} elements={}",
                zoneId, savedDraft.getVersion(), savedDraft.getRevision(),
                savedItems.size(), savedElements.size());
        writeAudit(savedDraft, "SAVE_PARKING_LAYOUT", null, Map.of(
                "zoneId", zoneId.toString(),
                "version", savedDraft.getVersion().toString(),
                "revision", savedDraft.getRevision().toString(),
                "spaceCount", String.valueOf(savedItems.size()),
                "elementCount", String.valueOf(savedElements.size())));
        return toResult(savedDraft, savedItems, savedElements);
    }

    @Override
    @Transactional
    public ZoneLayoutResult publishLayout(UUID zoneId, Long expectedDraftRevision) {
        Zone zone = findConfigurableZone(zoneId);
        ParkingLayoutVersion draft = layoutPortOut.findDraftByZoneId(zoneId)
                .orElseThrow(() -> new NotFoundException("Draft layout not found"));
        requireRevision(draft, expectedDraftRevision);
        ensureCanSwitchToSpaceTracking(zone);

        List<ParkingSpaceLayoutItem> items =
                layoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId());
        if (items.isEmpty()) {
            throw new BadRequestException("Draft layout has no parking spaces");
        }
        ParkingLevel level = findLevel(zone);
        layoutPolicy.validateUniqueCodes(items);
        layoutPolicy.validateWithinCanvas(items, level.getCanvasWidth(), level.getCanvasHeight());
        layoutPolicy.validateNoOverlap(items);
        List<ParkingLayoutElement> elements =
                layoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId());
        elements.forEach(layoutPolicy::validateLayoutElement);

        Map<String, ParkingSpace> activeByCode = activeSpacesByCode(zoneId);
        List<ParkingSpace> desiredSpaces = new ArrayList<>();
        for (ParkingSpaceLayoutItem item : items) {
            ParkingSpace space = activeByCode.get(item.getCode());
            if (item.getParkingSpaceId() != null) {
                ParkingSpace referenced = parkingSpacePortOut.findById(item.getParkingSpaceId())
                        .orElseThrow(() -> new BadRequestException("Layout references a missing parking space"));
                if (!zoneId.equals(referenced.getZoneId())) {
                    throw new BadRequestException("Space, zone, level and lot must share the same topology");
                }
                space = referenced;
            }
            if (space == null) {
                space = newSpace(zoneId, item);
            } else {
                space.setCode(item.getCode());
                space.setVehicleTypeId(item.getVehicleTypeId());
                applyGeometry(space, item);
                spacePolicy.validateState(space);
            }
            desiredSpaces.add(space);
        }

        Set<String> desiredCodes = items.stream().map(ParkingSpaceLayoutItem::getCode).collect(Collectors.toSet());
        for (ParkingSpace existing : activeByCode.values()) {
            if (!desiredCodes.contains(existing.getCode())) {
                ensureNoOpenSession(existing.getParkingSpaceId());
                ensureNoHeldOrActiveAllocation(existing.getParkingSpaceId());
                spacePolicy.archive(existing);
                parkingSpacePortOut.save(existing);
            }
        }

        List<ParkingSpace> materialized = parkingSpacePortOut.saveAll(desiredSpaces);
        Map<String, ParkingSpace> materializedByCode = materialized.stream()
                .collect(Collectors.toMap(ParkingSpace::getCode, value -> value));
        for (ParkingSpaceLayoutItem item : items) {
            item.setParkingSpaceId(materializedByCode.get(item.getCode()).getParkingSpaceId());
        }
        List<ParkingSpaceLayoutItem> boundItems = layoutPortOut.saveLayoutItems(items);

        layoutPortOut.findPublishedByZoneId(zoneId).ifPresent(previous -> {
            layoutPolicy.archive(previous);
            layoutPortOut.saveLayoutVersion(previous);
        });
        layoutPolicy.publish(draft, currentAccountPortIn.getCurrentAccountIdOrThrow());
        ParkingLayoutVersion published = layoutPortOut.saveLayoutVersion(draft);

        zonePolicy.publishSpaceLayout(zone, published.getVersion(), materialized.size());
        zonePortOut.save(zone);
        syncLotCapacity(zone.getParkingLotId());

        writeAudit(published, "PUBLISH_PARKING_LAYOUT", null, Map.of(
                "zoneId", zoneId.toString(),
                "version", published.getVersion().toString(),
                "spaceCount", String.valueOf(materialized.size())));
        Metrics.counter("parking.layout.published").increment();
        LOGGER.info("layout published zone={} version={} spaces={}",
                zoneId, published.getVersion(), materialized.size());
        ParkingRealtimeAfterCommit.publishAfterCommit(parkingRealtimePublisher,
                new ParkingRealtimeEvent(
                        "PARKING_LAYOUT_PUBLISHED",
                        zone.getParkingLotId(),
                        zone.getParkingLevelId(),
                        zone.getZoneId(),
                        null,
                        "PUBLISHED",
                        published.getVersion(),
                        Map.of("spaceCount", String.valueOf(materialized.size()))));
        ParkingRealtimeAfterCommit.publishAfterCommit(parkingRealtimePublisher,
                new ParkingRealtimeEvent(
                        "PARKING_AVAILABILITY_CHANGED",
                        zone.getParkingLotId(),
                        zone.getParkingLevelId(),
                        zone.getZoneId(),
                        null,
                        "PUBLISHED",
                        published.getVersion(),
                        Map.of("spaceCount", String.valueOf(materialized.size()))));
        return toResult(published, boundItems, elements);
    }

    @Override
    @Transactional
    public void discardDraftLayout(UUID zoneId, Long expectedDraftRevision) {
        Zone zone = findConfigurableZone(zoneId);
        ParkingLayoutVersion draft = layoutPortOut.findDraftByZoneId(zone.getZoneId())
                .orElseThrow(() -> new NotFoundException("Draft layout not found"));
        requireRevision(draft, expectedDraftRevision);
        layoutPolicy.archive(draft);
        ParkingLayoutVersion archived = layoutPortOut.saveLayoutVersion(draft);
        writeAudit(archived, "DISCARD_PARKING_LAYOUT_DRAFT", null, Map.of(
                "zoneId", zoneId.toString(),
                "version", archived.getVersion().toString(),
                "revision", archived.getRevision().toString()));
        LOGGER.info("draft discarded zone={} version={} revision={}",
                zoneId, archived.getVersion(), archived.getRevision());
    }

    @Override
    @Transactional(readOnly = true)
    public PublishPreviewResult previewPublish(UUID zoneId, Long expectedDraftRevision) {
        Zone zone = findReadableZone(zoneId);
        ParkingLayoutVersion draft = layoutPortOut.findDraftByZoneId(zone.getZoneId())
                .orElseThrow(() -> new NotFoundException("Draft layout not found"));
        requireRevision(draft, expectedDraftRevision);
        ensureCanSwitchToSpaceTracking(zone);
        List<ParkingSpaceLayoutItem> items =
                layoutPortOut.findLayoutItemsByVersionId(draft.getLayoutVersionId());
        if (items.isEmpty()) {
            throw new BadRequestException("Draft layout has no parking spaces");
        }
        List<ParkingLayoutElement> elements =
                layoutPortOut.findLayoutElementsByVersionId(draft.getLayoutVersionId());
        Map<String, ParkingSpace> activeByCode = activeSpacesByCode(zone.getZoneId());
        Set<String> desiredCodes = items.stream()
                .map(item -> item.getCode() == null
                        ? null
                        : item.getCode().trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        int newSpaces = 0;
        int updatedSpaces = 0;
        for (ParkingSpaceLayoutItem item : items) {
            String code = item.getCode() == null
                    ? null
                    : item.getCode().trim().toUpperCase(Locale.ROOT);
            if (code != null && activeByCode.containsKey(code)) {
                updatedSpaces++;
            } else {
                newSpaces++;
            }
        }
        int archivedSpaces = 0;
        for (String code : activeByCode.keySet()) {
            if (!desiredCodes.contains(code)) {
                archivedSpaces++;
            }
        }
        int capacityAfterPublish = items.size();
        return new PublishPreviewResult(
                zone.getZoneId(),
                draft.getVersion(),
                draft.getRevision(),
                newSpaces,
                updatedSpaces,
                archivedSpaces,
                elements.size(),
                capacityAfterPublish);
    }

    @Override
    @Transactional
    public ParkingLayoutVersion archiveLayout(UUID zoneId, Long version) {
        Zone zone = findConfigurableZone(zoneId);
        ParkingLayoutVersion target = layoutPortOut.findByZoneIdAndVersion(zone.getZoneId(), version)
                .orElseThrow(() -> new NotFoundException("Layout version not found"));
        if (target.getStatus() == LayoutVersionStatus.PUBLISHED && version.equals(zone.getLayoutVersion())) {
            throw new ConflictException("Cannot archive the currently published layout");
        }
        layoutPolicy.archive(target);
        return layoutPortOut.saveLayoutVersion(target);
    }

    private List<ParkingLayoutElement> saveElements(UUID layoutVersionId,
            List<SaveLayoutElementCommand> commands) {
        List<ParkingLayoutElement> elements = new ArrayList<>();
        for (int index = 0; index < commands.size(); index++) {
            SaveLayoutElementCommand command = commands.get(index);
            ParkingLayoutElement element = new ParkingLayoutElement();
            element.setLayoutElementId(UUID.randomUUID());
            element.setLayoutVersionId(layoutVersionId);
            element.setElementType(command.elementType());
            element.setGeometry(command.geometry());
            element.setStyle(command.style());
            element.setLabel(command.label());
            element.setDisplayOrder(command.displayOrder() == null ? index : command.displayOrder());
            layoutPolicy.validateLayoutElement(element);
            elements.add(element);
        }
        return elements.isEmpty() ? List.of() : layoutPortOut.saveLayoutElements(elements);
    }

    private void requireRevision(ParkingLayoutVersion draft, Long expectedRevision) {
        if (expectedRevision == null) {
            throw new BadRequestException("expectedDraftRevision must not be null");
        }
        if (!expectedRevision.equals(draft.getRevision())) {
            Metrics.counter("parking.layout.draft.conflict").increment();
            LOGGER.warn("draft conflict zone={} expected={} actual={}",
                    draft.getZoneId(), expectedRevision, draft.getRevision());
            throw new ConflictException("Layout was modified by another user. Please reload and retry.");
        }
    }

    private Zone findReadableZone(UUID zoneId) {
        Zone zone = getZone(zoneId);
        organizationAccessGuard.ensureCanAccessParkingLot(getLot(zone.getParkingLotId()));
        return zone;
    }

    private Zone findConfigurableZone(UUID zoneId) {
        Zone zone = getZone(zoneId);
        organizationAccessGuard.ensureCanConfigureParkingLot(getLot(zone.getParkingLotId()));
        return zone;
    }

    private Zone getZone(UUID zoneId) {
        if (zoneId == null) {
            throw new NotFoundException("Zone not found");
        }
        return zonePortOut.findById(zoneId).orElseThrow(() -> new NotFoundException("Zone not found"));
    }

    private ParkingLot getLot(UUID parkingLotId) {
        return parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    private ParkingLevel findLevel(Zone zone) {
        if (zone.getParkingLevelId() == null) {
            throw new BadRequestException("Zone has no parking level assigned");
        }
        ParkingLevel level = parkingLevelPortOut.findById(zone.getParkingLevelId())
                .orElseThrow(() -> new NotFoundException("Parking level not found"));
        if (!zone.getParkingLotId().equals(level.getParkingLotId())) {
            throw new BadRequestException("Zone, level and lot must share the same topology");
        }
        return level;
    }

    private Map<String, ParkingSpace> activeSpacesByCode(UUID zoneId) {
        Map<String, ParkingSpace> result = new LinkedHashMap<>();
        for (ParkingSpace space : parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE")) {
            result.put(normalizeCode(space.getCode()), space);
        }
        return result;
    }

    private ParkingSpaceLayoutItem toLayoutItem(SaveLayoutItemCommand command) {
        ParkingSpaceLayoutItem item = new ParkingSpaceLayoutItem();
        item.setCode(normalizeCode(command.code()));
        item.setVehicleTypeId(command.vehicleTypeId());
        item.setX(command.x());
        item.setY(command.y());
        item.setWidth(command.width());
        item.setHeight(command.height());
        item.setRotation(command.rotation());
        return item;
    }

    private ParkingSpace newSpace(UUID zoneId, ParkingSpaceLayoutItem item) {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(UUID.randomUUID());
        space.setZoneId(zoneId);
        space.setCode(item.getCode());
        space.setVehicleTypeId(item.getVehicleTypeId());
        applyGeometry(space, item);
        spacePolicy.initialize(space);
        return space;
    }

    private void applyGeometry(ParkingSpace space, ParkingSpaceLayoutItem item) {
        space.setX(item.getX());
        space.setY(item.getY());
        space.setWidth(item.getWidth());
        space.setHeight(item.getHeight());
        space.setRotation(item.getRotation());
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("code must not be blank");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private ParkingLayoutVersion newDraftVersion(Zone zone) {
        long nextVersion = Math.max(zone.getLayoutVersion() == null ? 0L : zone.getLayoutVersion(),
                layoutPortOut.findAllByZoneId(zone.getZoneId()).stream()
                        .mapToLong(value -> value.getVersion() == null ? 0L : value.getVersion())
                        .max().orElse(0L)) + 1;
        ParkingLayoutVersion draft = new ParkingLayoutVersion();
        draft.setLayoutVersionId(UUID.randomUUID());
        draft.setZoneId(zone.getZoneId());
        draft.setVersion(nextVersion);
        // Keep revision null so Hibernate seeds the @Version column on persist.
        // Pre-setting 0L makes Spring Data route save() to merge(), which issues
        // an UPDATE for a row that does not exist yet and surfaces as a 409.
        layoutPolicy.initialize(draft);
        return layoutPortOut.saveLayoutVersion(draft);
    }

    private ZoneLayoutResult createDraftFromPublished(Zone zone) {
        ParkingLayoutVersion draft = newDraftVersion(zone);
        Optional<ParkingLayoutVersion> published = layoutPortOut.findPublishedByZoneId(zone.getZoneId());
        if (published.isEmpty()) {
            return toResult(draft, List.of(), List.of());
        }

        UUID draftVersionId = draft.getLayoutVersionId();
        UUID publishedVersionId = published.get().getLayoutVersionId();
        List<ParkingSpaceLayoutItem> clonedItems = layoutPortOut.findLayoutItemsByVersionId(publishedVersionId)
                .stream()
                .map(source -> new ParkingSpaceLayoutItem(
                        UUID.randomUUID(), draftVersionId, source.getParkingSpaceId(), source.getCode(),
                        source.getVehicleTypeId(), source.getX(), source.getY(), source.getWidth(),
                        source.getHeight(), source.getRotation()))
                .toList();
        List<ParkingLayoutElement> clonedElements = layoutPortOut.findLayoutElementsByVersionId(publishedVersionId)
                .stream()
                .map(source -> new ParkingLayoutElement(
                        UUID.randomUUID(), draftVersionId, source.getElementType(),
                        source.getGeometry() == null ? null : source.getGeometry().deepCopy(),
                        source.getStyle() == null ? null : source.getStyle().deepCopy(),
                        source.getLabel(), source.getDisplayOrder()))
                .toList();

        List<ParkingSpaceLayoutItem> savedItems = clonedItems.isEmpty()
                ? List.of()
                : layoutPortOut.saveLayoutItems(clonedItems);
        List<ParkingLayoutElement> savedElements = clonedElements.isEmpty()
                ? List.of()
                : layoutPortOut.saveLayoutElements(clonedElements);
        LOGGER.info("draft created from published layout zone={} sourceVersion={} draftVersion={} spaces={} elements={}",
                zone.getZoneId(), published.get().getVersion(), draft.getVersion(),
                savedItems.size(), savedElements.size());
        return toResult(draft, savedItems, savedElements);
    }

    private ZoneLayoutResult toResult(ParkingLayoutVersion version) {
        return toResult(version,
                layoutPortOut.findLayoutItemsByVersionId(version.getLayoutVersionId()),
                layoutPortOut.findLayoutElementsByVersionId(version.getLayoutVersionId()));
    }

    private ZoneLayoutResult toResult(ParkingLayoutVersion version, List<ParkingSpaceLayoutItem> items,
            List<ParkingLayoutElement> elements) {
        List<ZoneLayoutResult.LayoutItemView> itemViews = items.stream()
                .map(item -> new ZoneLayoutResult.LayoutItemView(
                        item.getLayoutItemId(), item.getLayoutVersionId(), item.getParkingSpaceId(),
                        item.getCode(), item.getVehicleTypeId(), item.getX(), item.getY(),
                        item.getWidth(), item.getHeight(), item.getRotation()))
                .toList();
        List<ZoneLayoutResult.LayoutElementView> elementViews = elements.stream()
                .map(element -> new ZoneLayoutResult.LayoutElementView(
                        element.getLayoutElementId(), element.getElementType(), element.getGeometry(),
                        element.getStyle(), element.getLabel(), element.getDisplayOrder()))
                .toList();
        return new ZoneLayoutResult(version, itemViews, elementViews);
    }

    private void ensureNoOpenSession(UUID parkingSpaceId) {
        if (parkingSessionPortOut.existsOpenByParkingSpaceId(parkingSpaceId)) {
            throw new ConflictException("Parking space has an open parking session");
        }
    }

    private void ensureCanSwitchToSpaceTracking(Zone zone) {
        if (zone.getTrackingMode() != TrackingMode.SPACE
                && parkingSessionPortOut.countOpenByZoneId(zone.getZoneId()) > 0) {
            throw new ConflictException(
                    "Cannot switch zone to SPACE tracking while parking sessions are open");
        }
    }

    private void ensureNoHeldOrActiveAllocation(UUID parkingSpaceId) {
        Instant now = Instant.now();
        boolean blocked = allocationPortOut.findByParkingSpaceId(parkingSpaceId).stream()
                .anyMatch(allocation -> allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.ACTIVE
                        || allocation.getAllocationStatus() == ParkingSpaceAllocationStatus.HELD
                        && (allocation.getHoldExpiresAt() == null
                            || allocation.getHoldExpiresAt().isAfter(now)));
        if (blocked) {
            throw new ConflictException("Parking space has a subscription allocation");
        }
    }

    private void syncLotCapacity(UUID parkingLotId) {
        ParkingLot lot = getLot(parkingLotId);
        int total = zonePortOut.findAll(parkingLotId, null, null, null).stream()
                .mapToInt(item -> item.getCapacity() == null ? 0 : item.getCapacity())
                .sum();
        lot.setTotalCapacity(total);
        parkingLotPortOut.save(lot);
    }

    private void writeAudit(ParkingLayoutVersion version, String action,
            Map<String, Object> oldData, Map<String, Object> newData) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        auditLog.setAction(action);
        auditLog.setTargetSchema("parking");
        auditLog.setTargetTable("parking_layout_versions");
        auditLog.setTargetId(version.getLayoutVersionId());
        auditLog.setOldData(oldData);
        auditLog.setNewData(newData);
        auditLogPortOut.save(auditLog);
    }
}
