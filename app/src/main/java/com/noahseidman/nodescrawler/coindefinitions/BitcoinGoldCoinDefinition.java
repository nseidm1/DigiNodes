package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class BitcoinGoldCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new BitcoinGoldCoinDefinition());
    }

    private BitcoinGoldCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "eu-dnsseed.bitcoingold-official.org",
            "dnsseed.bitcoingold.org",
            "dnsseed.bitcoingold.dev"
    };

    @Override
    public String getCoinName() {
        return "Bitcoin Gold";
    }

    @Override
    public int getProtocolVersion() {
        return 70016;
    }

    @Override
    public int getPort() {
        return 8338;
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
        return 0xe1476d44;
    }
}