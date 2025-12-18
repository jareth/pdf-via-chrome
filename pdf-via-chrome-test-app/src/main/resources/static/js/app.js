// Sample HTML templates
const templates = {
    simple: `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Simple Document</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
            line-height: 1.6;
        }
        h1 {
            color: #333;
        }
    </style>
</head>
<body>
    <h1>Simple Document</h1>
    <p>This is a simple document with minimal styling.</p>
    <p>Perfect for basic text content.</p>
</body>
</html>`,

    styled: `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Styled Document</title>
    <style>
        body {
            font-family: 'Georgia', serif;
            margin: 40px;
            background: linear-gradient(to bottom, #f0f9ff 0%, #ffffff 100%);
            color: #333;
        }
        .header {
            background: #007bff;
            color: white;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 30px;
        }
        .content {
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 { margin: 0; }
        h2 { color: #007bff; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
        p { line-height: 1.8; }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #007bff;
            text-align: center;
            color: #666;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>Styled Document Example</h1>
        <p>A beautifully styled PDF document</p>
    </div>
    <div class="content">
        <h2>Introduction</h2>
        <p>This document demonstrates advanced CSS styling in PDF generation.
        You can use colors, backgrounds, borders, and more.</p>

        <h2>Features</h2>
        <p>The styling includes gradients, shadows, rounded corners, and custom fonts.</p>
    </div>
    <div class="footer">
        <p>Generated with PDF via Chrome</p>
    </div>
</body>
</html>`,

    invoice: `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Invoice</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 30px;
        }
        .header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 30px;
        }
        .company-info h1 { margin: 0; color: #007bff; }
        .invoice-info { text-align: right; }
        .invoice-info h2 { margin: 0; color: #333; }
        .invoice-details {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 30px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 30px;
        }
        th {
            background: #007bff;
            color: white;
            padding: 12px;
            text-align: left;
        }
        td {
            padding: 10px;
            border-bottom: 1px solid #ddd;
        }
        .total-row {
            font-weight: bold;
            background: #f8f9fa;
        }
        .total-amount {
            font-size: 1.2em;
            color: #007bff;
        }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #007bff;
            text-align: center;
            color: #666;
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="company-info">
            <h1>ACME Corporation</h1>
            <p>123 Business Street<br>
            City, State 12345<br>
            Phone: (555) 123-4567</p>
        </div>
        <div class="invoice-info">
            <h2>INVOICE</h2>
            <p><strong>Invoice #:</strong> INV-2024-001<br>
            <strong>Date:</strong> January 15, 2024<br>
            <strong>Due Date:</strong> February 15, 2024</p>
        </div>
    </div>

    <div class="invoice-details">
        <h3>Bill To:</h3>
        <p><strong>John Doe</strong><br>
        456 Client Avenue<br>
        Town, State 67890</p>
    </div>

    <table>
        <thead>
            <tr>
                <th>Description</th>
                <th style="text-align: right;">Quantity</th>
                <th style="text-align: right;">Price</th>
                <th style="text-align: right;">Total</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>Professional Services</td>
                <td style="text-align: right;">10 hours</td>
                <td style="text-align: right;">$150.00</td>
                <td style="text-align: right;">$1,500.00</td>
            </tr>
            <tr>
                <td>Software License</td>
                <td style="text-align: right;">1</td>
                <td style="text-align: right;">$299.00</td>
                <td style="text-align: right;">$299.00</td>
            </tr>
            <tr>
                <td>Technical Support</td>
                <td style="text-align: right;">5 hours</td>
                <td style="text-align: right;">$100.00</td>
                <td style="text-align: right;">$500.00</td>
            </tr>
            <tr class="total-row">
                <td colspan="3" style="text-align: right;">Subtotal:</td>
                <td style="text-align: right;">$2,299.00</td>
            </tr>
            <tr class="total-row">
                <td colspan="3" style="text-align: right;">Tax (10%):</td>
                <td style="text-align: right;">$229.90</td>
            </tr>
            <tr class="total-row">
                <td colspan="3" style="text-align: right;"><strong>TOTAL:</strong></td>
                <td style="text-align: right;" class="total-amount"><strong>$2,528.90</strong></td>
            </tr>
        </tbody>
    </table>

    <div class="footer">
        <p>Thank you for your business!<br>
        Payment terms: Net 30 days</p>
    </div>
</body>
</html>`,

    report: `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Sales Report</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 30px;
        }
        .header {
            background: #28a745;
            color: white;
            padding: 20px;
            margin-bottom: 30px;
        }
        .header h1 { margin: 0; }
        .summary {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 20px;
            margin-bottom: 30px;
        }
        .summary-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            border-left: 4px solid #28a745;
        }
        .summary-card h3 {
            margin: 0 0 10px 0;
            color: #666;
            font-size: 0.9em;
        }
        .summary-card .value {
            font-size: 1.8em;
            font-weight: bold;
            color: #28a745;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 30px;
        }
        th {
            background: #28a745;
            color: white;
            padding: 12px;
            text-align: left;
        }
        td {
            padding: 10px;
            border-bottom: 1px solid #ddd;
        }
        tr:hover {
            background: #f8f9fa;
        }
        .chart-placeholder {
            background: #f8f9fa;
            padding: 40px;
            text-align: center;
            border-radius: 8px;
            color: #666;
            margin-bottom: 30px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>Q4 2024 Sales Report</h1>
        <p>Quarterly Performance Analysis</p>
    </div>

    <div class="summary">
        <div class="summary-card">
            <h3>Total Revenue</h3>
            <div class="value">$1.2M</div>
        </div>
        <div class="summary-card">
            <h3>New Customers</h3>
            <div class="value">340</div>
        </div>
        <div class="summary-card">
            <h3>Growth Rate</h3>
            <div class="value">23%</div>
        </div>
    </div>

    <h2>Sales by Region</h2>
    <table>
        <thead>
            <tr>
                <th>Region</th>
                <th style="text-align: right;">Q3 2024</th>
                <th style="text-align: right;">Q4 2024</th>
                <th style="text-align: right;">Change</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>North America</td>
                <td style="text-align: right;">$450,000</td>
                <td style="text-align: right;">$520,000</td>
                <td style="text-align: right; color: #28a745;">+15.6%</td>
            </tr>
            <tr>
                <td>Europe</td>
                <td style="text-align: right;">$380,000</td>
                <td style="text-align: right;">$440,000</td>
                <td style="text-align: right; color: #28a745;">+15.8%</td>
            </tr>
            <tr>
                <td>Asia Pacific</td>
                <td style="text-align: right;">$200,000</td>
                <td style="text-align: right;">$240,000</td>
                <td style="text-align: right; color: #28a745;">+20.0%</td>
            </tr>
        </tbody>
    </table>

    <h2>Top Products</h2>
    <table>
        <thead>
            <tr>
                <th>Product</th>
                <th style="text-align: right;">Units Sold</th>
                <th style="text-align: right;">Revenue</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>Product A</td>
                <td style="text-align: right;">1,250</td>
                <td style="text-align: right;">$625,000</td>
            </tr>
            <tr>
                <td>Product B</td>
                <td style="text-align: right;">890</td>
                <td style="text-align: right;">$445,000</td>
            </tr>
            <tr>
                <td>Product C</td>
                <td style="text-align: right;">670</td>
                <td style="text-align: right;">$335,000</td>
            </tr>
        </tbody>
    </table>
</body>
</html>`,

    newsletter: `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Monthly Newsletter</title>
    <style>
        body {
            font-family: 'Helvetica Neue', Arial, sans-serif;
            margin: 0;
            padding: 0;
            background: #f4f4f4;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background: white;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px 30px;
            text-align: center;
        }
        .header h1 { margin: 0; font-size: 2em; }
        .header p { margin: 10px 0 0 0; opacity: 0.9; }
        .content {
            padding: 30px;
        }
        .article {
            margin-bottom: 30px;
            padding-bottom: 30px;
            border-bottom: 1px solid #e0e0e0;
        }
        .article:last-child {
            border-bottom: none;
        }
        .article h2 {
            color: #667eea;
            margin-top: 0;
        }
        .article .meta {
            color: #999;
            font-size: 0.9em;
            margin-bottom: 10px;
        }
        .article p {
            line-height: 1.6;
            color: #333;
        }
        .cta-button {
            display: inline-block;
            background: #667eea;
            color: white;
            padding: 12px 30px;
            text-decoration: none;
            border-radius: 5px;
            margin-top: 10px;
        }
        .footer {
            background: #333;
            color: white;
            padding: 30px;
            text-align: center;
        }
        .footer p { margin: 5px 0; }
        .social-links {
            margin-top: 20px;
        }
        .social-links a {
            color: white;
            text-decoration: none;
            margin: 0 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Tech Insights</h1>
            <p>Your Monthly Technology Newsletter - January 2024</p>
        </div>

        <div class="content">
            <div class="article">
                <h2>The Future of AI in Development</h2>
                <div class="meta">By John Smith • January 10, 2024</div>
                <p>Artificial Intelligence is transforming how we write and maintain code.
                From intelligent code completion to automated testing, AI tools are becoming
                indispensable for modern developers.</p>
                <p>In this article, we explore the latest trends and how your team can
                leverage these technologies to improve productivity.</p>
                <a href="#" class="cta-button">Read More</a>
            </div>

            <div class="article">
                <h2>Cloud Native Applications</h2>
                <div class="meta">By Sarah Johnson • January 12, 2024</div>
                <p>Cloud-native development is more than just hosting in the cloud.
                It's about building applications that take full advantage of cloud computing
                models, including microservices, containers, and serverless architectures.</p>
                <a href="#" class="cta-button">Read More</a>
            </div>

            <div class="article">
                <h2>Security Best Practices</h2>
                <div class="meta">By Mike Chen • January 15, 2024</div>
                <p>With cyber threats evolving rapidly, it's crucial to stay updated on
                security best practices. Learn about the latest security frameworks and
                how to implement them in your projects.</p>
                <a href="#" class="cta-button">Read More</a>
            </div>
        </div>

        <div class="footer">
            <p><strong>Tech Insights Newsletter</strong></p>
            <p>123 Tech Street, Silicon Valley, CA 94000</p>
            <div class="social-links">
                <a href="#">Twitter</a> |
                <a href="#">LinkedIn</a> |
                <a href="#">GitHub</a>
            </div>
            <p style="margin-top: 20px; font-size: 0.8em;">
                © 2024 Tech Insights. All rights reserved.
            </p>
        </div>
    </div>
</body>
</html>`
};

