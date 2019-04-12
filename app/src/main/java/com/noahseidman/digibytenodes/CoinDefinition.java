package com.noahseidman.digibytenodes;

import com.matthewmitchell.peercoinj.core.Utils;

import java.math.BigInteger;

/**
 * Created with IntelliJ IDEA.
 * User: HashEngineering
 * Date: 8/13/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoinDefinition {


    public static final String coinName = "DigiByte";

    //Original Values
    public static final int TARGET_TIMESPAN = (int) (0.10 * 24 * 60 * 60);  // 72 minutes per difficulty cycle, on average.
    public static final int TARGET_SPACING = (int) (1 * 60);  // 40 seconds per block.
    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;  //108 blocks

    //need to look into
    public static int spendableCoinbaseDepth = 8; //main.h: static const int COINBASE_MATURITY
    public static final int MAX_COINS = 2000000000;                 //main.h:  MAX_MONEY
    public static final int PROTOCOL_VERSION = 70016;          //version.h PROTOCOL_VERSION
    public static final int MIN_PROTOCOL_VERSION = 70016;        //version.h MIN_PROTO_VERSION
    public static final boolean supportsBloomFiltering = false;

    public static final int Port = 12024;       //protocol.h GetDefaultPort(testnet=false)

    //
    //  Production
    //
    public static final int AddressHeader = 30;             //base58.h CBitcoinAddress::PUBKEY_ADDRESS
    public static final int p2shHeader = 5;             //base58.h CBitcoinAddress::SCRIPT_ADDRESS
    public static final long PacketMagic = 0xfac3b6da;      //0xfa, 0xc3, 0xb6, 0xda

    //Genesis Block Information from main.cpp: LoadBlockIndex
    static public long genesisBlockDifficultyTarget = (0x1e0ffff0L);         //main.cpp: LoadBlockIndex
    static public long genesisBlockTime = 1389388394L;                       //main.cpp: LoadBlockIndex
    static public long genesisBlockNonce = (2447652);                         //main.cpp: LoadBlockIndex
    static public String genesisMerkleRoot = "72ddd9496b004221ed0557358846d9248ecd4c440ebd28ed901efc18757d0fad";
    static public int genesisBlockValue = 8000;                                                              //main.cpp: LoadBlockIndex
    static public String genesisTxInBytes = "04ffff001d01044555534120546f6461793a2031302f4a616e2f323031342c205461726765743a20446174612073746f6c656e2066726f6d20757020746f203131304d20637573746f6d657273";
    static public String genesisTxOutBytes = "00";


    //net.cpp strDNSSeed
    static public String[] dnsSeeds = new String[]{
            "seed.digibyteservers.io",
            "seed1.digibyte.co",
            "seed2.hashdragon.com",
            "dgb.cryptoservices.net",
            "digibytewiki.com",
            "digiexplorer.info",
            "seed.digibyte.io"
    };

    public static int subsidyDecreaseBlockCount = 100000;
    public static BigInteger proofOfWorkLimit = Utils.decodeCompactBits(0x1e0fffffL);  //main.cpp bnProofOfWorkLimit (~uint256(0) >> 20); // digitalcoin: starting difficulty is 1 / 2^12
    public static final String SATOSHI_KEY = "040184710fa689ad5023690c80f3a49c8f13f8d45b8c857fbcbc8bc4a8e4d3eb4b10f4d4604fa08dce601aaf0f470216fe1b51850b4acf21b179c45070ac7b03a9";
    public static final String ID_MAINNET = "org.digibyte.production";
}