package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.AccountStatusHistory;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.AccountStatusHistoryEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class AccountStatusHistoryPersistenceMapperImpl implements AccountStatusHistoryPersistenceMapper {

    @Override
    public AccountStatusHistoryEntity toEntity(AccountStatusHistory domain) {
        if ( domain == null ) {
            return null;
        }

        AccountStatusHistoryEntity accountStatusHistoryEntity = new AccountStatusHistoryEntity();

        accountStatusHistoryEntity.setAccountStatusHistoryId( domain.getAccountStatusHistoryId() );
        accountStatusHistoryEntity.setAccountId( domain.getAccountId() );
        accountStatusHistoryEntity.setOldStatus( domain.getOldStatus() );
        accountStatusHistoryEntity.setNewStatus( domain.getNewStatus() );
        accountStatusHistoryEntity.setReason( domain.getReason() );
        accountStatusHistoryEntity.setChangedAt( domain.getChangedAt() );
        accountStatusHistoryEntity.setChangedBy( domain.getChangedBy() );

        return accountStatusHistoryEntity;
    }

    @Override
    public AccountStatusHistory toDomain(AccountStatusHistoryEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AccountStatusHistory accountStatusHistory = new AccountStatusHistory();

        accountStatusHistory.setAccountStatusHistoryId( entity.getAccountStatusHistoryId() );
        accountStatusHistory.setAccountId( entity.getAccountId() );
        accountStatusHistory.setOldStatus( entity.getOldStatus() );
        accountStatusHistory.setNewStatus( entity.getNewStatus() );
        accountStatusHistory.setReason( entity.getReason() );
        accountStatusHistory.setChangedAt( entity.getChangedAt() );
        accountStatusHistory.setChangedBy( entity.getChangedBy() );

        return accountStatusHistory;
    }
}
