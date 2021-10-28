package api.shoppingcart

import api.logging.RequestInfo
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import types.MessageWrapper
import types.shoppingcart.AddToCart
import types.shoppingcart.RemoveFromCart
import java.util.*

@RestController
@RequestMapping("/shopping-carts")
class ShoppingCartController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {
    @PostMapping()
    fun createShoppingCart(): String {
        return UUID.randomUUID().toString()
    }

    @PostMapping("/{cartId}")
    fun addToCart(@PathVariable cartId: String, @RequestBody product: ShoppingCartProduct) {
        kafkaTemplate.send("add-to-cart", cartId, MessageWrapper(requestInfo.requestId, AddToCart(product.productId, product.amount)))
    }

    @DeleteMapping("/{cartId}/products")
    fun removeFromCart(@PathVariable cartId: String, @RequestBody product: ShoppingCartProduct) {
        kafkaTemplate.send("remove-from-cart", cartId, MessageWrapper(requestInfo.requestId, RemoveFromCart(product.productId, product.amount)))
    }

    data class ShoppingCartProduct(val productId: String, val amount: Int)
}