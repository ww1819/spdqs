package com.qs.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID7 生成（RFC 9562），用于新建业务表主键。
 */
public final class IdUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdUtils() {
    }

    public static String dashedUuid7() {
        long timestampMs = System.currentTimeMillis();
        byte[] rand = new byte[10];
        RANDOM.nextBytes(rand);

        long ms = timestampMs & 0xFFFFFFFFFFFFL;
        long hi = (ms << 16) | ((rand[0] & 0xFFL) << 8) | (rand[1] & 0xFFL);
        long mid = ((rand[2] & 0xFFL) << 48)
                | ((rand[3] & 0xFFL) << 40)
                | ((rand[4] & 0xFFL) << 32)
                | ((rand[5] & 0xFFL) << 24)
                | (0x7L << 12)
                | (rand[6] & 0x0FL);
        long lo = (0x2L << 62)
                | ((rand[7] & 0x3FL) << 56)
                | ((rand[8] & 0xFFL) << 48)
                | ((rand[9] & 0xFFL) << 40);

        return new UUID(hi, mid | lo).toString();
    }

    public static String simpleUuid7() {
        return dashedUuid7().replace("-", "");
    }
}
