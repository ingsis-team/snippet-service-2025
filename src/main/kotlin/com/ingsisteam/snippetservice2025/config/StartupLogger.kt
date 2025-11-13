package com.ingsisteam.snippetservice2025.config

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupLogger {

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        // Service started successfully
    }
}
