public class Transaction {
    private final String txId;
    private final PublicKey from; // may be null for coinbase
    private final PublicKey to;
    private final double amount;
    private final byte[] signature;

    // factory لمعاملة مكافأة (بدون توقيع)
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
        return "{\"from\":\""+f+"\",\"to\":\""+t+"\",\"amount\":"+amount+"}";
    }
}
