package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class VertCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new VertCoinDefinition());
    }

    private VertCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "useast1.vtconline.org",
            "vtc.gertjaap.org"
    };

    @Override
    public String getCoinName() {
        return "Syscoin";
    }

    @Override
    public int getProtocolVersion() {
        return 70015;
    }

    @Override
    public int getPort() {
        return 5889;
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
        return 0xfabfb5da;
    }
}