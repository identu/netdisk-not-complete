package com.server.file.util;

import java.util.HashMap;
import java.util.Map;


public class ContentTypeSelector {
    public static final Map<String, String> contentType = new HashMap<String, String>() {{
        // video预览Content-Type
        put("rm", "application/vnd.rn-realmedia");
        put("rmvb", "application/vnd.rn-realmedia-vbr");
        put("mp4", "video/mpeg4");
        // image预览Content-Type
        put("jpg","image/jpeg");
        put("gif","image/gif");
        put("png","image/png");
        // audio预览Content-Type

    }};


}
