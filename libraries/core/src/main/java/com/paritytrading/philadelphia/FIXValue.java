/*
 * Copyright 2015 Philadelphia authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.paritytrading.philadelphia;

import static com.paritytrading.philadelphia.FIX.*;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;

/**
 * A value container.
 */
public class FIXValue {

    private final ByteBuffer bytes;

    /**
     * Construct a new value container.
     *
     * @param capacity the capacity
     */
    public FIXValue(int capacity) {
        System.out.println("FIXValue capacity = " + capacity);
        bytes = ByteBuffer.allocateDirect(capacity);

        bytes.put(0, SOH);

        bytes.mark();
    }

    /**
     * Get the byte at the specified index. The index must be between 0 and
     * the length of the value - 1.
     *
     * @param index the index
     * @return the byte at the specified index
     * @throws IndexOutOfBoundsException if the index is outside of this
     *   value container
     */
    public byte byteAt(int index) {
        return bytes.get(index);
    }

    /**
     * Get the length of the value.
     *
     * @return the length of the value
     */
    public int length() {
        return bytes.limit() - 1;
    }

    /**
     * Copy the value to a byte array.
     *
     * @param bytes a byte array
     * @throws IndexOutOfBoundsException if the length of the value is greater
     *     than the length of the byte array
     */
    public void copyTo(byte[] bytes) {

        System.out.println("copyTo: " + this.bytes.toString());
       // System.out.println("copyTo: " + this.bytes.get());
       // System.out.println("copyTo: " + this.bytes.get());
       // System.out.println("copyTo: " + this.bytes.get());
        this.bytes.reset();
        System.out.println("copyTo: " + this.bytes.toString());
        this.bytes.get(bytes, 0, this.bytes.remaining() - 1);
        System.out.println("array " + (char)bytes[0]);
        System.out.println("array " + (char)bytes[1]);
        System.out.println("array " + (char)bytes[2]);
    }

    /**
     * Reset the value.
     */
    public void reset() {
        bytes.clear();
    }

    /**
     * Set the value.
     *
     * @param value a value container
     */
    public void set(FIXValue value) {
        System.out.println("set other " + value.bytes.toString());
        System.out.println("set " + bytes.toString());
        
        bytes.limit(value.bytes.limit());
        bytes.position(value.bytes.reset().position()).mark();
        bytes.put(value.bytes);
        System.out.println("set other " + value.bytes.toString());
    }

    /**
     * Get the value as a boolean.
     *
     * @return the value as a boolean
     * @throws FIXValueFormatException if the value is not a boolean
     */
    public boolean asBoolean() {
        if (bytes.limit() != 2)
            notBoolean();

        return bytes.get(0) == YES;
    }

    /**
     * Set the value to a boolean.
     *
     * @param b a boolean
     */
    public void setBoolean(boolean b) {
        bytes.put(b ? YES : NO);
        bytes.put(SOH);
        
        bytes.flip();
        bytes.mark();
    }

    /**
     * Get the value as a character.
     *
     * @return the value as a character
     * @throws FIXValueFormatException if the value is not a character
     */
    public char asChar() {
        if (bytes.limit() != 2)
            notChar();

        return (char)bytes.get();
    }

    /**
     * Set the value to a character.
     *
     * @param c a character
     */
    public void setChar(char c) {
        bytes.put((byte)c);
        bytes.put(SOH);

        bytes.flip();
        bytes.mark();
    }

    /**
     * Get the value as an integer.
     *
     * @return the value as an integer
     * @throws FIXValueFormatException if the value is not an integer
     */
    public long asInt() {
        System.out.println("asInt " + bytes.toString());
        boolean negative = false;

        bytes.reset();

        if (bytes.get() == '-') {
            negative = true;
        } 
        else {
            bytes.reset();
        } 

        long value = 0;

        while (bytes.remaining() - 1 > 0) {
            byte b = bytes.get();
            System.out.println("remaining = " + bytes.remaining());

            if (b < '0' || b > '9') {
                System.out.println("b = " + b);
                notInt();
            }

            value = 10 * value + b - '0';
        }

        return negative ? -value : +value;
    }

    /**
     * Set the value to an integer.
     *
     * @param i an integer
     */
    public void setInt(long i) {
        System.out.println("setInt " + bytes.toString());
        bytes.put(bytes.limit() - 1, SOH);

        long j = Math.abs(i); //TODO: maybe another to do this faster

        int k = bytes.limit() - 2;

        do {
            bytes.put(k--, (byte)('0' + j % 10));

            j /= 10;
        } while (j > 0);

        if (i < 0)
            bytes.put(k--, (byte)('-'));

        System.out.println("position = " + k);
        bytes.position(k + 1).mark();
    }

