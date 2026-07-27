package com.example.e_rechnung.Erechnung.mapper;




import com.example.e_rechnung.Erechnung.dto.request.CreateInvoiceRequest;
import com.example.e_rechnung.Erechnung.model.InvoiceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface InvoiceDataMapper {
    InvoiceDataMapper INSTANCE = Mappers.getMapper(InvoiceDataMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "xmlContent", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    InvoiceEntity toEntity(CreateInvoiceRequest dto);
}

