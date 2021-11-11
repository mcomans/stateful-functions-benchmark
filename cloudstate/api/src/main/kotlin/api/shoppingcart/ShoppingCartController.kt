package api.shoppingcart

import api.logging.RequestInfo
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.web.bind.annotation.*
import shoppingcart.ShoppingCartServiceGrpc
import shoppingcart.Shoppingcart
import java.util.*

@RestController
@RequestMapping("/shopping-carts")
class ShoppingCartController(val requestInfo: RequestInfo) {

    @GrpcClient("shoppingcart-service")
    private lateinit var shoppingCartStub: ShoppingCartServiceGrpc.ShoppingCartServiceBlockingStub;

    @PostMapping()
    fun createShoppingCart(): String {
        return UUID.randomUUID().toString()
    }

    @PostMapping("/{cartId}/products")
    fun addToCart(@PathVariable cartId: String, @RequestBody product: ShoppingCartProduct) {
        shoppingCartStub.addToCart(
            Shoppingcart.AddToCartMessage.newBuilder()
                .setCartId(cartId)
                .setProductId(product.productId)
                .setAmount(product.amount)
                .build()
        )
    }

    @DeleteMapping("/{cartId}/products")
    fun removeFromCart(@PathVariable cartId: String, @RequestBody product: ShoppingCartProduct) {
        shoppingCartStub.removeFromCart(
            Shoppingcart.RemoveFromCartMessage.newBuilder()
                .setCartId(cartId)
                .setProductId(product.productId)
                .setAmount(product.amount)
                .build()
        )
    }

    data class ShoppingCartProduct(val productId: String, val amount: Int)
}