    /**
     * Get the value as a float.
     *
     * @return the value as a float
     * @throws FIXValueFormatException if the value is not a float
     */
    public double asFloat() {
        long   value  = 0;
        double factor = 0.0;

        bytes.reset();

        long sign  = bytes.get() == '-' ? -1 : +1;
        int  start = sign < 0 ?  1 : 0;
        if (sign > 0) 
            bytes.reset();

        for (int i = start; i < bytes.limit() - 1; i++) {
            byte b = bytes.get();
            System.out.println("asFloat b = " + (char)b);
            if (b < '0' || b > '9') {
                if (factor == 0.0 && b == '.') {
                    factor = 1.0;
                    continue;
                }

                notFloat();
            }

            value   = 10 * value + b - '0';
            factor *= 10;
        }

        return sign * (factor > 0.0 ? value / factor : value);
    }

    /**
     * Set the value to a float.
     *
     * @param f a float
     * @param decimals the number of decimals
     */
    public void setFloat(double f, int decimals) {
        long i = Math.round(Longs.POWERS_OF_TEN[decimals] * Math.abs(f));

        bytes.put(bytes.limit() - 1, SOH);

        int j = bytes.limit() - 2;

        for (int k = 0; k < decimals; k++) {
            bytes.put(j--, (byte)('0' + i % 10));

            i /= 10;
        }

        if (decimals > 0)
            bytes.put(j--, (byte)'.');

        do {
            bytes.put(j--, (byte)('0' + i % 10));

            i /= 10;
        } while (i > 0);

        if (f < 0)
            bytes.put(j--, (byte)'-');
        System.out.println("setFloat");
        bytes.position(j + 1).mark();
    }

    /**
     * Get the value as a string.
     *
     * @return the value as a string
     */
    public String asString() {
        System.out.println("asSting " + bytes.toString());
        bytes.reset();
        bytes.limit(bytes.limit() - 1);
        return StandardCharsets.US_ASCII.decode(bytes.slice()).toString();
    }

    /**
     * Get the value as a string. The value is appended to the provided
     * string builder.
     *
     * @param s a string builder
     */
    public void asString(StringBuilder s) {
        bytes.reset();
        for (int i = 0; i < bytes.limit() - 1; i++)
            s.append((char)bytes.get());
    }

    /**
     * Set the value to a string.
     *
     * @param s a string
     * @throws IndexOutOfBoundsException if the string is too long
     */
    public void setString(CharSequence s) {
        System.out.println("setString:" + s.toString() + " bytes:" + bytes.toString());
        
        for (int i = 0; i < s.length(); i++)
            bytes.put((byte)s.charAt(i));

        bytes.put(SOH);
        bytes.flip();
        bytes.mark();
        System.out.println("setString:" + s.toString() + " bytes:" + bytes.toString());

    }

    /**
     * Get the value as a date.
     *
     * @param d a date
     * @throws FIXValueFormatException if the value is not a date
     */
    public void asDate(MutableDateTime d) {
        if (bytes.limit() != 8)
            notDate();

        int year        = getDigits(4);
        int monthOfYear = getDigits(2);
        int dayOfMonth  = getDigits(2);

        d.setDateTime(year, monthOfYear, dayOfMonth, 0, 0, 0, 0);
    }

    /**
     * Set the value to a date.
     *
     * @param d a date
     */
    public void setDate(ReadableDateTime d) {
        System.out.println("setDate " + bytes.toString());
        setDigits(d.getYear(), 0, 4);
        setDigits(d.getMonthOfYear(), 4, 2);
        setDigits(d.getDayOfMonth(), 6, 2);
        bytes.put(8, SOH);
        bytes.position(9).flip().mark();
        System.out.println("setDate " + bytes.toString());
    }

    /**
     * Get the value as a time only.
     *
     * @param t a time only
     * @throws FIXValueFormatException if the value is not a time only
     */
    public void asTimeOnly(MutableDateTime t) {
        System.out.println("asTimeOnly " + bytes.toString());
        if (bytes.limit() != 8 && bytes.limit() != 12)
            notTimeOnly();

        t.setHourOfDay(getDigits(2));
        bytes.position(bytes.position() + 1);
        t.setMinuteOfHour(getDigits(2));
        bytes.position(bytes.position() + 1);
        t.setSecondOfMinute(getDigits(2));
        if (bytes.limit() == 12)
            bytes.position(bytes.position() + 1);
        t.setMillisOfSecond(bytes.limit() == 12 ? getDigits(3) : 0);
    }

    /**
     * Set the value to a time only.
     *
     * @param t a time only
     * @param millis if true set milliseconds, otherwise do not set milliseconds
     */
    public void setTimeOnly(ReadableDateTime t, boolean millis) {
        setDigits(t.getHourOfDay(), 0, 2);
        bytes.put(2, (byte)':');
        setDigits(t.getMinuteOfHour(), 3, 2);
        bytes.put(5, (byte)':');
        setDigits(t.getSecondOfMinute(), 6, 2);

        if (millis) {
            bytes.put(8, (byte)'.');
            setDigits(t.getMillisOfSecond(), 9, 3);
            bytes.put(12, SOH);
            bytes.position(13).flip().mark();
        } else {
            bytes.put(8, SOH);
            bytes.position(9).flip().mark();
        }
    }

