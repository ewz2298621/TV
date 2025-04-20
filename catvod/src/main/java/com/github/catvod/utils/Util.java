package com.github.catvod.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Base64;

import com.github.catvod.Init;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttp;
import okhttp3.Request;

public class Util {

    public static final String OKHTTP = "okhttp/" + OkHttp.VERSION;
    public static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    public static final int URL_SAFE = Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP;

    public static String base64(String s) {
        return base64(s.getBytes());
    }

    public static String base64(byte[] bytes) {
        return base64(bytes, Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static String base64(String s, int flags) {
        return base64(s.getBytes(), flags);
    }

    public static String base64(byte[] bytes, int flags) {
        return Base64.encodeToString(bytes, flags);
    }

    public static byte[] decode(String s) {
        return decode(s, Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static byte[] decode(String s, int flags) {
        return Base64.decode(s, flags);
    }

    public static String basic(String userInfo) {
        return "Basic " + base64(userInfo, Base64.NO_WRAP);
    }

    public static byte[] hex2byte(String s) {
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = Integer.valueOf(s.substring(i * 2, i * 2 + 2), 16).byteValue();
        return bytes;
    }

    public static boolean equals(String name, String md5) {
        return md5(Path.jar(name)).equalsIgnoreCase(md5);
    }

    public static String md5(String src) {
        try {
            if (TextUtils.isEmpty(src)) return "";
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(src.getBytes());
            BigInteger no = new BigInteger(1, bytes);
            StringBuilder sb = new StringBuilder(no.toString(16));
            while (sb.length() < 32) sb.insert(0, "0");
            return sb.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String md5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[4096];
            int count;
            while ((count = fis.read(bytes)) != -1) digest.update(bytes, 0, count);
            fis.close();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean containOrMatch(String text, String regex) {
        try {
            return text.contains(regex) || text.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }

    public static String substring(String text) {
        return substring(text, 1);
    }

    public static String substring(String text, int num) {
        if (text != null && text.length() > num) return text.substring(0, text.length() - num);
        return text;
    }

    public static String getIp() {
        try {
            WifiManager manager = (WifiManager) Init.context().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int address = manager.getConnectionInfo().getIpAddress();
            if (address != 0) return Formatter.formatIpAddress(address);
            return getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

    private static String getHostAddress() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface nif = en.nextElement();
            String name = nif.getName();
            if (name.startsWith("veth") || name.startsWith("tun") || name.startsWith("tap") || nif.getHardwareAddress() == null) continue;
            for (Enumeration<InetAddress> addresses = nif.getInetAddresses(); addresses.hasMoreElements(); ) {
                InetAddress inetAddress = addresses.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        }
        return "";
    }

    public static String digest(String userInfo, String header, Request request) {
        Map<String, String> params = parse(header.substring(7));
        String[] parts = userInfo.split(":", 2);
        String nc = "00000001";
        String username = parts[0];
        String password = parts[1];
        String qop = params.get("qop");
        String realm = params.get("realm");
        String nonce = params.get("nonce");
        String opaque = params.get("opaque");
        String uri = request.url().encodedPath();
        String hash1 = Util.md5(username + ":" + realm + ":" + password);
        String hash2 = Util.md5(request.method() + ":" + uri);
        String cnonce = Long.toHexString(System.currentTimeMillis());
        String response = Util.md5(hash1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + (qop != null ? qop : "") + ":" + hash2);
        StringBuilder auth = new StringBuilder("Digest ");
        auth.append("username=\"").append(username).append("\", ");
        if (realm != null) auth.append("realm=\"").append(realm).append("\", ");
        if (nonce != null) auth.append("nonce=\"").append(nonce).append("\", ");
        auth.append("uri=\"").append(uri).append("\", ");
        auth.append("cnonce=\"").append(cnonce).append("\", ");
        auth.append("nc=").append(nc).append(", ");
        if (qop != null) auth.append("qop=\"").append(qop).append("\", ");
        auth.append("response=\"").append(response).append("\"");
        if (opaque != null) auth.append(", opaque=\"").append(opaque).append("\"");
        return auth.toString();
    }

    private static Map<String, String> parse(String header) {
        Map<String, String> params = new HashMap<>();
        for (String part : header.split(",\\s*")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) params.put(kv[0].trim(), kv[1].trim().replace("\"", ""));
        }
        return params;
    }
}
