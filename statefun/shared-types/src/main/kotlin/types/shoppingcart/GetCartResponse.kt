package types.shoppingcart

class GetCartResponse(
    val contents: List<CartResponseProduct>, val requestId: String
) {
    class CartResponseProduct(val productId: String, val amount: Int)
}