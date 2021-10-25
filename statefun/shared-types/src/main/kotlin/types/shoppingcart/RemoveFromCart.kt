package types.shoppingcart

import java.util.*

class RemoveFromCart(val productId: String, val amount: Int, val requestId: String = UUID.randomUUID().toString())