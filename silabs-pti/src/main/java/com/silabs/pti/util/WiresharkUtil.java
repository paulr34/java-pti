/*******************************************************************************
 * # License
 * Copyright 2020 Silicon Laboratories Inc. www.silabs.com
 *******************************************************************************
 *
 * The licensor of this software is Silicon Laboratories Inc. Your use of this
 * software is governed by the terms of Silicon Labs Master Software License
 * Agreement (MSLA) available at
 * www.silabs.com/about-us/legal/master-software-license-agreement. This
 * software is distributed to you in Source Code format and is governed by the
 * sections of the MSLA applicable to Source Code.
 *
 ******************************************************************************/

package com.silabs.pti.util;

import com.silabs.na.pcap.util.ByteArrayUtil;
import com.silabs.pti.debugchannel.EventType;

/**
 * Various utilities shared by pti and wireshark exporter.
 *
 * Created on Apr 26, 2017
 * 
 * @author Timotej Ecimovic
 */
public class WiresharkUtil {

  public enum PcapngExportMode {
    NOFCS("nofcs"), WISUN_AUTO("wisun-auto"), WISUN_RAIL_WISUN("wisun"), WISUN_RAIL_CUSTOM("wisun-custom"), SILABS_DEBUG_CHANNEL("silabs-dch");

    private final String id;

    PcapngExportMode(final String id) {
      this.id = id;
    }

    public static PcapngExportMode resolve(final String s) {
      for (PcapngExportMode m : values()) {
        if (m.id.equals(s))
          return m;
      }
      return defaultMode();
    }

    public static PcapngExportMode defaultMode() {
      return SILABS_DEBUG_CHANNEL;
    }

    public String id() {
      return id;
    }

    public boolean isWisun() {
      return id.startsWith("wisun");
    }
  }

  /**
   * Prints out the content in a format where text2pcap can consume it.
   * 
   * @param time Time.
   * @param data Data.
   */
  public static String printText2Pcap(final long time, final byte[] data) {
    byte[] prefix = WiresharkUtil.PCAP_DATA_PREFIX;
    long useconds = time % 1000000;
    long seconds = (time / 1000000);
    long minutes = seconds / 60;
    long hours = minutes / 60;
    final byte[] raw = prefix;

    return String.format("%02d:%02d:%02d.%06d 000000 %s %s",
                         hours,
                         minutes % 60,
                         seconds % 60,
                         useconds,
                         ByteArrayUtil.formatByteArray(raw),
                         ByteArrayUtil.formatByteArray(data));

  }

  /**
   * Returns a 2-byte crc, as wireshark expects it
   *
   * @return int
   */
  public static int lsbfCrc(final byte[] data, final int startIndex, final int endIndex, final int polynom) {
    int crc = 0;
    for (int j = startIndex; j < endIndex; j++) {
      if (j < 0 || j >= data.length)
        break;
      byte x = data[j];
      crc ^= (x & 0xFF);
      for (int i = 0; i < 8; i++) {
        if ((crc & 1) != 0) {
          crc = (crc >> 1) ^ polynom;
        } else {
          crc >>= 1;
        }
      }
    }
    return crc & 0xFFFF;
  }

  /**
   * Returns an array of 2 numbers that specify how many bytes to drop from
   * beginning and end of the packet, respectively.
   *
   * @return int[]
   */
  public static int[] dropBytesFromBeginningEnd(final EventType type) {
    int[] drops = new int[] { 0, 0 };

    // Remove the length byte
    if (!type.hasNoLengthByte())
      drops[0] = 1;

    // Now remove parts of radio info after the CRC
    if (type == EventType.TX_350 || type == EventType.TX_250) {
      // Drop 1 byte at the end
      drops[1] = 1;
    } else if (type == EventType.RX_350 || type == EventType.RX_250) {
      // Drop 3 bytes at the end
      drops[1] = 3;
    } else if (type == EventType.RX_EFR) {
      drops[1] = 5;
      drops[0]++; // The HW stuff.
    } else if (type == EventType.TX_EFR) {
      drops[1] = 3;
      drops[0]++; // The HW stuff
    }
    return drops;
  }

  public static byte[] PCAP_DATA_PREFIX = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                            (byte) 0x80, (byte) 0x9A };

}
