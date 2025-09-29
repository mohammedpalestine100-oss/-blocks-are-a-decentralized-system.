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
-        if (!tx.verify()) throw new IllegalArgumentException("Invalid signature");
+        if (!tx.isCoinbase() && !tx.verify()) throw new IllegalArgumentException("Invalid signature");
+
+        // (اختياري) منع الصرف الزائد بشكل بسيط عبر الرصيد الحالي + المعلّقات
+        if (!tx.isCoinbase()) {
+            double available = getBalance(tx.getFrom());
+            double pendingOut = 0.0;
+            for (Transaction p : pending) {
+                if (p.getFrom() != null && p.getFrom().equals(tx.getFrom())) {
+                    pendingOut += p.getAmount();
+                }
+            }
+            if (available - pendingOut < tx.getAmount()) {
+                throw new IllegalArgumentException("Insufficient funds");
+            }
+        }
         pending.add(tx);
     }
 
     public Block minePendingTransactions(PublicKey miner) {
-        // reward
-        Transaction reward = Transaction.createAndSign(
-            // self-signed reward by "system" - using miner key to sign simplifies demo
-            // In production, a protocol rule mints reward without signature verification
-            nullSafePrivateKey(), miner, miner, miningReward
-        );
-        // For reward we bypass signature verify (or consider it valid)
+        // مكافأة حقيقية coinbase (from = null، بدون توقيع)
+        Transaction reward = Transaction.createCoinbase(miner, miningReward);
         List<Transaction> pack = new ArrayList<>(pending);
         pack.add(reward);
         Block b = new Block(pack, getLatestBlock().getHash());
         b.mineBlock(difficulty);
         chain.add(b);
         pending.clear();
         return b;
     }
 
-    // Helper to create a "system" reward without a private key (no sig verify on reward)
-    private java.security.PrivateKey nullSafePrivateKey() {
-        // Not used for verification in demo; reward tx is accepted by rule
-        return new java.security.PrivateKey() {
-            public String getAlgorithm(){ return "NONE"; }
-            public String getFormat(){ return "NONE"; }
-            public byte[] getEncoded(){ return new byte[0]; }
-        };
-    }
+    // حُذفت nullSafePrivateKey لأنها لم تعد مطلوبة
