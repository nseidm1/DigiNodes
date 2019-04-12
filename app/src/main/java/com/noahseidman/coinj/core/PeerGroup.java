/**
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


package com.noahseidman.coinj.core;

import com.google.common.collect.Lists;
import com.noahseidman.coinj.net.NioClientManager;
import com.noahseidman.coinj.net.discovery.PeerDiscovery;
import com.noahseidman.coinj.net.discovery.PeerDiscoveryException;
import com.noahseidman.coinj.utils.ListenerRegistration;
import com.noahseidman.nodescrawler.CoinDefinition;
import net.jcip.annotations.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class PeerGroup {
    private static final Logger log = LoggerFactory.getLogger(PeerGroup.class);

    private final NioClientManager channels;
    private final CopyOnWriteArrayList<ListenerRegistration<PeerEventListener>> peerEventListeners;
    private final CopyOnWriteArraySet<PeerDiscovery> peerDiscoverers;
    @GuardedBy("lock") private VersionMessage versionMessage;
    private volatile int vMinRequiredProtocolVersion = CoinDefinition.MIN_PROTOCOL_VERSION;
    private final NetworkParameters params;
    private MemoryPool pool = new MemoryPool();
    private Executor executor = Executors.newSingleThreadExecutor();

    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10000;

    public PeerGroup(NetworkParameters params) {
        this.params = checkNotNull(params);
        versionMessage = new VersionMessage(params, 0);
        versionMessage.relayTxesBeforeFilter = true;
        channels = new NioClientManager();
        peerDiscoverers = new CopyOnWriteArraySet<>();
        peerEventListeners = new CopyOnWriteArrayList<>();
    }

    void setVersionMessage(VersionMessage ver) {
        versionMessage = ver;
    }

    VersionMessage getVersionMessage() {
        return versionMessage;
    }

    public void addEventListener(PeerEventListener listener, Executor executor) {
        peerEventListeners.add(new ListenerRegistration<>(checkNotNull(listener), executor));
    }

    public void addEventListener(PeerEventListener listener) {
        addEventListener(listener, executor);
    }

    public boolean removeEventListener(PeerEventListener listener) {
        return ListenerRegistration.removeFromList(listener, peerEventListeners);
    }

    public void clearEventListeners() {
        peerEventListeners.clear();
    }

    public void addPeerDiscovery(PeerDiscovery peerDiscovery) {
        peerDiscoverers.add(peerDiscovery);
	}

	public void discoverPeers() throws PeerDiscoveryException {

		CopyOnWriteArraySet<PeerDiscovery> discoverers = peerDiscoverers;

        if (discoverers.isEmpty())
            throw new PeerDiscoveryException("No peer discoverers registered");
        
        long start = System.currentTimeMillis();

        final List<PeerAddress> addressList = Lists.newLinkedList();
        for (PeerDiscovery peerDiscovery : discoverers) {
            InetSocketAddress[] addresses;
            addresses = peerDiscovery.getPeers(5, TimeUnit.SECONDS);
            for (final ListenerRegistration<PeerEventListener> registration : peerEventListeners) {
                registration.executor.execute(() -> registration.listener.onDnsDiscovery(addresses));
            }
        }
        log.info("{}eer discovery took {}msec and returned {} items", "P",
                System.currentTimeMillis() - start, addressList.size());
    }

    public void startUp() {
        channels.startAsync();
        channels.awaitRunning();
    }

    public void shutDown() {
        channels.stopAsync();
        channels.awaitTerminated();
        stopPeerDiscovery();
    }

    public void stopPeerDiscovery() {
        for (PeerDiscovery peerDiscovery : peerDiscoverers) {
            peerDiscovery.shutdown();
        }
    }

    public void closeConnections() {
        channels.closeConnections(channels.getConnectedClientCount());
    }

    public boolean hasConnections() {
        return channels.getConnectedClientCount() > 0;
    }

    public Peer connectTo(PeerAddress address) {
        VersionMessage ver = getVersionMessage().duplicate();
        ver.bestHeight = 0;
        ver.time = Utils.currentTimeSeconds();
        Peer peer = new Peer(params, ver, address, null, pool, false, this);
        for (ListenerRegistration<PeerEventListener> registration : peerEventListeners) {
            peer.addEventListener(registration.listener, executor);
        }
        peer.setSocketTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
        peer.setMinProtocolVersion(vMinRequiredProtocolVersion);
        try {
            channels.openConnection(address.toSocketAddress(), peer);
        } catch (Exception e) {
            log.warn("Failed to connect to " + address + ": " + e.getMessage());
            handlePeerDeath(peer);
            return null;
        }
        return peer;
    }

    void handlePeerDeath(final Peer peer) {
        for (final ListenerRegistration<PeerEventListener> registration : peerEventListeners) {
            registration.executor.execute(() -> registration.listener.onPeerDisconnected(peer));
        }
    }
}