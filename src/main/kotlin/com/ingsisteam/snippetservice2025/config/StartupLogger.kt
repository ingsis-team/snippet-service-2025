package com.ingsisteam.snippetservice2025.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupLogger {
    private val logger = LoggerFactory.getLogger(StartupLogger::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("\n============================================================")
        logger.info("🔐 PERMISSION SERVICE IS RUNNING!")
        logger.info("📍 Server: http://localhost:8080")
        logger.info("📚 API Docs: http://localhost:8080/swagger-ui.html")
        logger.info("============================================================\n")
    }
}
