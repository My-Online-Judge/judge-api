package vn.thanhtuanle.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashUtilTest {

    /**
     * Guards the judge_server token handshake: the backend must send sha256(TOKEN).hexdigest().
     * This is the exact value QingdaoU judge_server computes for TOKEN=123456.
     */
    @Test
    void sha256Hex_matchesJudgeServerExpectation() {
        assertEquals(
                "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
                HashUtil.sha256Hex("123456"));
    }

    @Test
    void sha256Hex_emptyString() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                HashUtil.sha256Hex(""));
    }
}
