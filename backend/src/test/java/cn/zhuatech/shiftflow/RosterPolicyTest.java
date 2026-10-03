// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.shiftflow;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;

/** 时间边界、半开区间及跨午夜资格规则。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
class RosterPolicyTest {
  final Instant a = Instant.parse("2026-10-05T01:00:00Z");

  @Test
  void twelveHoursAllowed() {
    assertDoesNotThrow(() -> RosterPolicy.times(a, a.plusSeconds(43200), a, 90));
  }

  @Test
  void extraSecondRejected() {
    assertThrows(Problem.class, () -> RosterPolicy.times(a, a.plusSeconds(43201), a, 90));
  }

  @Test
  void zeroLengthRejected() {
    assertThrows(Problem.class, () -> RosterPolicy.times(a, a, a, 90));
  }

  @Test
  void pastRejected() {
    assertThrows(
        Problem.class, () -> RosterPolicy.times(a, a.plusSeconds(3600), a.plusSeconds(1), 90));
  }

  @Test
  void horizonBoundary() {
    assertDoesNotThrow(() -> RosterPolicy.times(a.plusSeconds(86400), a.plusSeconds(90000), a, 1));
    assertThrows(
        Problem.class, () -> RosterPolicy.times(a.plusSeconds(86401), a.plusSeconds(90000), a, 1));
  }

  @Test
  void adjacentZeroGap() {
    assertFalse(
        RosterPolicy.conflicts(
            a, a.plusSeconds(3600), a.plusSeconds(3600), a.plusSeconds(7200), 0));
  }

  @Test
  void overlapSecond() {
    assertTrue(
        RosterPolicy.conflicts(
            a, a.plusSeconds(3600), a.plusSeconds(3599), a.plusSeconds(7200), 0));
  }

  @Test
  void exactEightHourGap() {
    assertFalse(
        RosterPolicy.conflicts(
            a, a.plusSeconds(3600), a.plusSeconds(32400), a.plusSeconds(36000), 8));
  }

  @Test
  void insufficientGapBothDirections() {
    assertTrue(
        RosterPolicy.conflicts(
            a, a.plusSeconds(3600), a.plusSeconds(32399), a.plusSeconds(36000), 8));
    assertTrue(
        RosterPolicy.conflicts(
            a.plusSeconds(32399), a.plusSeconds(36000), a, a.plusSeconds(3600), 8));
  }

  @Test
  void midnightEndExclusive() {
    var from = LocalDate.parse("2026-10-05");
    assertTrue(
        RosterPolicy.covers(
            from,
            from,
            Instant.parse("2026-10-05T14:00:00Z"),
            Instant.parse("2026-10-05T16:00:00Z")));
  }

  @Test
  void midnightSecondRequiresNextDate() {
    var from = LocalDate.parse("2026-10-05");
    assertFalse(
        RosterPolicy.covers(
            from,
            from,
            Instant.parse("2026-10-05T14:00:00Z"),
            Instant.parse("2026-10-05T16:00:01Z")));
  }

  @Test
  void expiredQualification() {
    assertFalse(
        RosterPolicy.covers(
            LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-04"), a, a.plusSeconds(3600)));
  }
}
