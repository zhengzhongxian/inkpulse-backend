package com.inkpulse.crypto;

public interface ICryptographyService {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
