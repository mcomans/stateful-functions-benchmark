package types.shoppingcart

import types.WrappedMessage

class GetCartResponse(
    val contents: List<CartResponseProduct>
) : WrappedMessage {
    class CartResponseProduct(val productId: String, val amount: Int)
}