package com.noahseidman.nodescrawler.coindefinitions;

import com.noahseidman.coinj.params.MainNetParams;
import com.noahseidman.nodescrawler.interfaces.Definition;

/**
 * Substantially simplified CoinDefition only for node crawling
 */
public class BitcoinCoinDefition implements Definition {

    public static MainNetParams get() {
        return MainNetParams.get(new BitcoinCoinDefition());
    }

    private BitcoinCoinDefition(){}

    private String[] dnsSeeds = new String[]{
            "seed.bitcoin.sipa.be",         // Pieter Wuille
            "dnsseed.bluematt.me",          // Matt Corallo
            "dnsseed.bitcoin.dashjr.org",   // Luke Dashjr
            "seed.bitcoinstats.com",        // Chris Decker
            "seed.bitcoin.jonasschnelli.ch",// Jonas Schnelli
            "seed.btc.petertodd.org",       // Peter Todd
            "seed.bitcoin.sprovoost.nl",    // Sjors Provoost
            "seed.bitnodes.io",             // Addy Yeow
            "dnsseed.emzy.de",              // Stephan Oeste
    };

    @Override
    public String getCoinName() {
        return "Bitcoin";
    }

    @Override
    public int getMinProtocolVersion() {
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
        return 0xf9beb4d9L;
    }
}