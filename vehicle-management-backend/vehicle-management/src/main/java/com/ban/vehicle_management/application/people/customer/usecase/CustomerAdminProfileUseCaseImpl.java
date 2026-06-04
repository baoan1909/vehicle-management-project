package com.ban.vehicle_management.application.people.customer.usecase;

import com.ban.vehicle_management.application.people.customer.model.command.CreateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminVehicleDiffCommand;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import com.ban.vehicle_management.application.people.customer.port.in.CustomerAdminProfilePortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customer.policy.CustomerPolicy;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.people.customervehicle.policy.CustomerVehiclePolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.IdentifierGenerationUtils;
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
public class CustomerAdminProfileUseCaseImpl implements CustomerAdminProfilePortIn {

    private final UserProfilePortOut userProfilePortOut;
    private final CustomerPortOut customerPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();
    private final CustomerPolicy customerPolicy = new CustomerPolicy();
    private final CustomerVehiclePolicy customerVehiclePolicy = new CustomerVehiclePolicy();

    public CustomerAdminProfileUseCaseImpl(
            UserProfilePortOut userProfilePortOut,
            CustomerPortOut customerPortOut,
            CustomerVehiclePortOut customerVehiclePortOut
    ) {
        this.userProfilePortOut = userProfilePortOut;
        this.customerPortOut = customerPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
    }

    @Override
    @Transactional
    public CustomerAdminProfileResult createCustomerAdminProfile(CreateCustomerAdminProfileCommand command) {
        UserProfile userProfile = requireCreateUserProfile(command);
        Customer customer = command.customer() == null ? new Customer() : command.customer();
        List<CustomerVehicle> customerVehicles = safeCreateCustomerVehicles(command);

        userProfilePolicy.initialize(userProfile);
        validateUniqueUserProfile(userProfile);

        UUID userProfileId = UUID.randomUUID();
        userProfile.setUserProfileId(userProfileId);
        UserProfile savedUserProfile = userProfilePortOut.save(userProfile);

        customer.setUserProfileId(userProfileId);
        assignGeneratedCustomerIdentity(customer);
        customerPolicy.initialize(customer);
        Customer savedCustomer = customerPortOut.save(customer);

        List<CustomerVehicle> preparedCustomerVehicles = prepareCreateVehicles(savedCustomer.getCustomerId(), customerVehicles);
        for (CustomerVehicle customerVehicle : preparedCustomerVehicles) {
            customerVehiclePortOut.save(customerVehicle);
        }

        return buildResult(savedUserProfile, savedCustomer);
    }

    @Override
    @Transactional
    public CustomerAdminProfileResult updateCustomerAdminProfile(UUID customerId, UpdateCustomerAdminProfileCommand command) {
        ensureUpdatePayloadHasContent(command);

        Customer existingCustomer = customerPortOut.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        UserProfile existingUserProfile = userProfilePortOut.findById(existingCustomer.getUserProfileId())
                .orElseThrow(() -> new NotFoundException("User profile not found"));
        List<CustomerVehicle> existingCustomerVehicles =
                customerVehiclePortOut.findAll(existingCustomer.getCustomerId(), null, null, null, null);
        VehicleDiffPreparation preparedVehicleDiff = prepareVehicleDiff(existingCustomer.getCustomerId(), existingCustomerVehicles, command.vehicles());

        if (hasUserProfileChanges(command.userProfile())) {
            applyUserProfileChanges(existingUserProfile, command.userProfile());
            userProfilePolicy.validateState(existingUserProfile);
            validateUniqueUserProfile(existingUserProfile, existingUserProfile.getUserProfileId());
            existingUserProfile = userProfilePortOut.save(existingUserProfile);
        }

        if (hasCustomerChanges(command.customer())) {
            applyCustomerChanges(existingCustomer, command.customer());
            customerPolicy.validateState(existingCustomer);
            existingCustomer = customerPortOut.save(existingCustomer);
        }

        for (CustomerVehicle customerVehicleToInactivate : preparedVehicleDiff.vehiclesToInactivate()) {
            customerVehiclePortOut.save(customerVehicleToInactivate);
        }

        for (CustomerVehicle customerVehicleToUpdate : preparedVehicleDiff.vehiclesToUpdate()) {
            customerVehiclePortOut.save(customerVehicleToUpdate);
        }

        for (CustomerVehicle customerVehicleToCreate : preparedVehicleDiff.vehiclesToCreate()) {
            customerVehiclePortOut.save(customerVehicleToCreate);
        }

        for (CustomerVehicle defaultVehicleCorrection : preparedVehicleDiff.defaultVehicleCorrections()) {
            customerVehiclePortOut.save(defaultVehicleCorrection);
        }

        return buildResult(existingUserProfile, existingCustomer);
    }

