package com.decentra.chain;

public class App {
    public static void main(String[] args) {
        System.out.println("DecentraChain (PoW, ECDSA tx) – Java 17");

        Blockchain bc = new Blockchain(3, 10.0);

        Wallet alice = new Wallet();
        Wallet bob = new Wallet();
        Wallet miner = new Wallet();

        System.out.println("Alice: " + alice.getAddress());
        System.out.println("Bob:   " + bob.getAddress());
        System.out.println("Miner: " + miner.getAddress());

        // Bootstrap: miner mines an empty block to get first reward (coinbase)
        bc.minePendingTransactions(miner.getPublicKey());
        System.out.printf("Miner balance: %.2f%n", bc.getBalance(miner.getPublicKey()));

        // Miner sends some to Alice & Bob
        Transaction t1 = miner.sendFunds(alice.getPublicKey(), 25.0);
        Transaction t2 = miner.sendFunds(bob.getPublicKey(), 15.0);

        bc.addTransaction(t1);
        bc.addTransaction(t2);

        System.out.println("Mining block with txs...");
        bc.minePendingTransactions(miner.getPublicKey());

        System.out.printf("Alice balance: %.2f%n", bc.getBalance(alice.getPublicKey()));
        System.out.printf("Bob balance:   %.2f%n", bc.getBalance(bob.getPublicKey()));
        System.out.printf("Miner balance: %.2f%n", bc.getBalance(miner.getPublicKey()));

        System.out.println("Chain valid? " + bc.isValid());
        for (int i = 0; i < bc.getChain().size(); i++) {
            System.out.println("#" + i + " " + bc.getChain().get(i));
        }
    }


    
}
package com.decentra.chain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Block {
    private String hash;
    private final String previousHash;
    private final long timeStamp;
    private int nonce;
    private final List<Transaction> transactions;
    private final String merkleRoot;

    public Block(List<Transaction> transactions, String previousHash) {
        this.transactions = new ArrayList<>(transactions);
        this.previousHash = previousHash;
        this.timeStamp = System.currentTimeMillis();
        this.nonce = 0;

        List<String> txHashes = new ArrayList<>();
        for (Transaction t : this.transactions) {
            txHashes.add(t.getTxId());
        }
        this.merkleRoot = CryptoUtil.merkleRoot(txHashes);
        this.hash = calculateHash();
    }

    public String calculateHash() {
        return CryptoUtil.sha256(previousHash + timeStamp + nonce + merkleRoot);
    }

    public void mineBlock(int difficulty) {
        String target = "0".repeat(difficulty);
        while (!hash.startsWith(target)) {
            nonce++;
            hash = calculateHash();
        }
    }

    public boolean validateTransactions() {
        for (Transaction t : transactions) {
            // coinbase لا تحتاج توقيع
            if (!t.isCoinbase() && !t.verify()) return false;
        }
        return true;
    }

    public String getHash(){ return hash; }
    public String getPreviousHash(){ return previousHash; }
    public long getTimeStamp(){ return timeStamp; }
    public int getNonce(){ return nonce; }
    public String getMerkleRoot(){ return merkleRoot; }
    public List<Transaction> getTransactions(){ return new ArrayList<>(transactions); }

    @Override public String toString() {
        return "Block{hash=" + hash.substring(0, 12) + ".., txs=" + transactions.size() + ", ts=" + new Date(timeStamp) + "}";
    }
}


