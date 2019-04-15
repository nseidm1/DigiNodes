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
import com.google.common.collect.Sets
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private val nodes: MutableSet<PeerAddress> = Sets.newSetFromMap(ConcurrentHashMap<PeerAddress, Boolean>());
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
            getString(R.string.bitcoinsv), getString(R.string.litecoin), getString(R.string.block),
            getString(R.string.zcoin), getString(R.string.komodo), getString(R.string.stratis),
            getString(R.string.pivx), getString(R.string.mue), getString(R.string.phore))
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
        openCheckerExecutor.scheduleAtFixedRate(OpenCheckerRunnable(this, 1000), 2500, 1, TimeUnit.MILLISECONDS)
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
        nodes.clear()
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
                    peer.connectionOpenFuture.addListener(Runnable {
                        getAddresses?.cancel()
                        getAddresses = GetAddressesRunnable(this@MainActivity, peer, peerGroup!!)
                        scheduledExecutor.schedule(getAddresses, 0, TimeUnit.MILLISECONDS)
                    }, it)
                }
            }

            override fun onPeersDiscovered(peer: Peer, peerAddresses: List<PeerAddress>) {
                scheduledExecutor.execute {
                    showProgressBar(true)
                    getAddresses?.cancel()
                    showMessage("getAddr received: processing")
                    nodes.addAll(peerAddresses)
                    updateShareIntent()
                    updateCounts(peerAddresses)
                    peerGroup!!.closeConnections()
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
                        nodes.addAll(peerAddresses)
                        updateCounts(peerAddresses)
                        updateShareIntent()
                    }
                }
            }

            override fun onPeerDisconnected(peer: Peer) {
                scheduledExecutor.let {
                    if (!peer.alreadyDisconnectedFlag) { peer.alreadyDisconnectedFlag = true } else { return }
                    getAddresses?.cancel()
                    peerGroup?.closeConnections()
                    showMessage("node disconnected")
                    handler.postDelayed({
                        this@MainActivity.getNewPeerFlag = true
                    } , 1000)
                }
            }

            override fun timeoutOccured() {
                showMessage("timeout connecting")
            }
        })
        peerGroup!!.addPeerDiscovery(DnsDiscovery(SelectedNetParams.instance))
    }

    private fun showProgressBar(show: Boolean) {
        handler.post {
            if (show) {
                spinner.isEnabled = false
                progress.visibility = View.VISIBLE
            } else {
                spinner.isEnabled = true
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
                nodes.add(peerAddress)
                updateCounts(null)
                handler.post {
                    handler.post { showMessage("Manual Node Added") }
                    adapter_nodes.addItem(peerAddress)
                    scrollToBottom()
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
        showMessage("starting selected coin")
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
                15 -> {
                    init(StratisDefinition.get())
                }
                16 -> {
                    init(PivxCoinDefinition.get())
                }
                17 -> {
                    init(MueCoinDefinition.get())
                }
                18 -> {
                    init(PhoreCoinDefinition.get())
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

    private fun updateCounts(viewModels: List<PeerAddress>? = null) {
        handler.post {
            count.text = String.format(getString(R.string.nodes), nodes.size, getOpenCount(), getRecentsCount())
            viewModels?.let { vm ->
                var hasNewAddress = false
                vm.forEach {
                    if (!it.existing) {
                        adapter_nodes.addItem(it)
                        hasNewAddress = true
                    }
                }
                if (hasNewAddress) {
                    scrollToBottom()
                }
            }
        }
    }

    private fun scrollToBottom() {
        if ((recycler_nodes.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == nodes.size) {
            (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, nodes.size)
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
        for (peerAddress in nodes) {
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

    private fun getRecentsCount(): Int {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.HOUR, -8)
        var count = 0
        for (peerAddress in nodes) {
            if (peerAddress.time.after(calendar.time)) {
                count++
            }
        }
        return count
    }

    private fun getNewOpenPeer(): PeerAddress? {
        if (nodes.isEmpty()) {
            return null
        }
        val openList = nodes.filter { it.open }
        if (openList.isEmpty()) {
            return null
        } else {
            return openList.random()

        }
    }

    private fun getOpenCount(): Int {
        var openCount = 0
        for (node in nodes) {
            if (node.open) {
                openCount++
            }
        }
        return openCount
    }

    ////////////////////
    /////Runnables
    ////////////////////

    private class OpenCheckerRunnable(val activity: MainActivity, val speed: Int): Runnable {
        override fun run() {
            if (!activity.nodes.isEmpty()) {
                val peerAddress = activity.nodes.random()
                if (!peerAddress.open && NetUtils.checkServerListening(peerAddress.addr.hostAddress, SelectedNetParams.instance.port, speed)) {
                    activity.updateCounts()
                    peerAddress.open = true
                }
            }
        }
    }

    private class GetAddressesRunnable(val activity: MainActivity, val peer: Peer, val peerGroup: PeerGroup): Runnable {

        private var canceled = false
        private var sendGetAddrCount = 0

        fun cancel() {
            canceled = true
        }

        override fun run() {
            if (sendGetAddrCount > 20) {
                peerGroup.closeConnections()
                return
            }
            if (!canceled) {
                activity.showMessage("sending getAddr: #" + sendGetAddrCount)
                peer.getAddresses()
                sendGetAddrCount++
                activity.scheduledExecutor.schedule(this, 3000, TimeUnit.MILLISECONDS)
            }
        }
    }

    private class RequestNewPeerRunnable(val activity: MainActivity): Runnable {
        override fun run() {
            if (!activity.getNewPeerFlag) {
                return
            }
            val openNode = activity.getNewOpenPeer()
            openNode?.let {
                activity.showMessage("requesting new node")
                activity.scheduledExecutor.schedule( { activity.peerGroup?.connectTo(it) }, 1000, TimeUnit.MILLISECONDS)
                activity.getNewPeerFlag = false
            }
        }
    }
}