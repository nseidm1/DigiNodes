/**
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noahseidman.coinj.core;

import com.google.common.base.Objects;
import com.noahseidman.nodescrawler.SelectedNetParams;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class NetworkParameters implements Serializable {

    public abstract boolean allowEmptyPeerChain();
    public static int PROTOCOL_VERSION;
    public static int MIN_PROTOCOL_VERSION;
    protected static String PAYMENT_PROTOCOL_ID_MAINNET;
    static final int BIP16_ENFORCE_TIME = 1333238400;
    public static Coin MAX_MONEY = Coin.ZERO;
    private Block genesisBlock;
    protected int port;
    protected long packetMagic;
    protected int addressHeader;
    protected int p2shHeader;
    protected String id;
    int spendableCoinbaseDepth;
    protected int[] acceptableAddressCodes;
    protected String[] dnsSeeds;
    private Map<Integer, Sha256Hash> checkpoints = new HashMap<Integer, Sha256Hash>();
    protected int dumpedPrivateKeyHeader;
    private byte[] alertSigningKey;
    public String coinName;

    protected NetworkParameters() {
        genesisBlock = new Block(this);
    }

    /** Alias for MainNetParams.get(), use that instead */
    @Deprecated
    public static NetworkParameters prodNet() {
        return SelectedNetParams.instance;
    }

    /**
     * A Java package style string acting as unique ID for these parameters
     */
    public String getId() {
        return id;
    }

    public abstract String getPaymentProtocolId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkParameters other = (NetworkParameters) o;
        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    /** Returns the network parameters for the given string ID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromID(String id) {
        return SelectedNetParams.instance;
    }

    /** Returns the network parameters for the given string paymentProtocolID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromPmtProtocolID(String pmtProtocolId) {
        return SelectedNetParams.instance;
    }

    public int getSpendableCoinbaseDepth() {
        return spendableCoinbaseDepth;
    }

    /**
     * Returns true if the NetworkParameters is for a ShapeShift coin. ie. not NuBits
     */
    public boolean isShapeShift() {
        return false;
    }

    /**
     * Returns true if the block height is either not a checkpoint, or is a checkpoint and the hash matches.
     */
    boolean passesCheckpoint(int height, Sha256Hash hash) {
        Sha256Hash checkpointHash = checkpoints.get(height);
        return checkpointHash == null || checkpointHash.equals(hash);
    }

    /**
     * Returns true if the given height has a recorded checkpoint.
     */
    boolean isCheckpoint(int height) {
        Sha256Hash checkpointHash = checkpoints.get(height);
        return checkpointHash != null;
    }

    /** Returns DNS names that when resolved, give IP addresses of active peers. */
    public String[] getDnsSeeds() {
        return dnsSeeds;
    }

    /**
     * <p>Genesis block for this chain.</p>
     *
     * <p>The first block in every chain is a well known constant shared between all Peercoin implemenetations. For a
     * block to be valid, it must be eventually possible to work backwards to the genesis block by following the
     * prevBlockHash pointers in the block headers.</p>
     *
     * <p>The genesis blocks for both test and prod networks contain the timestamp of when they were created,
     * and a message in the coinbase transaction. It says, <i>"Matonis 07-AUG-2012 Parallel Currencies And The
     * Roadmap To Monetary Freedom"</i>.</p>
     */
    public Block getGenesisBlock() {
        return genesisBlock;
    }

    /** Default TCP port on which to connect to nodes. */
    public int getPort() {
        return port;
    }

    /** The header bytes that identify the start of a packet on this network. */
    public long getPacketMagic() {
        return packetMagic;
    }

    /**
     * First byte of a base58 encoded address. See {@link com.noahseidman.coinj.core.Address}. This is the same as acceptableAddressCodes[0] and
     * is the one used for "normal" addresses. Other types of address may be encountered with version codes found in
     * the acceptableAddressCodes array.
     */
    public int getAddressHeader() {
        return addressHeader;
    }

    /**
     * First byte of a base58 encoded P2SH address.  P2SH addresses are defined as part of BIP0013.
     */
    public int getP2SHHeader() {
        return p2shHeader;
    }

    /** First byte of a base58 encoded dumped private key. See {@link com.noahseidman.coinj.core.DumpedPrivateKey}. */
    public int getDumpedPrivateKeyHeader() {
        return dumpedPrivateKeyHeader;
    }

    /**
     * The version codes that prefix addresses which are acceptable on this network. Although Satoshi intended these to
     * be used for "versioning", in fact they are today used to discriminate what kind of data is contained in the
     * address and to prevent accidentally sending coins across chains which would destroy them.
     */
    public int[] getAcceptableAddressCodes() {
        return acceptableAddressCodes;
    }

    /**
     * The key used to sign {@link com.noahseidman.coinj.core.AlertMessage}s. You can use {@link com.noahseidman.coinj.core.ECKey#verify(byte[], byte[], byte[])} to verify
     * signatures using it.
     */
    public byte[] getAlertSigningKey() {
        return alertSigningKey;
    }

    /**
     * Used to parse a coin string into a Monetary for this network.
     */
    public Monetary parseCoin(String str) {
        return Coin.parseCoin(str);
    }
}
