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

import static com.paritytrading.philadelphia.fix42.FIX42Enumerations.*;
import static com.paritytrading.philadelphia.fix42.FIX42Tags.*;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import org.joda.time.MutableDateTime;
import org.junit.Before;
import org.junit.Test;

public class FIXMessageTest {

    private FIXMessage message;

    @Before
    public void setUp() {
        message = new FIXMessage(32, 32);
    }

    @Test
    public void format() {
        MutableDateTime timestamp = new MutableDateTime(2015, 9, 24, 9, 30, 5, 250);

        message.addString(ClOrdID, "123");
        message.addChar(HandlInst, HandlInstValues.AutomatedExecutionNoIntervention);
        message.addString(Symbol, "FOO");
        message.addChar(Side, SideValues.Buy);
        message.addTimestamp(TransactTime, timestamp, true);
        message.addInt(OrderQty, 100);
        message.addChar(OrdType, OrdTypeValues.Limit);
        message.addFloat(Price, 150.25, 2);

        ByteBuffer buffer = ByteBuffer.allocateDirect(256);

        message.put(buffer);
        buffer.flip();

        assertEquals("11=123\u000121=1\u000155=FOO\u000154=1\u0001" +
                "60=20150924-09:30:05.250\u000138=100\u000140=2\u0001" +
                "44=150.25\u0001", ByteBuffers.getString(buffer));
    }

    @Test
    public void parse() {
        String input = "11=123|21=1|55=FOO|54=1|60=20150924-09:30:05.250|" +
                "38=100|40=2|44=150.25|";

        String output = FIXMessage.fromString(input).toString();

        assertEquals(input, output);
    }

    @Test
    public void print() {
        MutableDateTime timestamp = new MutableDateTime(2015, 9, 24, 9, 30, 5, 250);

        message.addString(ClOrdID, "123");
        message.addChar(HandlInst, HandlInstValues.AutomatedExecutionNoIntervention);
        message.addString(Symbol, "FOO");
        message.addChar(Side, SideValues.Buy);
        message.addTimestamp(TransactTime, timestamp, true);
        message.addInt(OrderQty, 100);
        message.addChar(OrdType, OrdTypeValues.Limit);
        message.addFloat(Price, 150.25, 2);

        assertEquals("11=123|21=1|55=FOO|54=1|60=20150924-09:30:05.250|" +
                "38=100|40=2|44=150.25|", message.toString());
    }

}