package com.decentra.chain;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    private final List<Block> chain;
    private final List<Transaction> pending;
    private int difficulty;
    private final double miningReward;

    public Blockchain(int difficulty, double miningReward) {
        this.chain = new ArrayList<>();
        this.pending = new ArrayList<>();
        this.difficulty = difficulty;
        this.miningReward = miningReward;
        // genesis
        Block genesis = new Block(new ArrayList<>(), "0".repeat(64));
        genesis.mineBlock(this.difficulty);
        chain.add(genesis);
    }

    public void addTransaction(Transaction tx) {
        if (tx == null) throw new IllegalArgumentException("tx null");
        if (!tx.isCoinbase() && !tx.verify()) throw new IllegalArgumentException("Invalid signature");

        // (اختياري قوي) منع الصرف الزائد بشكل بسيط: الرصيد الحالي - المعلّقات
        if (!tx.isCoinbase()) {
            double available = getBalance(tx.getFrom());
            double pendingOut = 0.0;
            for (Transaction p : pending) {
                if (p.getFrom() != null && p.getFrom().equals(tx.getFrom())) {
                    pendingOut += p.getAmount();
                }
            }
            if (available - pendingOut < tx.getAmount()) {
                throw new IllegalArgumentException("Insufficient funds");
            }
        }
        pending.add(tx);
    }

    public Block minePendingTransactions(PublicKey miner) {
        // مكافأة coinbase (from=null، بدون توقيع)
        Transaction reward = Transaction.createCoinbase(miner, miningReward);

        List<Transaction> pack = new ArrayList<>(pending);
        pack.add(reward);

        Block b = new Block(pack, getLatestBlock().getHash());
        b.mineBlock(difficulty);
        chain.add(b);
        pending.clear();
        return b;
    }

    public boolean isValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block cur = chain.get(i), prev = chain.get(i - 1);
            if (!cur.getHash().equals(cur.calculateHash())) return false;
            if (!cur.getPreviousHash().equals(prev.getHash())) return false;
            if (!cur.getHash().startsWith("0".repeat(difficulty))) return false;
            if (!cur.validateTransactions()) return false;
        }
        return true;
    }

    public double getBalance(PublicKey address) {
        double bal = 0.0;
        for (Block b : chain) {
            for (Transaction t : b.getTransactions()) {
                if (t.getFrom() != null && t.getFrom().equals(address)) bal -= t.getAmount();
                if (t.getTo()   != null && t.getTo().equals(address))   bal += t.getAmount();
            }
        }
        return bal;
    }

    public Block getLatestBlock(){ return chain.get(chain.size()-1); }
    public int getDifficulty(){ return difficulty; }
    public void setDifficulty(int d){ this.difficulty = d; }
    public List<Block> getChain(){ return new ArrayList<>(chain); }
}


package com.decentra.chain;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class CryptoUtil {
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String merkleRoot(List<String> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            return sha256("");
        }
        List<String> layer = new ArrayList<>(leafHashes);
        while (layer.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i = 0; i < layer.size(); i += 2) {
                String left = layer.get(i);
                String right = (i + 1 < layer.size()) ? layer.get(i + 1) : left;
                next.add(sha256(left + right));
            }
            layer = next;
        }
        return layer.get(0);
    }
}


package com.decentra.chain;

import java.security.*;
import java.util.Base64;

public class Transaction {
    private final String txId;
    private final PublicKey from; // may be null for coinbase
    private final PublicKey to;
    private final double amount;
    private final byte[] signature;

    private Transaction(String txId, PublicKey from, PublicKey to, double amount, byte[] signature) {
        this.txId = txId;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.signature = signature;
    }

    // معاملة مكافأة بدون توقيع (from = null)
    public static Transaction createCoinbase(PublicKey to, double amount) {
        String payload = canonical(null, to, amount);
        String txId = CryptoUtil.sha256(payload);
        return new Transaction(txId, null, to, amount, new byte[0]);
    }

    public static Transaction createAndSign(PrivateKey priv, PublicKey from, PublicKey to, double amount) {
        try {
            String payload = canonical(from, to, amount);
            String txId = CryptoUtil.sha256(payload);

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(priv);
            sig.update(payload.getBytes());
            byte[] signature = sig.sign();

            return new Transaction(txId, from, to, amount, signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create/sign transaction", e);
        }
    }

    public boolean isCoinbase() {
        return from == null;
    }

    public boolean verify() {
        if (isCoinbase()) return true; // coinbase لا تحتاج توقيع
        try {
            String payload = canonical(from, to, amount);
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(from);
            sig.update(payload.getBytes());
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static String canonical(PublicKey from, PublicKey to, double amount) {
        String f = (from == null) ? "COINBASE" :
                Base64.getEncoder().encodeToString(from.getEncoded());
        String t = Base64.getEncoder().encodeToString(to.getEncoded());
        return "{\"from\":\"" + f + "\",\"to\":\"" + t + "\",\"amount\":" + amount + "}";
    }

    public String getTxId(){ return txId; }
    public PublicKey getFrom(){ return from; }
    public PublicKey getTo(){ return to; }
    public double getAmount(){ return amount; }
    public byte[] getSignature(){ return signature; }

    @Override public String toString() {
        return "TX{" + txId.substring(0, 8) + ".., amount=" + amount + "}";
    }
}


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
