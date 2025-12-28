package examples;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import com.fostermoore.pdfviachrome.api.PdfOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Example demonstrating PDF generation from org.w3c.dom.Document objects.
 *
 * This example shows how to:
 * 1. Create a DOM Document programmatically
 * 2. Generate a PDF from the Document using PdfGenerator
 * 3. Apply custom PDF options
 */
public class DocumentToPdfExample {

    public static void main(String[] args) throws Exception {
        // Example 1: Generate PDF from a simple programmatically created Document
        basicDocumentExample();

        // Example 2: Generate PDF with custom options
        customOptionsExample();

        // Example 3: Parse an XML/HTML file into a Document and convert to PDF
        fileBasedExample();
    }

    /**
     * Example 1: Create a simple HTML document and convert to PDF
     */
    private static void basicDocumentExample() throws Exception {
        System.out.println("Example 1: Basic Document to PDF");

        // Create a DOM Document programmatically
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        // Build HTML structure: <html><body><h1>Hello from Document</h1></body></html>
        Element html = document.createElement("html");
        Element body = document.createElement("body");
        Element h1 = document.createElement("h1");
        h1.setTextContent("Hello from DOM Document!");

        body.appendChild(h1);
        html.appendChild(body);
        document.appendChild(html);

        // Generate PDF from the Document
        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromDocument(document).generate();

            // Save to file
            Path outputPath = Paths.get("output-basic-document.pdf");
            Files.write(outputPath, pdf);
            System.out.println("✓ PDF generated: " + outputPath.toAbsolutePath());
        }
    }

    /**
     * Example 2: Generate PDF with custom options
     */
    private static void customOptionsExample() throws Exception {
        System.out.println("\nExample 2: Document to PDF with Custom Options");

        // Create a more complex document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element html = document.createElement("html");
        Element head = document.createElement("head");
        Element style = document.createElement("style");
        style.setTextContent(
            "body { font-family: Arial, sans-serif; margin: 20px; background: linear-gradient(to right, #667eea 0%, #764ba2 100%); }" +
            "h1 { color: white; }" +
            "p { color: white; }"
        );
        head.appendChild(style);

        Element body = document.createElement("body");
        Element h1 = document.createElement("h1");
        h1.setTextContent("Styled Document Example");
        Element p = document.createElement("p");
        p.setTextContent("This document has custom styling and will be converted to PDF with custom options.");

        body.appendChild(h1);
        body.appendChild(p);
        html.appendChild(head);
        html.appendChild(body);
        document.appendChild(html);

        // Configure custom PDF options
        PdfOptions options = PdfOptions.builder()
            .paperSize(PdfOptions.PaperFormat.A4)
            .landscape(false)
            .printBackground(true)  // Important: enable background graphics for gradients
            .margins(0.5)           // 0.5 inch margins
            .scale(0.9)
            .simplePageNumbers()    // Add page numbers
            .build();

        // Generate PDF with custom options
        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromDocument(document)
                .withOptions(options)
                .generate();

            Path outputPath = Paths.get("output-styled-document.pdf");
            Files.write(outputPath, pdf);
            System.out.println("✓ PDF generated with custom options: " + outputPath.toAbsolutePath());
        }
    }

    /**
     * Example 3: Parse an existing HTML file and convert to PDF
     */
    private static void fileBasedExample() throws Exception {
        System.out.println("\nExample 3: Parse HTML File and Convert to PDF");

        // First, create a sample HTML file
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Sample Document</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px; }
                    h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
                    .content { margin-top: 20px; line-height: 1.6; }
                    .highlight { background-color: #ffffcc; padding: 2px 5px; }
                </style>
            </head>
            <body>
                <h1>File-Based Document Example</h1>
                <div class="content">
                    <p>This HTML content was <span class="highlight">loaded from a file</span> and parsed into a DOM Document.</p>
                    <p>It demonstrates how you can:</p>
                    <ul>
                        <li>Parse existing HTML files</li>
                        <li>Convert them to DOM Documents</li>
                        <li>Generate PDFs from the parsed content</li>
                    </ul>
                </div>
            </body>
            </html>
            """;

        Path htmlFile = Paths.get("sample-input.html");
        Files.writeString(htmlFile, htmlContent);
        System.out.println("✓ Created sample HTML file: " + htmlFile.toAbsolutePath());

        // Parse the HTML file into a Document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document document = docBuilder.parse(new File(htmlFile.toString()));

        // Generate PDF from the parsed document
        try (PdfGenerator generator = PdfGenerator.create().build()) {
            byte[] pdf = generator.fromDocument(document)
                .withOptions(PdfOptions.builder()
                    .printBackground(true)
                    .paperSize(PdfOptions.PaperFormat.LETTER)
                    .build())
                .generate();

            Path outputPath = Paths.get("output-from-file.pdf");
            Files.write(outputPath, pdf);
            System.out.println("✓ PDF generated from file: " + outputPath.toAbsolutePath());
        }

        // Clean up
        Files.deleteIfExists(htmlFile);
    }
}
