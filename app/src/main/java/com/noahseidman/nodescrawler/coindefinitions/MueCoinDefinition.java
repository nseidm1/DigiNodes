package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class MueCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new MueCoinDefinition());
    }

    private MueCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "dns1.monetaryunit.org",
            "dns2.monetaryunit.org",
            "dns3.monetaryunit.org"
    };

    @Override
    public String getCoinName() {
        return "MUE";
    }

    @Override
    public int getProtocolVersion() {
        return 70703;
    }

    @Override
    public int getPort() {
        return 19687;
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
        return 0x91c4fdea;
    }
}