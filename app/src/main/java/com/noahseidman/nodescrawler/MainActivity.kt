package com.noahseidman.nodescrawler

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import org.json.JSONArray
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private val nodes: LinkedHashSet<PeerAddress> = LinkedHashSet()
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
    private var openCount = 0
    private var recentsCount = 0

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
                    showMessage("getAddr received: processing")
                    Log.d("Crawl", "Received: ${peerAddresses.size} nodes from getAddr")
                    val newNodes: LinkedList<PeerAddress> = LinkedList()
                    peerAddresses.forEach {
                        if (nodes.add(it)) {
                            newNodes.add(it)
                            addNode(it)
                            updateCounts()
                        }
                    }
                    if (newNodes.size > 0) {
                        showMessage("${newNodes.size} new nodes added")
                    } else {
                        showMessage("no new nodes found")
                    }
                    updateRecentsCount()
                    updateCounts()
                    val shareJson = getShareJson(newNodes)
                    updateShareIntent(shareJson)
                    if (peer.getAddrCount > 1) {
                        getAddresses?.cancel()
                        peerGroup?.closeConnections()
                    } else {
                        peer.getAddrCount++
                    }
                    showProgressBar(false)
                }
            }

            override fun onDnsDiscovery(addresses: Array<out InetSocketAddress>) {
                scheduledExecutor.execute {
                    if (addresses.isEmpty()) {
                        showMessage("dns discovery: failed")
                    } else {
                        showMessage("dns discovery: success")
                        val peerAddresses = addresses.map { PeerAddress(it.address, it.port) }
                        nodes.addAll(peerAddresses)
                        peerAddresses.forEach {
                            addNode(it)
                        }
                        openCount = 0
                        recentsCount = 0
                        updateRecentsCount()
                        updateCounts()
                        val shareJson = getShareJson(peerAddresses)
                        updateShareIntent(shareJson)
                        crowSource(shareJson)
                    }
                }
                spinner.isEnabled = true
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
                if (edit.text.isNullOrEmpty()) { throw NullPointerException() }
                val address = InetAddress.getByName(edit.text.toString())
                val peerAddress = PeerAddress(address)
                nodes.add(peerAddress)
                updateCounts()
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
        spinner.isEnabled = false
        shutdownExistingPeer(OnShutdownCompleteCallback {
            when (position) {
                0 -> init(DigiByteCoinDefition.get())
                1 -> init(BitcoinCoinDefition.get())
                2 -> init(VertCoinDefinition.get())
                3 -> init(RapidsCoinDefition.get())
                4 -> init(DogeCoinDefition.get())
                5 -> init(ZCashCoinDefition.get())
                6 -> init(DashCoinDefinition.get())
                7 -> init(BitcoinGoldCoinDefinition.get())
                8 -> init(BitcoinCashCoinDefinition.get())
                9 -> init(BitcoinDiamondCoinDefinition.get())
                10 -> init(BitcoinSVCoinDefinition.get())
                11 -> init(LitecoinDefinition.get())
                12 -> init(BlocknetDefinition.get())
                13 -> init(ZCoinDefinition.get())
                14 -> init(KomodoDefinition.get())
                15 -> init(StratisDefinition.get())
                16 -> init(PivxCoinDefinition.get())
                17 -> init(MueCoinDefinition.get())
                18 -> init(PhoreCoinDefinition.get())
            }
        })
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

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

    private fun addNode(peerAddress: PeerAddress) {
        handler.post {
            adapter_nodes.addItem(peerAddress)
            handler.post {
                scrollToBottom()
            }
        }
    }

    private fun updateCounts() {
        handler.post {
            count.text = String.format(getString(R.string.nodes), nodes.size, openCount, recentsCount)
        }
    }

    private fun scrollToBottom() {
        (recycler_nodes.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_nodes, null, nodes.size)
    }

    private fun showMessage(message: String) {
        handler.post {
            adapter_info.addItem(InfoModel(message))
            (recycler_info.layoutManager as LinearLayoutManager).smoothScrollToPosition(recycler_info, null, adapter_info.itemCount)
            messages.text = String.format(getString(R.string.messages), adapter_info.itemCount)
        }
    }

    private fun getShareJson(nodes: List<PeerAddress>): JSONArray {
        val array = JSONArray(nodes)
        array.put(SelectedNetParams.instance.coinName)
        for (peerAddress in nodes) { array.put(peerAddress.addr.hostAddress) }
        return array
    }

    private fun crowSource(shareJson: JSONArray) {

    }

    @Suppress("UnstableApiUsage")
    private fun updateShareIntent(array: JSONArray) {
        val directory = File(filesDir, "nodes")
        directory.mkdirs()
        val xmlFile = File(directory, "addresses.json")
        xmlFile.createNewFile()
        ByteStreams.copy(ByteArrayInputStream(array.toString().toByteArray()), FileOutputStream(xmlFile))

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
        intent.type = "file/zip"
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        handler.post { shareActionProvider?.setShareIntent(intent) }
    }

    private fun updateRecentsCount() {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.HOUR, -8)
        val currentTime = calendar.time
        var count = 0
        for (peerAddress in nodes) {
            if (peerAddress.time.after(currentTime)) {
                count++
            }
        }
        this.recentsCount = count
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
                    activity.openCount++
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