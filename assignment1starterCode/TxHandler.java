import java.util.ArrayList;
import java.util.HashMap;

public class TxHandler {
    UTXOPool utxoPool;
    HashMap<byte[], Transaction> block;
    static boolean haveGenesis = false;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is {@code utxoPool}. This should make a copy of utxoPool
     * by using the UTXOPool(UTXOPool uPool) constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
        block = new HashMap<byte[], Transaction>();
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
        boolean genesis = false;
        for (Transaction.Input input : tx.getInputs()) {
            if (input.prevTxHash == null) {
                if (haveGenesis) {
                    return false;
                }
                genesis = true;
            } else if (genesis) {
                // one of the inputs had a null hash
                return false;
            } else {
                Transaction prevtx = block.get(input.signature);
                if (prevtx == null) {
                    return false;
                }
                Transaction.Output prev_output = prevtx.getOutput(input.outputIndex);
                // (2) if it has a valid signature
                if (!Crypto.verifySignature(prev_output.address,
                                            prevtx.getRawDataToSign(input.outputIndex),
                                            input.signature)) {
                    return false;
                }
                inputSum += prev_output.value;
            }
        }
        UTXOPool tmpPool = new UTXOPool();
        for (int index = 0; index < tx.numOutputs(); index++) {
            Transaction.Output output = tx.getOutput(index);
            // (4)
            if (output.value < 0.0) {
                return false;
            }
            inputSum -= output.value;
            // (5)
            if (inputSum < 0.0 && !genesis) {
                return false;
            }
            UTXO utxo = new UTXO(tx.getHash(), index);
            // (3)
            if (tmpPool.contains(utxo)) {
                return false;
            }
            tmpPool.addUTXO(utxo, output);
            // (1)
            if (!utxoPool.contains(utxo)) {
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
	        for (int index = 0; index < tx.numOutputs(); index++) {
	            UTXO utxo = new UTXO(tx.getHash(), index);
	            utxoPool.removeUTXO(utxo);
	        }
        }
        return (Transaction[]) validTxs.toArray();
    }
}
