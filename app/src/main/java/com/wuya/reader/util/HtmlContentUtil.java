package com.wuya.reader.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/10/20.
 */

public class HtmlContentUtil {
    public static Map<String, String> getHtmlContent(String url, String orientContent) {
        //笔趣岛
        if (url.contains("www.biqudao.com")) {
            return getBiqudaoContent(url,orientContent);
        }
        else if (url.contains("m.biqudao.com")) {
            return getMBiqudaoContent(url,orientContent);
        }
        //零点看书
        else if (url.contains("m.lingdiankanshu.co")) {
            return getLingdiankanshuContent(url,orientContent);
        }
        //17k
        else if (url.contains("www.17k.com")) {
            return get17KContent(url,orientContent);
        }
        else if (url.contains("h5.17k.com")) {
            return getH517KContent(url,orientContent);
        }
        //红袖添香
        else if (url.contains("www.hongxiu.com")) {
            return getHongxiuContent(url,orientContent);
        }
        //全书网
        else if (url.contains("www.quanshuwang.com")) {
            return getQuanshuContent(url,orientContent);
        }
        else if (url.contains("m.quanshuwang.com")) {
            return getMQuanshuContent(url,orientContent);
        }
        //书书吧，主要是一些经典小说都有
        else if (url.contains("shushu8.com")) {
            return getShushu8Content(url,orientContent);
        }
        //99藏书
        else if (url.contains("www.99lib.net")) {
            return get99libContent(url,orientContent);
        }
        //落霞小说
        else if (url.contains("www.luoxia.com")) {
            return getLuoxiaContent(url,orientContent);
        }
        //缺省为笔趣岛
        else {
            return getBiqudaoContent(url,orientContent);
        }


    }

