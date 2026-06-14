$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$root = Split-Path -Parent $PSScriptRoot

function Write-JavaFile {
    param(
        [string]$RelativePath,
        [string]$Content
    )

    $fullPath = Join-Path $root $RelativePath
    $directory = Split-Path -Parent $fullPath
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    [System.IO.File]::WriteAllText($fullPath, $Content.TrimStart("`n", "`r") + "`n", $utf8)
}

function Get-TypeImports {
    param(
        [string]$Type,
        [string[]]$EnumNames
    )

    if ($Type -like 'Map<*') {
        return @('java.util.Map')
    }

    switch ($Type) {
        'UUID' { return @('java.util.UUID') }
        'Instant' { return @('java.time.Instant') }
        'LocalDate' { return @('java.time.LocalDate') }
        'LocalTime' { return @('java.time.LocalTime') }
        'BigDecimal' { return @('java.math.BigDecimal') }
        default {
            if ($EnumNames -contains $Type) {
                return @("com.ban.vehicle_management.shared.enumeration.$Type")
            }
            return @()
        }
    }
}

function Get-AccessorSuffix {
    param([string]$Name)
    return $Name.Substring(0, 1).ToUpper() + $Name.Substring(1)
}

function Get-DomainBaseClass {
    param([string]$AuditKind)
    switch ($AuditKind) {
        'AUDITABLE' { return 'AuditableDomainModel' }
        default { return $null }
    }
}

function Get-EntityBaseClass {
    param([string]$AuditKind)
    switch ($AuditKind) {
        'AUDITABLE' { return 'AuditableEntity' }
        default { return $null }
    }
}

function Get-ExtraAuditFields {
    param([string]$AuditKind)

    switch ($AuditKind) {
        'CREATED_AUDIT' {
            return @(
                @{ Name = 'createdAt'; Type = 'Instant'; Column = 'created_at'; Nullable = $false },
                @{ Name = 'createdBy'; Type = 'UUID'; Column = 'created_by' }
            )
        }
        'CREATED_AT' {
            return @(
                @{ Name = 'createdAt'; Type = 'Instant'; Column = 'created_at'; Nullable = $false }
            )
        }
        default {
            return @()
        }
    }
}

function Render-DomainClass {
    param(
        [hashtable]$Table,
        [string[]]$EnumNames
    )

    $packageName = "com.ban.vehicle_management.domain.$($Table.SchemaPackage).$($Table.FeaturePackage).model"
    $imports = New-Object System.Collections.Generic.HashSet[string]
    $baseClass = Get-DomainBaseClass $Table.AuditKind

    if ($baseClass) {
        $imports.Add("com.ban.vehicle_management.domain.common.model.$baseClass") | Out-Null
    }

    foreach ($field in $Table.Fields) {
        foreach ($import in (Get-TypeImports $field.Type $EnumNames)) {
            $imports.Add($import) | Out-Null
        }
    }
    foreach ($field in (Get-ExtraAuditFields $Table.AuditKind)) {
        foreach ($import in (Get-TypeImports $field.Type $EnumNames)) {
            $imports.Add($import) | Out-Null
        }
    }
    $imports.Add('lombok.AllArgsConstructor') | Out-Null
    $imports.Add('lombok.Getter') | Out-Null
    $imports.Add('lombok.NoArgsConstructor') | Out-Null
    $imports.Add('lombok.Setter') | Out-Null

    $extraAuditFields = @(Get-ExtraAuditFields $Table.AuditKind)
    $content = @()
    $content += "package $packageName;"
    $content += ''
    foreach ($import in ($imports | Sort-Object)) {
        $content += "import $import;"
    }
    if ($imports.Count -gt 0) {
        $content += ''
    }

    $extendsClause = if ($baseClass) { " extends $baseClass" } else { '' }
    $content += '@Getter'
    $content += '@Setter'
    $content += '@NoArgsConstructor'
    $content += '@AllArgsConstructor'
    $content += "public class $($Table.ClassName)$extendsClause {"
    $content += ''

    foreach ($field in $extraAuditFields) {
        $content += "    private $($field.Type) $($field.Name);"
    }
    if ($extraAuditFields.Count -gt 0 -and $Table.Fields.Count -gt 0) {
        $content += ''
    }
    foreach ($field in $Table.Fields) {
        $content += "    private $($field.Type) $($field.Name);"
    }

    $content += '}'

    Write-JavaFile "src/main/java/com/ban/vehicle_management/domain/$($Table.SchemaPackage)/$($Table.FeaturePackage)/model/$($Table.ClassName).java" ($content -join "`n")
}

