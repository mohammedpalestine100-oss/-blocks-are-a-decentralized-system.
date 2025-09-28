package com.decentra.chain;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class Wallet {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public Wallet() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            kpg.initialize(ecSpec, new SecureRandom());
            KeyPair kp = kpg.generateKeyPair();
            this.privateKey = kp.getPrivate();
            this.publicKey = kp.getPublic();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate wallet keys", e);
        }
    }

    public PrivateKey getPrivateKey() { return privateKey; }
    public PublicKey getPublicKey() { return publicKey; }

    public String getAddress() {
        // Address = SHA256(Base64(pubkey))
        String pub = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return CryptoUtil.sha256(pub).substring(0, 40);
    }

    public Transaction sendFunds(PublicKey recipient, double amount) {
        return Transaction.createAndSign(this.privateKey, this.publicKey, recipient, amount);
    }
}
