package com.noahseidman.digibytenodes

import com.noahseidman.digibytenodes.adapter.LayoutBinding

data class InfoModel(val message: String): LayoutBinding {

    override fun getLayoutId(): Int {
        return R.layout.info_view
    }
}