function Render-EntityClass {
    param(
        [hashtable]$Table,
        [string[]]$EnumNames
    )

    $packageName = "com.ban.vehicle_management.infrastructure.persistence.$($Table.SchemaPackage).$($Table.FeaturePackage)"
    $imports = New-Object System.Collections.Generic.HashSet[string]
    $baseClass = Get-EntityBaseClass $Table.AuditKind

    $imports.Add('jakarta.persistence.Column') | Out-Null
    $imports.Add('jakarta.persistence.Entity') | Out-Null
    $imports.Add('jakarta.persistence.Id') | Out-Null
    $imports.Add('jakarta.persistence.Table') | Out-Null
    $imports.Add('lombok.AllArgsConstructor') | Out-Null
    $imports.Add('lombok.Getter') | Out-Null
    $imports.Add('lombok.NoArgsConstructor') | Out-Null
    $imports.Add('lombok.Setter') | Out-Null

    if ($baseClass) {
        $imports.Add("com.ban.vehicle_management.infrastructure.persistence.common.entity.$baseClass") | Out-Null
    }

    $hasJson = $false
    $hasEnum = $false
    $entityFields = @()
    $entityFields += @(Get-ExtraAuditFields $Table.AuditKind)
    $entityFields += $Table.Fields
    foreach ($field in $entityFields) {
        foreach ($import in (Get-TypeImports $field.Type $EnumNames)) {
            $imports.Add($import) | Out-Null
        }
        if ($field.ContainsKey('Json') -and $field.Json) {
            $hasJson = $true
        }
        if ($EnumNames -contains $field.Type) {
            $hasEnum = $true
        }
    }

    if ($hasJson) {
        $imports.Add('org.hibernate.annotations.JdbcTypeCode') | Out-Null
        $imports.Add('org.hibernate.type.SqlTypes') | Out-Null
    }
    if ($hasEnum) {
        $imports.Add('jakarta.persistence.EnumType') | Out-Null
        $imports.Add('jakarta.persistence.Enumerated') | Out-Null
    }

    $content = @()
    $content += "package $packageName;"
    $content += ''
    foreach ($import in ($imports | Sort-Object)) {
        $content += "import $import;"
    }
    $content += ''
    $content += '@Entity'
    $content += "@Table(name = `"$($Table.TableName)`", schema = `"$($Table.TableSchema)`")"
    $content += '@Getter'
    $content += '@Setter'
    $content += '@NoArgsConstructor'
    $content += '@AllArgsConstructor'

    $extendsClause = if ($baseClass) { " extends $baseClass" } else { '' }
    $content += "public class $($Table.ClassName)Entity$extendsClause {"
    $content += ''

    foreach ($field in $entityFields) {
        if ($field.ContainsKey('Id') -and $field.Id) {
            $content += '    @Id'
        }
        if ($field.ContainsKey('Json') -and $field.Json) {
            $content += '    @JdbcTypeCode(SqlTypes.JSON)'
        }
        if ($EnumNames -contains $field.Type) {
            $content += '    @Enumerated(EnumType.STRING)'
        }

        $columnArgs = New-Object System.Collections.Generic.List[string]
        $columnArgs.Add("name = `"$($field.Column)`"")
        if ($field.ContainsKey('Nullable') -and (-not $field.Nullable)) {
            $columnArgs.Add('nullable = false')
        }
        if ($field.ContainsKey('Unique') -and $field.Unique) {
            $columnArgs.Add('unique = true')
        }
        if ($field.ContainsKey('Precision')) {
            $columnArgs.Add("precision = $($field.Precision)")
        }
        if ($field.ContainsKey('Scale')) {
            $columnArgs.Add("scale = $($field.Scale)")
        }
        if ($field.ContainsKey('ColumnDefinition')) {
            $columnArgs.Add("columnDefinition = `"$($field.ColumnDefinition)`"")
        }
        $content += "    @Column($($columnArgs -join ', '))"
        $content += "    private $($field.Type) $($field.Name);"
        $content += ''
    }

    $content += '}'

    Write-JavaFile "src/main/java/com/ban/vehicle_management/infrastructure/persistence/$($Table.SchemaPackage)/$($Table.FeaturePackage)/$($Table.ClassName)Entity.java" ($content -join "`n")
}

function Render-Repository {
    param([hashtable]$Table)

    $packageName = "com.ban.vehicle_management.infrastructure.persistence.$($Table.SchemaPackage).$($Table.FeaturePackage)"
    $className = "$($Table.ClassName)Repository"
    $entityName = "$($Table.ClassName)Entity"

    $content = @()
    $content += "package $packageName;"
    $content += ''
    $content += 'import java.util.UUID;'
    $content += 'import org.springframework.data.jpa.repository.JpaRepository;'
    $content += ''
    $content += "public interface $className extends JpaRepository<$entityName, UUID> {"
    $content += '}'

    Write-JavaFile "src/main/java/com/ban/vehicle_management/infrastructure/persistence/$($Table.SchemaPackage)/$($Table.FeaturePackage)/$className.java" ($content -join "`n")
}

function Render-Mapper {
    param([hashtable]$Table)

    $packageName = "com.ban.vehicle_management.infrastructure.mapper.$($Table.SchemaPackage).$($Table.FeaturePackage)"
    $className = "$($Table.ClassName)PersistenceMapper"
    $domainName = $Table.ClassName
    $entityName = "$($Table.ClassName)Entity"

    $content = @()
    $content += "package $packageName;"
    $content += ''
    $content += "import com.ban.vehicle_management.domain.$($Table.SchemaPackage).$($Table.FeaturePackage).model.$domainName;"
    $content += "import com.ban.vehicle_management.infrastructure.persistence.$($Table.SchemaPackage).$($Table.FeaturePackage).$entityName;"
    $content += 'import org.mapstruct.Mapper;'
    $content += ''
    $content += '@Mapper(componentModel = "spring")'
    $content += "public interface $className {"
    $content += ''
    $content += "    $entityName toEntity($domainName domain);"
    $content += ''
    $content += "    $domainName toDomain($entityName entity);"
    $content += '}'

    Write-JavaFile "src/main/java/com/ban/vehicle_management/infrastructure/mapper/$($Table.SchemaPackage)/$($Table.FeaturePackage)/$className.java" ($content -join "`n")
}

