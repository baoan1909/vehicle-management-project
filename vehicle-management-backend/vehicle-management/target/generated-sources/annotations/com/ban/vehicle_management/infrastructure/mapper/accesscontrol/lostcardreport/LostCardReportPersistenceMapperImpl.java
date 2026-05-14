package com.ban.vehicle_management.infrastructure.mapper.accesscontrol.lostcardreport;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.lostcardreport.LostCardReportEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class LostCardReportPersistenceMapperImpl implements LostCardReportPersistenceMapper {

    @Override
    public LostCardReportEntity toEntity(LostCardReport domain) {
        if ( domain == null ) {
            return null;
        }

        LostCardReportEntity lostCardReportEntity = new LostCardReportEntity();

        lostCardReportEntity.setCreatedAt( domain.getCreatedAt() );
        lostCardReportEntity.setCreatedBy( domain.getCreatedBy() );
        lostCardReportEntity.setUpdatedAt( domain.getUpdatedAt() );
        lostCardReportEntity.setUpdatedBy( domain.getUpdatedBy() );
        lostCardReportEntity.setLostCardReportId( domain.getLostCardReportId() );
        lostCardReportEntity.setCardId( domain.getCardId() );
        lostCardReportEntity.setCustomerId( domain.getCustomerId() );
        lostCardReportEntity.setParkingSessionId( domain.getParkingSessionId() );
        lostCardReportEntity.setNotificationTime( domain.getNotificationTime() );
        lostCardReportEntity.setTimeOfLost( domain.getTimeOfLost() );
        lostCardReportEntity.setTicketPrice( domain.getTicketPrice() );
        lostCardReportEntity.setLostCardFee( domain.getLostCardFee() );
        lostCardReportEntity.setReporterName( domain.getReporterName() );
        lostCardReportEntity.setReporterPhone( domain.getReporterPhone() );
        lostCardReportEntity.setIdentifyCard( domain.getIdentifyCard() );
        lostCardReportEntity.setRegistrationLicense( domain.getRegistrationLicense() );
        lostCardReportEntity.setNote( domain.getNote() );
        lostCardReportEntity.setStatus( domain.getStatus() );
        lostCardReportEntity.setResolvedBy( domain.getResolvedBy() );
        lostCardReportEntity.setResolvedAt( domain.getResolvedAt() );

        return lostCardReportEntity;
    }

    @Override
    public LostCardReport toDomain(LostCardReportEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LostCardReport lostCardReport = new LostCardReport();

        lostCardReport.setCreatedAt( entity.getCreatedAt() );
        lostCardReport.setCreatedBy( entity.getCreatedBy() );
        lostCardReport.setUpdatedAt( entity.getUpdatedAt() );
        lostCardReport.setUpdatedBy( entity.getUpdatedBy() );
        lostCardReport.setLostCardReportId( entity.getLostCardReportId() );
        lostCardReport.setCardId( entity.getCardId() );
        lostCardReport.setCustomerId( entity.getCustomerId() );
        lostCardReport.setParkingSessionId( entity.getParkingSessionId() );
        lostCardReport.setNotificationTime( entity.getNotificationTime() );
        lostCardReport.setTimeOfLost( entity.getTimeOfLost() );
        lostCardReport.setTicketPrice( entity.getTicketPrice() );
        lostCardReport.setLostCardFee( entity.getLostCardFee() );
        lostCardReport.setReporterName( entity.getReporterName() );
        lostCardReport.setReporterPhone( entity.getReporterPhone() );
        lostCardReport.setIdentifyCard( entity.getIdentifyCard() );
        lostCardReport.setRegistrationLicense( entity.getRegistrationLicense() );
        lostCardReport.setNote( entity.getNote() );
        lostCardReport.setStatus( entity.getStatus() );
        lostCardReport.setResolvedBy( entity.getResolvedBy() );
        lostCardReport.setResolvedAt( entity.getResolvedAt() );

        return lostCardReport;
    }
}
