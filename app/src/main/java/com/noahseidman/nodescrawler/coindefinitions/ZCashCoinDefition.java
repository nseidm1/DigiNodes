package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class ZCashCoinDefition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new ZCashCoinDefition());
    }

    private ZCashCoinDefition(){}

    private String[] dnsSeeds = new String[]{
            "dnsseed.z.cash",
            "dnsseed.str4d.xyz",
            "dnsseed.znodes.org"
    };

    @Override
    public String getCoinName() {
        return "ZCash";
    }

    @Override
    public int getMinProtocolVersion() {
        return 170007;
    }

    @Override
    public int getPort() {
        return 8233;
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
        return 0x24e92764;
    }
}