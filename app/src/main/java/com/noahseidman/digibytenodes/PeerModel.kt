package com.noahseidman.digibytenodes

import com.noahseidman.digibytenodes.adapter.LayoutBinding

data class PeerModel(val address: String, val port: Int): LayoutBinding {
    override fun getLayoutId(): Int {
        return R.layout.peer_view
    }
}