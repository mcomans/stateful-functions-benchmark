package types.shoppingcart

import java.util.*

class AddToCart(val productId: String, val amount: Int, val requestId: String = UUID.randomUUID().toString())