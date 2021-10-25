package types.user

import java.util.*

class RetractCreditResponse(val success: Boolean, val requestId: String = UUID.randomUUID().toString())