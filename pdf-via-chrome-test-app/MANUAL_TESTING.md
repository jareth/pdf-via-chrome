# Manual Testing Guide for PDF API

This guide provides examples for manually testing the PDF generation endpoints.

## Starting the Application

```bash
# From the project root directory
mvn spring-boot:run -pl pdf-via-chrome-test-app

# Or build and run the JAR
mvn clean package -pl pdf-via-chrome-test-app
java -jar pdf-via-chrome-test-app/target/pdf-via-chrome-test-app-1.0.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## Testing Endpoints

### 1. Health Check

```bash
curl http://localhost:8080/health
```

### 2. Generate PDF from HTML (Basic)

```bash
curl -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<html><body><h1>Hello World</h1><p>This is a test PDF.</p></body></html>"
  }' \
  --output test.pdf
```

### 3. Generate PDF with Custom Options

```bash
curl -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<html><body><h1>Custom PDF</h1><p>This PDF has custom options.</p></body></html>",
    "options": {
      "paperFormat": "A4",
      "landscape": true,
      "printBackground": true,
      "margins": "1cm"
    }
  }' \
  --output custom.pdf
```

### 4. Generate PDF with Complex HTML and Styling

```bash
curl -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<!DOCTYPE html><html><head><style>body { font-family: Arial, sans-serif; margin: 20px; } h1 { color: #333; } .box { background-color: #f0f0f0; padding: 10px; border: 1px solid #ccc; }</style></head><body><h1>Styled Document</h1><div class=\"box\"><p>This is a styled paragraph.</p></div></body></html>",
    "options": {
      "paperFormat": "LETTER",
      "printBackground": true,
      "scale": 0.9
    }
  }' \
  --output styled.pdf
```

### 5. Generate PDF with Individual Margins

```bash
curl -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<html><body><h1>Custom Margins</h1></body></html>",
    "options": {
      "marginTop": "2cm",
      "marginBottom": "2cm",
      "marginLeft": "1cm",
      "marginRight": "1cm"
    }
  }' \
  --output margins.pdf
```

### 6. Test Validation Errors

```bash
# Empty content should return 400 Bad Request
curl -v -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": ""
  }'
```

### 7. Test Invalid Paper Format

```bash
# Invalid paper format should return 400 Bad Request
curl -v -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<html><body><h1>Test</h1></body></html>",
    "options": {
      "paperFormat": "INVALID"
    }
  }'
```

### 8. Test Invalid Scale

```bash
# Scale outside 0.1-2.0 range should return 400 Bad Request
curl -v -X POST http://localhost:8080/api/pdf/from-html \
  -H "Content-Type: application/json" \
  -d '{
    "content": "<html><body><h1>Test</h1></body></html>",
    "options": {
      "scale": 5.0
    }
  }'
```

## Using PowerShell (Windows)

For Windows PowerShell, use this format:

```powershell
# Basic PDF generation
$body = @{
    content = "<html><body><h1>Hello World</h1></body></html>"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/pdf/from-html" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -OutFile "test.pdf"

# With options
$body = @{
    content = "<html><body><h1>Custom PDF</h1></body></html>"
    options = @{
        paperFormat = "A4"
        landscape = $true
        printBackground = $true
        margins = "1cm"
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/pdf/from-html" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -OutFile "custom.pdf"
```

## Available Options

### Paper Formats
- `LETTER` (8.5 x 11 inches)
- `LEGAL` (8.5 x 14 inches)
- `TABLOID` (11 x 17 inches)
- `LEDGER` (17 x 11 inches)
- `A0`, `A1`, `A2`, `A3`, `A4`, `A5`, `A6`

### Margin Units
- `cm` - centimeters (e.g., "1cm", "2.5cm")
- `in` - inches (e.g., "0.5in", "1in")
- `px` - pixels (e.g., "10px", "50px")

### Other Options
- `landscape`: `true` or `false` (default: false)
- `printBackground`: `true` or `false` (default: false)
- `scale`: Number between 0.1 and 2.0 (default: 1.0)
- `displayHeaderFooter`: `true` or `false` (default: false)
- `headerTemplate`: HTML string for header
- `footerTemplate`: HTML string for footer
- `pageRanges`: String like "1-5, 8, 11-13" (default: all pages)
- `preferCssPageSize`: `true` or `false` (default: false)

## Expected HTTP Status Codes

- `200 OK` - PDF generated successfully
- `400 Bad Request` - Validation error or invalid options
- `500 Internal Server Error` - PDF generation failed
- `503 Service Unavailable` - Chrome browser not found
- `504 Gateway Timeout` - Browser operation timed out
