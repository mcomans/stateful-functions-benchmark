package types.product

import types.WrappedMessage

class GetProductResponse(val price: Int, val stock: Int) : WrappedMessage