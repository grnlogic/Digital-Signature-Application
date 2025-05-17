package com.example.digitalsignature.service;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.rctcwyvrn.blake3.Blake3;

@Service
public class CryptoService {

    private final KeyPair keyPair;
    
    @Value("${signature.validityPeriod:604800000}") // Default 1 minggu dalam milidetik
    private long signatureValidityPeriod;
    
    @Value("${SIGNATURE_PRIVATE_KEY:#{null}}")
    private String privateKeyEnv;

    @Value("${SIGNATURE_PUBLIC_KEY:#{null}}")
    private String publicKeyEnv;
    
    public CryptoService() throws Exception {
        this.keyPair = loadOrGenerateKeyPair();
    }
    
    private KeyPair loadOrGenerateKeyPair() throws Exception {
        // Coba baca dari environment variables
        String privateKeyStr = System.getenv("SIGNATURE_PRIVATE_KEY");
        String publicKeyStr = System.getenv("SIGNATURE_PUBLIC_KEY");
        
        if (privateKeyStr != null && publicKeyStr != null && 
            !privateKeyStr.isEmpty() && !publicKeyStr.isEmpty()) {
            // Tambahkan log sukses
            System.out.println("==== USING EXISTING KEY PAIR FROM ENV VARIABLES ====");
            System.out.println("Private key found (first 10 chars): " + privateKeyStr.substring(0, 10) + "...");
            System.out.println("Public key found (first 10 chars): " + publicKeyStr.substring(0, 10) + "...");
            return restoreKeyPairFromString(privateKeyStr, publicKeyStr);
        } else {
            // Generate key baru jika tidak ditemukan
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256);
            KeyPair newKeyPair = keyGen.generateKeyPair();
            
            // Print ke console
            System.out.println("==== GENERATED NEW KEY PAIR ====");
            System.out.println("SIGNATURE_PRIVATE_KEY=" + 
                Base64.getEncoder().encodeToString(newKeyPair.getPrivate().getEncoded()));
            System.out.println("SIGNATURE_PUBLIC_KEY=" + 
                Base64.getEncoder().encodeToString(newKeyPair.getPublic().getEncoded()));
            System.out.println("================================");
            
            return newKeyPair;
        }
    }
    
    private KeyPair restoreKeyPairFromString(String privateKeyB64, String publicKeyB64) throws Exception {
        // Decode Base64 keys
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyB64);
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyB64);
        
        // Reconstruct private key
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        
        // Reconstruct public key
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        
        return new KeyPair(publicKey, privateKey);
    }
    
    public String hashWithBlake3(byte[] data) {
        Blake3 hasher = Blake3.newInstance();
        hasher.update(data);
        byte[] hash = hasher.digest();
        return Base64.getEncoder().encodeToString(hash);
    }

    public String signData(byte[] hash, long validityPeriodMillis) throws Exception {
        // Dapatkan waktu saat ini
        long currentTime = System.currentTimeMillis();
        // Hitung waktu kedaluwarsa
        long expiryTime = currentTime + validityPeriodMillis;
        
        // Data yang akan ditandatangani: hash + waktu kedaluwarsa
        ByteBuffer buffer = ByteBuffer.allocate(hash.length + 8);
        buffer.put(hash);
        buffer.putLong(expiryTime);
        byte[] dataToSign = buffer.array();
        
        // Proses tanda tangan
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(dataToSign);
        byte[] signatureBytes = signature.sign();
        
        // Format hasil: Base64(signature) + ":" + expiryTime
        return Base64.getEncoder().encodeToString(signatureBytes) + ":" + expiryTime;
    }

    // Overload method untuk backward compatibility
    public String signData(byte[] hash) throws Exception {
        // Default validity: 7 days
        return signData(hash, 7 * 24 * 60 * 60 * 1000L);
    }

    public boolean verifySignature(byte[] hash, String signatureWithExpiry) throws Exception {
        // Parse signature dan waktu kedaluwarsa
        String[] parts = signatureWithExpiry.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Format tanda tangan tidak valid");
        }
        
        String base64Signature = parts[0];
        long expiryTime = Long.parseLong(parts[1]);
        
        // Periksa apakah signature sudah kedaluwarsa
        long currentTime = System.currentTimeMillis();
        if (currentTime > expiryTime) {
            return false; // Tanda tangan sudah kedaluwarsa
        }
        
        // Rekonstruksi data yang ditandatangani
        ByteBuffer buffer = ByteBuffer.allocate(hash.length + 8);
        buffer.put(hash);
        buffer.putLong(expiryTime);
        byte[] dataToVerify = buffer.array();
        
        // Verifikasi tanda tangan
        byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(keyPair.getPublic());
        signature.update(dataToVerify);
        
        return signature.verify(signatureBytes);
    }

    public PublicKey getPublicKey() {   
        return keyPair.getPublic();
    }
}
