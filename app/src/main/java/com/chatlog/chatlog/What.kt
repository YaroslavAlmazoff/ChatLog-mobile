package com.chatlog.chatlog

import com.tylerjroach.eventsource.EventSource
import com.tylerjroach.eventsource.EventSourceHandler
import android.util.Log
import com.tylerjroach.eventsource.MessageEvent

class What {
    private var sseHandler: SSEHandler? = SSEHandler()
    private var eventSource: EventSource? = null
    private fun startEventSource() {
        eventSource = EventSource.Builder("")
            .eventHandler(sseHandler)
            .build()
        eventSource?.connect()
    }

    private fun stopEventSource() {
        if (eventSource != null) eventSource!!.close()
        sseHandler = null
    }

    /**
     * All callbacks are currently returned on executor thread.
     * If you want to update the ui from a callback, make sure to post to main thread
     */
    private inner class SSEHandler : EventSourceHandler {
        override fun onConnect() {
            Log.v("SSE Connected", "True")
        }

        override fun onMessage(event: String, message: MessageEvent) {
            Log.v("SSE Message", event)
            Log.v("SSE Message: ", message.lastEventId)
            Log.v("SSE Message: ", message.data)
        }

        override fun onComment(comment: String) {
            //comments only received if exposeComments turned on
            Log.v("SSE Comment", comment)
        }

        override fun onError(t: Throwable) {
            //ignore ssl NPE on eventSource.close()
        }

        override fun onClosed(willReconnect: Boolean) {
            Log.v("SSE Closed", "reconnect? $willReconnect")
        }
    }
}