// Initialize application
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    initializeTooltips();
    updateCharCount();
});

// Initialize Bootstrap tooltips
function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

// Initialize event listeners
function initializeEventListeners() {
    // Template selector
    document.getElementById('templateSelector').addEventListener('change', handleTemplateChange);

    // HTML content textarea
    document.getElementById('htmlContent').addEventListener('input', updateCharCount);

    // Scale slider
    document.getElementById('scale').addEventListener('input', handleScaleChange);

    // Header/Footer toggle
    document.getElementById('displayHeaderFooter').addEventListener('change', toggleHeaderFooterOptions);

    // Generate PDF button
    document.getElementById('generatePdfBtn').addEventListener('click', generatePdf);

    // Clear button
    document.getElementById('clearBtn').addEventListener('click', clearForm);
}

// Handle template selection
function handleTemplateChange(e) {
    const template = e.target.value;
    if (template && templates[template]) {
        document.getElementById('htmlContent').value = templates[template];
        updateCharCount();
    }
}

// Update character count
function updateCharCount() {
    const content = document.getElementById('htmlContent').value;
    document.getElementById('charCount').textContent = content.length.toLocaleString();
}

// Handle scale slider change
function handleScaleChange(e) {
    document.getElementById('scaleValue').textContent = e.target.value;
}

