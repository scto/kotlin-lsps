package org.kotlinlsp.index.worker

import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WorkQueue<T> {
    private val editQueue: Stack<T> = Stack()
    private val indexQueue: Queue<T> = LinkedList()
    private val mutex = ReentrantLock()
    private val notEmpty = mutex.newCondition()
    private val notFullEdit = mutex.newCondition()
    private val notFullIndex = mutex.newCondition()

    companion object {
        const val MAX_EDIT_SIZE = 20
        const val MAX_INDEX_SIZE = 100
    }

    fun putEditQueue(item: T) {
        mutex.withLock {
            while (editQueue.size >= MAX_EDIT_SIZE) {
                notFullEdit.await()
            }
            editQueue.push(item)
            notEmpty.signal()
        }
    }

    fun putIndexQueue(item: T) {
        mutex.withLock {
            while (indexQueue.size >= MAX_INDEX_SIZE) {
                notFullIndex.await()
            }
            indexQueue.offer(item)
            notEmpty.signal()
        }
    }

    fun take(): T {
        mutex.withLock {
            while (editQueue.isEmpty() && indexQueue.isEmpty()) {
                notEmpty.await()
            }

            val item = if (editQueue.isNotEmpty()) {
                val item = editQueue.pop()
                if (editQueue.size == MAX_EDIT_SIZE - 1) {
                    notFullEdit.signal()
                }
                item
            } else {
                val item = indexQueue.poll()
                if (indexQueue.size == MAX_INDEX_SIZE - 1) {
                    notFullIndex.signal()
                }
                item
            }

            return item
        }
    }
}
