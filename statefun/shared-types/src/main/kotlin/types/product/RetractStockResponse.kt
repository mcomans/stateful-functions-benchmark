package types.product

import types.WrappedMessage

class RetractStockResponse(val success: Boolean, val amount: Int, val price: Int) : WrappedMessage