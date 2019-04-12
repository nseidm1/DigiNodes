package com.noahseidman.digibytenodes

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.content.FileProvider
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.common.io.ByteStreams
import com.matthewmitchell.peercoinj.core.AbstractPeerEventListener
import com.matthewmitchell.peercoinj.core.Peer
import com.matthewmitchell.peercoinj.core.PeerAddress
import com.matthewmitchell.peercoinj.core.PeerGroup
import com.matthewmitchell.peercoinj.net.discovery.DnsDiscovery
import com.matthewmitchell.peercoinj.params.MainNetParams
import com.noahseidman.digibytenodes.adapter.MultiTypeDataBoundAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.xembly.Directives
import org.xembly.Xembler
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit




class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val connections: ConcurrentSkipListSet<PeerAddress> = ConcurrentSkipListSet()
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val addressExecutor = Executors.newCachedThreadPool()
    private lateinit var adapter_nodes: MultiTypeDataBoundAdapter
    private lateinit var adapter_info: MultiTypeDataBoundAdapter
    private val timer = Timer("Nodes", true)
    private lateinit var peerGroup: PeerGroup
    private var peer = false
    private var shareActionProvider: ShareActionProvider? = null

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
                activity.executor.schedule(this, 6000, TimeUnit.MILLISECONDS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        add.setOnClickListener(this)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_name)
        supportActionBar?.setIcon(R.mipmap.ic_launcher)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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
                        executor.schedule(getAddresses, 6000, TimeUnit.MILLISECONDS)
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
                    val filteredAddress = peerAddresses.filter { it.time >= 28800000 }.filter { !contains(it) }
                    if (filteredAddress.isNotEmpty()) {
                        showMessage("getAddr received: " + filteredAddress.size)
                        val previousSize = connections.size
                        connections.addAll(filteredAddress)
                        updateShareIntent()
                        getAddresses?.cancel()
                        peerGroup.closeConnections()
                        this@MainActivity.peer = false
                        if (previousSize != connections.size) {
                            val list: List<PeerModel> = filteredAddress.map { PeerModel(it.addr.hostAddress, it.port) }
                            handler.post {
                                count.text = "Nodes (" + connections.size + ")"
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
                    connections.add(PeerAddress(address.address, address.port))
                    updateShareIntent()
                    handler.post{
                        adapter_nodes.addItem(PeerModel(address.address.hostAddress, address.port))
                        (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, connections.size)
                    }
                }
                handler.post { count.text = "Nodes (" + connections.size + ")" }
            }

            override fun onPeerDisconnected(peer: Peer) {
                if (!peer.alreadyDisconnectedFlag) { peer.alreadyDisconnectedFlag = true } else { return }
                getAddresses?.cancel()
                peerGroup.closeConnections()
                this@MainActivity.peer = false
                showMessage("peer disconnected")
            }
        })
        peerGroup.addPeerDiscovery(DnsDiscovery(MainNetParams.get()))
        executor.execute {
            handler.post {progress.visibility = View.VISIBLE }
            peerGroup.startUp()
            peerGroup.discoverPeers()
            timer.schedule(object: TimerTask() {
                override fun run() {
                    executor.execute { requestNewPeer(peerGroup) }
                }
            }, 2500, 2500)
            handler.post {progress.visibility = View.GONE }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.execute {
            peerGroup.shutDown()
            timer.cancel()
            getAddresses?.cancel()
        }
    }

    private fun requestNewPeer(peerGroup: PeerGroup) {
        if (peer) {
            return
        }
        handler.post {progress.visibility = View.VISIBLE }
        val peerAddress = connections.elementAt(random.nextInt(connections.size - 1))
        if (!NetUtils.checkServerListening(peerAddress.addr.hostAddress, 12024, 350, null) || peerAddress.addr is Inet6Address) {
            requestNewPeer(peerGroup)
            return
        }
        showMessage("requesting new peer")
        executor.schedule( { peerGroup.connectTo(peerAddress) }, 1000, TimeUnit.MILLISECONDS)
        peer = true
        handler.post {progress.visibility = View.GONE }
    }

    private fun showMessage(message: String) {
        handler.post {
            adapter_info.addItem(InfoModel(message))
            (recycler_info.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_info, null, adapter_info.itemCount)
            messages.text = "Messages (" + adapter_info.itemCount + ")"
        }
    }

    override fun onClick(v: View?) {
        executor.execute {
            try {
                if (edit.text.isNullOrEmpty()) {
                    throw NullPointerException()
                }
                val address = InetAddress.getByName(edit.text.toString())
                val peerAddress = PeerAddress(address)
                connections.add(peerAddress)
                count.text = "Nodes (" + connections.size + ")"
                handler.post {
                    handler.post { showMessage("Manual Node Added") }
                    adapter_nodes.addItem( PeerModel(peerAddress.addr.hostAddress, peerAddress.port))
                    (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, connections.size)
                    edit.text.clear()
                }
            } catch(e: UnknownHostException) {
                handler.post { showMessage("Invalid Node") }
            } catch(e: java.lang.NullPointerException) {
                handler.post { showMessage("Invalid Node") }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share, menu)
        menu.findItem(R.id.menu_item_share).also { menuItem ->
            shareActionProvider = MenuItemCompat.getActionProvider(menuItem) as? ShareActionProvider
        }
        return true
    }

    private fun updateShareIntent() {
        val directives = Directives().add("Nodes");
        for (peerAddress in connections) {
            directives.add("Node").set(peerAddress.addr.hostAddress).up();
        }
        val directory = File(filesDir, "nodes")
        directory.mkdirs()
        val xmlFile = File(directory, "addresses.xml")
        xmlFile.createNewFile()
        ByteStreams.copy(ByteArrayInputStream(Xembler(directives).xml().toByteArray(Charset.defaultCharset())), FileOutputStream(xmlFile))

        val zipFile = File(directory, "addresses.zip")
        ByteStreams.copy(ByteArrayInputStream(ZipUtil.packEntry(xmlFile)), FileOutputStream(zipFile))
        xmlFile.delete()

        val uri = FileProvider.getUriForFile(this, "com.noahseidman.digibytenodes.fileprovider", zipFile)
        val resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.setType("file/zip")
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        handler.post { shareActionProvider?.setShareIntent(intent) }
    }
}