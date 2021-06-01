package user

import createJsonType
import types.user.AddCredit
import types.user.RetractCredit
import types.user.RetractCreditResponse

object UserMessages {
    val ADD_CREDIT = createJsonType("user", AddCredit::class)
    val RETRACT_CREDIT = createJsonType("user", RetractCredit::class)
    val RETRACT_CREDIT_RESPONSE = createJsonType("user", RetractCreditResponse::class)
}