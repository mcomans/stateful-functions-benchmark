package types.shoppingcart

import java.util.*

class GetCartResponse(
    val contents: List<CartResponseProduct>, val requestId: String = UUID.randomUUID().toString()) {
    class CartResponseProduct(val productId: String, val amount: Int)
}