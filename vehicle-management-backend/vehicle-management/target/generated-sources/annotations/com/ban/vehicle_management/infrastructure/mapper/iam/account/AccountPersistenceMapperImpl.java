package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.AccountEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class AccountPersistenceMapperImpl implements AccountPersistenceMapper {

    @Override
    public AccountEntity toEntity(Account domain) {
        if ( domain == null ) {
            return null;
        }

        AccountEntity accountEntity = new AccountEntity();

        accountEntity.setCreatedAt( domain.getCreatedAt() );
        accountEntity.setCreatedBy( domain.getCreatedBy() );
        accountEntity.setUpdatedAt( domain.getUpdatedAt() );
        accountEntity.setUpdatedBy( domain.getUpdatedBy() );
        accountEntity.setAccountId( domain.getAccountId() );
        accountEntity.setUserProfileId( domain.getUserProfileId() );
        accountEntity.setUsername( domain.getUsername() );
        accountEntity.setEmail( domain.getEmail() );
        accountEntity.setHashPassword( domain.getHashPassword() );
        accountEntity.setRoleId( domain.getRoleId() );
        accountEntity.setStatus( domain.getStatus() );
        accountEntity.setLastLoginAt( domain.getLastLoginAt() );
        accountEntity.setFailedLoginCount( domain.getFailedLoginCount() );
        accountEntity.setLockedUntil( domain.getLockedUntil() );
        accountEntity.setPasswordChangedAt( domain.getPasswordChangedAt() );

        return accountEntity;
    }

    @Override
    public Account toDomain(AccountEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Account account = new Account();

        account.setCreatedAt( entity.getCreatedAt() );
        account.setCreatedBy( entity.getCreatedBy() );
        account.setUpdatedAt( entity.getUpdatedAt() );
        account.setUpdatedBy( entity.getUpdatedBy() );
        account.setAccountId( entity.getAccountId() );
        account.setUserProfileId( entity.getUserProfileId() );
        account.setUsername( entity.getUsername() );
        account.setEmail( entity.getEmail() );
        account.setHashPassword( entity.getHashPassword() );
        account.setRoleId( entity.getRoleId() );
        account.setStatus( entity.getStatus() );
        account.setLastLoginAt( entity.getLastLoginAt() );
        account.setFailedLoginCount( entity.getFailedLoginCount() );
        account.setLockedUntil( entity.getLockedUntil() );
        account.setPasswordChangedAt( entity.getPasswordChangedAt() );

        return account;
    }
}
