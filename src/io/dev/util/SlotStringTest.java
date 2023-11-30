package io.dev.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class SlotStringTest {

  public static void main(String[] args) {
    Map<String, Object> dest = buildDest();
    
    SlotString slot1 = new SlotString();
    testQFormat(slot1, dest);
    System.out.println("[I][main] QFORMAT TEST PASSED");
    
    SlotString slot2 = new SlotString();
    testFormat(slot2, dest);
    System.out.println("[I][main] FORMAT TEST PASSED");
    
    SlotString slot3 = new SlotString();
    testQFormat(slot3, dest);
    testFormat(slot3, dest);
    System.out.println("[I][main] MIXED TEST PASSED");
    
    testOverride(dest);
    System.out.println("[I][main] AS_STRING OVERRIDE TEST PASSED");
    
    System.out.println("[I][main] ALL TESTS PASSED");
    
    performanceComparison();
  }
  
  private static Map<String, Object> buildDest() {
    Map<String, Object> ret = new HashMap<>();
    ret.put("123", -123);
    ret.put("Abc", "-Abc");
    ret.put("1_A", "-1_A");
    ret.put("0", new BigDecimal("1E+10"));
    ret.put("1", 1.0);
    ret.put("2", 2.0f);
    ret.put("3", 1000);
    ret.put("4", null);
    ret.put("\\", "*\\");
    ret.put("}", "*}");
    return ret;
  }
  
  private static void testQFormat(SlotString slot, Map<String, Object> dest) {
    assertEquals("[-123] [-Abc] [-1_A]", slot.qformat("[${123}] [#{Abc}] [{1_A}]", dest));
    assertEquals("[-123] [-123] [-123]", slot.qformat("[${123}] [#{123}] [{123}]", dest));
    assertEquals("[] [] [] []", slot.qformat("[${ABC}] [#{NONE}] [{}] [${4}]", dest));
    
    assertEquals("[#{0}] [${0}] [{0}] [\\] [*\\] [*}]", slot.qformat("[\\#\\{0\\}] [\\$\\{0\\}] [\\{0\\}] [\\\\] [{\\\\}] [{\\}}]", dest));
    assertEquals(" <#1.0> <#2.0> <#$1.0> <#${2}> <#", slot.qformat(" <#${1}> <\\#${2}> <#\\${1}> <#$\\{2}> <#${1\\}>", dest));
    assertEquals(" <?2.0> <@1000> <%", slot.qformat(" <\\?{2}> <@{3}> <%{0", dest));
  }
  
  private static void testFormat(SlotString slot, Map<String, Object> dest) {
    slot.compile("[${123}] [#{Abc}] [{1_A}]");
    assertEquals("[-123] [-Abc] [-1_A]", slot.format(dest));
    dest.put("6", 0);
    assertEquals("[-123] [-Abc] [-1_A]", slot.format(dest));
    assertEquals("[-123] [-123] [] ", new SlotString("[${123}] [#{123}] [{1_a}] ").format(dest));
    
    assertEquals("[-123] [-123] [-123]", slot.compile("[${123}] [#{123}] [{123}]").format(dest));
    dest.remove("6");
    assertEquals("[-123] [-123] [-123]", slot.format(dest));
    
    assertEquals("[] [] [] []", slot.compile("[${ABC}] [#{NONE}] [{}] [${4}]").format(dest));
    
    assertEquals("[#{0}] [${0}] [{0}] [\\] [*\\] [*}]", slot.compile("[\\#\\{0\\}] [\\$\\{0\\}] [\\{0\\}] [\\\\] [{\\\\}] [{\\}}]").format(dest));
    assertEquals(" <#1.0> <#2.0> <#$1.0> <#${2}> <#", slot.compile(" <#${1}> <\\#${2}> <#\\${1}> <#$\\{2}> <#${1\\}>").format(dest));
    assertEquals(" <?2.0> <@1000> <%", slot.compile(" <\\?{2}> <@{3}> <%{0").format(dest));
  }
  
  private static void testOverride(Map<String, Object> dest) {
    dest.put("DATE", new Date(1701364467446L));
    
    ArrayList<Integer> list = new ArrayList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    dest.put("JOIN_LIST", list);
    
    dest.put("BIGINT", new BigInteger("123456789012345"));
    
    SlotString slot4 = new SlotString() {
      
      private SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      @SuppressWarnings("unchecked")
      @Override
      protected String asString(Object val, boolean preventDefault) {
        super.asString(val, true);
        if (val instanceof Date) {
          return dateFmt.format(val);
        } else if (val instanceof ArrayList<?>) {
          StringJoiner listFmt = new StringJoiner(", ");
          ((ArrayList<Integer>) val).forEach(e -> listFmt.add(e.toString()));
          return listFmt.toString();
        } else {
          return val.toString();
        }
      }

    };
    
    String expected = "[2023-12-01 01:14:27] [1, 2, 3] [123456789012345]";
    String pattern = "[#{DATE}] [${JOIN_LIST}] [{BIGINT}]";
    assertEquals(expected, slot4.compile(pattern).format(dest));
    assertEquals(expected, slot4.qformat(pattern, dest));
  }
  
  private static void performanceComparison() {
    String pattern = "SELECT T1.ID, T1.CREATE_TIME, T1.UPDATE_TIME, T1.BOOK_NAME, T2.AUTHOR_NAME, T3.CLS_NAME "
        + "FROM BOOK${YEAR} T1 "
        + "INNER JOIN ( "
        + "  SELECT T1.ID "
        + "  FROM BOOK${YEAR} T1 "
        + "  INNER JOIN BOOK_AUTHOR T2 ON T1.AUTHOR_ID = T2.ID "
        + "  INNER JOIN BOOK_CLS T3 ON T1.CLS_ID = T3.ID "
        + "  WHERE T1.ISSN = '#{ISSN}' "
        + "    AND T1.PRICE BETWEEN {MIN_PRICE} AND {MAX_PRICE} "
        + "    AND T2.AUTHOR_NAME = '#{AUTHOR_NAME}' "
        + "    AND T3.CLS_NAME IN (${CLS_NAMES}) "
        + "  LIMIT {0}, {1} "
        + ") T2 ON T1.ID = T2.ID "
        + "INNER JOIN BOOK_AUTHOR T2 ON T1.AUTHOR_ID = T2.ID "
        + "INNER JOIN BOOK_CLS T3 ON T1.CLS_ID = T3.ID {2};";
    String expected = "SELECT T1.ID, T1.CREATE_TIME, T1.UPDATE_TIME, T1.BOOK_NAME, T2.AUTHOR_NAME, T3.CLS_NAME "
        + "FROM BOOK2023 T1 "
        + "INNER JOIN ( "
        + "  SELECT T1.ID "
        + "  FROM BOOK2023 T1 "
        + "  INNER JOIN BOOK_AUTHOR T2 ON T1.AUTHOR_ID = T2.ID "
        + "  INNER JOIN BOOK_CLS T3 ON T1.CLS_ID = T3.ID "
        + "  WHERE T1.ISSN = 'DFEEE5004014D300F440E5BE03511465A12C548485D8AEA3BEF105C70B188FFC' "
        + "    AND T1.PRICE BETWEEN 0.123 AND 456.123 "
        + "    AND T2.AUTHOR_NAME = 'B0368EE5217694853C1A24E97A96A377' "
        + "    AND T3.CLS_NAME IN ('aUZVPTlyHLe-qTrkfPXXUCuOrtF1U-EsLv5ifqDPGik','UpnQfkJsFGQGr_7MFFNiCkeCXM9Cl0rJiMOgJpYejsA') "
        + "  LIMIT 100, 200 "
        + ") T2 ON T1.ID = T2.ID "
        + "INNER JOIN BOOK_AUTHOR T2 ON T1.AUTHOR_ID = T2.ID "
        + "INNER JOIN BOOK_CLS T3 ON T1.CLS_ID = T3.ID AND T1.DESCRIPTION IS NOT NULL;";
    Map<String, Object> dest = new HashMap<>();
    dest.put("YEAR", 2023);
    dest.put("ISSN", "DFEEE5004014D300F440E5BE03511465A12C548485D8AEA3BEF105C70B188FFC");
    dest.put("AUTHOR_NAME", "B0368EE5217694853C1A24E97A96A377");
    dest.put("MIN_PRICE", 0.123f);
    dest.put("MAX_PRICE", 456.123);
    dest.put("CLS_NAMES", "'aUZVPTlyHLe-qTrkfPXXUCuOrtF1U-EsLv5ifqDPGik','UpnQfkJsFGQGr_7MFFNiCkeCXM9Cl0rJiMOgJpYejsA'");
    dest.put("0", 100);
    dest.put("1", 200);
    dest.put("2", "AND T1.DESCRIPTION IS NOT NULL");
    int loop = 500000;
    
    SlotString slot5 = new SlotString();
    long t = System.currentTimeMillis();
    for (int i = 0; i < loop; ++i) {
      assertEquals(expected, slot5.qformat(pattern, dest));
    }
    t = System.currentTimeMillis() - t;
    System.out.println("[I][performanceOfSamePattern] elapsed time of qformat(ms): " + t);
    
    SlotString slot6 = new SlotString();
    slot6.compile(pattern);
    t = System.currentTimeMillis();
    for (int i = 0; i < loop; ++i) {
      assertEquals(expected, slot6.format(dest));
    }
    t = System.currentTimeMillis() - t;
    System.out.println("[I][performanceOfSamePattern] elapsed time of compile+format(ms): " + t);
    
    SlotString slot7 = new SlotString();
    t = System.currentTimeMillis();
    for (int i = 0; i < loop; ++i) {
      String rndStr = String.valueOf(Math.random()) + String.valueOf(Math.random());
      assertEquals(rndStr + expected, slot7.qformat(rndStr + pattern, dest));
    }
    t = System.currentTimeMillis() - t;
    System.out.println("[I][performanceOfDiffPattern] elapsed time of qformat(ms): " + t);
    
    SlotString slot8 = new SlotString();
    t = System.currentTimeMillis();
    for (int i = 0; i < loop; ++i) {
      String rndStr = String.valueOf(Math.random()) + String.valueOf(Math.random());
      assertEquals(rndStr + expected, slot8.compile(rndStr + pattern).format(dest));
    }
    t = System.currentTimeMillis() - t;
    System.out.println("[I][performanceOfDiffPattern] elapsed time of compile+format(ms): " + t);
  }
  
  private static void assertEquals(String expected, String actual) {
    if (!Objects.equals(expected, actual)) {
      throw new RuntimeException("[E][Assertion]\n expected: (" + expected + ")\n   actual: (" + actual + ")");
    }
  }

}
