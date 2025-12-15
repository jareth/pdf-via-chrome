package com.github.headlesschromepdf.api;

/**
 * Configuration options for PDF generation.
 *
 * This class provides a builder-style API for configuring PDF output parameters
 * such as page size, margins, orientation, and rendering preferences. These options
 * map to the Chrome DevTools Protocol Page.printToPDF parameters.
 */
public class PdfOptions {

    private final boolean landscape;
    private final boolean displayHeaderFooter;
    private final boolean printBackground;
    private final double scale;
    private final double paperWidth;
    private final double paperHeight;
    private final double marginTop;
    private final double marginBottom;
    private final double marginLeft;
    private final double marginRight;
    private final String pageRanges;
    private final String headerTemplate;
    private final String footerTemplate;
    private final boolean preferCssPageSize;

    private PdfOptions(Builder builder) {
        this.landscape = builder.landscape;
        this.displayHeaderFooter = builder.displayHeaderFooter;
        this.printBackground = builder.printBackground;
        this.scale = builder.scale;
        this.paperWidth = builder.paperWidth;
        this.paperHeight = builder.paperHeight;
        this.marginTop = builder.marginTop;
        this.marginBottom = builder.marginBottom;
        this.marginLeft = builder.marginLeft;
        this.marginRight = builder.marginRight;
        this.pageRanges = builder.pageRanges;
        this.headerTemplate = builder.headerTemplate;
        this.footerTemplate = builder.footerTemplate;
        this.preferCssPageSize = builder.preferCssPageSize;
    }

    public boolean isLandscape() {
        return landscape;
    }

    public boolean isDisplayHeaderFooter() {
        return displayHeaderFooter;
    }

    public boolean isPrintBackground() {
        return printBackground;
    }

    public double getScale() {
        return scale;
    }

    public double getPaperWidth() {
        return paperWidth;
    }

    public double getPaperHeight() {
        return paperHeight;
    }

    public double getMarginTop() {
        return marginTop;
    }

    public double getMarginBottom() {
        return marginBottom;
    }

    public double getMarginLeft() {
        return marginLeft;
    }

    public double getMarginRight() {
        return marginRight;
    }

    public String getPageRanges() {
        return pageRanges;
    }

    public String getHeaderTemplate() {
        return headerTemplate;
    }

    public String getFooterTemplate() {
        return footerTemplate;
    }

    public boolean isPreferCssPageSize() {
        return preferCssPageSize;
    }

    /**
     * Creates a new builder for PdfOptions.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates PdfOptions with default settings.
     *
     * @return PdfOptions with default values
     */
    public static PdfOptions defaults() {
        return new Builder().build();
    }

    /**
     * Builder class for constructing PdfOptions instances.
     */
    public static class Builder {
        private boolean landscape = false;
        private boolean displayHeaderFooter = false;
        private boolean printBackground = false;
        private double scale = 1.0;
        private double paperWidth = 8.5; // inches (US Letter width)
        private double paperHeight = 11.0; // inches (US Letter height)
        private double marginTop = 0.4; // inches (default Chrome margin)
        private double marginBottom = 0.4;
        private double marginLeft = 0.4;
        private double marginRight = 0.4;
        private String pageRanges = "";
        private String headerTemplate = "";
        private String footerTemplate = "";
        private boolean preferCssPageSize = false;

        /**
         * Parses a margin string with unit (e.g., "1cm", "0.5in", "10px") and converts to inches.
         *
         * @param marginStr the margin string with unit
         * @return the margin value in inches
         * @throws IllegalArgumentException if the format is invalid or unit is not supported
         */
        private double parseMargin(String marginStr) {
            if (marginStr == null || marginStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Margin string cannot be null or empty");
            }

            String trimmed = marginStr.trim();

            // Extract number and unit using regex (allow optional minus sign for better error messages)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(-?[0-9]*\\.?[0-9]+)\\s*(cm|in|px)$");
            java.util.regex.Matcher matcher = pattern.matcher(trimmed);

            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                    "Invalid margin format: '" + marginStr + "'. Expected format: number + unit (e.g., '1cm', '0.5in', '10px')"
                );
            }

