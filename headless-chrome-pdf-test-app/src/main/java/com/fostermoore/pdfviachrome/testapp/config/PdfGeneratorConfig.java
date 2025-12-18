package com.fostermoore.pdfviachrome.testapp.config;

import com.fostermoore.pdfviachrome.api.PdfGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Spring configuration for PdfGenerator bean.
 */
@Configuration
public class PdfGeneratorConfig {

    private static final Logger logger = LoggerFactory.getLogger(PdfGeneratorConfig.class);

    /**
     * Creates a singleton PdfGenerator bean for the application.
     * The generator uses lazy initialization and is thread-safe.
     *
     * @return configured PdfGenerator instance
     */
    @Bean(destroyMethod = "close")
    public PdfGenerator pdfGenerator() {
        logger.info("Initializing PdfGenerator bean");

        return PdfGenerator.create()
            .withTimeout(Duration.ofSeconds(30))
            .withHeadless(true)
            .build();
    }
}
