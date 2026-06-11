package com.ban.vehicle_management.application.people.customervehicle.usecase;

import com.ban.vehicle_management.application.people.customervehicle.authorization.CustomerVehicleAccessGuard;
import com.ban.vehicle_management.application.people.customervehicle.model.command.CustomerVehicleBatchCommand;
import com.ban.vehicle_management.application.people.customervehicle.port.in.CustomerVehiclePortIn;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.people.customervehicle.policy.CustomerVehiclePolicy;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerVehicleUseCaseImpl implements CustomerVehiclePortIn {

    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final CustomerVehiclePolicy customerVehiclePolicy = new CustomerVehiclePolicy();
    private final CustomerVehicleAccessGuard customerVehicleAccessGuard;

    public CustomerVehicleUseCaseImpl(CustomerVehiclePortOut customerVehiclePortOut, CustomerVehicleAccessGuard customerVehicleAccessGuard) {
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.customerVehicleAccessGuard = customerVehicleAccessGuard;
    }

    @Override
    @Transactional
    public CustomerVehicle createCustomerVehicle(CustomerVehicle customerVehicle) {
        customerVehicle.setCustomerId(customerVehicleAccessGuard.resolveCustomerIdForCreate(customerVehicle.getCustomerId()));
        customerVehiclePolicy.initialize(customerVehicle);
        validateReferences(customerVehicle);
        validateUniqueLicensePlate(customerVehicle);

        customerVehicle.setCustomerVehicleId(UUID.randomUUID());
        CustomerVehicle savedCustomerVehicle = customerVehiclePortOut.save(customerVehicle);
        applySingleDefaultRule(savedCustomerVehicle);
        return savedCustomerVehicle;
    }

    @Override
    @Transactional
    public List<CustomerVehicle> applyCustomerVehicleBatch(CustomerVehicleBatchCommand command) {
        ensureBatchPayloadHasContent(command);
        UUID customerId = resolveCustomerIdForBatch(command);
        validateCustomerExists(customerId);

        List<CustomerVehicle> existingCustomerVehicles =
                customerVehiclePortOut.findAll(customerId, null, null, null, null);
        VehicleBatchPreparation preparedVehicleBatch =
                prepareVehicleBatch(customerId, existingCustomerVehicles, command);

        for (CustomerVehicle customerVehicleToInactivate : preparedVehicleBatch.vehiclesToInactivate()) {
            customerVehiclePortOut.save(customerVehicleToInactivate);
        }

        for (CustomerVehicle customerVehicleToUpdate : preparedVehicleBatch.vehiclesToUpdate()) {
            customerVehiclePortOut.save(customerVehicleToUpdate);
        }

        for (CustomerVehicle customerVehicleToCreate : preparedVehicleBatch.vehiclesToCreate()) {
            customerVehiclePortOut.save(customerVehicleToCreate);
        }

        for (CustomerVehicle defaultVehicleCorrection : preparedVehicleBatch.defaultVehicleCorrections()) {
            customerVehiclePortOut.save(defaultVehicleCorrection);
        }

        return customerVehiclePortOut.findAll(customerId, null, null, null, null);
    }

    @Override
    @Transactional
    public CustomerVehicle updateCustomerVehicle(UUID customerVehicleId, CustomerVehicle customerVehicle) {
        CustomerVehicle existingCustomerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanUpdate(existingCustomerVehicle);

        existingCustomerVehicle.setVehicleTypeId(customerVehicle.getVehicleTypeId());
        existingCustomerVehicle.setLicensePlate(customerVehicle.getLicensePlate());
        existingCustomerVehicle.setBrand(customerVehicle.getBrand());
        existingCustomerVehicle.setColor(customerVehicle.getColor());
        if (customerVehicle.getIsDefault() != null) {
            existingCustomerVehicle.setIsDefault(customerVehicle.getIsDefault());
        }

        customerVehiclePolicy.validateState(existingCustomerVehicle);
        validateVehicleTypeExists(existingCustomerVehicle.getVehicleTypeId());
        validateUniqueLicensePlate(existingCustomerVehicle, customerVehicleId);

        CustomerVehicle savedCustomerVehicle = customerVehiclePortOut.save(existingCustomerVehicle);
        applySingleDefaultRule(savedCustomerVehicle);
        return savedCustomerVehicle;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerVehicle getCustomerVehicleById(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanRead(customerVehicle);
        return customerVehicle;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerVehicle> getAllCustomerVehicle(
            UUID customerId,
            CustomerVehicleStatus status,
            UUID vehicleTypeId,
            Boolean isDefault,
            String keyword
    ) {
        UUID resolvedCustomerId = customerVehicleAccessGuard.resolveCustomerIdForRead(customerId);
        return customerVehiclePortOut.findAll(resolvedCustomerId, status, vehicleTypeId, isDefault, keyword);
    }

    @Override
    @Transactional
    public void deleteCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle existingCustomerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanDelete(existingCustomerVehicle);
        if (existingCustomerVehicle.getStatus() == CustomerVehicleStatus.INACTIVE) {
            return;
        }

        customerVehiclePolicy.inactivate(existingCustomerVehicle);
        customerVehiclePortOut.save(existingCustomerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle activateCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanActivateOrInactivate(customerVehicle);
        customerVehiclePolicy.activate(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle inactivateCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanActivateOrInactivate(customerVehicle);
        customerVehiclePolicy.inactivate(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle blockCustomerVehicle(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanBlock();
        customerVehiclePolicy.block(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    @Override
    @Transactional
    public CustomerVehicle markCustomerVehicleAsDefault(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanUpdate(customerVehicle);
        customerVehiclePolicy.markDefault(customerVehicle);
        CustomerVehicle savedCustomerVehicle = customerVehiclePortOut.save(customerVehicle);
        applySingleDefaultRule(savedCustomerVehicle);
        return savedCustomerVehicle;
    }

    @Transactional
    public CustomerVehicle unmarkCustomerVehicleAsDefault(UUID customerVehicleId) {
        CustomerVehicle customerVehicle = findCustomerVehicleOrThrow(customerVehicleId);
        customerVehicleAccessGuard.ensureCanUpdate(customerVehicle);
        customerVehiclePolicy.unmarkDefault(customerVehicle);
        return customerVehiclePortOut.save(customerVehicle);
    }

    private CustomerVehicle findCustomerVehicleOrThrow(UUID customerVehicleId) {
        return customerVehiclePortOut.findById(customerVehicleId)
                .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));
    }

    private void validateReferences(CustomerVehicle customerVehicle) {
        validateCustomerExists(customerVehicle.getCustomerId());
        validateVehicleTypeExists(customerVehicle.getVehicleTypeId());
    }

    private void validateCustomerExists(UUID customerId) {
        if (!customerVehiclePortOut.existsCustomerById(customerId)) {
            throw new NotFoundException("Customer not found");
        }
    }

    private void validateVehicleTypeExists(UUID vehicleTypeId) {
        if (!customerVehiclePortOut.existsVehicleTypeById(vehicleTypeId)) {
            throw new NotFoundException("Vehicle type not found");
        }
    }

    private void validateUniqueLicensePlate(CustomerVehicle customerVehicle) {
        if (customerVehiclePortOut.existsByLicensePlate(customerVehicle.getLicensePlate())) {
            throw new ConflictException("Customer vehicle license plate already exists");
        }
    }

    private void validateUniqueLicensePlate(CustomerVehicle customerVehicle, UUID customerVehicleId) {
        if (customerVehiclePortOut.existsByLicensePlateAndCustomerVehicleIdNot(
                customerVehicle.getLicensePlate(),
                customerVehicleId
        )) {
            throw new ConflictException("Customer vehicle license plate already exists");
        }
    }

    private void ensureBatchPayloadHasContent(CustomerVehicleBatchCommand command) {
        if (command == null || (safeCreateVehicles(command).isEmpty()
                && safeUpdateVehicles(command).isEmpty()
                && safeInactivateVehicleIds(command).isEmpty())) {
            throw new BadRequestException("At least one customer vehicle change must be provided");
        }
    }

    private UUID resolveCustomerIdForBatch(CustomerVehicleBatchCommand command) {
        UUID resolvedCustomerId = safeCreateVehicles(command).isEmpty()
                ? customerVehicleAccessGuard.resolveCustomerIdForRead(command.customerId())
                : customerVehicleAccessGuard.resolveCustomerIdForCreate(command.customerId());
        if (resolvedCustomerId == null) {
            throw new BadRequestException("customerId must not be null");
        }
        return resolvedCustomerId;
    }

    private VehicleBatchPreparation prepareVehicleBatch(
            UUID customerId,
            List<CustomerVehicle> existingCustomerVehicles,
            CustomerVehicleBatchCommand command
    ) {
        Map<UUID, CustomerVehicle> existingVehiclesById = new HashMap<>();
        for (CustomerVehicle existingVehicle : existingCustomerVehicles) {
            existingVehiclesById.put(existingVehicle.getCustomerVehicleId(), copyCustomerVehicle(existingVehicle));
        }

        ensureNoDuplicateVehicleIds(safeInactivateVehicleIds(command), "inactivate");
        ensureNoDuplicateVehicleIds(
                safeUpdateVehicles(command).stream()
                        .map(CustomerVehicle::getCustomerVehicleId)
                        .toList(),
                "update"
        );
        ensureNoVehicleIdActionOverlap(
                safeUpdateVehicles(command).stream().map(CustomerVehicle::getCustomerVehicleId).toList(),
                safeInactivateVehicleIds(command)
        );

        List<CustomerVehicle> preparedVehiclesToInactivate = new ArrayList<>();
        for (UUID customerVehicleId : safeInactivateVehicleIds(command)) {
            CustomerVehicle customerVehicle = copyCustomerVehicle(resolveExistingVehicle(existingVehiclesById, customerVehicleId));
            ensureVehicleBelongsToCustomer(customerId, customerVehicle);
            customerVehicleAccessGuard.ensureCanActivateOrInactivate(customerVehicle);
            customerVehiclePolicy.inactivate(customerVehicle);
            preparedVehiclesToInactivate.add(customerVehicle);
        }

        List<CustomerVehicle> preparedVehiclesToUpdate = new ArrayList<>();
        for (CustomerVehicle requestedVehicleUpdate : safeUpdateVehicles(command)) {
            CustomerVehicle existingVehicle = copyCustomerVehicle(resolveExistingVehicle(
                    existingVehiclesById,
                    requestedVehicleUpdate.getCustomerVehicleId()
            ));
            preparedVehiclesToUpdate.add(buildUpdatedCustomerVehicle(customerId, existingVehicle, requestedVehicleUpdate));
        }

        List<CustomerVehicle> preparedVehiclesToCreate = new ArrayList<>();
        for (CustomerVehicle requestedVehicleCreate : safeCreateVehicles(command)) {
            ensureCreateCustomerIdMatchesBatch(customerId, requestedVehicleCreate);
            requestedVehicleCreate.setCustomerId(customerId);
            customerVehiclePolicy.initialize(requestedVehicleCreate);
            validateVehicleTypeExists(requestedVehicleCreate.getVehicleTypeId());
            requestedVehicleCreate.setCustomerVehicleId(UUID.randomUUID());
            preparedVehiclesToCreate.add(requestedVehicleCreate);
        }

        validateVehicleLicensePlates(
                existingCustomerVehicles,
                preparedVehiclesToUpdate,
                preparedVehiclesToCreate
        );

        List<CustomerVehicle> defaultVehicleCorrections = resolveDefaultVehicleCorrections(
                existingCustomerVehicles,
                preparedVehiclesToInactivate,
                preparedVehiclesToUpdate,
                preparedVehiclesToCreate
        );

        return new VehicleBatchPreparation(
                preparedVehiclesToCreate,
                preparedVehiclesToUpdate,
                preparedVehiclesToInactivate,
                defaultVehicleCorrections
        );
    }

    private CustomerVehicle buildUpdatedCustomerVehicle(
            UUID customerId,
            CustomerVehicle existingCustomerVehicle,
            CustomerVehicle requestedCustomerVehicle
    ) {
        if (requestedCustomerVehicle.getCustomerVehicleId() == null) {
            throw new BadRequestException("customerVehicleId must not be null when updating a customer vehicle");
        }

        ensureVehicleBelongsToCustomer(customerId, existingCustomerVehicle);
        customerVehicleAccessGuard.ensureCanUpdate(existingCustomerVehicle);

        existingCustomerVehicle.setVehicleTypeId(requestedCustomerVehicle.getVehicleTypeId());
        existingCustomerVehicle.setLicensePlate(requestedCustomerVehicle.getLicensePlate());
        existingCustomerVehicle.setBrand(requestedCustomerVehicle.getBrand());
        existingCustomerVehicle.setColor(requestedCustomerVehicle.getColor());
        if (requestedCustomerVehicle.getIsDefault() != null) {
            existingCustomerVehicle.setIsDefault(requestedCustomerVehicle.getIsDefault());
        }

        customerVehiclePolicy.validateState(existingCustomerVehicle);
        validateVehicleTypeExists(existingCustomerVehicle.getVehicleTypeId());
        return existingCustomerVehicle;
    }

    private void validateVehicleLicensePlates(
            List<CustomerVehicle> existingCustomerVehicles,
            List<CustomerVehicle> vehiclesToUpdate,
            List<CustomerVehicle> vehiclesToCreate
    ) {
        Map<UUID, CustomerVehicle> finalExistingVehiclesById = new HashMap<>();
        for (CustomerVehicle existingVehicle : existingCustomerVehicles) {
            finalExistingVehiclesById.put(existingVehicle.getCustomerVehicleId(), copyCustomerVehicle(existingVehicle));
        }

        for (CustomerVehicle vehicleToUpdate : vehiclesToUpdate) {
            finalExistingVehiclesById.put(vehicleToUpdate.getCustomerVehicleId(), copyCustomerVehicle(vehicleToUpdate));
        }

        Set<String> seenLicensePlates = new HashSet<>();
        for (CustomerVehicle vehicleToUpdate : vehiclesToUpdate) {
            ensureUniqueLicensePlateInRequest(seenLicensePlates, vehicleToUpdate.getLicensePlate());
        }
        for (CustomerVehicle vehicleToCreate : vehiclesToCreate) {
            ensureUniqueLicensePlateInRequest(seenLicensePlates, vehicleToCreate.getLicensePlate());
        }

        for (CustomerVehicle vehicleToUpdate : vehiclesToUpdate) {
            validateLicensePlateAgainstDatabase(vehicleToUpdate.getLicensePlate(), vehicleToUpdate.getCustomerVehicleId(), finalExistingVehiclesById);
        }
        for (CustomerVehicle vehicleToCreate : vehiclesToCreate) {
            validateLicensePlateAgainstDatabase(vehicleToCreate.getLicensePlate(), null, finalExistingVehiclesById);
        }
    }

    private void validateLicensePlateAgainstDatabase(
            String licensePlate,
            UUID currentCustomerVehicleId,
            Map<UUID, CustomerVehicle> finalExistingVehiclesById
    ) {
        Optional<CustomerVehicle> existingVehicleWithSamePlate = customerVehiclePortOut.findByLicensePlate(licensePlate);
        if (existingVehicleWithSamePlate.isEmpty()) {
            return;
        }

        CustomerVehicle existingVehicle = existingVehicleWithSamePlate.get();
        if (currentCustomerVehicleId != null && existingVehicle.getCustomerVehicleId().equals(currentCustomerVehicleId)) {
            return;
        }

        CustomerVehicle projectedExistingVehicle = finalExistingVehiclesById.get(existingVehicle.getCustomerVehicleId());
        if (projectedExistingVehicle != null && licensePlate.equals(projectedExistingVehicle.getLicensePlate())) {
            throw new ConflictException("Customer vehicle license plate already exists");
        }

        if (projectedExistingVehicle == null) {
            throw new ConflictException("Customer vehicle license plate already exists");
        }
    }

    private List<CustomerVehicle> resolveDefaultVehicleCorrections(
            List<CustomerVehicle> existingCustomerVehicles,
            List<CustomerVehicle> vehiclesToInactivate,
            List<CustomerVehicle> vehiclesToUpdate,
            List<CustomerVehicle> vehiclesToCreate
    ) {
        List<CustomerVehicle> requestedDefaultVehicles = new ArrayList<>();
        for (CustomerVehicle vehicleToUpdate : vehiclesToUpdate) {
            if (Boolean.TRUE.equals(vehicleToUpdate.getIsDefault())) {
                requestedDefaultVehicles.add(vehicleToUpdate);
            }
        }
        for (CustomerVehicle vehicleToCreate : vehiclesToCreate) {
            if (Boolean.TRUE.equals(vehicleToCreate.getIsDefault())) {
                requestedDefaultVehicles.add(vehicleToCreate);
            }
        }
        if (requestedDefaultVehicles.size() > 1) {
            throw new BadRequestException("Only one customer vehicle can be marked as default after applying the update");
        }

        Map<UUID, CustomerVehicle> finalExistingVehiclesById = new HashMap<>();
        for (CustomerVehicle existingVehicle : existingCustomerVehicles) {
            finalExistingVehiclesById.put(existingVehicle.getCustomerVehicleId(), copyCustomerVehicle(existingVehicle));
        }
        for (CustomerVehicle vehicleToInactivate : vehiclesToInactivate) {
            finalExistingVehiclesById.put(vehicleToInactivate.getCustomerVehicleId(), copyCustomerVehicle(vehicleToInactivate));
        }
        for (CustomerVehicle vehicleToUpdate : vehiclesToUpdate) {
            finalExistingVehiclesById.put(vehicleToUpdate.getCustomerVehicleId(), copyCustomerVehicle(vehicleToUpdate));
        }

        List<CustomerVehicle> finalVehicles = new ArrayList<>(finalExistingVehiclesById.values());
        for (CustomerVehicle vehicleToCreate : vehiclesToCreate) {
            finalVehicles.add(copyCustomerVehicle(vehicleToCreate));
        }

        if (requestedDefaultVehicles.size() == 1) {
            UUID finalDefaultVehicleId = requestedDefaultVehicles.get(0).getCustomerVehicleId();
            List<CustomerVehicle> defaultVehicleCorrections = new ArrayList<>();
            for (CustomerVehicle existingVehicle : existingCustomerVehicles) {
                if (!Boolean.TRUE.equals(existingVehicle.getIsDefault())) {
                    continue;
                }
                if (existingVehicle.getCustomerVehicleId().equals(finalDefaultVehicleId)) {
                    continue;
                }

                CustomerVehicle correctedVehicle = copyCustomerVehicle(existingVehicle);
                customerVehiclePolicy.unmarkDefault(correctedVehicle);
                defaultVehicleCorrections.add(correctedVehicle);
            }
            return defaultVehicleCorrections;
        }

        List<CustomerVehicle> defaultVehicles = finalVehicles.stream()
                .filter(vehicle -> Boolean.TRUE.equals(vehicle.getIsDefault()))
                .toList();
        if (defaultVehicles.size() > 1) {
            throw new BadRequestException("Only one customer vehicle can be marked as default after applying the update");
        }

        if (defaultVehicles.isEmpty()) {
            return List.of();
        }

        UUID finalDefaultVehicleId = defaultVehicles.get(0).getCustomerVehicleId();
        List<CustomerVehicle> defaultVehicleCorrections = new ArrayList<>();
        for (CustomerVehicle existingVehicle : existingCustomerVehicles) {
            if (!Boolean.TRUE.equals(existingVehicle.getIsDefault())) {
                continue;
            }
            if (existingVehicle.getCustomerVehicleId().equals(finalDefaultVehicleId)) {
                continue;
            }

            CustomerVehicle projectedVehicle = finalExistingVehiclesById.get(existingVehicle.getCustomerVehicleId());
            if (projectedVehicle != null && !Boolean.TRUE.equals(projectedVehicle.getIsDefault())) {
                continue;
            }

            CustomerVehicle correctedVehicle = copyCustomerVehicle(existingVehicle);
            customerVehiclePolicy.unmarkDefault(correctedVehicle);
            defaultVehicleCorrections.add(correctedVehicle);
        }

        return defaultVehicleCorrections;
    }

    private CustomerVehicle resolveExistingVehicle(Map<UUID, CustomerVehicle> existingVehiclesById, UUID customerVehicleId) {
        if (customerVehicleId == null) {
            throw new BadRequestException("customerVehicleId must not be null");
        }
        CustomerVehicle existingVehicle = existingVehiclesById.get(customerVehicleId);
        if (existingVehicle == null) {
            throw new NotFoundException("Customer vehicle not found");
        }
        return existingVehicle;
    }

    private void ensureCreateCustomerIdMatchesBatch(UUID customerId, CustomerVehicle customerVehicle) {
        if (customerVehicle.getCustomerId() != null && !customerId.equals(customerVehicle.getCustomerId())) {
            throw new BadRequestException("Customer vehicle create customerId must match batch customerId");
        }
    }

    private void ensureVehicleBelongsToCustomer(UUID customerId, CustomerVehicle customerVehicle) {
        if (!customerId.equals(customerVehicle.getCustomerId())) {
            throw new BadRequestException("Customer vehicle does not belong to the requested customer");
        }
    }

    private void ensureNoDuplicateVehicleIds(List<UUID> customerVehicleIds, String actionName) {
        Set<UUID> uniqueCustomerVehicleIds = new HashSet<>();
        for (UUID customerVehicleId : customerVehicleIds) {
            if (customerVehicleId == null) {
                throw new BadRequestException("customerVehicleId must not be null in " + actionName + " operation");
            }
            if (!uniqueCustomerVehicleIds.add(customerVehicleId)) {
                throw new BadRequestException("Duplicate customerVehicleId found in " + actionName + " operation");
            }
        }
    }

    private void ensureNoVehicleIdActionOverlap(List<UUID> updateVehicleIds, List<UUID> inactivateVehicleIds) {
        Set<UUID> updateVehicleIdSet = new HashSet<>(updateVehicleIds);
        for (UUID inactivateVehicleId : inactivateVehicleIds) {
            if (updateVehicleIdSet.contains(inactivateVehicleId)) {
                throw new BadRequestException("A customer vehicle cannot be updated and inactivated in the same request");
            }
        }
    }

    private void ensureUniqueLicensePlateInRequest(Set<String> seenLicensePlates, String licensePlate) {
        if (!seenLicensePlates.add(licensePlate)) {
            throw new BadRequestException("Duplicate customer vehicle license plate found in request");
        }
    }

    private CustomerVehicle copyCustomerVehicle(CustomerVehicle customerVehicle) {
        return new CustomerVehicle(
                customerVehicle.getCustomerVehicleId(),
                customerVehicle.getCustomerId(),
                customerVehicle.getVehicleTypeId(),
                customerVehicle.getLicensePlate(),
                customerVehicle.getBrand(),
                customerVehicle.getColor(),
                customerVehicle.getIsDefault(),
                customerVehicle.getStatus()
        );
    }

    private List<CustomerVehicle> safeCreateVehicles(CustomerVehicleBatchCommand command) {
        if (command == null || command.createVehicles() == null) {
            return List.of();
        }
        return command.createVehicles();
    }

    private List<CustomerVehicle> safeUpdateVehicles(CustomerVehicleBatchCommand command) {
        if (command == null || command.updateVehicles() == null) {
            return List.of();
        }
        return command.updateVehicles();
    }

    private List<UUID> safeInactivateVehicleIds(CustomerVehicleBatchCommand command) {
        if (command == null || command.inactivateVehicleIds() == null) {
            return List.of();
        }
        return command.inactivateVehicleIds();
    }

    private void applySingleDefaultRule(CustomerVehicle customerVehicle) {
        if (!Boolean.TRUE.equals(customerVehicle.getIsDefault())) {
            return;
        }

        List<CustomerVehicle> defaultVehicles =
                customerVehiclePortOut.findDefaultVehiclesByCustomerId(customerVehicle.getCustomerId());

        for (CustomerVehicle existingDefaultVehicle : defaultVehicles) {
            if (existingDefaultVehicle.getCustomerVehicleId().equals(customerVehicle.getCustomerVehicleId())) {
                continue;
            }
            customerVehiclePolicy.unmarkDefault(existingDefaultVehicle);
            customerVehiclePortOut.save(existingDefaultVehicle);
        }
    }

    private record VehicleBatchPreparation(
            List<CustomerVehicle> vehiclesToCreate,
            List<CustomerVehicle> vehiclesToUpdate,
            List<CustomerVehicle> vehiclesToInactivate,
            List<CustomerVehicle> defaultVehicleCorrections
    ) {
    }
}

