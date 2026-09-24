plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("gemma_asset_pack")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
