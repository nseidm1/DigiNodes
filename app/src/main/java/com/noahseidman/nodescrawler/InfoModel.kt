package com.noahseidman.nodescrawler

import com.noahseidman.nodescrawler.adapter.LayoutBinding

data class InfoModel(val message: String): LayoutBinding {

    override fun getLayoutId(): Int {
        return R.layout.info_view
    }
}