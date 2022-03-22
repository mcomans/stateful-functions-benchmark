package api.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Component
@Order(1)
class LoggingFilter(val requestInfo: RequestInfo) : Filter {
    private val logger: Logger = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request is HttpServletRequest) {
            MDC.put("requestId", requestInfo.requestId)
            MDC.put("status", "HTTP_EXECUTING")
            logger.info("Request started: ${requestInfo.requestId}")
            chain.doFilter(request, response)
            MDC.remove("requestId")
            MDC.remove("status")
        }
    }
}