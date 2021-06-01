package types.product

class RetractStockResponse(val productId: String, val success: Boolean, val amount: Int, val price: Int)