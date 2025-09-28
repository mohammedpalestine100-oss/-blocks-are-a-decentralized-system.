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

        // Bootstrap: miner mines an empty block to get first reward
        bc.minePendingTransactions(miner.getPublicKey());
        System.out.printf("Miner balance: %.2f\n", bc.getBalance(miner.getPublicKey()));

        // Miner sends some to Alice & Bob
        Transaction t1 = miner.sendFunds(alice.getPublicKey(), 25.0);
        Transaction t2 = miner.sendFunds(bob.getPublicKey(), 15.0);

        bc.addTransaction(t1);
        bc.addTransaction(t2);

        System.out.println("Mining block with txs...");
        bc.minePendingTransactions(miner.getPublicKey());

        System.out.printf("Alice balance: %.2f\n", bc.getBalance(alice.getPublicKey()));
        System.out.printf("Bob balance:   %.2f\n", bc.getBalance(bob.getPublicKey()));
        System.out.printf("Miner balance: %.2f\n", bc.getBalance(miner.getPublicKey()));

        System.out.println("Chain valid? " + bc.isValid());
        for (int i=0;i<bc.getChain().size();i++) {
            System.out.println("#"+i+" "+bc.getChain().get(i));
        }
    }
}
