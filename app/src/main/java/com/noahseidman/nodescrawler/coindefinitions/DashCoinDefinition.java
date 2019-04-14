package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class DashCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new DashCoinDefinition());
    }

    private DashCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "dnsseed.dash.org",
            "dnsseed.dashdot.io",
            "dnsseed.masternode.io"
    };

    @Override
    public String getCoinName() {
        return "Dash";
    }

    @Override
    public int getProtocolVersion() {
        return 70213;
    }

    @Override
    public int getPort() {
        return 9999;
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
        return 0xbf0c6bd;
    }
}