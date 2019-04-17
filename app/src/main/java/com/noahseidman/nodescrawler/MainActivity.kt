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
import java.util.concurrent.*


class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private val nodes = Collections.newSetFromMap(ConcurrentHashMap<PeerAddress, Boolean>());
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter_nodes: MultiTypeDataBoundAdapter
    private lateinit var adapter_info: MultiTypeDataBoundAdapter
    private var generalExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var exportAggregatorExecutor: Executor = Executors.newSingleThreadExecutor()
    private var openCheckerExecutor: Executor = Executors.newCachedThreadPool()
    private var peerGroup: PeerGroup? = null
    private var shareActionProvider: ShareActionProvider? = null
    private var getAddresses: GetAddressesRunnable? = null
    private val calendar = Calendar.getInstance()
    private var getNewPeerFlag = true
    private var openCount = 0
    private var recentsCount = 0
    private val exportJson = JSONArray()

    companion object {
        private var openCheckIndex = 0
    }

    init {
        generalExecutor.scheduleAtFixedRate(RequestNewPeerRunnable(this), 2500, 2500, TimeUnit.MILLISECONDS)
        openCheckerExecutor.execute(OpenCheckerRunnable(this, 500))
        openCheckerExecutor.execute(OpenCheckerRunnable(this, 500))
    }

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
            getString(R.string.pivx), getString(R.string.mue), getString(R.string.phore), getString(R.string.syscoin))
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, coins)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownExistingPeer()
    }

    private fun init(networkParameters: NetworkParameters) {
        exportJson.put(networkParameters.coinName)
        generalExecutor.execute {
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
        openCheckIndex = 0
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
                generalExecutor.let {
                    peer.connectionOpenFuture.addListener(Runnable {
                        getAddresses?.cancel()
                        getAddresses = GetAddressesRunnable(this@MainActivity, peer, peerGroup!!)
                        generalExecutor.schedule(getAddresses, 0, TimeUnit.MILLISECONDS)
                    }, it)
                }
            }

            override fun onPeersDiscovered(peer: Peer, peerAddresses: List<PeerAddress>) {
                generalExecutor.execute {
                    showProgressBar(true)
                    showMessage("getAddr received: processing")
                    Log.d("Crawl", "Received: ${peerAddresses.size} nodes from getAddr")
                    val newNodes: LinkedList<PeerAddress> = LinkedList()
                    peerAddresses.forEach {
                        Log.d("Addresses", "Address: " + it.addr.hostAddress)
                        if (nodes.add(it)) {
                            newNodes.add(it)
                            exportJson.put(it.addr.hostAddress)
                        }
                    }
                    updateCounts()
                    addNodes(newNodes)
                    if (newNodes.size > 0) {
                        showMessage("${newNodes.size} new nodes added")
                    } else {
                        showMessage("no new nodes found")
                    }
                    updateRecentsCount()
                    updateCounts()
                    updateShareIntent()
                    if (peer.getAddrCount >= 1) {
                        getAddresses?.cancel()
                        peerGroup?.closeConnections()
                    } else {
                        peer.getAddrCount++
                    }
                    showProgressBar(false)
                }
            }

            override fun onDnsDiscovery(addresses: Array<out InetSocketAddress>?) {
                generalExecutor.execute {
                    if (addresses.isNullOrEmpty()) {
                        showMessage("dns discovery: failed")
                    } else {
                        showMessage("dns discovery: success")
                        val peerAddresses = addresses.map { PeerAddress(it.address, it.port) }
                        nodes.addAll(peerAddresses)
                        addNodes(peerAddresses)
                        peerAddresses.forEach {
                            exportJson.put(it.addr.hostAddress)
                        }
                        openCount = 0
                        recentsCount = 0
                        updateRecentsCount()
                        updateCounts()
                        updateShareIntent()
                    }
                }
                handler.post{ spinner.isEnabled = true }
            }

            override fun onPeerDisconnected(peer: Peer) {
                generalExecutor.let {
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
        generalExecutor.execute {
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
                19 -> init(SyscoinDefinition.get())
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

    private fun addNodes(peerAddresses: List<PeerAddress>) {
        handler.post {
            adapter_nodes.addItems(peerAddresses)
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

    private fun updateOpenCheckerCount() {
        handler.post {
            open_checker.text = String.format(getString(R.string.open_checker), openCheckIndex)
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

    @Suppress("UnstableApiUsage")
    private fun updateShareIntent() {
        exportAggregatorExecutor.execute {
            try {
                val directory = File(filesDir, "nodes")
                directory.mkdirs()
                val jsonFile = File(directory, "addresses.json")
                jsonFile.createNewFile()
                ByteStreams.copy(ByteArrayInputStream(exportJson.toString().toByteArray()), FileOutputStream(jsonFile))

                val zipFile = File(directory, "addresses.zip")
                zipFile.delete()
                zipFile.createNewFile()
                ByteStreams.copy(ByteArrayInputStream(ZipUtil.packEntry(jsonFile)), FileOutputStream(zipFile))
                jsonFile.delete()

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
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
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

    private class OpenCheckerRunnable(val activity: MainActivity, val timeout: Int): Runnable {
        override fun run() {
            //Lots of not empty checks to account for coin switching
            if (activity.nodes.isNotEmpty()) {
                val peerAddress = activity.nodes.elementAt(openCheckIndex)
                synchronized(peerAddress) {
                    if (!peerAddress.open && NetUtils.checkServerListening(peerAddress, timeout) && activity.nodes.isNotEmpty()) {
                        activity.nodes.remove(peerAddress)
                        peerAddress.open = true
                        activity.nodes.add(peerAddress)
                        activity.updateCounts()
                        activity.openCount++
                    }
                }
                if (activity.nodes.isNotEmpty()) {
                    synchronized(openCheckIndex) {
                        if (openCheckIndex < activity.nodes.size - 1) {
                            openCheckIndex++
                        } else {
                            openCheckIndex = 0
                        }
                        activity.updateOpenCheckerCount()
                    }
                }
            }
            Thread.sleep(350)
            activity.openCheckerExecutor.execute(this)
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
                activity.generalExecutor.schedule(this, 3000, TimeUnit.MILLISECONDS)
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
                activity.generalExecutor.schedule( { activity.peerGroup?.connectTo(it) }, 1000, TimeUnit.MILLISECONDS)
                activity.getNewPeerFlag = false
            }
        }
    }
}