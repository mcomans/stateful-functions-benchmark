package types.product

import java.util.*

class GetProductResponse(val price: Int, val stock: Int, val requestId: String = UUID.randomUUID().toString())