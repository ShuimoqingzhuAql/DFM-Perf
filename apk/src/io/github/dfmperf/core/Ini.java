package io.github.dfmperf.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 极简 INI 编辑器：保留原行尾、BOM、其它字段；行为与 DeltaConfig 的 setField/removeFields 对齐。 */
public final class Ini {
    private static final Pattern HEADER = Pattern.compile("^[ \\t]*\\[([^\\]\\r\\n]+)\\][ \\t]*(?:[;#].*)?$");

    private Ini() {}

    public static String get(String text, String section, String key) {
        boolean inSection = false;
        for (Line l : split(text)) {
            Matcher h = HEADER.matcher(l.bare);
            if (h.matches()) {
                inSection = h.group(1).equalsIgnoreCase(section);
                continue;
            }
            if (!inSection) continue;
            int p = l.bare.indexOf('=');
            if (p < 0) continue;
            if (l.bare.substring(0, p).trim().equalsIgnoreCase(key)) {
                String v = l.bare.substring(p + 1).trim();
                int c = v.indexOf(';');
                int c2 = v.indexOf('#');
                if (c >= 0 && (c2 < 0 || c < c2)) v = v.substring(0, c);
                if (c2 >= 0 && (c < 0 || c2 < c)) v = v.substring(0, c2);
                return v.trim();
            }
        }
        return null;
    }

    public static String set(String text, String section, String key, String value) {
        List<Line> lines = split(text);
        String eol = detectEol(text);
        int secStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            Matcher h = HEADER.matcher(lines.get(i).bare);
            if (!h.matches()) continue;
            if (!h.group(1).equalsIgnoreCase(section)) continue;
            if (secStart >= 0) throw new IllegalArgumentException("目标配置节重复: " + section);
            secStart = i;
        }
        if (secStart < 0) {
            StringBuilder sb = new StringBuilder(text);
            if (text.isEmpty() || text.endsWith(eol + eol)) {
                // 已经是空行结尾
            } else if (text.endsWith("\n") || text.endsWith("\r")) {
                sb.append(eol);
            } else {
                sb.append(eol).append(eol);
            }
            sb.append('[').append(section).append(']').append(eol).append(key).append('=').append(value).append(eol);
            return sb.toString();
        }
        int secEnd = lines.size() - 1;
        for (int i = secStart + 1; i < lines.size(); i++) {
            if (HEADER.matcher(lines.get(i).bare).matches()) {
                secEnd = i - 1;
                break;
            }
        }
        for (int i = secStart + 1; i <= secEnd; i++) {
            Line l = lines.get(i);
            int p = l.bare.indexOf('=');
            if (p < 0) continue;
            if (!l.bare.substring(0, p).trim().equalsIgnoreCase(key)) continue;
            String after = l.bare.substring(p + 1);
            String indent = after.replaceAll("^([ \\t]*).*$", "$1");
            l.setValue(key, value, indent);
            return join(lines);
        }
        Line nl = new Line();
        nl.raw = key + "=" + value + eol;
        nl.bare = key + "=" + value;
        lines.add(secEnd + 1, nl);
        return join(lines);
    }

    public static String remove(String text, String section, String... keys) {
        List<Line> lines = split(text);
        boolean inSection = false;
        StringBuilder sb = new StringBuilder(text.length());
        outer:
        for (Line l : lines) {
            Matcher h = HEADER.matcher(l.bare);
            if (h.matches()) {
                inSection = h.group(1).equalsIgnoreCase(section);
                sb.append(l.raw);
                continue;
            }
            if (inSection) {
                int p = l.bare.indexOf('=');
                if (p >= 0) {
                    String k = l.bare.substring(0, p).trim();
                    for (String want : keys) {
                        if (k.equalsIgnoreCase(want)) continue outer;
                    }
                }
            }
            sb.append(l.raw);
        }
        return sb.toString();
    }

    private static String detectEol(String text) {
        if (text.contains("\r\n")) return "\r\n";
        if (!text.contains("\n") && text.contains("\r")) return "\r";
        return "\n";
    }

    private static List<Line> split(String text) {
        List<Line> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int j = i;
            while (j < text.length() && text.charAt(j) != '\r' && text.charAt(j) != '\n') j++;
            if (j < text.length()) {
                if (text.charAt(j) == '\r' && j + 1 < text.length() && text.charAt(j + 1) == '\n') j++;
                j++;
            }
            Line l = new Line();
            l.raw = text.substring(i, j);
            l.bare = l.raw.replaceAll("[\\r\\n]+$", "");
            out.add(l);
            i = j;
        }
        return out;
    }

    private static String join(List<Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (Line l : lines) sb.append(l.raw);
        return sb.toString();
    }

    private static final class Line {
        String raw;
        String bare;
        boolean inSection;

        void setValue(String key, String value, String indent) {
            String eol = raw.endsWith("\r\n") ? "\r\n" : raw.endsWith("\n") ? "\n" : raw.endsWith("\r") ? "\r" : "";
            raw = key + "=" + indent + value + eol;
            bare = raw.substring(0, raw.length() - eol.length());
        }
    }
}
