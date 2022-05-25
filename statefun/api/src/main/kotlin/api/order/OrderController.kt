package api.order

import api.logging.LoggingFilter
import api.logging.RequestInfo
import api.logging.sendLogged
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import types.MessageWrapper
import types.order.Checkout
import java.util.*

@RestController
@RequestMapping("/orders")
class OrderController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {
    private val logger: Logger = LoggerFactory.getLogger(OrderController::class.java)

    @PostMapping("/checkout")
    fun checkout(@RequestBody orderCheckout: OrderCheckout) {
        val orderId = UUID.randomUUID().toString()

        kafkaTemplate.sendLogged("checkout", orderId, MessageWrapper(requestInfo.requestId, Checkout(orderCheckout.cartId, orderCheckout.userId)), logger)
    }

    data class OrderCheckout(val cartId: String, val userId: String)
}