package com.noahseidman.nodescrawler

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
import com.noahseidman.coinj.core.AbstractPeerEventListener
import com.noahseidman.coinj.core.Peer
import com.noahseidman.coinj.core.PeerAddress
import com.noahseidman.coinj.core.PeerGroup
import com.noahseidman.coinj.net.discovery.DnsDiscovery
import com.noahseidman.coinj.params.MainNetParams
import com.noahseidman.nodescrawler.adapter.MultiTypeDataBoundAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.xembly.Directives
import org.xembly.Xembler
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val connections: ConcurrentSkipListSet<PeerAddress> = ConcurrentSkipListSet()
    private val openConnections: HashSet<PeerAddress> = HashSet()
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
    private val openCheckerExecutor = Executors.newSingleThreadScheduledExecutor()
    private lateinit var adapter_nodes: MultiTypeDataBoundAdapter
    private lateinit var adapter_info: MultiTypeDataBoundAdapter
    private lateinit var peerGroup: PeerGroup
    private var peer = false
    private var shareActionProvider: ShareActionProvider? = null
    private var getAddresses: GetAddresses? = null
    private val slowOpenChecker: OpenChecker
    private val requestNewPeer: RequestNewPeer

    init {
        slowOpenChecker = OpenChecker(this, 350)
        requestNewPeer = RequestNewPeer(this)
    }

    private class GetAddresses(val activity: MainActivity, val peer: Peer, val peerGroup: PeerGroup): Runnable {

        private var canceled = false
        private var sendCount = 0

        fun cancel() {
            canceled = true
        }

        override fun run() {
            if (sendCount > 20) {
                peerGroup.closeConnections()
                return
            }
            if (!canceled) {
                activity.showMessage("sending getAddr: #" + sendCount)
                peer.getAddresses()
                sendCount++
                activity.scheduledExecutor.schedule(this, 3000, TimeUnit.MILLISECONDS)
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
                        scheduledExecutor.schedule(getAddresses, 0, TimeUnit.MILLISECONDS)
                    }
                }, scheduledExecutor)
            }

            override fun onPeersDiscovered(peer: Peer, peerAddresses: List<PeerAddress>) {
                scheduledExecutor.execute {
                    handler.post {progress.visibility = View.VISIBLE }
                    val filteredAddress = peerAddresses.filter { !contains(it, connections) }
                    if (filteredAddress.isNotEmpty()) {
                        showMessage("getAddr received: " + filteredAddress.size)
                        val viewModels: List<PeerModel> = filteredAddress.map { PeerModel(it.addr.hostAddress, it.port) }
                        connections.addAll(filteredAddress)
                        updateShareIntent()
                        updateCounts(viewModels)
                        getAddresses?.cancel()
                        peerGroup.closeConnections()
                    }
                    handler.post {progress.visibility = View.GONE }
                }
            }

            override fun onDnsDiscovery(addresses: Array<out InetSocketAddress>) {
                scheduledExecutor.execute {
                    if (addresses.isEmpty()) {
                        showMessage("dns discovery: failed")
                    } else {
                        showMessage("dns discovery: success")
                        val addresses = addresses.map { PeerAddress(it.address, it.port) }
                        val viewModels: List<PeerModel> = addresses.map { PeerModel(it.addr.hostAddress, it.port) }
                        connections.addAll(addresses)
                        updateCounts(viewModels)
                        updateShareIntent()
                    }
                }
            }

            override fun onPeerDisconnected(peer: Peer) {
                if (!peer.alreadyDisconnectedFlag) { peer.alreadyDisconnectedFlag = true } else { return }
                getAddresses?.cancel()
                peerGroup.closeConnections()
                showMessage("peer disconnected")
                handler.postDelayed({
                    this@MainActivity.peer = false
                } , 1000)
            }
        })
        peerGroup.addPeerDiscovery(DnsDiscovery(MainNetParams.get()))
        scheduledExecutor.execute {
            handler.post {progress.visibility = View.VISIBLE }
            peerGroup.startUp()
            peerGroup.discoverPeers()
            handler.post {progress.visibility = View.GONE }
        }
        scheduledExecutor.scheduleAtFixedRate(requestNewPeer, 2500, 2500, TimeUnit.MILLISECONDS)
        openCheckerExecutor.scheduleAtFixedRate(slowOpenChecker, 2500, 1/* has to be greater than 0, contains a blocking operation to slow it down */, TimeUnit.MILLISECONDS)
    }

    private fun updateCounts(viewModels: List<PeerModel>? = null) {
        handler.post {
            count.text = String.format(getString(R.string.nodes), connections.size, openConnections.size)
            viewModels?.let {
                adapter_nodes.addItems(viewModels)
                (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, connections.size)
            }
        }
    }

    private class OpenChecker(val activity: MainActivity, val speed: Int): Runnable {
        private val random = Random()

        override fun run() {
            if (activity.connections.isEmpty()) {
                return
            }
            val peerAddress = activity.connections.elementAt(random.nextInt(activity.connections.size - 1))
            if (!peerAddress.open && NetUtils.checkServerListening(peerAddress.addr.hostAddress, 12024, speed) && !activity.contains(peerAddress, activity.openConnections)) {
                activity.openConnections.add(peerAddress)
                activity.updateCounts()
                peerAddress.open = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor.execute {
            scheduledExecutor.shutdown()
            peerGroup.shutDown()
            getAddresses?.cancel()
        }
    }

    private class RequestNewPeer(val activity: MainActivity): Runnable {
        override fun run() {
            if (activity.peer || activity.openConnections.isEmpty()) {
                return
            }
            val peerAddress = activity.openConnections.elementAt(activity.random.nextInt(activity.openConnections.size - 1))
            activity.showMessage("requesting new peer")
            activity.scheduledExecutor.schedule( { activity.peerGroup.connectTo(peerAddress) }, 1000, TimeUnit.MILLISECONDS)
            activity.peer = true
        }
    }

    private fun showMessage(message: String) {
        handler.post {
            adapter_info.addItem(InfoModel(message))
            (recycler_info.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_info, null, adapter_info.itemCount)
            messages.text = String.format(getString(R.string.messages), adapter_info.itemCount)
        }
    }

    override fun onClick(v: View?) {
        scheduledExecutor.execute {
            try {
                if (edit.text.isNullOrEmpty()) {
                    throw NullPointerException()
                }
                val address = InetAddress.getByName(edit.text.toString())
                val peerAddress = PeerAddress(address)
                connections.add(peerAddress)
                updateCounts(null)
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

        val uri = FileProvider.getUriForFile(this, "com.noahseidman.nodescrawler.fileprovider", zipFile)
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


    private fun contains(check: PeerAddress, collection: Set<PeerAddress>): Boolean {
        for (peerAddress in collection) {
            if (peerAddress.equals(check)) {
                return true
            }
        }
        return false
    }
}