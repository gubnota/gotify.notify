package notify.gotify

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


class NotifyListener: WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send("ping")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("NotifyListener", "Received: $text")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("NotifyListener", "Error: ${t.message}")
    }
}