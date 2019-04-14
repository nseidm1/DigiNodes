package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class SyscoinCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new SyscoinCoinDefinition());
    }

    private SyscoinCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seed.syscoin.com",
            "seed.multisyscoin.org",
            "seed2.multisyscoin.org",
            "seed.syscoinr.syscoin.com",
            "98.203.82.233"
    };

    @Override
    public String getCoinName() {
        return "Syscoin";
    }

    @Override
    public int getProtocolVersion() {
        return 70227;
    }

    @Override
    public int getPort() {
        return 8369;
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
        return 0xcee2caff;
    }
}