    private UserProfile requireCreateUserProfile(CreateCustomerAdminProfileCommand command) {
        if (command == null || command.userProfile() == null) {
            throw new BadRequestException("userProfile must not be null");
        }
        return command.userProfile();
    }

    private void ensureUpdatePayloadHasContent(UpdateCustomerAdminProfileCommand command) {
        if (command == null || (!hasUserProfileChanges(command.userProfile())
                && !hasCustomerChanges(command.customer())
                && !hasVehicleDiffChanges(command.vehicles()))) {
            throw new BadRequestException("At least one profile field, customer field, or customer vehicle change must be provided");
        }
    }

    private void validateUniqueUserProfile(UserProfile userProfile) {
        if (userProfile.getPhoneNumber() != null && userProfilePortOut.existsByPhoneNumber(userProfile.getPhoneNumber())) {
            throw new ConflictException("User profile phone number already exists");
        }
        if (userProfile.getIdentifyCard() != null && userProfilePortOut.existsByIdentifyCard(userProfile.getIdentifyCard())) {
            throw new ConflictException("User profile identify card already exists");
        }
    }

    private void validateUniqueUserProfile(UserProfile userProfile, UUID userProfileId) {
        if (userProfile.getPhoneNumber() != null
                && userProfilePortOut.existsByPhoneNumberAndUserProfileIdNot(userProfile.getPhoneNumber(), userProfileId)) {
            throw new ConflictException("User profile phone number already exists");
        }
        if (userProfile.getIdentifyCard() != null
                && userProfilePortOut.existsByIdentifyCardAndUserProfileIdNot(userProfile.getIdentifyCard(), userProfileId)) {
            throw new ConflictException("User profile identify card already exists");
        }
    }

    private void assignGeneratedCustomerIdentity(Customer customer) {
        UUID generatedCustomerId = UUID.randomUUID();
        String generatedCustomerCode = IdentifierGenerationUtils.generateCustomerCode(generatedCustomerId);

        while (customerPortOut.existsByCustomerCode(generatedCustomerCode)) {
            generatedCustomerId = UUID.randomUUID();
            generatedCustomerCode = IdentifierGenerationUtils.generateCustomerCode(generatedCustomerId);
        }

        customer.setCustomerId(generatedCustomerId);
        customer.setCustomerCode(generatedCustomerCode);
    }

