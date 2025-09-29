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
