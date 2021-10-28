package api.order

import api.logging.RequestInfo
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import types.MessageWrapper
import types.order.Checkout
import java.util.*

@RestController
@RequestMapping
class OrderController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {

    @PostMapping("/checkout")
    fun checkout(@RequestBody orderCheckout: OrderCheckout) {
        val orderId = UUID.randomUUID().toString()
        kafkaTemplate.send("checkout", orderId, MessageWrapper(requestInfo.requestId, Checkout(orderCheckout.cartId, orderCheckout.userId)))
    }

    data class OrderCheckout(val cartId: String, val userId: String)
}