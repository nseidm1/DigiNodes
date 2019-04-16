package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class KomodoDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new KomodoDefinition());
    }

    private KomodoDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seeds.veruscoin.io",
            "seeds.komodoplatform.com",
            "static.kolo.supernet.org",
            "dynamic.kolo.supernet.org"
    };

    @Override
    public String getCoinName() {
        return "Komodo";
    }

    @Override
    public int getMinProtocolVersion() {
        return 170007;
    }

    @Override
    public int getPort() {
        return 7770;
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
        return 0xf9eee48d;
    }
}