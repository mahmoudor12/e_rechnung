package com.example.e_rechnung.Erechnung.service.generation;

import com.example.e_rechnung.Erechnung.dto.request.CreateInvoiceRequest;
import com.example.e_rechnung.Erechnung.util.InvoiceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.mustangproject.Invoice;
import org.mustangproject.Item;
import org.mustangproject.Product;
import org.mustangproject.TradeParty;
import org.mustangproject.Contact;
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider;
import org.mustangproject.ZUGFeRD.Profiles;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class XRechnungGeneratorService {

    private final InvoiceNumberGenerator invoiceNumberGenerator;

    public byte[] generate(CreateInvoiceRequest request) {
        Invoice invoice = buildInvoice(request);
        try {
            ZUGFeRD2PullProvider xmlProvider = new ZUGFeRD2PullProvider();
            xmlProvider.setProfile(Profiles.getByName("XRechnung"));
            xmlProvider.generateXML(invoice);

            byte[] xmlBytes = xmlProvider.getXML();

            log.info("XRechnung XML successfully generated!");
            return xmlBytes;
        } catch (Exception e) {
            log.error("Error generating XRechnung", e);
            throw new RuntimeException("Failed to generate XRechnung", e);
        }
    }

    private Invoice buildInvoice(CreateInvoiceRequest request) {
        String invNumber = request.getInvoiceNumber() != null ? request.getInvoiceNumber() : invoiceNumberGenerator.generate();
        String sellerEmail = request.getSellerEmail() != null ? request.getSellerEmail() : "info@example.com";

        // Party Verkäufer
        TradeParty sender = new TradeParty(
                request.getSellerName(),
                "Musterstraße 1",
                "12345",
                "Musterstadt",
                "DE"
        );
        if (request.getSellerVatId() != null) {
            sender.addVATID(request.getSellerVatId());
        }
        sender.setContact(new Contact(request.getSellerName(), "", sellerEmail));

        // Party Käufer
        TradeParty recipient = new TradeParty(
                request.getBuyerName(),
                "Käuferstraße 2",
                "54321",
                "Käuferstadt",
                "DE"
        );
        if (request.getBuyerVatId() != null) {
            recipient.addVATID(request.getBuyerVatId());
        }

        // Datumswerte sicher konvertieren
        LocalDate issueLocalDate = request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now();
        Date issueDate = convertToDate(issueLocalDate);
        Date dueDate = request.getDueDate() != null ? convertToDate(request.getDueDate()) : null;

        // Invoice-Objekt aufbauen
        Invoice invoice = new Invoice()
                .setNumber(invNumber)
                .setIssueDate(issueDate) // Hier wird jetzt java.util.Date übergeben!
                .setDueDate(dueDate)     // Hier ebenso!
                .setCurrency("EUR")
                .setSender(sender)
                .setRecipient(recipient);

        if (request.getBuyerReference() != null) {
            invoice.setReferenceNumber(request.getBuyerReference());
        }

        // Positionen hinzufügen
        if (request.getItems() != null) {
            for (CreateInvoiceRequest.InvoiceItemDto itemDto : request.getItems()) {
                BigDecimal vatRate = itemDto.getVatRate() != null ? itemDto.getVatRate() : new BigDecimal("19.00");

                Product product = new Product(
                        itemDto.getDescription(),
                        "",
                        itemDto.getUnitCode() != null ? itemDto.getUnitCode() : "C62",
                        vatRate
                );

                Item item = new Item(product, itemDto.getUnitPrice(), itemDto.getQuantity());
                invoice.addItem(item);
            }
        }

        return invoice;
    }

    // Hilfsmethode zur Konvertierung von LocalDate zu Date
    private Date convertToDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}