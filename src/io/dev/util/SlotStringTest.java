package io.dev.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class SlotStringTest {
  
  private static final int PARALLEL_THREAD_NUMBER = 32;
  
  private static final int PERFORMANCE_TEST_LOOP_TIMES = 500000;

  private List<TestCase> testCases;

  public static void main(String[] args) {
    SlotStringTest test = new SlotStringTest();
    test.run();
  }

  public void run() {
    String tag = "main";
    
    genTestCases();
    testQformat();
    testCompileFormat();
    testHybirdFormat();
    testOverrideAsStringFormat();
    testPerformance();
    
    logi(tag, "All Test Completed");
  }

  private void genTestCases() {
    testCases = new ArrayList<>();
    Map<String, Object> dest;
    
    dest = new HashMap<>();
    dest.put("var0", "1甲A");
    dest.put("var1", "2乙B");
    dest.put("var2", "3丙C");
    // different slots
    testCases.add(new TestCase("C1", "[1甲A] [2乙B] [3丙C]", "[${var0}] [#{var1}] [{var2}]", dest));
    // same slots
    testCases.add(new TestCase("C2", "[1甲A] [1甲A] [1甲A]", "[${var0}] [#{var0}] [{var0}]", dest));
    // case-sensitive key and non-existent value
    testCases.add(new TestCase("C3", "[1甲A] []", "[{var0}] [{Var0}]", dest));
    // single and double quotes with no special purpose
    testCases.add(new TestCase("C4", "'1甲A' \"1甲A\"", "'{var0}' \"{var0}\"", dest));
    
    dest = new HashMap<>();
    dest.put("LONG", 10000L);
    dest.put("BIG_DECMIAL", new BigDecimal("1E+10"));
    dest.put("NULL", null);
    // special value
    // no thousand separator, no exponential form, and avoiding null value
    testCases.add(new TestCase("C5", "[10000] [10000000000] []", "[{LONG}] [{BIG_DECMIAL}] [{NULL}]", dest));
    
    dest = new HashMap<>();
    dest.put("0", "/* SHOULD NOT DISPLAY */");
    // escape any character or symbol of a regular text type
    testCases.add(new TestCase("C6", "[\\] [${0}] [#{0}] [{0}] [A]", "[\\\\] [\\$\\{0\\}] [\\#\\{0\\}] [\\{0\\}] [\\A]", dest));
    
    dest = new HashMap<>();
    dest.put("\\", "backslash");
    dest.put("}", "close curly");
    dest.put("A", "any character");
    dest.put("", "empty key");
    dest.put("变量", "variable");
    // escape any character or symbol of a slot key type
    testCases.add(new TestCase("C7", "[backslash] [close curly] [any character]", "[{\\\\}] [{\\}}] [{\\A}]", dest));
    // empty string can be used as key
    testCases.add(new TestCase("C8", "[empty key]", "[${}]", dest));
    // non-ASCII character can be used as key
    testCases.add(new TestCase("C9", "[variable]", "[#{变量}]", dest));
    
    dest = new HashMap<>(0);
    // null pattern
    testCases.add(new TestCase("C10A", null, null, null));
    testCases.add(new TestCase("C10B", null, null, dest));
    // empty string as pattern
    testCases.add(new TestCase("C11A", "", "", null));
    testCases.add(new TestCase("C11B", "", "", dest));
    // pattern without slots
    testCases.add(new TestCase("C12A", " 123甲乙丙ABC'\" ", " 123甲乙丙ABC'\" ", null));
    testCases.add(new TestCase("C12B", " 123甲乙丙ABC'\" ", " 123甲乙丙ABC'\" ", dest));
    
    dest = new HashMap<>();
    dest.put("a", "0v0");
    // actually, it's just regular text and slots
    testCases.add(new TestCase("C13", " [@0v0] [%0v0] [^0v0] [*0v0] [?0v0?] ", " [@{a}] [%{a}] [^{a}] [*{a}] [?{a}?] ", dest));
    // WRONG UNEXPECTED BEHAVIOR: Unsupported symbol combination
    testCases.add(new TestCase("E1", "[#a] [$0v0]", "[#a] [$#{a}]", dest));
    // WRONG UNEXPECTED BEHAVIOR: Incomplete escape
    testCases.add(new TestCase("E2", "[#0v0]", "[\\#{a}]", dest));
    // UNSTABLE UNEXPECTED BEHAVIOR:
    // Although isolated close curly will not be removed in the current implementation,
    // it is still recommended to *ESCAPE IT*.
    testCases.add(new TestCase("E3", "[#{a}]", "[\\#\\{a}]", dest));
    // WRONG UNEXPECTED BEHAVIOR: No closed slot
    testCases.add(new TestCase("E4", "[", "[#{a]", dest));
    
    logi("genTestCases", "Test Case Generated");
  }

  private void testQformat() {
    String tag = "testQformat";
    
    testSharedBufferQformat();
    logi(tag, "Shared Buffer Qformat Test Passed");
    
    testDiscreteBufferQformat();
    logi(tag, "Discrete Buffer Qformat Test On Multi Thread Passed");
    
    logi(tag, "All Qformat Test Passed");
  }
  
  private void testSharedBufferQformat() {
    SlotString target = new SlotString(false);
    runQformatTest(target);
  }
  
  private void testDiscreteBufferQformat() {
    // multi-thread mode (discrete buffer mode)
    final SlotString target = new SlotString(true);
    
    runOnMultiThread(() -> runQformatTest(target));
  }

  private void runQformatTest(SlotString target) {
    for (TestCase testCase : testCases) {
      assertEquals(testCase.id, testCase.expected, target.qformat(testCase.pattern, testCase.dest));
    }
  }

  private void testCompileFormat() {
    String tag = "testCompileFormat";
    
    testDiffPatternCompileFormat();
    logi(tag, "Different Pattern Compile+Format Test Passed");
    
    testSamePatternDiffDestCompileFormatOnMultiThread(false);
    logi(tag, "Same Pattern Different Destination Compile+Format Test On Multi Thread Passed");
    
    logi(tag, "All Compile+Format Test Passed");
  }
  
  private void testDiffPatternCompileFormat() {
    for (TestCase testCase : testCases) {
      assertEquals(testCase.id, testCase.expected, new SlotString(testCase.pattern).format(testCase.dest));
    } 
  }
  
  private void testSamePatternDiffDestCompileFormatOnMultiThread(boolean testQformat) {
    final SlotString target = new SlotString("MT [{RANDOM}] [{CUR_MS}] [{1}] vAL");
    runOnMultiThread(() -> {
      double rnd = Math.random();
      long ms = System.currentTimeMillis();
      double rnd2 = Math.random();
      
      String expected = "MT [" + rnd + "] [" + ms + "] [" + rnd2 + "] vAL";
      
      Map<String, Object> dest = new HashMap<>();
      dest.put("RANDOM", rnd);
      dest.put("CUR_MS", ms);
      dest.put("1", rnd2);
      assertEquals("", expected, target.format(dest));
      
      if (testQformat) {
        runQformatTest(target);
      }
    });
  }

  private void testHybirdFormat() {
    testSamePatternDiffDestCompileFormatOnMultiThread(true);
    logi("testHybirdFormat", "Hybird Format Test Passed");
  }
  
  private void testOverrideAsStringFormat() {
    String tag = "testOverrideAsStringFormat";
    
    String pattern = "[#{date}] [${join_list}] [{big_decimal}] [{date2}]";
    String expected = "[2023-12-12 11:11:11] [1, 2, 3] [10000000000] [2023-12-01 01:14:27]";
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    ArrayList<Integer> list = new ArrayList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    Map<String, Object> dest = new HashMap<>();
    dest.put("date", LocalDateTime.parse("2023-12-12T11:11:11.11"));
    dest.put("date2", new Date(1701364467446L));
    dest.put("join_list", list);
    dest.put("big_decimal", new BigDecimal("1E+10"));
    
    SlotString target = new SlotString(pattern) {

      @SuppressWarnings("unchecked")
      @Override
      protected String asString(Object val, boolean preventDefault) {
        String ret = super.asString(val, true);
        if (ret != null) {
          return ret;
        }
        if (val instanceof LocalDateTime) {
          return dateFmt.format((LocalDateTime) val);
        } else if (val instanceof ArrayList<?>) {
          StringJoiner listFmt = new StringJoiner(", ");
          ((ArrayList<Integer>) val).forEach(e -> listFmt.add(e.toString()));
          return listFmt.toString();
        } else if (val instanceof Date) {
          return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) val);
        } else {
          return val.toString();
        }
      }
      
    };
    
    assertEquals("OC1", expected, target.qformat(pattern, dest));
    logi(tag, "Qformat With Overriding AsString Test Passed");
    
    assertEquals("OC2", expected, target.format(dest));
    logi(tag, "Compile+Format With Overriding AsString Test Passed");
    
    logi(tag, "All Format With Overriding AsString Test Passed");
  }

  private void testPerformance() {
    String tag = "testPerformance";
    String samePatternTag = "testPerformance:samePattern";
    String diffPatternTag = "testPerformance:diffPattern";
    String ratioTag = "testPerformance:ratio";
    
    TestCase testCase = genPerformanceTestCase();
    logi(tag, "Performance Test Case Generated");
    logi(tag, "Starting Performance Test (Loop Times: " + PERFORMANCE_TEST_LOOP_TIMES + ")");
    
    long ss = testQformatPerformance(false, testCase, false);
    logi(samePatternTag, "Elapsed time of shared buffer qformat (ms): " + ss);
    
    long sd = testQformatPerformance(true, testCase, false);
    logi(samePatternTag, "Elapsed time of discrete buffer qformat (ms): " + sd);
    
    long sc = testCompileFormatPerformance(testCase, false);
    logi(samePatternTag, "Elapsed time of compile+format (ms): " + sc);
    
    long ds = testQformatPerformance(false, testCase, true);
    logi(diffPatternTag, "Elapsed time of shared buffer qformat (ms): " + ds);
    
    long dd = testQformatPerformance(true, testCase, true);
    logi(diffPatternTag, "Elapsed time of discrete buffer qformat (ms): " + dd);
    
    long dc = testCompileFormatPerformance(testCase, true);
    logi(diffPatternTag, "Elapsed time of compile+format (ms): " + dc);
    
    long smax = Math.max(Math.max(ss, sd), sc);
    double rss = (double) ss / smax;
    double rsd = (double) sd / smax;
    double rsc = (double) sc / smax;
    
    long dmax = Math.max(Math.max(ds, dd), dc);
    double rds = (double) ds / dmax;
    double rdd = (double) dd / dmax;
    double rdc = (double) dc / dmax;

    logi(ratioTag, "Elapsed time ratio for Shared-Buffer qformat, Discrete-Buffer qformat and compile+format:"
        + "\n  Same Pattern: " + rss + ", " + rsd + ", " + rsc
        + "\n  Diff Pattern: " + rds + ", " + rdd + ", " + rdc);
    
    logi(tag, "Performance Test Finished");
  }
  
  private TestCase genPerformanceTestCase() {
    String expected = "CREATE TABLE IF NOT EXISTS USER2023 (\n"
        + "  ID BINARY(16) PRIMARY KEY,\n"
        + "  CREATE_TIME DATETIME,\n"
        + "  UPDATE_TIME DATETIME,\n"
        + "  CODE BIGINT UNSIGNED,\n"
        + "  DISPLAY_NAME VARCHAR(100),\n"
        + "  EMAIL VARCHAR(200)\n"
        + ");\n"
        + "\n"
        + "CREATE TABLE IF NOT EXISTS USER_DIMENSION2023 (\n"
        + "  ID BINARY(16) PRIMARY KEY,\n"
        + "  CREATE_TIME DATETIME,\n"
        + "  UPDATE_TIME DATETIME,\n"
        + "  USER_ID BINARY(16),\n"
        + "  DIM_NAME VARCHAR(500),\n"
        + "  DIM_VALUE BINARY(16)\n"
        + ");\n"
        + "\n"
        + "INSERT INTO USER2023(ID, CREATE_TIME, UPDATE_TIME, DISPLAY_NAME, EMAIL) VALUES (UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), NOW(), NOW(), 1000046, 'flyingfish', 'hCXl0tRqp6g@qmail.com');\n"
        + "INSERT INTO USER2023(ID, CREATE_TIME, UPDATE_TIME, DISPLAY_NAME, EMAIL) VALUES (UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), NOW(), NOW(), 1005102, 'divingcat', 'I3Cv-7-hBjk@vmail.com');\n"
        + "\n"
        + "-- {ABCDEFGHIJKLMNOPQRSTUVWXYZ}\n"
        + "/* {ZYXWVUTSRQPONMLKJIHGFEDCBA} */\n"
        + "INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), 'BBS_NRBLFHSV', UUID_TO_BIN('9a32463a300a6f492dfb16bf2f50ce58'));\n"
        + "INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), 'GAME_TVMHSRM', UUID_TO_BIN('2e9f8f71b6f7b434bfa5c03e5f0d40d3'));\n"
        + "INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('1905C123-2AC0-4802-A184-F1AFDCE0C6AA'), 'GAME_HGZIIZRO', UUID_TO_BIN('85447153b0ede2a8ec9ab3cc61defe48'));\n"
        + "INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), 'BBS_NRBLFHSV', UUID_TO_BIN('bf2312129f116234a8d79cfb83bd9475'));\n"
        + "INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), 'GAME_TVMHSRM', UUID_TO_BIN('b43db12a322cf37ee229aa1d5bf10080'));\n"
        + "INSERT INTO USER_DIMENSION2023(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), NOW(), NOW(), UUID_TO_BIN('BA681A63-D3B3-4105-A1A0-1CA2522D08C0'), 'GAME_HGZIIZRO', UUID_TO_BIN('805b7b9f97e1566c533060356a4edf84'));\n"
        + "\n"
        + "SELECT T1.ID, T2.DIM_NAME, T2.DIM_VALUE\n"
        + "FROM USER2023 T1\n"
        + "INNER JOIN USER_DIMENSION2023 T2 ON T1.ID = T2.USER_ID\n"
        + "WHERE T1.CODE BETWEEN 1000000 AND 2000000\n"
        + "  AND T2.DIM_NAME = :DIM_NAME\n"
        + "  AND T1.EMAIL LIKE '%mail.com';";
    String pattern = "CREATE TABLE IF NOT EXISTS USER${YEAR} (\n"
        + "  ID BINARY(16) PRIMARY KEY,\n"
        + "  CREATE_TIME DATETIME,\n"
        + "  UPDATE_TIME DATETIME,\n"
        + "  CODE BIGINT UNSIGNED,\n"
        + "  DISPLAY_NAME VARCHAR(100),\n"
        + "  EMAIL VARCHAR(200)\n"
        + ");\n"
        + "\n"
        + "CREATE TABLE IF NOT EXISTS USER_DIMENSION${YEAR} (\n"
        + "  ID BINARY(16) PRIMARY KEY,\n"
        + "  CREATE_TIME DATETIME,\n"
        + "  UPDATE_TIME DATETIME,\n"
        + "  USER_ID BINARY(16),\n"
        + "  DIM_NAME VARCHAR(500),\n"
        + "  DIM_VALUE BINARY(16)\n"
        + ");\n"
        + "\n"
        + "INSERT INTO USER${YEAR}(ID, CREATE_TIME, UPDATE_TIME, DISPLAY_NAME, EMAIL) VALUES (UUID_TO_BIN('{USER1_ID}'), #{TIME}, #{TIME}, {USER1_CODE}, 'flyingfish', 'hCXl0tRqp6g@qmail.com');\n"
        + "INSERT INTO USER${YEAR}(ID, CREATE_TIME, UPDATE_TIME, DISPLAY_NAME, EMAIL) VALUES (UUID_TO_BIN('{USER2_ID}'), #{TIME}, #{TIME}, {USER2_CODE}, 'divingcat', 'I3Cv-7-hBjk@vmail.com');\n"
        + "\n"
        + "-- \\{ABCDEFGHIJKLMNOPQRSTUVWXYZ\\}\n"
        + "/* \\{ZYXWVUTSRQPONMLKJIHGFEDCBA\\} */\n"
        + "INSERT INTO USER_DIMENSION${YEAR}(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), #{TIME}, #{TIME}, UUID_TO_BIN('{USER1_ID}'), 'BBS_NRBLFHSV', UUID_TO_BIN('9a32463a300a6f492dfb16bf2f50ce58'));\n"
        + "INSERT INTO USER_DIMENSION${YEAR}(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), #{TIME}, #{TIME}, UUID_TO_BIN('{USER1_ID}'), 'GAME_TVMHSRM', UUID_TO_BIN('2e9f8f71b6f7b434bfa5c03e5f0d40d3'));\n"
        + "INSERT INTO USER_DIMENSION${YEAR}(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), #{TIME}, #{TIME}, UUID_TO_BIN('{USER1_ID}'), 'GAME_HGZIIZRO', UUID_TO_BIN('85447153b0ede2a8ec9ab3cc61defe48'));\n"
        + "INSERT INTO USER_DIMENSION${YEAR}(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), #{TIME}, #{TIME}, UUID_TO_BIN('{USER2_ID}'), 'BBS_NRBLFHSV', UUID_TO_BIN('bf2312129f116234a8d79cfb83bd9475'));\n"
        + "INSERT INTO USER_DIMENSION${YEAR}(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), #{TIME}, #{TIME}, UUID_TO_BIN('{USER2_ID}'), 'GAME_TVMHSRM', UUID_TO_BIN('b43db12a322cf37ee229aa1d5bf10080'));\n"
        + "INSERT INTO USER_DIMENSION${YEAR}(ID, CREATE_TIME, UPDATE_TIME, USER_ID, DIM_NAME, DIM_VALUE) VALUES (UUID_TO_BIN(UUID()), #{TIME}, #{TIME}, UUID_TO_BIN('{USER2_ID}'), 'GAME_HGZIIZRO', UUID_TO_BIN('805b7b9f97e1566c533060356a4edf84'));\n"
        + "\n"
        + "SELECT T1.ID, T2.DIM_NAME, T2.DIM_VALUE\n"
        + "FROM USER${YEAR} T1\n"
        + "INNER JOIN USER_DIMENSION${YEAR} T2 ON T1.ID = T2.USER_ID\n"
        + "WHERE T1.CODE BETWEEN {START_CODE} AND {END_CODE}\n"
        + "  #{DIM_NAME_PARAM}\n"
        + "  AND T1.EMAIL LIKE '%mail.com'#{EMPTY_SPL};";
    Map<String, Object> dest = new HashMap<>();
    dest.put("YEAR", 2023);
    dest.put("USER1_ID", "1905C123-2AC0-4802-A184-F1AFDCE0C6AA");
    dest.put("USER2_ID", "BA681A63-D3B3-4105-A1A0-1CA2522D08C0");
    dest.put("TIME", "NOW()");
    dest.put("USER1_CODE", 1000046L);
    dest.put("USER2_CODE", 1005102L);
    dest.put("START_CODE", 1000000L);
    dest.put("END_CODE", 2000000L);
    dest.put("DIM_NAME_PARAM", "AND T2.DIM_NAME = :DIM_NAME");
    dest.put("EMPTY_SQL", "");
    return new TestCase("P", expected, pattern, dest);
  }

  private long testQformatPerformance(boolean multiThread, TestCase performanceTestCase, boolean diffPattern) {
    final String tag = "PQ" + (multiThread ? "D" : "S") + (diffPattern ? "D" : "S");
    if (diffPattern) {
      long t = System.currentTimeMillis();
      SlotString target = new SlotString(multiThread);
      for (int i = 0; i < PERFORMANCE_TEST_LOOP_TIMES; ++i) {
        String seq = "/* SEQ = " + Math.random() + " */";
        String pattern = seq + performanceTestCase.pattern;
        String expected = seq + performanceTestCase.expected;
        String actual = target.qformat(pattern, performanceTestCase.dest);
        assertEquals(tag, expected, actual);
      }
      return System.currentTimeMillis() - t;
    } else {
      long t = System.currentTimeMillis();
      SlotString target = new SlotString(multiThread);
      for (int i = 0; i < PERFORMANCE_TEST_LOOP_TIMES; ++i) {
        String expected = performanceTestCase.expected;
        String actual = target.qformat(performanceTestCase.pattern, performanceTestCase.dest);
        assertEquals(tag, expected, actual);
      }
      return System.currentTimeMillis() - t;
    }
  }

  private long testCompileFormatPerformance(TestCase performanceTestCase, boolean diffPattern) {
    final String tag = "PC" + (diffPattern ? "D" : "S");
    if (diffPattern) {
      long t = System.currentTimeMillis();
      for (int i = 0; i < PERFORMANCE_TEST_LOOP_TIMES; ++i) {
        String seq = "/* SEQ = " + Math.random() + " */";
        String pattern = seq + performanceTestCase.pattern;
        String expected = seq + performanceTestCase.expected;
        SlotString target = new SlotString(pattern);
        String actual = target.format(performanceTestCase.dest);
        assertEquals(tag, expected, actual);
      }
      return System.currentTimeMillis() - t;
    } else {
      long t = System.currentTimeMillis();
      SlotString target = new SlotString(performanceTestCase.pattern);
      for (int i = 0; i < PERFORMANCE_TEST_LOOP_TIMES; ++i) {
        String expected = performanceTestCase.expected;
        String actual = target.format(performanceTestCase.dest);
        assertEquals(tag, expected, actual);
      }
      return System.currentTimeMillis() - t;
    }
  }
  
  private static void runOnMultiThread(Runnable target) {
    ExecutorService executorService = Executors.newFixedThreadPool(PARALLEL_THREAD_NUMBER);
    CountDownLatch countDownLatch = new CountDownLatch(PARALLEL_THREAD_NUMBER);
    
    for (int i = 0; i < PARALLEL_THREAD_NUMBER; ++i) {
      executorService.execute(() -> {
        try {
          target.run();
        } finally {
          countDownLatch.countDown(); // reduce count when thread task is completed
        }
      });
    }
    
    try {
        countDownLatch.await(); // waiting for all threads to complete
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    } finally {
        executorService.shutdown(); // shutdown thread pool
    }
  }

  private static void assertEquals(String message, String expected, String actual) {
    if (!Objects.equals(expected, actual)) {
      throw new RuntimeException(
          "[E][Assertion]\n  message: " + message + "\n expected: <(" + expected + ")>\n   actual: <(" + actual + ")>");
    }
  }

  private static void logi(String tag, String str) {
    System.out.println("[I][" + tag + "] " + str);
  }

  private static class TestCase {
    public String id;
    public String expected;
    public String pattern;
    public Map<String, Object> dest;

    public TestCase(String id, String expected, String pattern, Map<String, Object> dest) {
      this.id = id;
      this.expected = expected;
      this.pattern = pattern;
      this.dest = dest;
    }

    @Override
    public String toString() {
      return "TestCase [id=" + id + ", expected=" + expected + ", pattern=" + pattern + ", dest=" + dest + "]";
    }
  }
}