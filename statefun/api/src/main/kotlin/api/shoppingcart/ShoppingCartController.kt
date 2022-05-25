package api.shoppingcart

import api.logging.RequestInfo
import api.logging.sendLogged
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import types.MessageWrapper
import types.shoppingcart.AddToCart
import types.shoppingcart.RemoveFromCart
import java.util.*

@RestController
@RequestMapping("/shopping-carts")
class ShoppingCartController(val kafkaTemplate: KafkaTemplate<String, Any>, val requestInfo: RequestInfo) {

    private val logger: Logger = LoggerFactory.getLogger(ShoppingCartController::class.java)

    @PostMapping()
    fun createShoppingCart(): String {
        return UUID.randomUUID().toString()
    }

    @PostMapping("/{cartId}/products")
    fun addToCart(@PathVariable cartId: String, @RequestBody product: ShoppingCartProduct) {
        kafkaTemplate.sendLogged("add-to-cart", cartId, MessageWrapper(requestInfo.requestId, AddToCart(product.productId, product.amount)), logger)
    }

    @DeleteMapping("/{cartId}/products")
    fun removeFromCart(@PathVariable cartId: String, @RequestBody product: ShoppingCartProduct) {
        kafkaTemplate.sendLogged("remove-from-cart", cartId, MessageWrapper(requestInfo.requestId, RemoveFromCart(product.productId, product.amount)), logger)
    }

    data class ShoppingCartProduct(val productId: String, val amount: Int)
}