package com.ingsisteam.snippetservice2025.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestIdFilterTest {

    private lateinit var filter: RequestIdFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var chain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = RequestIdFilter()
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        chain = mockk(relaxed = true)
        MDC.clear() // Clear MDC before each test
    }

    @Test
    fun `doFilter should generate and set new Request ID when header is not present`() {
        every { request.getHeader(RequestIdFilter.REQUEST_ID_HEADER) } returns null
        every { request.method } returns "GET"
        every { request.requestURI } returns "/test"
        every { request.remoteAddr } returns "127.0.0.1"

        val requestIdSlot = slot<String>()
        val mdcValueAtChainCall = mutableMapOf<String, String>()

        every { chain.doFilter(capture(slot()), capture(slot())) } answers {
            mdcValueAtChainCall.putAll(MDC.getCopyOfContextMap())
        }

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { response.setHeader(eq(RequestIdFilter.REQUEST_ID_HEADER), capture(requestIdSlot)) }
        val generatedRequestId = requestIdSlot.captured

        assertNotNull(generatedRequestId)
        assertEquals(generatedRequestId, mdcValueAtChainCall[RequestIdFilter.REQUEST_ID_MDC_KEY])
        verify(exactly = 1) { chain.doFilter(request, response) }
        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)) // MDC should be cleaned up
    }

    @Test
    fun `doFilter should use existing Request ID when header is present`() {
        val existingRequestId = "existing-id"
        every { request.getHeader(RequestIdFilter.REQUEST_ID_HEADER) } returns existingRequestId
        every { request.method } returns "POST"
        every { request.requestURI } returns "/data"
        every { request.remoteAddr } returns "192.168.1.1"

        val requestIdSlot = slot<String>()
        val mdcValueAtChainCall = mutableMapOf<String, String>()

        every { chain.doFilter(capture(slot()), capture(slot())) } answers {
            mdcValueAtChainCall.putAll(MDC.getCopyOfContextMap())
        }

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { response.setHeader(eq(RequestIdFilter.REQUEST_ID_HEADER), capture(requestIdSlot)) }
        val finalRequestId = requestIdSlot.captured

        assertEquals(existingRequestId, finalRequestId)
        assertEquals(existingRequestId, mdcValueAtChainCall[RequestIdFilter.REQUEST_ID_MDC_KEY])
        verify(exactly = 1) { chain.doFilter(request, response) }
        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)) // MDC should be cleaned up
    }

    @Test
    fun `doFilter should clean up MDC even if chain throws an exception`() {
        every { request.getHeader(RequestIdFilter.REQUEST_ID_HEADER) } returns null
        every { chain.doFilter(any(), any()) } throws RuntimeException("Test exception")

        try {
            filter.doFilter(request, response, chain)
        } catch (e: RuntimeException) {
            // Expected exception
        }

        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)) // MDC should be cleaned up
        verify(exactly = 1) { chain.doFilter(request, response) } // chain.doFilter should be called once
    }

}
