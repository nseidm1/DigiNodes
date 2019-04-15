package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class PivxCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new PivxCoinDefinition());
    }

    private PivxCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "pivx.seed.fuzzbawls.pw",
            "pivx.seed2.fuzzbawls.pw",
            "coin-server.com",
            "s3v3nh4cks.ddns.net",
            "178.254.23.111"
    };

    @Override
    public String getCoinName() {
        return "Pivx";
    }

    @Override
    public int getProtocolVersion() {
        return 70915;
    }

    @Override
    public int getPort() {
        return 51472;
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
        return 0x90c4fde9;
    }
}