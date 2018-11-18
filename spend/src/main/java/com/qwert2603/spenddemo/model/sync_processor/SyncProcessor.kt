package com.qwert2603.spenddemo.model.sync_processor

import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.spenddemo.env.E
import com.qwert2603.spenddemo.model.entity.RecordChange
import com.qwert2603.spenddemo.utils.Const
import com.qwert2603.spenddemo.utils.executeAndWait
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class SyncProcessor<T : IdentifiableString, L : LocalItem>(
        private val remoteDBExecutor: ExecutorService,
        private val localDBExecutor: ExecutorService,
        private val lastUpdateStorage: LastUpdateStorage,
        private val remoteDataSource: RemoteDataSource<T>,
        private val localDataSource: LocalDataSource<L, T>,
        private val changeIdCounter: IdCounter,
        private val l2t: L.() -> T,
        private val t2l: T.(change: RecordChange?) -> L
) {

    companion object {
        private const val TAG = "SyncProcessor"
    }

    private val pendingClearAll = AtomicBoolean(false)

    fun start() {
        if (!E.env.syncWithServer) return

        Executors.newSingleThreadExecutor().execute {
            while (true) {
                try {
//                    Thread.sleep(926)//todo

                    if (pendingClearAll.compareAndSet(true, false)) {
                        localDBExecutor.executeAndWait {
                            localDataSource.deleteAll()
                            lastUpdateStorage.lastUpdateInfo = null
                        }
                    }

                    while (true) {
                        val locallyChangedItems = localDBExecutor.executeAndWait {
                            localDataSource.getLocallyChangedItems(50)
                        }
                        if (locallyChangedItems.isEmpty()) break

                        val (updated, deleted) = locallyChangedItems.partition { it.change!!.changeKindId == Const.CHANGE_KIND_UPSERT }
                        val deletedUuids = deleted.map { it.uuid }
                        remoteDBExecutor.executeAndWait {
                            remoteDataSource.saveChanges(
                                    updated = updated.map(l2t),
                                    deletedUuids = deletedUuids
                            )
                        }
                        localDBExecutor.executeAndWait {
                            localDataSource.onChangesSentToServer(
                                    editedRecords = updated.map { ItemsIds(it.uuid, it.change!!.id) },
                                    deletedUuids = deletedUuids
                            )
                        }
                    }

                    while (true) {
                        val updatesFromRemote = remoteDBExecutor.executeAndWait {
                            remoteDataSource.getUpdates(lastUpdateStorage.lastUpdateInfo, 50)
                        }
                        if (updatesFromRemote.isEmpty()) break
                        localDBExecutor.executeAndWait {
                            localDataSource.saveChangesFromRemote(updatesFromRemote)
                            lastUpdateStorage.lastUpdateInfo = updatesFromRemote.lastUpdateInfo
                        }
                    }
                } catch (t: Throwable) {
                    LogUtils.e(TAG, "remoteDBExecutor.execute", t)
                    Thread.sleep(10000)
                }
            }
        }
    }

    fun saveItems(ts: List<T>) {
        localDBExecutor.execute {
            localDataSource.saveItems(ts.map { it.t2l(RecordChange(changeIdCounter.getNext(), Const.CHANGE_KIND_UPSERT)) })
        }
    }

    fun removeItems(itemsUuids: List<String>) {
        localDBExecutor.execute {
            if (E.env.syncWithServer) {
                localDataSource.locallyDeleteItems(itemsUuids.map { ItemsIds(it, changeIdCounter.getNext()) })
            } else {
                localDataSource.deleteItems(itemsUuids)
            }
        }
    }

    fun clear() {
        if (E.env.syncWithServer) {
            pendingClearAll.set(true)
        } else {
            localDBExecutor.execute {
                localDataSource.deleteAll()
            }
        }
    }
}