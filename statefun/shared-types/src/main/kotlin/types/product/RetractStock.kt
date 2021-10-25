package types.product

import java.util.*

class RetractStock (val amount: Int, val requestId: String = UUID.randomUUID().toString())