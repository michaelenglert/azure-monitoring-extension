package com.appdynamics.monitors.azure.config;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConfigTask {
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
}
