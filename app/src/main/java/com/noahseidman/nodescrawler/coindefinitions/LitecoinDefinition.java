package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class LitecoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new LitecoinDefinition());
    }

    private LitecoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seed-a.litecoin.loshan.co.uk",
            "dnsseed.thrasher.io",
            "dnsseed.litecointools.com",
            "dnsseed.litecoinpool.org",
            "dnsseed.koin-project.com"
    };

    @Override
    public String getCoinName() {
        return "Litecoin";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70015;
    }

    @Override
    public int getPort() {
        return 9333;
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
        return 0xfbc0b6db;
    }
}