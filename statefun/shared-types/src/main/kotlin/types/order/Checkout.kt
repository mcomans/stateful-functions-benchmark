package types.order

import types.WrappedMessage

class Checkout(val shoppingCartId: String, val userId: String) : WrappedMessage