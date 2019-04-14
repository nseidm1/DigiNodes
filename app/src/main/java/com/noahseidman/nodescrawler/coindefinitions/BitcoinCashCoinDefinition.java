package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class BitcoinCashCoinDefinition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new BitcoinCashCoinDefinition());
    }

    private BitcoinCashCoinDefinition(){}

    private String[] dnsSeeds = new String[]{
            "seed.bitcoinabc.org",
            "seed-abc.bitcoinforks.org",
            "btccash-seeder.bitcoinunlimited.info",
            "seed.bitprim.org",
            "seed.deadalnix.me",
            "seeder.criptolayer.net"
    };

    @Override
    public String getCoinName() {
        return "Bitcoin Cash";
    }

    @Override
    public int getProtocolVersion() {
        return 70015;
    }

    @Override
    public int getPort() {
        return 8333;
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
        return 0xe3e1f3e8;
    }
}