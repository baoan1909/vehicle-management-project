package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.RefreshToken;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.RefreshTokenEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class RefreshTokenPersistenceMapperImpl implements RefreshTokenPersistenceMapper {

    @Override
    public RefreshTokenEntity toEntity(RefreshToken domain) {
        if ( domain == null ) {
            return null;
        }

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();

        refreshTokenEntity.setCreatedAt( domain.getCreatedAt() );
        refreshTokenEntity.setCreatedBy( domain.getCreatedBy() );
        refreshTokenEntity.setUpdatedAt( domain.getUpdatedAt() );
        refreshTokenEntity.setUpdatedBy( domain.getUpdatedBy() );
        refreshTokenEntity.setRefreshTokenId( domain.getRefreshTokenId() );
        refreshTokenEntity.setAccountId( domain.getAccountId() );
        refreshTokenEntity.setTokenHash( domain.getTokenHash() );
        refreshTokenEntity.setExpiresAt( domain.getExpiresAt() );
        refreshTokenEntity.setRevokedAt( domain.getRevokedAt() );
        refreshTokenEntity.setCreatedByIp( domain.getCreatedByIp() );
        refreshTokenEntity.setUserAgent( domain.getUserAgent() );

        return refreshTokenEntity;
    }

    @Override
    public RefreshToken toDomain(RefreshTokenEntity entity) {
        if ( entity == null ) {
            return null;
        }

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setCreatedAt( entity.getCreatedAt() );
        refreshToken.setCreatedBy( entity.getCreatedBy() );
        refreshToken.setUpdatedAt( entity.getUpdatedAt() );
        refreshToken.setUpdatedBy( entity.getUpdatedBy() );
        refreshToken.setRefreshTokenId( entity.getRefreshTokenId() );
        refreshToken.setAccountId( entity.getAccountId() );
        refreshToken.setTokenHash( entity.getTokenHash() );
        refreshToken.setExpiresAt( entity.getExpiresAt() );
        refreshToken.setRevokedAt( entity.getRevokedAt() );
        refreshToken.setCreatedByIp( entity.getCreatedByIp() );
        refreshToken.setUserAgent( entity.getUserAgent() );

        return refreshToken;
    }
}
