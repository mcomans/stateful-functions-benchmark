package types.product

import java.util.*

class AddStock(val amount: Int, val requestId: String = UUID.randomUUID().toString())