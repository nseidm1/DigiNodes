package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class DigiByteCoinDefition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new DigiByteCoinDefition());
    }

    private DigiByteCoinDefition(){}

    private String[] dnsSeeds = new String[]{
            "seed.digibyteservers.io",
            "seed1.digibyte.co",
            "seed2.hashdragon.com",
            "dgb.cryptoservices.net",
            "digibytewiki.com",
            "digiexplorer.info",
            "seed1.digibyte.io",
            "seed2.digibyte.io",
            "seed3.digibyte.io",
            "digihash.co",
            "seed.digibyteprojects.com",
            "seed.digibyte.io"
    };

    @Override
    public String getCoinName() {
        return "DigiByte";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70016;
    }

    @Override
    public int getPort() {
        return 12024;
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
        return 0xfac3b6da;
    }
}