import java.util.ArrayList;
import java.util.HashMap;

public class TxHandler {
    UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is {@code utxoPool}. This should make a copy of utxoPool
     * by using the UTXOPool(UTXOPool uPool) constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if: (1) all outputs claimed by {@code tx} are in the current
     *         UTXO pool, (2) the signatures on each input of {@code tx} are valid,
     *         (3) no UTXO is claimed multiple times by {@code tx}, (4) all of
     *         {@code tx}s output values are non-negative, and (5) the sum of
     *         {@code tx}s input values is greater than or equal to the sum of its
     *         output values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        if (tx.numInputs() == 0 || tx.numOutputs() == 0) {
            return false;
        }
        double inputSum = 0.0;
        UTXOPool tmpPool = new UTXOPool();
        for (int index = 0; index < tx.numInputs(); index++) {
            Transaction.Input input = tx.getInput(index);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            // (1)
            if (!utxoPool.contains(utxo)) {
                return false;
            }
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            // (2) if it has a valid signature
            if (!Crypto.verifySignature(output.address,
                                        tx.getRawDataToSign(index),
                                        input.signature)) {
                return false;
            }
            inputSum += output.value;
            // (3)
            if (tmpPool.contains(utxo)) {
                return false;
            }
            tmpPool.addUTXO(utxo, output);
        }
        for (Transaction.Output ut : tx.getOutputs()) {
            // (4)
            if (ut.value < 0.0) {
                return false;
            }
            inputSum -= ut.value;
            // (5)
            if (inputSum < 0.0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions,
     * checking each transaction for correctness, returning a mutually valid array
     * of accepted transactions, and updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
        		if (!isValidTx(tx)) {
        			continue;
        		}
	        validTxs.add(tx);
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                utxoPool.removeUTXO(utxo);
            }
	        for (int index = 0; index < tx.numOutputs(); index++) {
	            UTXO utxo = new UTXO(tx.getHash(), index);
	            utxoPool.addUTXO(utxo, tx.getOutput(index));
	        }
        }
        Transaction[] txs = new Transaction[validTxs.size()];
        return validTxs.toArray(txs);
    }
}
