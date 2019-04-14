package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class DogeCoinDefition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new DogeCoinDefition());
    }

    private DogeCoinDefition(){}

    private String[] dnsSeeds = new String[]{
            "seed.dogecoin.com",
            "seed.multidoge.org",
            "seed2.multidoge.org",
            "seed.doger.dogecoin.com"
    };

    @Override
    public String getCoinName() {
        return "Dogecoin";
    }

    @Override
    public int getProtocolVersion() {
        return 70004;
    }

    @Override
    public int getPort() {
        return 22556;
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
        return 0xc0c0c0c0;
    }
}