/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noahseidman.coinj.params;

import com.noahseidman.coinj.core.NetworkParameters;
import com.noahseidman.nodescrawler.interfaces.Definition;

public class MainNetParams extends NetworkParameters {

    private Definition definition;

    private MainNetParams(Definition definition) {
        super();
        this.definition = definition;
        port = definition.getPort();
        dnsSeeds = definition.getDnsSeeds();
        PROTOCOL_VERSION = definition.getProtocolVersion();
        MIN_PROTOCOL_VERSION = PROTOCOL_VERSION;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        packetMagic = definition.getPacketMagic();
        PAYMENT_PROTOCOL_ID_MAINNET = "main";
        coinName = definition.getCoinName();
    }

    @Override
    public boolean allowEmptyPeerChain() {
        return definition.getAllowEmptyPeers();
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get(Definition definition) {
        if (instance == null) {
            instance = new MainNetParams(definition);
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}