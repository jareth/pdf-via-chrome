package com.github.headlesschromepdf.testapp.dto;

import com.github.headlesschromepdf.api.PdfOptions;

/**
 * DTO for PDF generation options, used in REST API requests.
 * All fields are optional and will use library defaults if not specified.
 */
public class PdfOptionsDto {

    private Boolean landscape;
    private Boolean displayHeaderFooter;
    private Boolean printBackground;
    private Double scale;
    private String paperFormat; // e.g., "A4", "LETTER", "LEGAL"
    private Double paperWidth; // in inches
    private Double paperHeight; // in inches
    private String marginTop; // e.g., "1cm", "0.5in", "10px"
    private String marginBottom;
    private String marginLeft;
    private String marginRight;
    private String margins; // set all margins at once
    private String pageRanges;
    private String headerTemplate;
    private String footerTemplate;
    private Boolean preferCssPageSize;

    public PdfOptionsDto() {
    }

    /**
     * Converts this DTO to a PdfOptions instance.
     *
     * @return PdfOptions instance built from this DTO
     */
    public PdfOptions toPdfOptions() {
        PdfOptions.Builder builder = PdfOptions.builder();

        if (landscape != null) {
            builder.landscape(landscape);
        }
        if (displayHeaderFooter != null) {
            builder.displayHeaderFooter(displayHeaderFooter);
        }
        if (printBackground != null) {
            builder.printBackground(printBackground);
        }
        if (scale != null) {
            builder.scale(scale);
        }

        // Handle paper size - either format or explicit dimensions
        if (paperFormat != null) {
            try {
                PdfOptions.PaperFormat format = PdfOptions.PaperFormat.valueOf(paperFormat.toUpperCase());
                builder.paperSize(format);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid paper format: " + paperFormat +
                    ". Valid values are: LETTER, LEGAL, TABLOID, LEDGER, A0, A1, A2, A3, A4, A5, A6");
            }
        } else {
            if (paperWidth != null) {
                builder.paperWidth(paperWidth);
            }
            if (paperHeight != null) {
                builder.paperHeight(paperHeight);
            }
        }

        // Handle margins - either all at once or individually
        if (margins != null) {
            builder.margins(margins);
        } else {
            if (marginTop != null) {
                builder.marginTop(marginTop);
            }
            if (marginBottom != null) {
                builder.marginBottom(marginBottom);
            }
            if (marginLeft != null) {
                builder.marginLeft(marginLeft);
            }
            if (marginRight != null) {
                builder.marginRight(marginRight);
            }
        }

        if (pageRanges != null) {
            builder.pageRanges(pageRanges);
        }
        if (headerTemplate != null) {
            builder.headerTemplate(headerTemplate);
        }
        if (footerTemplate != null) {
            builder.footerTemplate(footerTemplate);
        }
        if (preferCssPageSize != null) {
            builder.preferCssPageSize(preferCssPageSize);
        }

        return builder.build();
    }

    // Getters and Setters

    public Boolean getLandscape() {
        return landscape;
    }

    public void setLandscape(Boolean landscape) {
        this.landscape = landscape;
    }

    public Boolean getDisplayHeaderFooter() {
        return displayHeaderFooter;
    }

    public void setDisplayHeaderFooter(Boolean displayHeaderFooter) {
        this.displayHeaderFooter = displayHeaderFooter;
    }

    public Boolean getPrintBackground() {
        return printBackground;
    }

    public void setPrintBackground(Boolean printBackground) {
        this.printBackground = printBackground;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public String getPaperFormat() {
        return paperFormat;
    }

    public void setPaperFormat(String paperFormat) {
        this.paperFormat = paperFormat;
    }

    public Double getPaperWidth() {
        return paperWidth;
    }

    public void setPaperWidth(Double paperWidth) {
        this.paperWidth = paperWidth;
    }

    public Double getPaperHeight() {
        return paperHeight;
    }

    public void setPaperHeight(Double paperHeight) {
        this.paperHeight = paperHeight;
    }

    public String getMarginTop() {
        return marginTop;
    }

    public void setMarginTop(String marginTop) {
        this.marginTop = marginTop;
    }

    public String getMarginBottom() {
        return marginBottom;
    }

    public void setMarginBottom(String marginBottom) {
        this.marginBottom = marginBottom;
    }

    public String getMarginLeft() {
        return marginLeft;
    }

    public void setMarginLeft(String marginLeft) {
        this.marginLeft = marginLeft;
    }

    public String getMarginRight() {
        return marginRight;
    }

    public void setMarginRight(String marginRight) {
        this.marginRight = marginRight;
    }

    public String getMargins() {
        return margins;
    }

    public void setMargins(String margins) {
        this.margins = margins;
    }

    public String getPageRanges() {
        return pageRanges;
    }

    public void setPageRanges(String pageRanges) {
        this.pageRanges = pageRanges;
    }

    public String getHeaderTemplate() {
        return headerTemplate;
    }

    public void setHeaderTemplate(String headerTemplate) {
        this.headerTemplate = headerTemplate;
    }

    public String getFooterTemplate() {
        return footerTemplate;
    }

    public void setFooterTemplate(String footerTemplate) {
        this.footerTemplate = footerTemplate;
    }

    public Boolean getPreferCssPageSize() {
        return preferCssPageSize;
    }

    public void setPreferCssPageSize(Boolean preferCssPageSize) {
        this.preferCssPageSize = preferCssPageSize;
    }
}
