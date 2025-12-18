package com.fostermoore.pdfviachrome.testapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fostermoore.pdfviachrome.testapp.dto.HtmlRequest;
import com.fostermoore.pdfviachrome.testapp.dto.PdfOptionsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PdfController.
 * These tests require Chrome to be available on the system.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGenerateFromHtml_ActualPdfGeneration() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Integration Test</h1><p>This is a test PDF.</p></body></html>");

        // Act
        MvcResult result = mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"generated.pdf\""))
            .andReturn();

        // Assert
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(0);

        // Verify PDF header (should start with %PDF)
        String pdfHeader = new String(pdfBytes, 0, 4);
        assertThat(pdfHeader).isEqualTo("%PDF");
    }

    @Test
    void testGenerateFromHtml_WithCustomOptions() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Custom Options Test</h1></body></html>");

        PdfOptionsDto options = new PdfOptionsDto();
        options.setPaperFormat("A4");
        options.setLandscape(true);
        options.setPrintBackground(true);
        options.setMargins("1cm");
        request.setOptions(options);

        // Act
        MvcResult result = mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andReturn();

        // Assert
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();

        String pdfHeader = new String(pdfBytes, 0, 4);
        assertThat(pdfHeader).isEqualTo("%PDF");
    }

    @Test
    void testGenerateFromHtml_ComplexHtml() throws Exception {
        // Arrange
        String complexHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    h1 { color: #333; }
                    .box { background-color: #f0f0f0; padding: 10px; border: 1px solid #ccc; }
                </style>
            </head>
            <body>
                <h1>Complex HTML Test</h1>
                <div class="box">
                    <p>This is a styled paragraph in a box.</p>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                        <li>Item 3</li>
                    </ul>
                </div>
                <table border="1">
                    <tr><th>Header 1</th><th>Header 2</th></tr>
                    <tr><td>Data 1</td><td>Data 2</td></tr>
                </table>
            </body>
            </html>
            """;

        HtmlRequest request = new HtmlRequest();
        request.setContent(complexHtml);

        PdfOptionsDto options = new PdfOptionsDto();
        options.setPrintBackground(true);
        request.setOptions(options);

        // Act
        MvcResult result = mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andReturn();

        // Assert
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(1000); // Complex HTML should produce larger PDF

        String pdfHeader = new String(pdfBytes, 0, 4);
        assertThat(pdfHeader).isEqualTo("%PDF");
    }

    @Test
    void testGenerateFromHtml_ValidationError() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent(""); // Empty content

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.errors.content").exists());
    }

    @Test
    void testGenerateFromHtml_InvalidScale() throws Exception {
        // Arrange
        HtmlRequest request = new HtmlRequest();
        request.setContent("<html><body><h1>Test</h1></body></html>");

        PdfOptionsDto options = new PdfOptionsDto();
        options.setScale(5.0); // Invalid scale (must be between 0.1 and 2.0)
        request.setOptions(options);

        // Act & Assert
        mockMvc.perform(post("/api/pdf/from-html")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.errors['options.scale']").value(org.hamcrest.Matchers.containsString("Scale must")));
    }
}
