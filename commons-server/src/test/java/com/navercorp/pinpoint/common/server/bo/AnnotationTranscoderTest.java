/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.common.server.bo;


import com.navercorp.pinpoint.common.util.BytesStringStringValue;
import com.navercorp.pinpoint.common.util.IntBooleanIntBooleanValue;
import com.navercorp.pinpoint.common.util.IntStringValue;
import com.navercorp.pinpoint.common.util.LongIntIntByteByteStringValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * @author emeroad
 */
public class AnnotationTranscoderTest {

    @Test
    public void testDecode() {
        typeCode("test");
        typeCode("");
        typeCode("adfesdfsesdfsdfserfsdfsdfe");

        typeCode(1);
        typeCode(0);
        typeCode(-1212);

        typeCode((short) 4);
        typeCode((short) -124);

        typeCode(2L);
        typeCode(-22342342L);
        typeCode(268435455L);
        typeCode(268435456L);
        typeCode(34359738367L);
        typeCode(34359738368L);
        typeCode(Long.MAX_VALUE);
        typeCode(Long.MIN_VALUE);

        typeCode(3f);
        typeCode(123.3f);

        typeCode(4D);
        typeCode(-124D);

        typeCode((byte) 4);
        typeCode((byte) -14);

        typeCode(true);
        typeCode(false);

        typeCode(null);

        typeUnsupportCode(new Date());

        typeBinaryCode(new byte[]{12, 3, 4, 1, 23, 4, 1, 2, 3, 4, 4});

    }

    private void typeCode(Object value) {
        AnnotationTranscoder transcoder = new AnnotationTranscoder();

        byte typeCode = transcoder.getTypeCode(value);
        byte[] bytes = transcoder.encode(value, typeCode);
        Object decode = transcoder.decode(typeCode, bytes);

        Assertions.assertEquals(value, decode);
    }

    private void typeUnsupportCode(Object value) {
        AnnotationTranscoder transcoder = new AnnotationTranscoder();

        byte typeCode = transcoder.getTypeCode(value);
        byte[] bytes = transcoder.encode(value, typeCode);
        Object decode = transcoder.decode(typeCode, bytes);

        Assertions.assertEquals(value.toString(), decode.toString());
    }

    private void typeBinaryCode(byte[] value) {
        AnnotationTranscoder transcoder = new AnnotationTranscoder();

        byte typeCode = transcoder.getTypeCode(value);
        byte[] bytes = transcoder.encode(value, typeCode);
        Object decode = transcoder.decode(typeCode, bytes);

        Assertions.assertArrayEquals(value, (byte[]) decode);
    }

    @Test
    public void testIntString() {

        testIntString(-1, "");
        testIntString(0, "");
        testIntString(1, "");
        testIntString(Integer.MAX_VALUE, "test");
        testIntString(Integer.MIN_VALUE, "test");
        testIntString(2, null);
    }

    private void testIntString(int intValue, String stringValue) {
        AnnotationTranscoder transcoder = new AnnotationTranscoder();
        IntStringValue tIntStringValue = new IntStringValue(intValue, stringValue);
        byte[] encode = transcoder.encode(tIntStringValue, AnnotationTranscoder.CODE_INT_STRING);
        IntStringValue decode = (IntStringValue) transcoder.decode(AnnotationTranscoder.CODE_INT_STRING, encode);
        Assertions.assertEquals(tIntStringValue.getIntValue(), decode.getIntValue());
        Assertions.assertEquals(tIntStringValue.getStringValue(), decode.getStringValue());
    }

    @Test
    public void testLongIntIntByteByteString() {
        testLongIntIntByteByteString(999999, 0, 123, (byte) 99, (byte) 1, "app7");
    }

    private void testLongIntIntByteByteString(long longValue, int intValue1, int intValue2, byte byteValue1, byte byteValue2, String stringValue) {
        AnnotationTranscoder transcoder = new AnnotationTranscoder();
        LongIntIntByteByteStringValue value = new LongIntIntByteByteStringValue(longValue, intValue1, intValue2,
                byteValue1, byteValue2, stringValue);

        byte[] encode = transcoder.encode(value, AnnotationTranscoder.CODE_LONG_INT_INT_BYTE_BYTE_STRING);
        LongIntIntByteByteStringValue decode = (LongIntIntByteByteStringValue) transcoder.decode(AnnotationTranscoder.CODE_LONG_INT_INT_BYTE_BYTE_STRING, encode);
        Assertions.assertEquals(value.getLongValue(), decode.getLongValue());
        Assertions.assertEquals(value.getIntValue1(), decode.getIntValue1());
        Assertions.assertEquals(value.getIntValue2(), decode.getIntValue2());
        Assertions.assertEquals(value.getByteValue1(), decode.getByteValue1());
        Assertions.assertEquals(value.getByteValue2(), decode.getByteValue2());
        Assertions.assertEquals(value.getStringValue(), decode.getStringValue());
    }

    @Test
    public void testBytesStringString() {
        testBytesStringString(new byte[]{1, 2, 3, 4, 5}, "string1", "string2");
    }

    private void testBytesStringString(byte[] bytesValue, String stringValue1, String stringValue2) {
        AnnotationTranscoder transcoder = new AnnotationTranscoder();
        BytesStringStringValue value = new BytesStringStringValue(bytesValue, stringValue1, stringValue2);

        byte[] encode = transcoder.encode(value, AnnotationTranscoder.CODE_BYTES_STRING_STRING);
        BytesStringStringValue decode = (BytesStringStringValue) transcoder.decode(AnnotationTranscoder.CODE_BYTES_STRING_STRING, encode);
        Assertions.assertArrayEquals(value.getBytesValue(), decode.getBytesValue());
        Assertions.assertEquals(value.getStringValue1(), decode.getStringValue1());
        Assertions.assertEquals(value.getStringValue2(), decode.getStringValue2());
    }

    @Test
    public void testIntBooleanIntBoolean() {
        AnnotationTranscoder transcoder = new AnnotationTranscoder();
        IntBooleanIntBooleanValue value = new IntBooleanIntBooleanValue(10, false, 5000, true);

        byte[] encode = transcoder.encode(value, AnnotationTranscoder.CODE_INT_BOOLEAN_INT_BOOLEAN);
        IntBooleanIntBooleanValue decode = (IntBooleanIntBooleanValue) transcoder.decode(AnnotationTranscoder.CODE_INT_BOOLEAN_INT_BOOLEAN, encode);
        Assertions.assertEquals(value.getIntValue1(), decode.getIntValue1());
        Assertions.assertFalse(decode.isBooleanValue1());
        Assertions.assertEquals(value.getIntValue2(), decode.getIntValue2());
        Assertions.assertTrue(decode.isBooleanValue2());
    }

}