// Toggle header/footer options visibility
function toggleHeaderFooterOptions() {
    const display = document.getElementById('displayHeaderFooter').checked;
    document.getElementById('headerFooterOptions').style.display = display ? 'block' : 'none';
}

// Generate PDF
async function generatePdf() {
    const htmlContent = document.getElementById('htmlContent').value.trim();

    // Validation
    if (!htmlContent) {
        showAlert('Please enter HTML content', 'warning');
        return;
    }

    // Build PDF options
    const options = buildPdfOptions();

    // Prepare request
    const requestData = {
        content: htmlContent,
        options: options
    };

    // Show loading state
    setLoading(true);
    clearAlerts();

    try {
        const response = await fetch('/api/pdf/from-html', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP error! status: ${response.status}`);
        }

        // Download the PDF
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `document-${Date.now()}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);

        showAlert('PDF generated successfully!', 'success');
    } catch (error) {
        console.error('Error generating PDF:', error);
        showAlert(`Failed to generate PDF: ${error.message}`, 'danger');
    } finally {
        setLoading(false);
    }
}

// Build PDF options from form
function buildPdfOptions() {
    const options = {};

    // Paper format
    const paperFormat = document.getElementById('paperFormat').value;
    if (paperFormat) {
        options.paperFormat = paperFormat;
    }

    // Landscape
    const landscape = document.querySelector('input[name="orientation"]:checked').value === 'true';
    options.landscape = landscape;

    // Margins
    const margins = document.getElementById('margins').value.trim();
    if (margins) {
        options.margins = margins;
    }

    // Scale
    const scale = parseFloat(document.getElementById('scale').value);
    if (scale !== 1.0) {
        options.scale = scale;
    }

    // Print background
    options.printBackground = document.getElementById('printBackground').checked;

    // Header/Footer
    const displayHeaderFooter = document.getElementById('displayHeaderFooter').checked;
    if (displayHeaderFooter) {
        options.displayHeaderFooter = true;

        const headerTemplate = document.getElementById('headerTemplate').value.trim();
        if (headerTemplate) {
            options.headerTemplate = headerTemplate;
        }

        const footerTemplate = document.getElementById('footerTemplate').value.trim();
        if (footerTemplate) {
            options.footerTemplate = footerTemplate;
        }
    }

    // Page ranges
    const pageRanges = document.getElementById('pageRanges').value.trim();
    if (pageRanges) {
        options.pageRanges = pageRanges;
    }

    // Prefer CSS page size
    const preferCssPageSize = document.getElementById('preferCssPageSize').checked;
    if (preferCssPageSize) {
        options.preferCssPageSize = true;
    }

    return options;
}

