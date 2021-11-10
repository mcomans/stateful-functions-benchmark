package api.order

import api.logging.RequestInfo
import net.devh.boot.grpc.client.inject.GrpcClient
import order.Order
import order.OrderServiceGrpc
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping
class OrderController(val requestInfo: RequestInfo) {

    @GrpcClient("order-service")
    private lateinit var orderStub: OrderServiceGrpc.OrderServiceBlockingStub;

    @PostMapping("/checkout")
    fun checkout(@RequestBody orderCheckout: OrderCheckout) {
        val orderId = UUID.randomUUID().toString()

        orderStub.checkout(
            Order.CheckoutMessage.newBuilder()
                .setOrderId(orderId)
                .setUserId(orderCheckout.userId)
                .setCartId(orderCheckout.cartId)
                .build()
        )
    }

    data class OrderCheckout(val cartId: String, val userId: String)
}