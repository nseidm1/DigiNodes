package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class ZCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new ZCoinDefinition());
    }

    private ZCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "amsterdam.zcoin.io",
            "australia.zcoin.io",
            "chicago.zcoin.io",
            "london.zcoin.io",
            "frankfurt.zcoin.io",
            "newjersey.zcoin.io",
            "sanfrancisco.zcoin.io",
            "tokyo.zcoin.io",
            "singapore.zcoin.io"
    };

    @Override
    public String getCoinName() {
        return "Zcoin";
    }

    @Override
    public int getMinProtocolVersion() {
        return 90026;
    }

    @Override
    public int getPort() {
        return 8168;
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
        return 0xe3d9fef1;
    }
}