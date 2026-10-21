package com.ban.vehicle_management.application.billing.invoice.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceUserResponse;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class InvoiceApiMapperUserResponseTest {

    private final InvoiceApiMapper mapper = Mappers.getMapper(InvoiceApiMapper.class);

    @Test
    void shouldMapUserResponseWithoutAuditFields() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setParkingLotId(UUID.randomUUID());
        invoice.setInvoiceNo("INV-001");
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setSubscriptionId(UUID.randomUUID());
        invoice.setAmount(new BigDecimal("140000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("140000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setLicensePlateDisplaySnapshot("51F-12345");
        invoice.setCreatedBy(UUID.randomUUID());
        invoice.setUpdatedBy(UUID.randomUUID());

        InvoiceUserResponse response = mapper.toUserResponse(invoice);

        assertEquals(invoice.getInvoiceId(), response.invoiceId());
        assertEquals("INV-001", response.invoiceNo());
        assertEquals(new BigDecimal("140000"), response.finalAmount());
        assertEquals(InvoiceStatus.UNPAID, response.status());
        assertEquals("51F-12345", response.licensePlateDisplaySnapshot());
    }

    @Test
    void shouldMapUserResponseList() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setStatus(InvoiceStatus.PAID);

        List<InvoiceUserResponse> responses = mapper.toUserResponses(List.of(invoice));

        assertEquals(1, responses.size());
        assertEquals(InvoiceStatus.PAID, responses.get(0).status());
    }
}
