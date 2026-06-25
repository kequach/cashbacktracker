package com.cashbacktracker.data.crypto

interface SensitiveDataCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}

fun SensitiveDataCipher.encryptNullable(value: String): String =
    if (value.isBlank()) "" else encrypt(value)

fun SensitiveDataCipher.decryptNullable(value: String): String =
    if (value.isBlank()) "" else decrypt(value)
