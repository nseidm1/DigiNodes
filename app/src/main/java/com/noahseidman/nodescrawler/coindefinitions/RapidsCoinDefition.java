package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class RapidsCoinDefition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new RapidsCoinDefition());
    }

    private RapidsCoinDefition(){}

    private String[] dnsSeeds = new String[]{
            "68.183.236.217",
            "159.65.189.155",
            "209.97.188.183",
            "104.248.169.67"
    };

    @Override
    public String getCoinName() {
        return "Rapids";
    }

    @Override
    public int getProtocolVersion() {
        return 70914;
    }

    @Override
    public int getPort() {
        return 28732;
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
        return 0x61a2f5cb;
    }
}