$enumDefinitions = @(
    @{ Name = 'AccountStatus'; Values = @('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING') },
    @{ Name = 'UserProfileStatus'; Values = @('ACTIVE', 'INACTIVE', 'SUSPENDED') },
    @{ Name = 'CustomerType'; Values = @('REGISTERED', 'VIP') },
    @{ Name = 'CustomerApprovalStatus'; Values = @('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED') },
    @{ Name = 'EmployeeStatus'; Values = @('ACTIVE', 'INACTIVE', 'SUSPENDED') },
    @{ Name = 'CustomerVehicleStatus'; Values = @('ACTIVE', 'INACTIVE', 'BLOCKED') },
    @{ Name = 'PricePlanAppliesTo'; Values = @('VISITOR', 'CUSTOMER', 'ALL') },
    @{ Name = 'PriceRuleUnit'; Values = @('TURN', 'DAY', 'MONTH') },
    @{ Name = 'CardStatus'; Values = @('AVAILABLE', 'ASSIGNED', 'IN_USE', 'LOST', 'BLOCKED', 'DAMAGED', 'RETIRED') },
    @{ Name = 'SubscriptionStatus'; Values = @('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED', 'REJECTED') },
    @{ Name = 'LostCardReportStatus'; Values = @('OPEN', 'RESOLVED', 'CANCELLED') },
    @{ Name = 'ParkingLotStatus'; Values = @('ACTIVE', 'MAINTENANCE', 'CLOSED') },
    @{ Name = 'ParkingSpaceStatus'; Values = @('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE') },
    @{ Name = 'LaneDirection'; Values = @('IN', 'OUT', 'BOTH') },
    @{ Name = 'LaneStatus'; Values = @('ACTIVE', 'MAINTENANCE', 'CLOSED') },
    @{ Name = 'ParkingSessionStatus'; Values = @('OPEN', 'CLOSED', 'LOST_CARD', 'CANCELLED') },
    @{ Name = 'ParkingEventType'; Values = @('CHECK_IN', 'CHECK_OUT', 'MANUAL_REVIEW', 'BARRIER_OPEN') },
    @{ Name = 'InvoiceStatus'; Values = @('UNPAID', 'PAID', 'CANCELLED', 'REFUNDED') },
    @{ Name = 'PaymentMethod'; Values = @('CASH', 'QR', 'BANK_TRANSFER', 'MOMO', 'VNPAY') },
    @{ Name = 'PaymentStatus'; Values = @('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') },
    @{ Name = 'ShiftStatus'; Values = @('OPEN', 'CLOSED', 'CANCELLED') },
    @{ Name = 'ApprovalRequestStatus'; Values = @('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') },
    @{ Name = 'SupportTicketStatus'; Values = @('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') },
    @{ Name = 'SupportTicketPriority'; Values = @('LOW', 'NORMAL', 'HIGH', 'URGENT') },
    @{ Name = 'DeviceType'; Values = @('CAMERA', 'KIOSK', 'CARD_READER', 'BARRIER') },
    @{ Name = 'DeviceStatus'; Values = @('ACTIVE', 'OFFLINE', 'MAINTENANCE', 'RETIRED') },
    @{ Name = 'NotificationChannel'; Values = @('WEB', 'EMAIL', 'PUSH', 'SMS') },
    @{ Name = 'NotificationStatus'; Values = @('PENDING', 'SENT', 'READ', 'FAILED') }
)

$enumNames = $enumDefinitions | ForEach-Object { $_.Name }

foreach ($enumDefinition in $enumDefinitions) {
    $content = @()
    $content += 'package com.ban.vehicle_management.shared.enumeration;'
    $content += ''
    $content += "public enum $($enumDefinition.Name) {"
    $content += "    $($enumDefinition.Values -join ', ')"
    $content += '}'
    Write-JavaFile "src/main/java/com/ban/vehicle_management/shared/enumeration/$($enumDefinition.Name).java" ($content -join "`n")
}

Write-JavaFile 'src/main/java/com/ban/vehicle_management/domain/common/model/AuditableDomainModel.java' @'
package com.ban.vehicle_management.domain.common.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditableDomainModel {

    private Instant createdAt;
    private UUID createdBy;

    private Instant updatedAt;
    private UUID updatedBy;
}
'@

Write-JavaFile 'src/main/java/com/ban/vehicle_management/infrastructure/persistence/common/entity/AuditableEntity.java' @'
package com.ban.vehicle_management.infrastructure.persistence.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
'@

