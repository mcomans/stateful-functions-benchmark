package types.shoppingcart

class GetCartResponse(val cartId: String, val contents: List<CartResponseProduct>) {
    class CartResponseProduct(val productId: String, val amount: Int)
}