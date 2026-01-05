package com.fostermoore.pdfviachrome.testapp.controller;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import com.fostermoore.pdfviachrome.api.PdfOptions;
import com.fostermoore.pdfviachrome.testapp.dto.HtmlRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for PDF generation endpoints.
 */
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private static final Logger logger = LoggerFactory.getLogger(PdfController.class);

    private final PdfGenerator pdfGenerator;

    public PdfController(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
    }

    /**
     * Generates a PDF from HTML content.
     *
     * @param request the HTML content and options
     * @return PDF file as byte array
     */
    @PostMapping("/from-html")
    public ResponseEntity<byte[]> generateFromHtml(@Valid @RequestBody HtmlRequest request) {
        long startTime = System.currentTimeMillis();

        logger.info("Received HTML-to-PDF generation request (content length: {} chars)",
            request.getContent().length());
        logger.debug("Request details - Options present: {}, Wait strategy: {}",
            request.getOptions() != null, request.getWaitStrategy());

        try {
            // Build PDF options from DTO (or use defaults)
            PdfOptions options = request.getOptions() != null
                ? request.getOptions().toPdfOptions()
                : PdfOptions.defaults();

            logger.debug("Starting PDF generation with options: landscape={}, scale={}, printBackground={}",
                options.isLandscape(), options.getScale(), options.isPrintBackground());

            // Build PDF generation request
            var generationBuilder = pdfGenerator.fromHtml(request.getContent());

            // Add base URL if provided
            if (request.getBaseUrl() != null && !request.getBaseUrl().trim().isEmpty()) {
                logger.debug("Using base URL: {}", request.getBaseUrl());
                generationBuilder.withBaseUrl(request.getBaseUrl());
            }

            // Generate PDF
            byte[] pdfBytes = generationBuilder
                .withOptions(options)
                .generate();

            long duration = System.currentTimeMillis() - startTime;

            logger.info("Successfully generated PDF (size: {} bytes, duration: {} ms)",
                pdfBytes.length, duration);

            // Log performance metrics
            org.slf4j.LoggerFactory.getLogger("com.fostermoore.pdfviachrome.testapp.performance")
                .info("PDF_GENERATION,html_length={},pdf_size={},duration_ms={},has_options={}",
                    request.getContent().length(), pdfBytes.length, duration,
                    request.getOptions() != null);

            // Build response with appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "generated.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("PDF generation failed after {} ms: {}", duration, e.getMessage(), e);
            throw e; // Re-throw to let GlobalExceptionHandler handle it
        }
    }
}
