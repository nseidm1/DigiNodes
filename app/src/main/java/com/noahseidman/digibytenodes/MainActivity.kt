package com.noahseidman.digibytenodes

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.matthewmitchell.peercoinj.core.AbstractPeerEventListener
import com.matthewmitchell.peercoinj.core.Peer
import com.matthewmitchell.peercoinj.core.PeerAddress
import com.matthewmitchell.peercoinj.core.PeerGroup
import com.matthewmitchell.peercoinj.net.discovery.DnsDiscovery
import com.matthewmitchell.peercoinj.params.MainNetParams
import com.noahseidman.digibytenodes.adapter.MultiTypeDataBoundAdapter
import kotlinx.android.synthetic.main.activity_main.*
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit




class MainActivity : AppCompatActivity() {

    private val connections: ConcurrentSkipListSet<PeerAddress> = ConcurrentSkipListSet()
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val addressExecutor = Executors.newCachedThreadPool()
    private lateinit var adapter_nodes: MultiTypeDataBoundAdapter
    private lateinit var adapter_info: MultiTypeDataBoundAdapter
    private val timer = Timer("Nodes", true)
    private lateinit var peerGroup: PeerGroup

    private var getAddresses: GetAddresses? = null

    private class GetAddresses(val activity: MainActivity, val peer: Peer, val peerGroup: PeerGroup): Runnable {

        private var canceled = false
        private var sendCount = 0

        fun cancel() {
            canceled = true
        }

        override fun run() {
            if (sendCount > 10) {
                peerGroup.closeConnections()
                return
            }
            if (!canceled) {
                activity.showMessage("sending getAddr: #" + sendCount)
                peer.getAddresses()
                sendCount++
                activity.executor.schedule(this, 2500, TimeUnit.MILLISECONDS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler_nodes.layoutManager = LinearLayoutManager(this)
        (recycler_nodes.layoutManager as LinearLayoutManager).setSmoothScrollbarEnabled(true);
        adapter_nodes = MultiTypeDataBoundAdapter(null, null)
        recycler_nodes.adapter = adapter_nodes

        recycler_info.layoutManager = LinearLayoutManager(this)
        (recycler_info.layoutManager as LinearLayoutManager).setSmoothScrollbarEnabled(true);
        adapter_info = MultiTypeDataBoundAdapter(null, null)
        recycler_info.adapter = adapter_info
        peerGroup = PeerGroup(MainNetParams.get())
        peerGroup.addEventListener(object: AbstractPeerEventListener() {

            override fun onPeerConnected(peer: Peer) {
                peer.connectionOpenFuture.addListener(object: Runnable {
                    override fun run() {
                        getAddresses?.cancel()
                        getAddresses = GetAddresses(this@MainActivity, peer, peerGroup)
                        executor.schedule(getAddresses, 2500, TimeUnit.MILLISECONDS)
                    }
                }, executor)
            }

            private fun contains(check: PeerAddress): Boolean {
                for (peerAddress in connections) {
                    if (peerAddress.equals(check)) {
                        return true
                    }
                }
                return false
            }

            override fun onPeersDiscovered(peer: Peer, peerAddresses: List<PeerAddress>) {
                addressExecutor.execute {
                    val previousSize = connections.size
                    val filteredAddress = peerAddresses.filter { it.time >= 28800000 }.filter { !contains(it) }
                    if (filteredAddress.isNotEmpty()) {
                        showMessage("getAddr received: " + filteredAddress.size)
                        connections.addAll(filteredAddress)
                        getAddresses?.cancel()
                        peerGroup.closeConnections()
                        if (previousSize != connections.size) {
                            val list: List<PeerModel> = filteredAddress.map { PeerModel(it.addr.hostAddress, it.port) }
                            handler.post {
                                count.text = "Count: " + connections.size
                                adapter_nodes.addItems(list)
                                (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, connections.size)
                            }
                        }
                    }
                }
            }

            override fun onDnsDiscovery(addresses: Array<out InetSocketAddress>) {
                showMessage("dns discovery")
                for (address in addresses) {
                    val peerAddress = PeerAddress(address.address, address.port)
                    connections.add(peerAddress)
                    handler.post{
                        adapter_nodes.addItem(PeerModel(address.address.hostAddress, address.port))
                        (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, connections.size)
                    }
                }
                handler.post { count.text = "Count: " + connections.size }

            }

            override fun onPeerDisconnected(peer: Peer) {
                if (!peer.alreadyDisconnectedFlag) {
                    peer.alreadyDisconnectedFlag = true
                } else {
                    return
                }
                getAddresses?.cancel()
                peerGroup.closeConnections()
                showMessage("peer disconnected")
            }
        })
        peerGroup.addPeerDiscovery(DnsDiscovery(MainNetParams.get()))
        peerGroup.discoverPeers()
        peerGroup.startUp()
        executor.execute { requestNewPeer(peerGroup) }
        timer.schedule(object: TimerTask() {

            override fun run() {
                if (!peerGroup.hasConnections()) {
                    executor.execute { requestNewPeer(peerGroup) }
                }
            }
        }, 1000, 5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        peerGroup.shutDown()
        timer.cancel()
        getAddresses?.cancel()
    }

    private fun requestNewPeer(peerGroup: PeerGroup) {
        val peerAddress = connections.elementAt(random.nextInt(connections.size - 1))
        if (!peerAddress.addr.isReachable(10) || peerAddress.addr is Inet6Address) {
            requestNewPeer(peerGroup)
            return
        }
        showMessage("requesting new peer")
        executor.schedule( { peerGroup.connectTo(peerAddress) }, 1000, TimeUnit.MILLISECONDS)
    }

    private fun showMessage(message: String) {
        handler.post {
            adapter_info.addItem(InfoModel(message))
            (recycler_info.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_info, null, adapter_info.itemCount)
        }
    }
}