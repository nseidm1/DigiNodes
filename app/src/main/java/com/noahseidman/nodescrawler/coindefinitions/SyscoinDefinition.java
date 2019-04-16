package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class SyscoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new SyscoinDefinition());
    }

    private SyscoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seed1.syscoin.org",
            "seed2.syscoin.org",
            "seed3.syscoin.org",
            "seed4.syscoin.org"
    };

    @Override
    public String getCoinName() {
        return "Syscoin";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70224;
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
        return 0xf9beb4d9;
    }
}