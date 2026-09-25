package com.kkfittracking.watch

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.kkfittracking.wear.WearPaths

/** Keeps the tile up to date with the phone's workout, even when the watch app is closed. */
class WatchDataListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearPaths.STATE) {
                DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray(WearPaths.STATE_KEY)?.let { WatchStore.saveState(this, it) }
            }
        }
    }
}
