package com.ban.vehicle_management.infrastructure.mapper.people.customer;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.persistence.people.customer.CustomerEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class CustomerPersistenceMapperImpl implements CustomerPersistenceMapper {

    @Override
    public CustomerEntity toEntity(Customer domain) {
        if ( domain == null ) {
            return null;
        }

        CustomerEntity customerEntity = new CustomerEntity();

        customerEntity.setCreatedAt( domain.getCreatedAt() );
        customerEntity.setCreatedBy( domain.getCreatedBy() );
        customerEntity.setUpdatedAt( domain.getUpdatedAt() );
        customerEntity.setUpdatedBy( domain.getUpdatedBy() );
        customerEntity.setCustomerId( domain.getCustomerId() );
        customerEntity.setUserProfileId( domain.getUserProfileId() );
        customerEntity.setCustomerCode( domain.getCustomerCode() );
        customerEntity.setCustomerType( domain.getCustomerType() );
        customerEntity.setApprovalStatus( domain.getApprovalStatus() );
        customerEntity.setApprovedBy( domain.getApprovedBy() );
        customerEntity.setApprovedAt( domain.getApprovedAt() );

        return customerEntity;
    }

    @Override
    public Customer toDomain(CustomerEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Customer customer = new Customer();

        customer.setCreatedAt( entity.getCreatedAt() );
        customer.setCreatedBy( entity.getCreatedBy() );
        customer.setUpdatedAt( entity.getUpdatedAt() );
        customer.setUpdatedBy( entity.getUpdatedBy() );
        customer.setCustomerId( entity.getCustomerId() );
        customer.setUserProfileId( entity.getUserProfileId() );
        customer.setCustomerCode( entity.getCustomerCode() );
        customer.setCustomerType( entity.getCustomerType() );
        customer.setApprovalStatus( entity.getApprovalStatus() );
        customer.setApprovedBy( entity.getApprovedBy() );
        customer.setApprovedAt( entity.getApprovedAt() );

        return customer;
    }
}