$tables = @(
    @{ SchemaPackage = 'iam'; FeaturePackage = 'role'; ClassName = 'Role'; TableSchema = 'iam'; TableName = 'roles'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'roleId'; Type = 'UUID'; Column = 'role_id'; Id = $true; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false; Unique = $true },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'description'; Type = 'String'; Column = 'description' },
        @{ Name = 'isSystem'; Type = 'Boolean'; Column = 'is_system'; Nullable = $false },
        @{ Name = 'isActive'; Type = 'Boolean'; Column = 'is_active'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'iam'; FeaturePackage = 'permission'; ClassName = 'Permission'; TableSchema = 'iam'; TableName = 'permissions'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'permissionId'; Type = 'UUID'; Column = 'permission_id'; Id = $true; Nullable = $false },
        @{ Name = 'permissionCode'; Type = 'String'; Column = 'permission_code'; Nullable = $false; Unique = $true },
        @{ Name = 'module'; Type = 'String'; Column = 'module'; Nullable = $false },
        @{ Name = 'action'; Type = 'String'; Column = 'action'; Nullable = $false },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'description'; Type = 'String'; Column = 'description' }
    ) },
    @{ SchemaPackage = 'people'; FeaturePackage = 'userprofile'; ClassName = 'UserProfile'; TableSchema = 'people'; TableName = 'user_profiles'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'userProfileId'; Type = 'UUID'; Column = 'user_profile_id'; Id = $true; Nullable = $false },
        @{ Name = 'fullName'; Type = 'String'; Column = 'full_name'; Nullable = $false },
        @{ Name = 'dateOfBirth'; Type = 'LocalDate'; Column = 'date_of_birth' },
        @{ Name = 'gender'; Type = 'String'; Column = 'gender' },
        @{ Name = 'phoneNumber'; Type = 'String'; Column = 'phone_number'; Unique = $true },
        @{ Name = 'address'; Type = 'String'; Column = 'address' },
        @{ Name = 'identifyCard'; Type = 'String'; Column = 'identify_card'; Unique = $true },
        @{ Name = 'avatarUrl'; Type = 'String'; Column = 'avatar_url' },
        @{ Name = 'status'; Type = 'UserProfileStatus'; Column = 'status'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'iam'; FeaturePackage = 'account'; ClassName = 'Account'; TableSchema = 'iam'; TableName = 'accounts'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'accountId'; Type = 'UUID'; Column = 'account_id'; Id = $true; Nullable = $false },
        @{ Name = 'userProfileId'; Type = 'UUID'; Column = 'user_profile_id'; Nullable = $false; Unique = $true },
        @{ Name = 'username'; Type = 'String'; Column = 'username'; Nullable = $false; Unique = $true },
        @{ Name = 'email'; Type = 'String'; Column = 'email'; Nullable = $false; Unique = $true; ColumnDefinition = 'citext' },
        @{ Name = 'roleId'; Type = 'UUID'; Column = 'role_id'; Nullable = $false },
        @{ Name = 'status'; Type = 'AccountStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'lastLoginAt'; Type = 'Instant'; Column = 'last_login_at' },
        @{ Name = 'failedLoginCount'; Type = 'Integer'; Column = 'failed_login_count'; Nullable = $false },
        @{ Name = 'lockedUntil'; Type = 'Instant'; Column = 'locked_until' },
        @{ Name = 'passwordChangedAt'; Type = 'Instant'; Column = 'password_changed_at' }
    ) },
    @{ SchemaPackage = 'iam'; FeaturePackage = 'role'; ClassName = 'RolePermission'; TableSchema = 'iam'; TableName = 'role_permissions'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'id'; Type = 'UUID'; Column = 'id'; Id = $true; Nullable = $false },
        @{ Name = 'roleId'; Type = 'UUID'; Column = 'role_id'; Nullable = $false },
        @{ Name = 'permissionId'; Type = 'UUID'; Column = 'permission_id'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'iam'; FeaturePackage = 'account'; ClassName = 'AccountStatusHistory'; TableSchema = 'iam'; TableName = 'account_status_history'; AuditKind = 'NONE'; Fields = @(
        @{ Name = 'accountStatusHistoryId'; Type = 'UUID'; Column = 'account_status_history_id'; Id = $true; Nullable = $false },
        @{ Name = 'accountId'; Type = 'UUID'; Column = 'account_id'; Nullable = $false },
        @{ Name = 'oldStatus'; Type = 'AccountStatus'; Column = 'old_status' },
        @{ Name = 'newStatus'; Type = 'AccountStatus'; Column = 'new_status'; Nullable = $false },
        @{ Name = 'reason'; Type = 'String'; Column = 'reason' },
        @{ Name = 'changedAt'; Type = 'Instant'; Column = 'changed_at'; Nullable = $false },
        @{ Name = 'changedBy'; Type = 'UUID'; Column = 'changed_by' }
    ) },
    @{ SchemaPackage = 'people'; FeaturePackage = 'customer'; ClassName = 'Customer'; TableSchema = 'people'; TableName = 'customers'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'customerId'; Type = 'UUID'; Column = 'customer_id'; Id = $true; Nullable = $false },
        @{ Name = 'userProfileId'; Type = 'UUID'; Column = 'user_profile_id'; Nullable = $false; Unique = $true },
        @{ Name = 'customerCode'; Type = 'String'; Column = 'customer_code'; Nullable = $false; Unique = $true },
        @{ Name = 'customerType'; Type = 'CustomerType'; Column = 'customer_type'; Nullable = $false },
        @{ Name = 'approvalStatus'; Type = 'CustomerApprovalStatus'; Column = 'approval_status'; Nullable = $false },
        @{ Name = 'approvedBy'; Type = 'UUID'; Column = 'approved_by' },
        @{ Name = 'approvedAt'; Type = 'Instant'; Column = 'approved_at' }
    ) },
    @{ SchemaPackage = 'people'; FeaturePackage = 'employee'; ClassName = 'Employee'; TableSchema = 'people'; TableName = 'employees'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'employeeId'; Type = 'UUID'; Column = 'employee_id'; Id = $true; Nullable = $false },
        @{ Name = 'userProfileId'; Type = 'UUID'; Column = 'user_profile_id'; Nullable = $false; Unique = $true },
        @{ Name = 'employeeCode'; Type = 'String'; Column = 'employee_code'; Nullable = $false; Unique = $true },
        @{ Name = 'jobTitle'; Type = 'String'; Column = 'job_title' },
        @{ Name = 'hiredAt'; Type = 'LocalDate'; Column = 'hired_at' },
        @{ Name = 'status'; Type = 'EmployeeStatus'; Column = 'status'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'people'; FeaturePackage = 'customervehicle'; ClassName = 'CustomerVehicle'; TableSchema = 'people'; TableName = 'customer_vehicles'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'customerVehicleId'; Type = 'UUID'; Column = 'customer_vehicle_id'; Id = $true; Nullable = $false },
        @{ Name = 'customerId'; Type = 'UUID'; Column = 'customer_id'; Nullable = $false },
        @{ Name = 'vehicleTypeId'; Type = 'UUID'; Column = 'vehicle_type_id'; Nullable = $false },
        @{ Name = 'licensePlate'; Type = 'String'; Column = 'license_plate'; Nullable = $false; Unique = $true },
        @{ Name = 'brand'; Type = 'String'; Column = 'brand' },
        @{ Name = 'color'; Type = 'String'; Column = 'color' },
        @{ Name = 'isDefault'; Type = 'Boolean'; Column = 'is_default'; Nullable = $false },
        @{ Name = 'status'; Type = 'CustomerVehicleStatus'; Column = 'status'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'catalog'; FeaturePackage = 'vehicletype'; ClassName = 'VehicleType'; TableSchema = 'catalog'; TableName = 'vehicle_types'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'vehicleTypeId'; Type = 'UUID'; Column = 'vehicle_type_id'; Id = $true; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false; Unique = $true },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'description'; Type = 'String'; Column = 'description' },
        @{ Name = 'isActive'; Type = 'Boolean'; Column = 'is_active'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'catalog'; FeaturePackage = 'tickettype'; ClassName = 'TicketType'; TableSchema = 'catalog'; TableName = 'ticket_types'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'ticketTypeId'; Type = 'UUID'; Column = 'ticket_type_id'; Id = $true; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false; Unique = $true },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'description'; Type = 'String'; Column = 'description' },
        @{ Name = 'durationDays'; Type = 'Integer'; Column = 'duration_days' },
        @{ Name = 'isActive'; Type = 'Boolean'; Column = 'is_active'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'catalog'; FeaturePackage = 'cardtype'; ClassName = 'CardType'; TableSchema = 'catalog'; TableName = 'card_types'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'cardTypeId'; Type = 'UUID'; Column = 'card_type_id'; Id = $true; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false; Unique = $true },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'description'; Type = 'String'; Column = 'description' },
        @{ Name = 'isReturnRequired'; Type = 'Boolean'; Column = 'is_return_required'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'catalog'; FeaturePackage = 'priceplan'; ClassName = 'PricePlan'; TableSchema = 'catalog'; TableName = 'price_plans'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'pricePlanId'; Type = 'UUID'; Column = 'price_plan_id'; Id = $true; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false; Unique = $true },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'description'; Type = 'String'; Column = 'description' },
        @{ Name = 'appliesTo'; Type = 'PricePlanAppliesTo'; Column = 'applies_to'; Nullable = $false },
        @{ Name = 'effectiveFrom'; Type = 'LocalDate'; Column = 'effective_from'; Nullable = $false },
        @{ Name = 'effectiveTo'; Type = 'LocalDate'; Column = 'effective_to' },
        @{ Name = 'isActive'; Type = 'Boolean'; Column = 'is_active'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'catalog'; FeaturePackage = 'pricerule'; ClassName = 'PriceRule'; TableSchema = 'catalog'; TableName = 'price_rules'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'priceRuleId'; Type = 'UUID'; Column = 'price_rule_id'; Id = $true; Nullable = $false },
        @{ Name = 'pricePlanId'; Type = 'UUID'; Column = 'price_plan_id'; Nullable = $false },
        @{ Name = 'vehicleTypeId'; Type = 'UUID'; Column = 'vehicle_type_id'; Nullable = $false },
        @{ Name = 'ticketTypeId'; Type = 'UUID'; Column = 'ticket_type_id' },
        @{ Name = 'ruleName'; Type = 'String'; Column = 'rule_name'; Nullable = $false },
        @{ Name = 'timeFrom'; Type = 'LocalTime'; Column = 'time_from' },
        @{ Name = 'timeTo'; Type = 'LocalTime'; Column = 'time_to' },
        @{ Name = 'basePrice'; Type = 'BigDecimal'; Column = 'base_price'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'unit'; Type = 'PriceRuleUnit'; Column = 'unit'; Nullable = $false },
        @{ Name = 'lostCardFee'; Type = 'BigDecimal'; Column = 'lost_card_fee'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'priority'; Type = 'Integer'; Column = 'priority'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'catalog'; FeaturePackage = 'holidaycalendar'; ClassName = 'HolidayCalendar'; TableSchema = 'catalog'; TableName = 'holiday_calendar'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'holidayId'; Type = 'UUID'; Column = 'holiday_id'; Id = $true; Nullable = $false },
        @{ Name = 'holidayDate'; Type = 'LocalDate'; Column = 'holiday_date'; Nullable = $false; Unique = $true },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'priceMultiplier'; Type = 'BigDecimal'; Column = 'price_multiplier'; Nullable = $false; Precision = 5; Scale = 2 }
    ) },
    @{ SchemaPackage = 'accesscontrol'; FeaturePackage = 'card'; ClassName = 'Card'; TableSchema = 'access_control'; TableName = 'cards'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'cardId'; Type = 'UUID'; Column = 'card_id'; Id = $true; Nullable = $false },
        @{ Name = 'cardNumber'; Type = 'String'; Column = 'card_number'; Nullable = $false; Unique = $true },
        @{ Name = 'uid'; Type = 'String'; Column = 'uid'; Nullable = $false; Unique = $true },
        @{ Name = 'cardTypeId'; Type = 'UUID'; Column = 'card_type_id'; Nullable = $false },
        @{ Name = 'vehicleTypeId'; Type = 'UUID'; Column = 'vehicle_type_id' },
        @{ Name = 'status'; Type = 'CardStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'issuedAt'; Type = 'Instant'; Column = 'issued_at' },
        @{ Name = 'blockedAt'; Type = 'Instant'; Column = 'blocked_at' },
        @{ Name = 'blockedReason'; Type = 'String'; Column = 'blocked_reason' }
    ) },
    @{ SchemaPackage = 'accesscontrol'; FeaturePackage = 'subscription'; ClassName = 'Subscription'; TableSchema = 'access_control'; TableName = 'subscriptions'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'subscriptionId'; Type = 'UUID'; Column = 'subscription_id'; Id = $true; Nullable = $false },
        @{ Name = 'customerId'; Type = 'UUID'; Column = 'customer_id'; Nullable = $false },
        @{ Name = 'customerVehicleId'; Type = 'UUID'; Column = 'customer_vehicle_id'; Nullable = $false },
        @{ Name = 'cardId'; Type = 'UUID'; Column = 'card_id' },
        @{ Name = 'ticketTypeId'; Type = 'UUID'; Column = 'ticket_type_id'; Nullable = $false },
        @{ Name = 'priceRuleId'; Type = 'UUID'; Column = 'price_rule_id' },
        @{ Name = 'effectiveFrom'; Type = 'LocalDate'; Column = 'effective_from'; Nullable = $false },
        @{ Name = 'effectiveTo'; Type = 'LocalDate'; Column = 'effective_to'; Nullable = $false },
        @{ Name = 'price'; Type = 'BigDecimal'; Column = 'price'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'status'; Type = 'SubscriptionStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'approvedBy'; Type = 'UUID'; Column = 'approved_by' },
        @{ Name = 'approvedAt'; Type = 'Instant'; Column = 'approved_at' },
        @{ Name = 'cardReceiptDate'; Type = 'LocalDate'; Column = 'card_receipt_date' }
    ) },
    @{ SchemaPackage = 'accesscontrol'; FeaturePackage = 'lostcardreport'; ClassName = 'LostCardReport'; TableSchema = 'access_control'; TableName = 'lost_card_reports'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'lostCardReportId'; Type = 'UUID'; Column = 'lost_card_report_id'; Id = $true; Nullable = $false },
        @{ Name = 'cardId'; Type = 'UUID'; Column = 'card_id'; Nullable = $false },
        @{ Name = 'customerId'; Type = 'UUID'; Column = 'customer_id' },
        @{ Name = 'parkingSessionId'; Type = 'UUID'; Column = 'parking_session_id' },
        @{ Name = 'notificationTime'; Type = 'Instant'; Column = 'notification_time'; Nullable = $false },
        @{ Name = 'timeOfLost'; Type = 'Instant'; Column = 'time_of_lost'; Nullable = $false },
        @{ Name = 'ticketPrice'; Type = 'BigDecimal'; Column = 'ticket_price'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'lostCardFee'; Type = 'BigDecimal'; Column = 'lost_card_fee'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'reporterName'; Type = 'String'; Column = 'reporter_name' },
        @{ Name = 'reporterPhone'; Type = 'String'; Column = 'reporter_phone' },
        @{ Name = 'identifyCard'; Type = 'String'; Column = 'identify_card' },
        @{ Name = 'registrationLicense'; Type = 'String'; Column = 'registration_license' },
        @{ Name = 'note'; Type = 'String'; Column = 'note' },
        @{ Name = 'status'; Type = 'LostCardReportStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'resolvedBy'; Type = 'UUID'; Column = 'resolved_by' },
        @{ Name = 'resolvedAt'; Type = 'Instant'; Column = 'resolved_at' }
    ) },
    @{ SchemaPackage = 'parking'; FeaturePackage = 'parkinglot'; ClassName = 'ParkingLot'; TableSchema = 'parking'; TableName = 'parking_lots'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'parkingLotId'; Type = 'UUID'; Column = 'parking_lot_id'; Id = $true; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false; Unique = $true },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'address'; Type = 'String'; Column = 'address' },
        @{ Name = 'totalCapacity'; Type = 'Integer'; Column = 'total_capacity'; Nullable = $false },
        @{ Name = 'status'; Type = 'ParkingLotStatus'; Column = 'status'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'parking'; FeaturePackage = 'zone'; ClassName = 'Zone'; TableSchema = 'parking'; TableName = 'zones'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'zoneId'; Type = 'UUID'; Column = 'zone_id'; Id = $true; Nullable = $false },
        @{ Name = 'parkingLotId'; Type = 'UUID'; Column = 'parking_lot_id'; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'vehicleTypeId'; Type = 'UUID'; Column = 'vehicle_type_id' },
        @{ Name = 'capacity'; Type = 'Integer'; Column = 'capacity'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'parking'; FeaturePackage = 'parkingspace'; ClassName = 'ParkingSpace'; TableSchema = 'parking'; TableName = 'parking_spaces'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'parkingSpaceId'; Type = 'UUID'; Column = 'parking_space_id'; Id = $true; Nullable = $false },
        @{ Name = 'zoneId'; Type = 'UUID'; Column = 'zone_id'; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false },
        @{ Name = 'status'; Type = 'ParkingSpaceStatus'; Column = 'status'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'parking'; FeaturePackage = 'lane'; ClassName = 'Lane'; TableSchema = 'parking'; TableName = 'lanes'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'laneId'; Type = 'UUID'; Column = 'lane_id'; Id = $true; Nullable = $false },
        @{ Name = 'parkingLotId'; Type = 'UUID'; Column = 'parking_lot_id'; Nullable = $false },
        @{ Name = 'code'; Type = 'String'; Column = 'code'; Nullable = $false },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'direction'; Type = 'LaneDirection'; Column = 'direction'; Nullable = $false },
        @{ Name = 'status'; Type = 'LaneStatus'; Column = 'status'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'parking'; FeaturePackage = 'parkingsession'; ClassName = 'ParkingSession'; TableSchema = 'parking'; TableName = 'parking_sessions'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'parkingSessionId'; Type = 'UUID'; Column = 'parking_session_id'; Id = $true; Nullable = $false },
        @{ Name = 'cardId'; Type = 'UUID'; Column = 'card_id'; Nullable = $false },
        @{ Name = 'customerId'; Type = 'UUID'; Column = 'customer_id' },
        @{ Name = 'customerVehicleId'; Type = 'UUID'; Column = 'customer_vehicle_id' },
        @{ Name = 'vehicleTypeId'; Type = 'UUID'; Column = 'vehicle_type_id'; Nullable = $false },
        @{ Name = 'parkingSpaceId'; Type = 'UUID'; Column = 'parking_space_id' },
        @{ Name = 'licensePlateIn'; Type = 'String'; Column = 'license_plate_in'; Nullable = $false },
        @{ Name = 'licensePlateOut'; Type = 'String'; Column = 'license_plate_out' },
        @{ Name = 'checkInTime'; Type = 'Instant'; Column = 'check_in_time'; Nullable = $false },
        @{ Name = 'checkOutTime'; Type = 'Instant'; Column = 'check_out_time' },
        @{ Name = 'status'; Type = 'ParkingSessionStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'totalPrice'; Type = 'BigDecimal'; Column = 'total_price'; Precision = 12; Scale = 2 },
        @{ Name = 'priceRuleId'; Type = 'UUID'; Column = 'price_rule_id' }
    ) },
    @{ SchemaPackage = 'parking'; FeaturePackage = 'parkingevent'; ClassName = 'ParkingEvent'; TableSchema = 'parking'; TableName = 'parking_events'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'parkingEventId'; Type = 'UUID'; Column = 'parking_event_id'; Id = $true; Nullable = $false },
        @{ Name = 'parkingSessionId'; Type = 'UUID'; Column = 'parking_session_id'; Nullable = $false },
        @{ Name = 'laneId'; Type = 'UUID'; Column = 'lane_id'; Nullable = $false },
        @{ Name = 'eventType'; Type = 'ParkingEventType'; Column = 'event_type'; Nullable = $false },
        @{ Name = 'eventTime'; Type = 'Instant'; Column = 'event_time'; Nullable = $false },
        @{ Name = 'licensePlateDetected'; Type = 'String'; Column = 'license_plate_detected' },
        @{ Name = 'imagePath'; Type = 'String'; Column = 'image_path' },
        @{ Name = 'actorAccountId'; Type = 'UUID'; Column = 'actor_account_id' },
        @{ Name = 'note'; Type = 'String'; Column = 'note' }
    ) },
    @{ SchemaPackage = 'billing'; FeaturePackage = 'invoice'; ClassName = 'Invoice'; TableSchema = 'billing'; TableName = 'invoices'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'invoiceId'; Type = 'UUID'; Column = 'invoice_id'; Id = $true; Nullable = $false },
        @{ Name = 'invoiceNo'; Type = 'String'; Column = 'invoice_no'; Nullable = $false; Unique = $true },
        @{ Name = 'customerId'; Type = 'UUID'; Column = 'customer_id' },
        @{ Name = 'parkingSessionId'; Type = 'UUID'; Column = 'parking_session_id' },
        @{ Name = 'subscriptionId'; Type = 'UUID'; Column = 'subscription_id' },
        @{ Name = 'lostCardReportId'; Type = 'UUID'; Column = 'lost_card_report_id' },
        @{ Name = 'amount'; Type = 'BigDecimal'; Column = 'amount'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'discountAmount'; Type = 'BigDecimal'; Column = 'discount_amount'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'finalAmount'; Type = 'BigDecimal'; Column = 'final_amount'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'status'; Type = 'InvoiceStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'issuedAt'; Type = 'Instant'; Column = 'issued_at'; Nullable = $false },
        @{ Name = 'paidAt'; Type = 'Instant'; Column = 'paid_at' }
    ) },
    @{ SchemaPackage = 'billing'; FeaturePackage = 'payment'; ClassName = 'Payment'; TableSchema = 'billing'; TableName = 'payments'; AuditKind = 'NONE'; Fields = @(
        @{ Name = 'paymentId'; Type = 'UUID'; Column = 'payment_id'; Id = $true; Nullable = $false },
        @{ Name = 'invoiceId'; Type = 'UUID'; Column = 'invoice_id'; Nullable = $false },
        @{ Name = 'paymentMethod'; Type = 'PaymentMethod'; Column = 'payment_method'; Nullable = $false },
        @{ Name = 'amount'; Type = 'BigDecimal'; Column = 'amount'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'transactionRef'; Type = 'String'; Column = 'transaction_ref' },
        @{ Name = 'status'; Type = 'PaymentStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'paidAt'; Type = 'Instant'; Column = 'paid_at'; Nullable = $false },
        @{ Name = 'receivedBy'; Type = 'UUID'; Column = 'received_by' },
        @{ Name = 'note'; Type = 'String'; Column = 'note' }
    ) },
    @{ SchemaPackage = 'operations'; FeaturePackage = 'shift'; ClassName = 'Shift'; TableSchema = 'operations'; TableName = 'shifts'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'shiftId'; Type = 'UUID'; Column = 'shift_id'; Id = $true; Nullable = $false },
        @{ Name = 'shiftCode'; Type = 'String'; Column = 'shift_code'; Nullable = $false; Unique = $true },
        @{ Name = 'parkingLotId'; Type = 'UUID'; Column = 'parking_lot_id'; Nullable = $false },
        @{ Name = 'startTime'; Type = 'Instant'; Column = 'start_time'; Nullable = $false },
        @{ Name = 'endTime'; Type = 'Instant'; Column = 'end_time' },
        @{ Name = 'status'; Type = 'ShiftStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'openingCash'; Type = 'BigDecimal'; Column = 'opening_cash'; Nullable = $false; Precision = 12; Scale = 2 },
        @{ Name = 'closingCash'; Type = 'BigDecimal'; Column = 'closing_cash'; Precision = 12; Scale = 2 }
    ) },
    @{ SchemaPackage = 'operations'; FeaturePackage = 'shift'; ClassName = 'ShiftAssignment'; TableSchema = 'operations'; TableName = 'shift_assignments'; AuditKind = 'NONE'; Fields = @(
        @{ Name = 'shiftAssignmentId'; Type = 'UUID'; Column = 'shift_assignment_id'; Id = $true; Nullable = $false },
        @{ Name = 'shiftId'; Type = 'UUID'; Column = 'shift_id'; Nullable = $false },
        @{ Name = 'employeeId'; Type = 'UUID'; Column = 'employee_id'; Nullable = $false },
        @{ Name = 'roleInShift'; Type = 'String'; Column = 'role_in_shift'; Nullable = $false },
        @{ Name = 'assignedAt'; Type = 'Instant'; Column = 'assigned_at'; Nullable = $false }
    ) },
    @{ SchemaPackage = 'operations'; FeaturePackage = 'approvalrequest'; ClassName = 'ApprovalRequest'; TableSchema = 'operations'; TableName = 'approval_requests'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'approvalRequestId'; Type = 'UUID'; Column = 'approval_request_id'; Id = $true; Nullable = $false },
        @{ Name = 'requestType'; Type = 'String'; Column = 'request_type'; Nullable = $false },
        @{ Name = 'targetSchema'; Type = 'String'; Column = 'target_schema'; Nullable = $false },
        @{ Name = 'targetTable'; Type = 'String'; Column = 'target_table'; Nullable = $false },
        @{ Name = 'targetId'; Type = 'UUID'; Column = 'target_id'; Nullable = $false },
        @{ Name = 'status'; Type = 'ApprovalRequestStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'requestedBy'; Type = 'UUID'; Column = 'requested_by' },
        @{ Name = 'approvedBy'; Type = 'UUID'; Column = 'approved_by' },
        @{ Name = 'approvedAt'; Type = 'Instant'; Column = 'approved_at' },
        @{ Name = 'note'; Type = 'String'; Column = 'note' }
    ) },
    @{ SchemaPackage = 'operations'; FeaturePackage = 'supportticket'; ClassName = 'SupportTicket'; TableSchema = 'operations'; TableName = 'support_tickets'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'supportTicketId'; Type = 'UUID'; Column = 'support_ticket_id'; Id = $true; Nullable = $false },
        @{ Name = 'customerId'; Type = 'UUID'; Column = 'customer_id' },
        @{ Name = 'title'; Type = 'String'; Column = 'title'; Nullable = $false },
        @{ Name = 'content'; Type = 'String'; Column = 'content'; Nullable = $false },
        @{ Name = 'status'; Type = 'SupportTicketStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'priority'; Type = 'SupportTicketPriority'; Column = 'priority'; Nullable = $false },
        @{ Name = 'assignedTo'; Type = 'UUID'; Column = 'assigned_to' },
        @{ Name = 'resolvedAt'; Type = 'Instant'; Column = 'resolved_at' }
    ) },
    @{ SchemaPackage = 'hardware'; FeaturePackage = 'device'; ClassName = 'Device'; TableSchema = 'hardware'; TableName = 'devices'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'deviceId'; Type = 'UUID'; Column = 'device_id'; Id = $true; Nullable = $false },
        @{ Name = 'parkingLotId'; Type = 'UUID'; Column = 'parking_lot_id'; Nullable = $false },
        @{ Name = 'laneId'; Type = 'UUID'; Column = 'lane_id' },
        @{ Name = 'deviceCode'; Type = 'String'; Column = 'device_code'; Nullable = $false; Unique = $true },
        @{ Name = 'deviceType'; Type = 'DeviceType'; Column = 'device_type'; Nullable = $false },
        @{ Name = 'name'; Type = 'String'; Column = 'name'; Nullable = $false },
        @{ Name = 'ipAddress'; Type = 'String'; Column = 'ip_address' },
        @{ Name = 'status'; Type = 'DeviceStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'lastHeartbeatAt'; Type = 'Instant'; Column = 'last_heartbeat_at' },
        @{ Name = 'config'; Type = 'Map<String, Object>'; Column = 'config'; ColumnDefinition = 'jsonb'; Json = $true }
    ) },
    @{ SchemaPackage = 'notification'; FeaturePackage = 'notification'; ClassName = 'Notification'; TableSchema = 'notification'; TableName = 'notifications'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'notificationId'; Type = 'UUID'; Column = 'notification_id'; Id = $true; Nullable = $false },
        @{ Name = 'accountId'; Type = 'UUID'; Column = 'account_id' },
        @{ Name = 'channel'; Type = 'NotificationChannel'; Column = 'channel'; Nullable = $false },
        @{ Name = 'title'; Type = 'String'; Column = 'title'; Nullable = $false },
        @{ Name = 'message'; Type = 'String'; Column = 'message'; Nullable = $false },
        @{ Name = 'status'; Type = 'NotificationStatus'; Column = 'status'; Nullable = $false },
        @{ Name = 'sentAt'; Type = 'Instant'; Column = 'sent_at' },
        @{ Name = 'readAt'; Type = 'Instant'; Column = 'read_at' },
        @{ Name = 'relatedSchema'; Type = 'String'; Column = 'related_schema' },
        @{ Name = 'relatedTable'; Type = 'String'; Column = 'related_table' },
        @{ Name = 'relatedId'; Type = 'UUID'; Column = 'related_id' }
    ) },
    @{ SchemaPackage = 'audit'; FeaturePackage = 'auditlog'; ClassName = 'AuditLog'; TableSchema = 'audit'; TableName = 'audit_logs'; AuditKind = 'AUDITABLE'; Fields = @(
        @{ Name = 'auditLogId'; Type = 'UUID'; Column = 'audit_log_id'; Id = $true; Nullable = $false },
        @{ Name = 'actorAccountId'; Type = 'UUID'; Column = 'actor_account_id' },
        @{ Name = 'action'; Type = 'String'; Column = 'action'; Nullable = $false },
        @{ Name = 'targetSchema'; Type = 'String'; Column = 'target_schema' },
        @{ Name = 'targetTable'; Type = 'String'; Column = 'target_table' },
        @{ Name = 'targetId'; Type = 'UUID'; Column = 'target_id' },
        @{ Name = 'oldData'; Type = 'Map<String, Object>'; Column = 'old_data'; ColumnDefinition = 'jsonb'; Json = $true },
        @{ Name = 'newData'; Type = 'Map<String, Object>'; Column = 'new_data'; ColumnDefinition = 'jsonb'; Json = $true },
        @{ Name = 'ipAddress'; Type = 'String'; Column = 'ip_address' },
        @{ Name = 'userAgent'; Type = 'String'; Column = 'user_agent' }
    ) }
)

foreach ($table in $tables) {
    Render-DomainClass -Table $table -EnumNames $enumNames
    Render-EntityClass -Table $table -EnumNames $enumNames
    Render-Repository -Table $table
    Render-Mapper -Table $table
}

Write-Host "Generated $($tables.Count) schema source groups."
