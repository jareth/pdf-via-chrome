package com.github.headlesschromepdf.testapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.headlesschromepdf.api.PdfGenerator;
import com.github.headlesschromepdf.exception.BrowserTimeoutException;
import com.github.headlesschromepdf.exception.ChromeNotFoundException;
import com.github.headlesschromepdf.exception.PdfGenerationException;
import com.github.headlesschromepdf.testapp.dto.HtmlRequest;
import com.github.headlesschromepdf.testapp.dto.PdfOptionsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PdfController.
 */
@WebMvcTest(PdfController.class)
class PdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PdfGenerator pdfGenerator;

    @MockBean
    private PdfGenerator.GenerationBuilder generationBuilder;

    @Test
    void testGenerateFromHtml_Success() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Test</h1></body></html>");

        byte[] mockPdf = new byte[]{0x25, 0x50, 0x44, 0x46}; // PDF header

        when(pdfGenerator.fromHtml(anyString())).thenReturn(generationBuilder);
        when(generationBuilder.withOptions(any())).thenReturn(generationBuilder);
        when(generationBuilder.generate()).thenReturn(mockPdf);

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().bytes(mockPdf));
    }

    @Test
    void testGenerateFromHtml_WithOptions() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Test</h1></body></html>");

        PdfOptionsDto options = new PdfOptionsDto();
        options.setPaperFormat("A4");
        options.setLandscape(true);
        options.setPrintBackground(true);
        request.setOptions(options);

        byte[] mockPdf = new byte[]{0x25, 0x50, 0x44, 0x46};

        when(pdfGenerator.fromHtml(anyString())).thenReturn(generationBuilder);
        when(generationBuilder.withOptions(any())).thenReturn(generationBuilder);
        when(generationBuilder.generate()).thenReturn(mockPdf);

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void testGenerateFromHtml_ValidationError_EmptyContent() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent(""); // Empty content should fail validation

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.errors.content").exists());
    }

    @Test
    void testGenerateFromHtml_ValidationError_NullContent() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent(null); // Null content should fail validation

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void testGenerateFromHtml_InvalidPaperFormat() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Test</h1></body></html>");

        PdfOptionsDto options = new PdfOptionsDto();
        options.setPaperFormat("INVALID_FORMAT");
        request.setOptions(options);

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid paper format")));
    }

    @Test
    void testGenerateFromHtml_ChromeNotFound() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Test</h1></body></html>");

        when(pdfGenerator.fromHtml(anyString())).thenReturn(generationBuilder);
        when(generationBuilder.withOptions(any())).thenReturn(generationBuilder);
        when(generationBuilder.generate()).thenThrow(new ChromeNotFoundException("Chrome not found"));

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("Service Unavailable"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Chrome browser not found")));
    }

    @Test
    void testGenerateFromHtml_BrowserTimeout() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Test</h1></body></html>");

        when(pdfGenerator.fromHtml(anyString())).thenReturn(generationBuilder);
        when(generationBuilder.withOptions(any())).thenReturn(generationBuilder);
        when(generationBuilder.generate()).thenThrow(new BrowserTimeoutException("Timeout"));

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.error").value("Gateway Timeout"));
    }

    @Test
    void testGenerateFromHtml_PdfGenerationException() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Test</h1></body></html>");

        when(pdfGenerator.fromHtml(anyString())).thenReturn(generationBuilder);
        when(generationBuilder.withOptions(any())).thenReturn(generationBuilder);
        when(generationBuilder.generate()).thenThrow(new PdfGenerationException("Generation failed"));

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
