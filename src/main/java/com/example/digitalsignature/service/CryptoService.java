package com.example.digitalsignature.service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import org.springframework.stereotype.Service;

import io.github.rctcwyvrn.blake3.Blake3;

@Service
public class CryptoService {

    private final KeyPair keyPair;

    public CryptoService() throws NoSuchAlgorithmException {
        // Generate ECDSA key pair (sebagai contoh statis)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        this.keyPair = keyGen.generateKeyPair();
    }

    public String hashWithBlake3(byte[] data) {
        Blake3 hasher = Blake3.newInstance();
        hasher.update(data);
        byte[] hash = hasher.digest();
        return Base64.getEncoder().encodeToString(hash);
    }

    public String signData(byte[] hash) throws Exception {
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initSign(keyPair.getPrivate());
        ecdsaSign.update(hash);
        byte[] signature = ecdsaSign.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    public boolean verifySignature(byte[] hash, String base64Signature) throws Exception {
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
        ecdsaVerify.initVerify(keyPair.getPublic());
        ecdsaVerify.update(hash);
        byte[] signature = Base64.getDecoder().decode(base64Signature);
        return ecdsaVerify.verify(signature);
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}
