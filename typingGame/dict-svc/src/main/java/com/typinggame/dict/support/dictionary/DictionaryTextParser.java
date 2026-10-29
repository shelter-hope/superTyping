package com.typinggame.dict.support.dictionary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class DictionaryTextParser {
    public static final int MAX_BYTES = 1024 * 1024;
    private static final Pattern INVALID_CHAR = Pattern.compile("[\\x00-\\x1F\\x7F]");
    private static final int MAX_WORD_LENGTH = 100;

    private DictionaryTextParser() {
    }

    public static String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("只接受 txt 或 csv 文件");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals("txt") && !extension.equals("csv")) {
            throw new IllegalArgumentException("只接受 txt 或 csv 文件");
        }
        return extension;
    }

    public static List<String> parse(String filename, String text) {
        if (text == null) {
            throw new IllegalArgumentException("词库内容为空");
        }
        if (text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("词库文件不能超过 1MB");
        }
        boolean csv = "csv".equals(extension(filename));
        Set<String> words = new LinkedHashSet<>();
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (csv) {
                String[] cells = line.split(",");
                if (i == 0 && cells.length > 0 && "word".equalsIgnoreCase(cells[0].trim())) continue;
                add(words, cells.length == 0 ? "" : cells[0]);
            } else {
                for (String part : line.split("[\\s\\p{Z}\\p{C}]+")) {
                    add(words, part);
                }
            }
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("文件里没有可用单词");
        }
        return new ArrayList<>(words);
    }

    private static void add(Set<String> words, String raw) {
        String word = INVALID_CHAR.matcher(raw.trim()).replaceAll("").trim();
        if (word.isEmpty() || word.length() > MAX_WORD_LENGTH) return;
        words.add(word);
    }
}
