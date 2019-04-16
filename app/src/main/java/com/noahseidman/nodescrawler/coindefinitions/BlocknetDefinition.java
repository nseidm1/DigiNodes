package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class BlocknetDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new BlocknetDefinition());
    }

    private BlocknetDefinition(){}

    private String[] dnsSeeds = new String[]{
            "178.62.90.213",
            "138.197.73.214",
            "34.235.49.248",
            "35.157.52.158",
            "18.196.208.65",
            "13.251.15.150",
            "13.229.39.34",
            "52.56.35.74",
            "35.177.173.53",
            "35.176.65.103",
            "35.178.142.231"
    };

    @Override
    public String getCoinName() {
        return "Blocknet";
    }

    @Override
    public int getMinProtocolVersion() {
        return 70712;
    }

    @Override
    public int getPort() {
        return 41412;
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
        return 0xa1a0a2a3;
    }
}