// Show alert message
function showAlert(message, type) {
    const alertContainer = document.getElementById('alertContainer');
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.role = 'alert';
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    alertContainer.appendChild(alert);

    // Auto-dismiss success alerts after 5 seconds
    if (type === 'success') {
        setTimeout(() => {
            alert.remove();
        }, 5000);
    }
}

// Clear all alerts
function clearAlerts() {
    document.getElementById('alertContainer').innerHTML = '';
}

// Set loading state
function setLoading(loading) {
    const button = document.getElementById('generatePdfBtn');
    const spinner = document.getElementById('loadingSpinner');

    if (loading) {
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Generating...';
        spinner.style.display = 'block';
    } else {
        button.disabled = false;
        button.innerHTML = '<i class="bi bi-file-earmark-pdf me-2"></i>Generate PDF';
        spinner.style.display = 'none';
    }
}

// Clear form
function clearForm() {
    if (confirm('Are you sure you want to clear the form?')) {
        document.getElementById('htmlContent').value = '';
        document.getElementById('templateSelector').value = '';
        document.getElementById('paperFormat').value = 'LETTER';
        document.getElementById('orientationPortrait').checked = true;
        document.getElementById('margins').value = '1cm';
        document.getElementById('scale').value = '1.0';
        document.getElementById('scaleValue').textContent = '1.0';
        document.getElementById('printBackground').checked = true;
        document.getElementById('displayHeaderFooter').checked = false;
        document.getElementById('headerTemplate').value = '';
        document.getElementById('footerTemplate').value = '';
        document.getElementById('pageRanges').value = '';
        document.getElementById('preferCssPageSize').checked = false;
        toggleHeaderFooterOptions();
        updateCharCount();
        clearAlerts();
    }
}
