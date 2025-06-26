package uk.ac.cam.cl.dtg.segue.auth.microsoft;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testcontainers.shaded.org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;

public class KeySetServlet extends HttpServlet {
    private final List<KeyPair> keys;
    private int requestCount = 0;

    public static Pair<Server, KeySetServlet> startServer(int port, List<KeyPair> keys) throws Exception {
        var handler = new ServletHandler();
        var servlet = new KeySetServlet(keys);
        handler.addServletWithMapping(new ServletHolder(servlet), "/keys");
        var server = new Server(port);
        server.setHandler(handler);
        server.start();
        return Pair.of(server, servlet);
    }

    private KeySetServlet(List<KeyPair> keys) {
        this.keys = keys;
    }

    public int getRequestCount() {
        return this.requestCount;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(keyStore());
        requestCount++;
    }

    private JSONObject keyStore() {
        return new JSONObject().put(
                "keys", keys.stream().reduce(
                        new JSONArray(),
                        (acc, key) -> acc.put(
                                new JSONObject().put("kty", "RSA")
                                        .put("use", "sig")
                                        .put("kid", key.getPublic().id())
                                        .put("n", key.getPublic().modulus())
                                        .put("e", key.getPublic().exponent())
                                        .put("cloud_instance_name", "microsoftonline.com")
                                // Microsoft's response also contains an X.509 certificate, which we don't test
                                // here. Sample at: https://login.microsoftonline.com/common/discovery/keys
                                // .put("x5t", key_id)
                                // .put("x5c", new JSONArray("some_string"))
                        ),
                        JSONArray::putAll));
    }
}