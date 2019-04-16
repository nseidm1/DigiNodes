package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class BitcoinSVCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new BitcoinSVCoinDefinition());
    }

    private BitcoinSVCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seed.bitcoinsv.io",
            "seed.cascharia.com",
            "seed.satoshisvision.network"
    };

    @Override
    public String getCoinName() {
        return "BitcoinSV";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70015;
    }

    @Override
    public int getPort() {
        return 8333;
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
        return 0xe3e1f3e8;
    }
}