            double value = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);

            if (value < 0) {
                throw new IllegalArgumentException("Margin value cannot be negative");
            }

            // Convert to inches based on unit
            switch (unit) {
                case "in":
                    return value;
                case "cm":
                    return value / 2.54; // 1 inch = 2.54 cm
                case "px":
                    return value / 96.0; // 1 inch = 96 pixels (CSS standard)
                default:
                    throw new IllegalArgumentException("Unsupported margin unit: " + unit);
            }
        }

        /**
         * Sets the page orientation.
         *
         * @param landscape true for landscape orientation, false for portrait
         * @return this builder
         */
        public Builder landscape(boolean landscape) {
            this.landscape = landscape;
            return this;
        }

        /**
         * Sets whether to display header and footer.
         * If enabled, you should also set headerTemplate and footerTemplate.
         *
         * @param displayHeaderFooter true to display header/footer, false otherwise
         * @return this builder
         */
        public Builder displayHeaderFooter(boolean displayHeaderFooter) {
            this.displayHeaderFooter = displayHeaderFooter;
            return this;
        }

        /**
         * Sets whether to print background graphics.
         *
         * @param printBackground true to print backgrounds, false otherwise
         * @return this builder
         */
        public Builder printBackground(boolean printBackground) {
            this.printBackground = printBackground;
            return this;
        }

        /**
         * Sets the scale of the webpage rendering.
         * Must be between 0.1 and 2.0.
         *
         * @param scale the scale factor (default: 1.0)
         * @return this builder
         * @throws IllegalArgumentException if scale is not between 0.1 and 2.0
         */
        public Builder scale(double scale) {
            if (scale < 0.1 || scale > 2.0) {
                throw new IllegalArgumentException("Scale must be between 0.1 and 2.0");
            }
            this.scale = scale;
            return this;
        }

        /**
         * Sets the paper width in inches.
         *
         * @param width the paper width in inches
         * @return this builder
         * @throws IllegalArgumentException if width is not positive
         */
        public Builder paperWidth(double width) {
            if (width <= 0) {
                throw new IllegalArgumentException("Paper width must be positive");
            }
            this.paperWidth = width;
            return this;
        }

        /**
         * Sets the paper height in inches.
         *
         * @param height the paper height in inches
         * @return this builder
         * @throws IllegalArgumentException if height is not positive
         */
        public Builder paperHeight(double height) {
            if (height <= 0) {
                throw new IllegalArgumentException("Paper height must be positive");
            }
            this.paperHeight = height;
            return this;
        }

        /**
         * Sets paper size using a predefined format.
         *
         * @param format the paper format (e.g., A4, LETTER, LEGAL)
         * @return this builder
         */
        public Builder paperSize(PaperFormat format) {
            this.paperWidth = format.getWidth();
            this.paperHeight = format.getHeight();
            return this;
        }

        /**
         * Sets the top margin in inches.
         *
         * @param margin the top margin in inches
         * @return this builder
         * @throws IllegalArgumentException if margin is negative
         */
        public Builder marginTop(double margin) {
            if (margin < 0) {
                throw new IllegalArgumentException("Margin cannot be negative");
            }
            this.marginTop = margin;
            return this;
        }

        /**
         * Sets the top margin with unit (e.g., "1cm", "0.5in", "10px").
         *
         * @param marginWithUnit the margin string with unit
         * @return this builder
         * @throws IllegalArgumentException if format is invalid or unit is not supported
         */
        public Builder marginTop(String marginWithUnit) {
            this.marginTop = parseMargin(marginWithUnit);
            return this;
        }

        /**
         * Sets the bottom margin in inches.
         *
         * @param margin the bottom margin in inches
         * @return this builder
         * @throws IllegalArgumentException if margin is negative
         */
        public Builder marginBottom(double margin) {
            if (margin < 0) {
                throw new IllegalArgumentException("Margin cannot be negative");
            }
            this.marginBottom = margin;
            return this;
        }

        /**
         * Sets the bottom margin with unit (e.g., "1cm", "0.5in", "10px").
         *
         * @param marginWithUnit the margin string with unit
         * @return this builder
         * @throws IllegalArgumentException if format is invalid or unit is not supported
         */
        public Builder marginBottom(String marginWithUnit) {
            this.marginBottom = parseMargin(marginWithUnit);
            return this;
        }

        /**
         * Sets the left margin in inches.
         *
         * @param margin the left margin in inches
         * @return this builder
         * @throws IllegalArgumentException if margin is negative
         */
        public Builder marginLeft(double margin) {
            if (margin < 0) {
                throw new IllegalArgumentException("Margin cannot be negative");
            }
            this.marginLeft = margin;
            return this;
        }

        /**
         * Sets the left margin with unit (e.g., "1cm", "0.5in", "10px").
         *
         * @param marginWithUnit the margin string with unit
         * @return this builder
         * @throws IllegalArgumentException if format is invalid or unit is not supported
         */
        public Builder marginLeft(String marginWithUnit) {
            this.marginLeft = parseMargin(marginWithUnit);
            return this;
        }

        /**
         * Sets the right margin in inches.
         *
         * @param margin the right margin in inches
         * @return this builder
         * @throws IllegalArgumentException if margin is negative
         */
        public Builder marginRight(double margin) {
            if (margin < 0) {
                throw new IllegalArgumentException("Margin cannot be negative");
            }
            this.marginRight = margin;
            return this;
        }

        /**
         * Sets the right margin with unit (e.g., "1cm", "0.5in", "10px").
         *
         * @param marginWithUnit the margin string with unit
         * @return this builder
         * @throws IllegalArgumentException if format is invalid or unit is not supported
         */
        public Builder marginRight(String marginWithUnit) {
            this.marginRight = parseMargin(marginWithUnit);
            return this;
        }

        /**
         * Sets all margins to the same value in inches.
         *
         * @param margin the margin in inches for all sides
         * @return this builder
         * @throws IllegalArgumentException if margin is negative
         */
        public Builder margins(double margin) {
            return marginTop(margin)
                .marginBottom(margin)
                .marginLeft(margin)
                .marginRight(margin);
        }

        /**
         * Sets all margins to the same value with unit (e.g., "1cm", "0.5in", "10px").
         *
         * @param marginWithUnit the margin string with unit for all sides
         * @return this builder
         * @throws IllegalArgumentException if format is invalid or unit is not supported
         */
        public Builder margins(String marginWithUnit) {
            double marginInInches = parseMargin(marginWithUnit);
            return marginTop(marginInInches)
                .marginBottom(marginInInches)
                .marginLeft(marginInInches)
                .marginRight(marginInInches);
        }

        /**
         * Sets the page ranges to print.
         * Format: "1-5, 8, 11-13" (empty string prints all pages)
         *
         * @param pageRanges the page ranges string
         * @return this builder
         */
        public Builder pageRanges(String pageRanges) {
            this.pageRanges = pageRanges != null ? pageRanges : "";
            return this;
        }

        /**
         * Sets the HTML template for the header.
         * Should be valid HTML with specific classes for page number, etc.
         *
         * @param template the header HTML template
         * @return this builder
         */
        public Builder headerTemplate(String template) {
            this.headerTemplate = template != null ? template : "";
            return this;
        }

        /**
         * Sets the HTML template for the footer.
         * Should be valid HTML with specific classes for page number, etc.
         *
         * @param template the footer HTML template
         * @return this builder
         */
        public Builder footerTemplate(String template) {
            this.footerTemplate = template != null ? template : "";
            return this;
        }

        /**
         * Sets whether to prefer CSS-defined page size over the provided paper size.
         *
         * @param preferCssPageSize true to prefer CSS page size, false otherwise
         * @return this builder
         */
        public Builder preferCssPageSize(boolean preferCssPageSize) {
            this.preferCssPageSize = preferCssPageSize;
            return this;
        }

        /**
         * Builds the PdfOptions instance.
         *
         * @return a new PdfOptions instance
         */
        public PdfOptions build() {
            return new PdfOptions(this);
        }
    }

    /**
     * Common paper formats with dimensions in inches.
     */
    public enum PaperFormat {
        LETTER(8.5, 11.0),
        LEGAL(8.5, 14.0),
        TABLOID(11.0, 17.0),
        LEDGER(17.0, 11.0),
        A0(33.1, 46.8),
        A1(23.4, 33.1),
        A2(16.5, 23.4),
        A3(11.7, 16.5),
        A4(8.27, 11.7),
        A5(5.83, 8.27),
        A6(4.13, 5.83);

        private final double width;
        private final double height;

        PaperFormat(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }
    }
}
