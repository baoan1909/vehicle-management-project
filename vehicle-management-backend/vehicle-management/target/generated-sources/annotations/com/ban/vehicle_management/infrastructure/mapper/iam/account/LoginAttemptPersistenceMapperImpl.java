package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.LoginAttempt;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.LoginAttemptEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class LoginAttemptPersistenceMapperImpl implements LoginAttemptPersistenceMapper {

    @Override
    public LoginAttemptEntity toEntity(LoginAttempt domain) {
        if ( domain == null ) {
            return null;
        }

        LoginAttemptEntity loginAttemptEntity = new LoginAttemptEntity();

        loginAttemptEntity.setLoginAttemptId( domain.getLoginAttemptId() );
        loginAttemptEntity.setAccountId( domain.getAccountId() );
        loginAttemptEntity.setUsernameOrEmail( domain.getUsernameOrEmail() );
        loginAttemptEntity.setSuccess( domain.getSuccess() );
        loginAttemptEntity.setFailureReason( domain.getFailureReason() );
        loginAttemptEntity.setIpAddress( domain.getIpAddress() );
        loginAttemptEntity.setUserAgent( domain.getUserAgent() );
        loginAttemptEntity.setAttemptedAt( domain.getAttemptedAt() );

        return loginAttemptEntity;
    }

    @Override
    public LoginAttempt toDomain(LoginAttemptEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LoginAttempt loginAttempt = new LoginAttempt();

        loginAttempt.setLoginAttemptId( entity.getLoginAttemptId() );
        loginAttempt.setAccountId( entity.getAccountId() );
        loginAttempt.setUsernameOrEmail( entity.getUsernameOrEmail() );
        loginAttempt.setSuccess( entity.getSuccess() );
        loginAttempt.setFailureReason( entity.getFailureReason() );
        loginAttempt.setIpAddress( entity.getIpAddress() );
        loginAttempt.setUserAgent( entity.getUserAgent() );
        loginAttempt.setAttemptedAt( entity.getAttemptedAt() );

        return loginAttempt;
    }
}
