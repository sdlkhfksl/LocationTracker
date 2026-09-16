package com.ljs.locationtracker;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocationHttpServer {
    private static final String TAG = "LocationHttpServer";
    public static final int DEFAULT_PORT = 18765;
    public static final String TOKEN_HEADER = "X-Loca-Token";

    public interface LocationProvider {
        JSONObject buildLocationJson();
    }

    private final int port;
    private final String token;
    private final LocationProvider locationProvider;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public LocationHttpServer(int port, String token, LocationProvider locationProvider) {
        this.port = port;
        this.token = token == null ? "" : token;
        this.locationProvider = locationProvider;
    }

    public int getPort() {
        return port;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(port));
                    Log.i(TAG, "HTTP server listening on " + port);
                    while (running.get()) {
                        try {
                            Socket socket = serverSocket.accept();
                            handleClient(socket);
                        } catch (IOException e) {
                            if (running.get()) {
                                Log.e(TAG, "accept failed", e);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "server bind failed", e);
                    running.set(false);
                }
            }
        }, "loca-http-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }
    }

    private void handleClient(Socket socket) {
        BufferedReader reader = null;
        OutputStream out = null;
        try {
            socket.setSoTimeout(10000);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
            out = socket.getOutputStream();

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.length() == 0) {
                writeResponse(out, 400, "{\"ok\":false,\"error\":\"bad_request\"}");
                return;
            }

            String auth = null;
            String line;
            while ((line = reader.readLine()) != null && line.length() > 0) {
                int idx = line.indexOf(':');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (TOKEN_HEADER.equalsIgnoreCase(key) || "Authorization".equalsIgnoreCase(key)) {
                    auth = value;
                }
            }

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "/";
            int q = path.indexOf('?');
            if (q >= 0) {
                path = path.substring(0, q);
            }

            if (!token.isEmpty()) {
                String got = auth == null ? "" : auth;
                if (got.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    got = got.substring(7).trim();
                }
                if (!token.equals(got)) {
                    writeResponse(out, 401, "{\"ok\":false,\"error\":\"unauthorized\"}");
                    return;
                }
            }

            if ("GET".equalsIgnoreCase(method) && ("/location".equals(path) || "/".equals(path))) {
                JSONObject json = locationProvider.buildLocationJson();
                if (json == null) {
                    writeResponse(out, 503, "{\"ok\":false,\"error\":\"no_location\"}");
                } else {
                    writeResponse(out, 200, json.toString());
                }
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/health".equals(path)) {
                writeResponse(out, 200, "{\"ok\":true}");
                return;
            }

            writeResponse(out, 404, "{\"ok\":false,\"error\":\"not_found\"}");
        } catch (Exception e) {
            Log.e(TAG, "handle client failed", e);
            try {
                if (out != null) {
                    writeResponse(out, 500, "{\"ok\":false,\"error\":\"server_error\"}");
                }
            } catch (IOException ignored) {
            }
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void writeResponse(OutputStream out, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        String status;
        if (code == 200) {
            status = "200 OK";
        } else if (code == 401) {
            status = "401 Unauthorized";
        } else if (code == 404) {
            status = "404 Not Found";
        } else if (code == 503) {
            status = "503 Service Unavailable";
        } else if (code == 400) {
            status = "400 Bad Request";
        } else {
            status = code + " Error";
        }
        String headers = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(Charset.forName("UTF-8")));
        out.write(bytes);
        out.flush();
    }
}
