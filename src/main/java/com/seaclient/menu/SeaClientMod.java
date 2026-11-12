package com.seaclient.menu;

import com.seaclient.menu.media.SystemMediaBridge;
import net.fabricmc.api.ClientModInitializer;

public class SeaClientMod implements ClientModInitializer {

    public static final String MOD_ID = "sea-client";

    @Override
    public void onInitializeClient() {
        SystemMediaBridge.getInstance().ensureStarted();
    }
}
