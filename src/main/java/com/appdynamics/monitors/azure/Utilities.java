package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.monitors.azure.config.Globals;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Utilities {
    public static String getFilters(List<Map> filters) throws UnsupportedEncodingException {
        StringBuilder filterUrl = null;
        if (filters != null && !filters.isEmpty()) {
            filterUrl = new StringBuilder("&$" + Globals.azureApiFilter + "=");
            Iterator<Map> iter = filters.iterator();
            while (iter.hasNext()) {
                Map filter = iter.next();
                filterUrl.append(URLEncoder.encode(Globals.filterBy +
                        Globals.filterComOp + "'" +
                        filter.get(Globals.filterBy) +
                        "'", Globals.urlEncoding));
                if ( iter.hasNext()) {
                    filterUrl.append(URLEncoder.encode(Globals.filterLogOp, Globals.urlEncoding));
                }
            }
        }
        return filterUrl != null ? filterUrl.toString() : null;
    }

    static String getClientKey(Map<String, ?> config) {
        String clientKey = (String) config.get(Globals.clientKey);
        if (!Strings.isNullOrEmpty(clientKey)) {
            return clientKey;
        }
        String encryptionKey = (String) config.get(Globals.encryptionKey);
        String encryptedClientKey = (String) config.get(Globals.encryptedClientKey);
        if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedClientKey)) {
            java.util.Map<String, String> cryptoMap = Maps.newHashMap();
            cryptoMap.put(Globals.passwordEncrypted, encryptedClientKey);
            cryptoMap.put(Globals.encryptionKey, encryptionKey);
            return CryptoUtil.getPassword(cryptoMap);
        }
        return null;
    }
}