    private CustomerVehicle buildUpdatedCustomerVehicle(UUID customerId, CustomerVehicle existingCustomerVehicle, CustomerVehicle customerVehicle) {
        if (customerVehicle.getCustomerVehicleId() == null) {
            throw new BadRequestException("customerVehicleId must not be null when updating a customer vehicle");
        }

        if (!customerId.equals(existingCustomerVehicle.getCustomerId())) {
            throw new BadRequestException("Customer vehicle does not belong to the requested customer");
        }

        existingCustomerVehicle.setVehicleTypeId(customerVehicle.getVehicleTypeId());
        existingCustomerVehicle.setLicensePlate(customerVehicle.getLicensePlate());
        existingCustomerVehicle.setBrand(customerVehicle.getBrand());
        existingCustomerVehicle.setColor(customerVehicle.getColor());
        if (customerVehicle.getIsDefault() != null) {
            existingCustomerVehicle.setIsDefault(customerVehicle.getIsDefault());
        }

        customerVehiclePolicy.validateState(existingCustomerVehicle);
        validateVehicleTypeExists(existingCustomerVehicle.getVehicleTypeId());
        return existingCustomerVehicle;
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

    private void applyUserProfileChanges(UserProfile existingUserProfile, UserProfile updatedUserProfile) {
        existingUserProfile.setFullName(updatedUserProfile.getFullName());
        existingUserProfile.setDateOfBirth(updatedUserProfile.getDateOfBirth());
        existingUserProfile.setGender(updatedUserProfile.getGender());
        existingUserProfile.setPhoneNumber(updatedUserProfile.getPhoneNumber());
        existingUserProfile.setAddress(updatedUserProfile.getAddress());
        existingUserProfile.setIdentifyCard(updatedUserProfile.getIdentifyCard());
        existingUserProfile.setAvatarUrl(updatedUserProfile.getAvatarUrl());
        if (updatedUserProfile.getStatus() != null) {
            existingUserProfile.setStatus(updatedUserProfile.getStatus());
        }
    }

    private void applyCustomerChanges(Customer existingCustomer, Customer updatedCustomer) {
        if (updatedCustomer.getCustomerType() != null) {
            existingCustomer.setCustomerType(updatedCustomer.getCustomerType());
        }
    }

    private boolean hasUserProfileChanges(UserProfile userProfile) {
        return userProfile != null
                && (userProfile.getFullName() != null
                || userProfile.getDateOfBirth() != null
                || userProfile.getGender() != null
                || userProfile.getPhoneNumber() != null
                || userProfile.getAddress() != null
                || userProfile.getIdentifyCard() != null
                || userProfile.getAvatarUrl() != null
                || userProfile.getStatus() != null);
    }

    private boolean hasCustomerChanges(Customer customer) {
        return customer != null && customer.getCustomerType() != null;
    }

    private boolean hasVehicleDiffChanges(UpdateCustomerAdminVehicleDiffCommand vehicles) {
        return vehicles != null
                && (!safeVehicleCreates(vehicles).isEmpty()
                || !safeVehicleUpdates(vehicles).isEmpty()
                || !safeVehicleInactivations(vehicles).isEmpty());
    }

    private void validateVehicleTypeExists(UUID vehicleTypeId) {
        if (!customerVehiclePortOut.existsVehicleTypeById(vehicleTypeId)) {
            throw new NotFoundException("Vehicle type not found");
        }
    }

    private void validateUniqueLicensePlate(String licensePlate) {
        if (customerVehiclePortOut.existsByLicensePlate(licensePlate)) {
            throw new ConflictException("Customer vehicle license plate already exists");
        }
    }

    private List<CustomerVehicle> prepareCreateVehicles(UUID customerId, List<CustomerVehicle> requestedCustomerVehicles) {
        if (requestedCustomerVehicles.isEmpty()) {
            return List.of();
        }

        List<CustomerVehicle> preparedCustomerVehicles = new ArrayList<>();
        for (CustomerVehicle requestedCustomerVehicle : requestedCustomerVehicles) {
            requestedCustomerVehicle.setCustomerId(customerId);
            customerVehiclePolicy.initialize(requestedCustomerVehicle);
            validateVehicleTypeExists(requestedCustomerVehicle.getVehicleTypeId());
            requestedCustomerVehicle.setCustomerVehicleId(UUID.randomUUID());
            preparedCustomerVehicles.add(requestedCustomerVehicle);
        }

        validateVehicleLicensePlates(List.of(), List.of(), preparedCustomerVehicles);
        resolveDefaultVehicleCorrections(List.of(), List.of(), List.of(), preparedCustomerVehicles);
        return preparedCustomerVehicles;
    }

    private VehicleDiffPreparation prepareVehicleDiff(
            UUID customerId,
            List<CustomerVehicle> existingCustomerVehicles,
            UpdateCustomerAdminVehicleDiffCommand vehicles
    ) {
        if (vehicles == null) {
            return VehicleDiffPreparation.empty();
        }

        Map<UUID, CustomerVehicle> existingVehiclesById = new HashMap<>();
        for (CustomerVehicle existingVehicle : existingCustomerVehicles) {
            existingVehiclesById.put(existingVehicle.getCustomerVehicleId(), copyCustomerVehicle(existingVehicle));
        }

        ensureNoDuplicateVehicleIds(safeVehicleInactivations(vehicles), "inactivate");
        ensureNoDuplicateVehicleIds(
                safeVehicleUpdates(vehicles).stream()
                        .map(CustomerVehicle::getCustomerVehicleId)
                        .toList(),
                "update"
        );
        ensureNoVehicleIdActionOverlap(
                safeVehicleUpdates(vehicles).stream().map(CustomerVehicle::getCustomerVehicleId).toList(),
                safeVehicleInactivations(vehicles)
        );

        List<CustomerVehicle> preparedVehiclesToInactivate = new ArrayList<>();
        for (UUID customerVehicleId : safeVehicleInactivations(vehicles)) {
            CustomerVehicle customerVehicle = copyCustomerVehicle(resolveExistingVehicle(existingVehiclesById, customerVehicleId));
            customerVehiclePolicy.inactivate(customerVehicle);
            preparedVehiclesToInactivate.add(customerVehicle);
        }

        List<CustomerVehicle> preparedVehiclesToUpdate = new ArrayList<>();
        for (CustomerVehicle requestedVehicleUpdate : safeVehicleUpdates(vehicles)) {
            CustomerVehicle existingVehicle = copyCustomerVehicle(resolveExistingVehicle(existingVehiclesById, requestedVehicleUpdate.getCustomerVehicleId()));
            preparedVehiclesToUpdate.add(buildUpdatedCustomerVehicle(customerId, existingVehicle, requestedVehicleUpdate));
        }

        List<CustomerVehicle> preparedVehiclesToCreate = new ArrayList<>();
        for (CustomerVehicle requestedVehicleCreate : safeVehicleCreates(vehicles)) {
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

        return new VehicleDiffPreparation(
                preparedVehiclesToCreate,
                preparedVehiclesToUpdate,
                preparedVehiclesToInactivate,
                defaultVehicleCorrections
        );
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

    private List<CustomerVehicle> safeVehicleCreates(UpdateCustomerAdminVehicleDiffCommand vehicles) {
        if (vehicles == null || vehicles.createVehicles() == null) {
            return List.of();
        }
        return vehicles.createVehicles();
    }

    private List<CustomerVehicle> safeVehicleUpdates(UpdateCustomerAdminVehicleDiffCommand vehicles) {
        if (vehicles == null || vehicles.updateVehicles() == null) {
            return List.of();
        }
        return vehicles.updateVehicles();
    }

    private List<UUID> safeVehicleInactivations(UpdateCustomerAdminVehicleDiffCommand vehicles) {
        if (vehicles == null || vehicles.inactivateVehicleIds() == null) {
            return List.of();
        }
        return vehicles.inactivateVehicleIds();
    }

    private List<CustomerVehicle> safeCreateCustomerVehicles(CreateCustomerAdminProfileCommand command) {
        if (command == null || command.customerVehicles() == null) {
            return List.of();
        }
        return command.customerVehicles();
    }

    private CustomerAdminProfileResult buildResult(UserProfile userProfile, Customer customer) {
        List<CustomerVehicle> customerVehicles =
                customerVehiclePortOut.findAll(customer.getCustomerId(), null, null, null, null);
        return new CustomerAdminProfileResult(userProfile, customer, customerVehicles);
    }

    private record VehicleDiffPreparation(
            List<CustomerVehicle> vehiclesToCreate,
            List<CustomerVehicle> vehiclesToUpdate,
            List<CustomerVehicle> vehiclesToInactivate,
            List<CustomerVehicle> defaultVehicleCorrections
    ) {
        private static VehicleDiffPreparation empty() {
            return new VehicleDiffPreparation(List.of(), List.of(), List.of(), List.of());
        }
    }
}