    /**
     * Get the value as a timestamp.
     *
     * @param t a timestamp
     * @throws FIXValueFormatException if the value is not a timestamp
     */
    public void asTimestamp(MutableDateTime t) {
        if (bytes.limit() != 17 && bytes.limit() != 21)
            notTimestamp();

        int year           = getDigits(4);
        int monthOfYear    = getDigits(2);
        int dayOfMonth     = getDigits(2);
        bytes.position(bytes.position() + 1);
        int hourOfDay      = getDigits(2);
        bytes.position(bytes.position() + 1);
        int minuteOfHour   = getDigits(2);
        bytes.position(bytes.position() + 1);
        int secondOfMinute = getDigits(2);
        int millisOfSecond = 0;
        if (bytes.limit() == 21) {
            bytes.position(bytes.position() + 1);
            millisOfSecond = getDigits(3);
        }
        t.setDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour,
                secondOfMinute, millisOfSecond);
    }

    /**
     * Set the value to a timestamp.
     *
     * @param t a timestamp
     * @param millis if true set milliseconds, otherwise do not set milliseconds
     */
    public void setTimestamp(ReadableDateTime t, boolean millis) {
        setDigits(t.getYear(), 0, 4);
        setDigits(t.getMonthOfYear(), 4, 2);
        setDigits(t.getDayOfMonth(), 6, 2);
        bytes.put(8, (byte)'-');
        setDigits(t.getHourOfDay(), 9, 2);
        bytes.put(11, (byte)':');
        setDigits(t.getMinuteOfHour(), 12, 2);
        bytes.put(14, (byte)':');
        setDigits(t.getSecondOfMinute(), 15, 2);

        if (millis) {
            bytes.put(17, (byte)'.');
            setDigits(t.getMillisOfSecond(), 18, 3);
            bytes.put(21, SOH);

            bytes.position(22).flip().mark();
        } else {
            bytes.put(17, SOH);

            bytes.position(18).flip().mark();
        }

    }

    /**
     * Get the value as a checksum.
     *
     * @return the value as a checksum
     * @throws FIXValueFormatException if the value is not an integer
     */
    public long asCheckSum() {
        System.out.println("asCheckSum");
        return asInt();
    }

    /**
     * Set the value to a checksum.
     *
     * @param c a checksum
     */
    public void setCheckSum(long c) {
        System.out.println("setCheckSum");
        setDigits(c & 0xff, 0, 3);
        bytes.put(3, SOH);

        bytes.position(4).flip().mark();
    }

    /**
     * Read this value from a buffer.
     *
     * @param buffer a buffer
     * @throws FIXValueOverflowException if the length of the value exceeds
     *   the capacity of this value container
     * @return true if a value was successfully read from the buffer, otherwise
     *   false
     */
    public boolean get(ByteBuffer buffer) throws FIXValueOverflowException {
        System.out.println("get " + bytes.toString());
        System.out.println("get buffer:" + buffer.toString());
        bytes.clear();
        int length = 0;
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            bytes.put(b);
            System.out.println("b = " + (char)b);
            if (b == SOH) {
                bytes.limit(bytes.position()).position(0).mark();
                System.out.println("finished: " + bytes.toString());
                return true;
            }
            if (++length == bytes.capacity())
                tooLongValue();
        }
        System.out.println("error in get");
        bytes.limit(bytes.position()).position(0).mark();
        return false;
    }

    /**
     * Write this value to a buffer.
     *
     * @param buffer a buffer
     * @throws BufferOverflowException if there are fewer bytes remaining in
     *   the buffer than what this value consists of
     * @throws ReadOnlyBufferException if the buffer is read-only
     */
    public void put(ByteBuffer buffer) {
        System.out.println("put");
        ByteBuffer r = bytes.reset().slice();
        System.out.println("r is " + r.toString());
        System.out.println((char)bytes.get(0));
        System.out.println((char)bytes.get(1));
        System.out.println((char)bytes.get(2));
        buffer.put(r);
        System.out.println(buffer.toString());
        bytes.rewind();
    }

    private int getDigits(int digits) {
        int value = 0;

        for (int i = 0; i < digits; i++) {
            byte b = bytes.get();

            if (b < '0' || b > '9')
                notDigit();

            value = 10 * value + b - '0';
        }

        return value;
    }

    private void setDigits(long l, int offset, int digits) {
        for (int i = offset + digits - 1; i >= offset; i--) {
            System.out.println("digit = " + i + " " + (char)('0' + l % 10));
            bytes.put(i, (byte)('0' + l % 10));
            l /= 10;
        }
    }

    private static void notBoolean() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not a boolean");
    }

    private static void notChar() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not a character");
    }

    private static void notDate() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not a date");
    }

    private static void notDigit() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not a digit");
    }

    private static void notFloat() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not a float");
    }

    private static void notInt() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not an integer");
    }

    private static void notTimeOnly() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not a time only");
    }

    private static void notTimestamp() throws FIXValueFormatException {
        throw new FIXValueFormatException("Not a timestamp");
    }

    private static void tooLongValue() throws FIXValueOverflowException {
        throw new FIXValueOverflowException("Too long value");
    }

}
