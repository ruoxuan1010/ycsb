/**
 * Copyright (c) 2016 YCSB contributors All rights reserved.
 * Copyright 2014 Basho Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.db;

import java.io.*;
import java.util.Map;
import java.util.Set;

import com.yahoo.ycsb.ByteArrayByteIterator;
import com.yahoo.ycsb.ByteIterator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utility class for Riak KV Client.
 *
 */
final class RiakUtils {

  private RiakUtils() {
    super();
  }

  private static byte[] toBytes(final int anInteger) {
    byte[] aResult = new byte[4];

    aResult[0] = (byte) (anInteger >> 24);
    aResult[1] = (byte) (anInteger >> 16);
    aResult[2] = (byte) (anInteger >> 8);
    aResult[3] = (byte) (anInteger /* >> 0 */);

    return aResult;
  }

  private static int fromBytes(final byte[] aByteArray) {
    checkArgument(aByteArray.length == 4);

    return (aByteArray[0] << 24) | (aByteArray[1] & 0xFF) << 16 | (aByteArray[2] & 0xFF) << 8 | (aByteArray[3] & 0xFF);
  }

  private static void close(final OutputStream anOutputStream) {
    try {
      anOutputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void close(final InputStream anInputStream) {
    try {
      anInputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static byte[] serializeTable(Map<String, ByteIterator> aTable) {
    final ByteArrayOutputStream anOutputStream = new ByteArrayOutputStream();
    final Set<Map.Entry<String, ByteIterator>> theEntries = aTable.entrySet();

    try {
      for (final Map.Entry<String, ByteIterator> anEntry : theEntries) {
        final byte[] aColumnName = anEntry.getKey().getBytes();

        anOutputStream.write(toBytes(aColumnName.length));
        anOutputStream.write(aColumnName);

        final byte[] aColumnValue = anEntry.getValue().toArray();

        anOutputStream.write(toBytes(aColumnValue.length));
        anOutputStream.write(aColumnValue);
      }
      return anOutputStream.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      close(anOutputStream);
    }
  }

  static void deserializeTable(final byte[] aValue, final Map<String, ByteIterator> theResult) {
    final ByteArrayInputStream anInputStream = new ByteArrayInputStream(aValue);
    byte[] aSizeBuffer = new byte[4];

    try {
      while (anInputStream.available() > 0) {
        anInputStream.read(aSizeBuffer);
        final int aColumnNameLength = fromBytes(aSizeBuffer);

        final byte[] aColumnNameBuffer = new byte[aColumnNameLength];
        anInputStream.read(aColumnNameBuffer);

        anInputStream.read(aSizeBuffer);
        final int aColumnValueLength = fromBytes(aSizeBuffer);

        final byte[] aColumnValue = new byte[aColumnValueLength];
        anInputStream.read(aColumnValue);

        theResult.put(new String(aColumnNameBuffer), new ByteArrayByteIterator(aColumnValue));
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      close(anInputStream);
    }
  }

  static Long getKeyAsLong(String key) {
    String keyString = key.replaceFirst("[a-zA-Z]*", "");

    return Long.parseLong(keyString);
  }
}
