package com.qs.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 中文名称 → 拼音首字母简码（大写），供项目分析检索。
 */
public final class PinyinCodeUtil {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private PinyinCodeUtil() {
    }

    public static String toJianpin(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : text.trim().toCharArray()) {
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                sb.append(Character.toUpperCase(ch));
                continue;
            }
            if (isChinese(ch)) {
                try {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
                    if (arr != null && arr.length > 0 && !arr[0].isEmpty()) {
                        sb.append(arr[0].charAt(0));
                    }
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                    // skip unmappable
                }
            }
        }
        String code = sb.toString();
        return code.length() > 100 ? code.substring(0, 100) : code;
    }

    private static boolean isChinese(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }
}