    /**
     * 笔趣岛
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getBiqudaoContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";
        if (orientContent.contains("<div id=\"content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"content\">") + 18);
        }
        if (orientContent.contains("var nextpage=\"")) {
            String temp = orientContent.substring(orientContent.indexOf("var nextpage=\"")+14);
            if (temp.substring(0,temp.indexOf("\"")).equals("./")) {
                nextUrl = url.substring(0,url.lastIndexOf("/"));
            }
            else {
                nextUrl = url.substring(0, url.lastIndexOf("/") + 1) + temp.substring(0, temp.indexOf("\""));
            }
        }
        if (orientContent.contains("<script>chaptererror();</script>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<script>chaptererror();</script>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 笔趣岛
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getMBiqudaoContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";
        if (orientContent.contains("<div id=\"chaptercontent\" class=\"Readarea ReadAjax_content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"chaptercontent\" class=\"Readarea ReadAjax_content\">") + 59);
        }
        if (orientContent.contains("\" id=\"pb_next\"") && orientContent.contains("目录</a>")) {
            String temp = orientContent.substring(orientContent.indexOf("目录</a>")+6);
            if(temp.contains("<a href=\"")) {
                temp = temp.substring(temp.indexOf("<a href=\"") + 9, temp.indexOf("\" id=\"pb_next\""));
                temp = temp.substring(temp.lastIndexOf("/")+1);
                if (temp.equals("./")) {
                    nextUrl = url.substring(0,url.lastIndexOf("/"));
                }
                else {
                    nextUrl = url.substring(0,url.lastIndexOf("/")+1)+temp;
                }
            }
        }
        if (orientContent.contains("/div>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("/div>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 零点看书
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getLingdiankanshuContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"chaptercontent\" class=\"Readarea ReadAjax_content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"chaptercontent\" class=\"Readarea ReadAjax_content\">") + 59);
        }
        if (orientContent.contains("id=\"pb_next\"") && orientContent.contains("目录</a>")) {
            String temp = orientContent.substring(orientContent.indexOf("目录</a>")+6);
            if(temp.contains("<a href=\"")) {
                temp = temp.substring(temp.indexOf("<a href=\"") + 9, temp.indexOf("\" id=\"pb_next\""));
                if (temp.equals("./")) {
                    nextUrl = url.substring(0,url.lastIndexOf("/"));
                }
                else {
                    nextUrl = url.substring(0,url.lastIndexOf("/")+1)+temp;
                }
            }
        }
        if (orientContent.contains("<script>app2()</script>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<script>app2()</script>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 17k内容，不支持自动翻页
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> get17KContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div class=\"readAreaBox content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div class=\"readAreaBox content\">") + 33);
        }
        if (orientContent.contains("<div class=\"author-say\"></div>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<div class=\"author-say\"></div>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }
    /**
     * 17k内容，不支持自动翻页
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getH517KContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"TextContent\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"TextContent\">") + 22);
        }
        if (orientContent.contains("<section class=\"ReadAD\" id=\"ad_gd2\"></section>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<section class=\"ReadAD\" id=\"ad_gd2\"></section>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 红袖添香
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getHongxiuContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div class=\"read-content j_readContent\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div class=\"read-content j_readContent\">") + 40);
        }
        if (orientContent.contains("<a id=\"j_chapterNext\" href=\"//")) {
            String temp = orientContent.substring(orientContent.indexOf("<a id=\"j_chapterNext\" href=\"//")+30);
            if(temp.contains("\">")) {
                nextUrl = "https://" + temp.substring(0, temp.indexOf("\">"));
            }
        }
        if (orientContent.contains("</div>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("</div>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 全书网
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getQuanshuContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div class=\"mainContenr\"   id=\"content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div class=\"mainContenr\"   id=\"content\">") + 40);
        }
        if (orientContent.contains("返回目录</a><a href=\"")) {
            String temp = orientContent.substring(orientContent.indexOf("返回目录</a><a href=\"")+17);
            if(temp.contains("\" class=\"next\">")) {
                nextUrl = temp.substring(0, temp.indexOf("\" class=\"next\">"));
            }
        }
        if (orientContent.contains("<script type=\"text/javascript\">style6();</script>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<script type=\"text/javascript\">style6();</script>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 全书网
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getMQuanshuContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"htmlContent\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"htmlContent\">") + 22);
        }
        if (orientContent.contains("var hou = \"")) {
            String temp = orientContent.substring(orientContent.indexOf("var hou = \"")+11);
            if(temp.contains("\";")) {
                nextUrl = url.substring(0,url.lastIndexOf("/")+1)+temp.substring(0, temp.indexOf("\";"));
            }
        }
        if (orientContent.contains("<script type=\"text/javascript\">style2();</script>")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<script type=\"text/javascript\">style2();</script>"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 书书吧
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getShushu8Content(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"content\">") + 19);
        }
        if (orientContent.contains("目录</a></td>")) {
            String temp = orientContent.substring(orientContent.indexOf("目录</a></td>")+11);
            if (temp.contains("<td><a href='")) {
                temp = temp.substring(temp.indexOf("<td><a href='")+13);
            }
            if(temp.contains("' title='")) {
                nextUrl = "http://www.shushu8.com"+temp.substring(0, temp.indexOf("' title='"));
            }
        }
        if (orientContent.contains("<div class=\"nr_page\">")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<div class=\"nr_page\">"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 书书吧
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getMShushu8Content(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"content\">") + 19);
        }
        if (orientContent.contains("目录</a></td>")) {
            String temp = orientContent.substring(orientContent.indexOf("目录</a></td>")+11);
            if (temp.contains("<td><a href='")) {
                temp = temp.substring(temp.indexOf("<td><a href='")+13);
            }
            if(temp.contains("' title='")) {
                nextUrl = "http://www.shushu8.com"+temp.substring(0, temp.indexOf("' title='"));
            }
        }
        if (orientContent.contains("<div class=\"nr_page\">")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<div class=\"nr_page\">"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 99lib
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> get99libContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"content\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"content\">") + 18);
        }
        if (orientContent.contains("目录</a><a href=\"")) {
            String temp = orientContent.substring(orientContent.indexOf("目录</a><a href=\"")+15);
            if(temp.contains("\" id=\"next\"")) {
                nextUrl = "http://www.99lib.net"+temp.substring(0, temp.indexOf("\" id=\"next\""));
            }
        }
        if (orientContent.contains("<div class=\"page\">")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<div class=\"page\">"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }


    /**
     * 落霞小说
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getLuoxiaContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"nr1\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"nr1\">") + 14);
        }
        if (orientContent.contains("<li class=\"next\">下一章：<a href=\"")) {
            String temp = orientContent.substring(orientContent.indexOf("<li class=\"next\">下一章：<a href=\"")+30);
            if(temp.contains("\" rel=\"next\">")) {
                nextUrl = temp.substring(0, temp.indexOf("\" rel=\"next\">"));
            }
        }
        if (orientContent.contains("<div id=\"anchor\" class=\"ggad clearfix\">")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<div id=\"anchor\" class=\"ggad clearfix\">"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * sogo小说
     * @param url
     * @param orientContent
     * @return
     */
    private static Map<String, String> getSogoContent(String url, String orientContent) {
        Map<String, String> result = new HashMap<String, String>(2);
        String nextUrl = "";

        if (orientContent.contains("<div id=\"nr1\">")) {
            orientContent = orientContent.substring(orientContent.indexOf("<div id=\"nr1\">") + 14);
        }
        if (orientContent.contains("<li class=\"next\">下一章：<a href=\"")) {
            String temp = orientContent.substring(orientContent.indexOf("<li class=\"next\">下一章：<a href=\"")+30);
            if(temp.contains("\" rel=\"next\">")) {
                nextUrl = temp.substring(0, temp.indexOf("\" rel=\"next\">"));
            }
        }
        if (orientContent.contains("<div id=\"anchor\" class=\"ggad clearfix\">")) {
            orientContent = orientContent.substring(0, orientContent.indexOf("<div id=\"anchor\" class=\"ggad clearfix\">"));
        }

        result.put("nextUrl", nextUrl);
        result.put("orientContent", getGeneralContent(orientContent));
        return result;
    }

    /**
     * 通用处理
     * @param orientContent
     * @return
     */
    private static String getGeneralContent(String orientContent) {
        if (orientContent.contains("<body")) {
            orientContent = orientContent.substring(orientContent.indexOf("<body")+5);
        }
        orientContent = orientContent.replaceAll("[\t|\r|\n|\\s*]","\n");
        orientContent = orientContent.replaceAll("<p>","        ");
        orientContent = orientContent.replaceAll("[a-zA-Z0-9]|[|\\[\\]\\^._<>=\"-/:_';@{}\\\\]","");
        orientContent = orientContent.replaceAll("\n\n","\n        ");

        return orientContent;
    }
}
