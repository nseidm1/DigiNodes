package com.noahseidman.nodescrawler

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.content.FileProvider
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.common.io.ByteStreams
import com.noahseidman.coinj.core.*
import com.noahseidman.coinj.net.discovery.DnsDiscovery
import com.noahseidman.nodescrawler.adapter.MultiTypeDataBoundAdapter
import com.noahseidman.nodescrawler.coindefinitions.*
import com.noahseidman.nodescrawler.interfaces.OnShutdownCompleteCallback
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet


class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private val connections: HashSet<PeerAddress> = HashSet()
    private val openConnections: HashSet<PeerAddress> = HashSet()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter_nodes: MultiTypeDataBoundAdapter
    private lateinit var adapter_info: MultiTypeDataBoundAdapter
    private var scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var openCheckerExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var peerGroup: PeerGroup? = null
    private var shareActionProvider: ShareActionProvider? = null
    private var getAddresses: GetAddressesRunnable? = null
    private val calendar = Calendar.getInstance()

    private var getNewPeerFlag = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        add.setOnClickListener(this)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setIcon(R.mipmap.ic_launcher)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        recycler_nodes.layoutManager = LinearLayoutManager(this)
        adapter_nodes = MultiTypeDataBoundAdapter(null, null)
        recycler_nodes.adapter = adapter_nodes

        recycler_info.layoutManager = LinearLayoutManager(this)
        (recycler_info.layoutManager as LinearLayoutManager).setSmoothScrollbarEnabled(true);
        adapter_info = MultiTypeDataBoundAdapter(null, null)
        recycler_info.adapter = adapter_info

        val coins = arrayOf(getString(R.string.digibyte), getString(R.string.bitcoin), getString(R.string.vertcoin),
            getString(R.string.rapids), getString(R.string.doge), getString(R.string.zcash), getString(R.string.dash),
            getString(R.string.bitcoingold), getString(R.string.bitcoincash), getString(R.string.bitcoindiamond),
            getString(R.string.bitcoinsv), getString(R.string.litecoin), getString(R.string.block), getString(R.string.zcoin), getString(R.string.komodo), getString(R.string.stratis))
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, coins)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownExistingPeer()
    }

    private fun init(networkParameters: NetworkParameters) {
        scheduledExecutor.scheduleAtFixedRate(RequestNewPeerRunnable(this), 2500, 2500, TimeUnit.MILLISECONDS)
        openCheckerExecutor.scheduleAtFixedRate(OpenCheckerRunnable(this, 350), 2500, 1, TimeUnit.MILLISECONDS)
        scheduledExecutor.execute {
            showProgressBar(true)
            setupNewPeerGroup(networkParameters)
            peerGroup!!.startUp()
            peerGroup!!.discoverPeers()
            showProgressBar(false)
        }
    }

    private fun shutdownExistingPeer(shutdownCompleteCallback: OnShutdownCompleteCallback? = null) {
        spinner.isEnabled = false
        getNewPeerFlag = false
        getAddresses?.cancel()
        adapter_nodes.clear()
        connections.clear()
        openConnections.clear()
        peerGroup?.shutDown()
        peerGroup = null
        handler.postDelayed( {
            getNewPeerFlag = true
            shutdownCompleteCallback?.onShutdownComplete()
        }, 2500)
    }

    /**
     * Do not call directly, use [init]
     *
     * @param networkParameters
     * Provide a NetworkParameters instance to create a new getNewPeerFlag
     */
    private fun setupNewPeerGroup(networkParameters: NetworkParameters) {
        SelectedNetParams.instance = networkParameters;
        peerGroup = PeerGroup(SelectedNetParams.instance)
        peerGroup!!.addEventListener(object: AbstractPeerEventListener() {
            override fun onPeerConnected(peer: Peer) {
                scheduledExecutor.let {
                    peer.connectionOpenFuture.addListener(object: Runnable {
                        override fun run() {
                            getAddresses?.cancel()
                            getAddresses = GetAddressesRunnable(this@MainActivity, peer, peerGroup!!)
                            scheduledExecutor.schedule(getAddresses, 0, TimeUnit.MILLISECONDS)
                        }
                    }, it)
                }
            }

            override fun onPeersDiscovered(peer: Peer, peerAddresses: List<PeerAddress>) {
                scheduledExecutor.execute {
                    showProgressBar(true)
                    updateRecents(peerAddresses)
                    val filteredAddress = peerAddresses.filter { !contains(it, connections) }
                    if (filteredAddress.isNotEmpty()) {
                        showMessage("getAddr received: " + filteredAddress.size)
                        val viewModels: List<PeerModel> = filteredAddress.map { PeerModel(it.addr.hostAddress, it.port) }
                        connections.addAll(filteredAddress)
                        updateShareIntent()
                        updateCounts(viewModels)
                        getAddresses?.cancel()
                        peerGroup!!.closeConnections()
                    }
                    showProgressBar(false)
                }
            }

            override fun onDnsDiscovery(addresses: Array<out InetSocketAddress>) {
                handler.post{ spinner.isEnabled = true }
                scheduledExecutor.execute {
                    if (addresses.isEmpty()) {
                        showMessage("dns discovery: failed")
                    } else {
                        showMessage("dns discovery: success")
                        val peerAddresses = addresses.map { PeerAddress(it.address, it.port) }
                        val viewModels: List<PeerModel> = peerAddresses.map { PeerModel(it.addr.hostAddress, it.port) }
                        connections.addAll(peerAddresses)
                        updateCounts(viewModels)
                        updateShareIntent()
                    }
                }
            }

            override fun onPeerDisconnected(peer: Peer) {
                scheduledExecutor.let {
                    if (!peer.alreadyDisconnectedFlag) { peer.alreadyDisconnectedFlag = true } else { return }
                    getAddresses?.cancel()
                    peerGroup?.closeConnections()
                    showMessage("peer disconnected")
                    handler.postDelayed({
                        this@MainActivity.getNewPeerFlag = true
                    } , 1000)
                }
            }
        })
        peerGroup!!.addPeerDiscovery(DnsDiscovery(SelectedNetParams.instance))
    }

    private fun showProgressBar(show: Boolean) {
        handler.post {
            if (show) {
                progress.visibility = View.VISIBLE
            } else {
                progress.visibility = View.GONE
            }
        }
    }

    /**
     * For the add custom address button
     */
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

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        shutdownExistingPeer(OnShutdownCompleteCallback {
            when (position) {
                0 -> {
                    init(DigiByteCoinDefition.get())
                }
                1 -> {
                    init(BitcoinCoinDefition.get())
                }
                2 -> {
                    init(VertCoinDefinition.get())
                }
                3 -> {
                    init(RapidsCoinDefition.get())
                }
                4 -> {
                    init(DogeCoinDefition.get())
                }
                5 -> {
                    init(ZCashCoinDefition.get())
                }
                6 -> {
                    init(DashCoinDefinition.get())
                }
                7 -> {
                    init(BitcoinGoldCoinDefinition.get())
                }
                8 -> {
                    init(BitcoinCashCoinDefinition.get())
                }
                9 -> {
                    init(BitcoinDiamondCoinDefinition.get())
                }
                10 -> {
                    init(BitcoinSVCoinDefinition.get())
                }
                11 -> {
                    init(LitecoinDefinition.get())
                }
                12 -> {
                    init(BlocknetDefinition.get())
                }
                13 -> {
                    init(ZCoinDefinition.get())
                }
                14 -> {
                    init(KomodoDefinition.get())
                }
                14 -> {
                    init(StratisDefinition.get())
                }
            }
        })
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share, menu)
        menu.findItem(R.id.menu_item_share).also { menuItem ->
            shareActionProvider = MenuItemCompat.getActionProvider(menuItem) as? ShareActionProvider
        }
        return true
    }

    //////////////////////
    /////Update UI methods
    //////////////////////

    private fun updateCounts(viewModels: List<PeerModel>? = null) {
        handler.post {
            count.text = String.format(getString(R.string.nodes), connections.size, openConnections.size, getRecentsCount())
            viewModels?.let {
                adapter_nodes.addItems(viewModels)
                (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, connections.size)
            }
        }
    }

    private fun showMessage(message: String) {
        handler.post {
            adapter_info.addItem(InfoModel(message))
            (recycler_info.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_info, null, adapter_info.itemCount)
            messages.text = String.format(getString(R.string.messages), adapter_info.itemCount)
        }
    }

    @Suppress("UnstableApiUsage")
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

    private fun updateRecents(rawAddresses: List<PeerAddress>) {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.HOUR, -8)
        val recentAddresses = rawAddresses.filter { it.time.after(calendar.time) && contains(it, connections) }
        if (recentAddresses.isNotEmpty()) {
            connections.addAll(recentAddresses)
            updateCounts()
        }
    }

    private fun getRecentsCount(): Int {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.HOUR, -8)
        var count = 0
        for (peerAddress in connections) {
            if (peerAddress.time.after(calendar.time)) {
                count++
            }
        }
        return count
    }

    ////////////////////
    /////Runnables
    ////////////////////

    private class OpenCheckerRunnable(val activity: MainActivity, val speed: Int): Runnable {
        private val random = Random()

        override fun run() {
            if (!activity.connections.isEmpty()) {
                val peerAddress = activity.connections.random()
                if (!peerAddress.open && NetUtils.checkServerListening(peerAddress.addr.hostAddress, SelectedNetParams.instance.port, speed) && !activity.contains(peerAddress, activity.openConnections)) {
                    activity.openConnections.add(peerAddress)
                    activity.updateCounts()
                    peerAddress.open = true
                }
            }
        }
    }

    private class GetAddressesRunnable(val activity: MainActivity, val peer: Peer, val peerGroup: PeerGroup): Runnable {

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

    private class RequestNewPeerRunnable(val activity: MainActivity): Runnable {
        override fun run() {
            if (activity.getNewPeerFlag && activity.openConnections.isNotEmpty()) {
                val peerAddress = activity.openConnections.random()
                activity.showMessage("requesting new peer")
                activity.scheduledExecutor.schedule( { activity.peerGroup?.connectTo(peerAddress) }, 1000, TimeUnit.MILLISECONDS)
                activity.getNewPeerFlag = false
            }
        }
    }
}