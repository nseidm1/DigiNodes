package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class PhoreCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new PhoreCoinDefinition());
    }

    private PhoreCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "dns0.phore.io",
            "phore.seed.rho.industries"
    };

    @Override
    public String getCoinName() {
        return "Phore";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70007;
    }

    @Override
    public int getPort() {
        return 11771;
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
        return 0x91c4fde9;
    }
}