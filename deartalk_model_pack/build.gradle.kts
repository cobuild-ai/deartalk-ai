plugins {
    alias(libs.plugins.android.asset.pack)
}

assetPack {
    packName.set("deartalk_model_pack")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
