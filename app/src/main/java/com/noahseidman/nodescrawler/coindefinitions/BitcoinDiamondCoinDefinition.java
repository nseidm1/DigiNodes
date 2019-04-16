package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class BitcoinDiamondCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new BitcoinDiamondCoinDefinition());
    }

    private BitcoinDiamondCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seed1.dns.btcd.io",
            "seed2.dns.btcd.io",
            "seed3.dns.btcd.io",
            "seed4.dns.btcd.io",
            "seed5.dns.btcd.io",
            "seed6.dns.btcd.io"
    };

    @Override
    public String getCoinName() {
        return "Bitcoin Diamond";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70015;
    }

    @Override
    public int getPort() {
        return 7117;
    }

    @Override
    public String[] getDnsSeeds() {
        return dnsSeeds;
    }

    @Override
    public boolean getAllowEmptyPeers() {
        return false;
    }

    @Override
    public long getPacketMagic() {
        return 0xbddeb4d9;
    }
}