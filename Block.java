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

    // >>> التعديل هنا <<<
    public boolean validateTransactions() {
        for (Transaction t : transactions) {
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
        return "Block{hash="+hash.substring(0,12)+".., txs="+transactions.size()+", ts="+new Date(timeStamp)+"}";
    }
}
