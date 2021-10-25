package types.order

import java.util.*

class Checkout(val shoppingCartId: String, val userId: String, val requestId: String = UUID.randomUUID().toString())