package types.product

import java.util.*

class RetractStockResponse(val success: Boolean, val amount: Int, val price: Int, val requestId: String = UUID.randomUUID().toString())