package `is`.xyz.mpv

import android.net.*
import android.util.Log
import java.io.FileDescriptor

import `is`.xyz.mpv.MPVLib
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.*

import android.system.ErrnoException
import java.io.IOException

private val TAG = "mpv"

/* Starts listening for clients at the Unix domain socket abstract address and returns the server's file descriptor, or
   null if it failed to start. */
fun commandServerListenAt(address: String): FileDescriptor? {
    Log.v(TAG, "Command server at '$address' starting")
    val server = try {
        LocalServerSocket(address)
    } catch (e: IOException) {
        Log.e(TAG, "Command server at '$address' failed to start", e)
        return null
    }
    val fd = server.getFileDescriptor()

    // Loop in a coroutine accepting clients until the file descriptor gets shut down
    GlobalScope.launch(Dispatchers.IO) {
        var nClients = 0
        while (true) {
            Log.v(TAG, "Command server at '$address' waiting to accept new client")
            val client = try {
                server.accept()
            } catch (e: IOException) {
                val cause = e.cause
                if (cause is ErrnoException && cause.errno == OsConstants.EINVAL)  // file descriptor was shut down
                    break
                Log.e(TAG, "Command server at '$address' accept failed", e)
                continue
            }
            val iClient = ++nClients
            Log.v(TAG, "Command server at '$address' client $iClient accepted")

            val fromClient = try {
                client.setSoTimeout(10000)
                client.getInputStream().bufferedReader()
            } catch (e: IOException) {
                Log.v(TAG, "Command server at '$address' client $iClient closing after setup failure", e)
                try { client.close() } catch (e: Exception) {}
                continue
            }

            // Loop running commands from this client in a coroutine
            launch {
                while (isActive) {
                    val cmd = try {
                        fromClient.readLine()
                    } catch (e: IOException) {
                        if (e.message == "Try again")  // timeout
                            continue
                        Log.e(TAG, "Command server at '$address' client $iClient read failed", e)
                        null
                    }
                    if (cmd == null)
                        break

                    Log.v(TAG, "Command server at '$address' client $iClient running command: $cmd")
                    MPVLib.commandString(cmd)
                }

                Log.v(TAG, "Command server at '$address' client $iClient closing")
                try {
                    client.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Command server at '$address' client $iClient closing failed", e)
                }
            }
        }

        Log.v(TAG, "Command server at '$address' closing")
        try {
            server.close()
        } catch (e: Exception) {
            Log.e(TAG, "Command server at '$address' closing failed", e)
        }
        coroutineContext.cancelChildren()
    }

    return fd
}

/* Stops the server listening on fd as well as any of its open client connections. */
fun commandServerCloseAndClients(fd: FileDescriptor) {
    Log.v(TAG, "Command server with file descriptor $fd shutting down")
    Os.shutdown(fd, OsConstants.SHUT_RDWR)
}
