package com.IceCreamQAQ.Yu.util

import java.lang.RuntimeException

class YuParaValueException(message: String) : RuntimeException(message)
class RainEventException(message: String = "", cause: Exception) : RuntimeException(cause)
