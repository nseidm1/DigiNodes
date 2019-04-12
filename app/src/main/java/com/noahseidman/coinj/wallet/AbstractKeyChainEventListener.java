package com.noahseidman.coinj.wallet;

import com.noahseidman.coinj.core.ECKey;

import java.util.List;

public class AbstractKeyChainEventListener implements KeyChainEventListener {
    @Override
    public void onKeysAdded(List<ECKey> keys) {
    }
}

