package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class VergeCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new VergeCoinDefinition());
    }

    private VergeCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "185.162.9.97",
            "104.131.144.82",
            "192.241.187.222",
            "105.228.198.44",
            "46.127.57.167",
            "98.5.123.15",
            "81.147.68.236",
            "77.67.46.100",
            "95.46.99.96",
            "138.201.91.159",
            "159.89.202.56",
            "163.158.20.118",
            "99.45.88.147",
            "114.145.237.35",
            "145.239.0.122",
            "73.247.117.99"
    };

    @Override
    public String getCoinName() {
        return "Verge";
    }

    @Override
    public int getMinProtocolVersion() {
        return 90007;
    }

    @Override
    public int getPort() {
        return 21102;
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
        return 0xf7a77eff;
    }
}