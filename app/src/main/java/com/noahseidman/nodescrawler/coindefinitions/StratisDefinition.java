package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class StratisDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new StratisDefinition());
    }

    private StratisDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seednode1.stratisplatform.com",
            "seednode2.stratis.cloud",
            "seednode3.stratisplatform.com",
            "seednode4.stratis.cloud"
    };

    @Override
    public String getCoinName() {
        return "Stratis";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70000;
    }

    @Override
    public int getPort() {
        return 16178;
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
        return 0x70352205;
    }
}