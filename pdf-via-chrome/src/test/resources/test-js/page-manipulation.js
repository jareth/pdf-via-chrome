// JavaScript for page manipulation before PDF generation

// Remove all elements with class 'removable'
function removeElements() {
    const removables = document.querySelectorAll('.removable');
    removables.forEach(element => element.remove());
    console.log(`Removed ${removables.length} elements`);
}

// Show dynamic content
function showDynamicContent() {
    const dynamicElements = document.querySelectorAll('.dynamic-content');
    dynamicElements.forEach(element => {
        element.style.display = 'block';
    });
    console.log(`Showed ${dynamicElements.length} dynamic elements`);
}

// Add a watermark to the document
function addWatermark(text) {
    const watermark = document.createElement('div');
    watermark.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%) rotate(-45deg);
        font-size: 80px;
        color: rgba(200, 200, 200, 0.3);
        font-weight: bold;
        z-index: 9999;
        pointer-events: none;
        white-space: nowrap;
    `;
    watermark.textContent = text || 'DRAFT';
    document.body.appendChild(watermark);
    console.log('Watermark added:', text);
}

// Modify page title
function modifyTitle(newTitle) {
    document.title = newTitle;
    const h1Elements = document.querySelectorAll('h1');
    if (h1Elements.length > 0) {
        h1Elements[0].textContent = newTitle;
    }
    console.log('Title modified to:', newTitle);
}

// Add a custom footer text to each page
function addCustomFooter(footerText) {
    const pages = document.querySelectorAll('.page-break');
    pages.forEach((page, index) => {
        const footer = document.createElement('div');
        footer.style.cssText = `
            margin-top: 20px;
            padding-top: 10px;
            border-top: 1px solid #cccccc;
            font-size: 10px;
            color: #666666;
            text-align: center;
        `;
        footer.textContent = `${footerText} - Page ${index + 1}`;
        page.parentNode.insertBefore(footer, page);
    });
    console.log('Custom footers added:', footerText);
}

// Highlight specific text
function highlightText(searchText) {
    const bodyText = document.body.innerHTML;
    const highlightedText = bodyText.replace(
        new RegExp(searchText, 'gi'),
        '<span style="background-color: yellow; font-weight: bold;">$&</span>'
    );
    document.body.innerHTML = highlightedText;
    console.log('Highlighted text:', searchText);
}

// Execute all default manipulations
function executeAllManipulations() {
    removeElements();
    showDynamicContent();
    console.log('All manipulations executed');
}

// Main execution - this will be called when the script is injected
// Comment this out if you want to call specific functions instead
executeAllManipulations();
