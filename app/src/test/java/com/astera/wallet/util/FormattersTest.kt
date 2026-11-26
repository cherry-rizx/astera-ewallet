package com.astera.wallet.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
  @Test
  fun maskedWalletNumber_showsLastFourDigits() {
    assertEquals("•••• 4821", maskedWalletNumber("5839 2071 4821"))
    assertEquals("•••• ----", maskedWalletNumber(null))
  }

  @Test
  fun formatRupiahCompact_shortensForChartLabels() {
    assertEquals("Rp 1,2 jt", formatRupiahCompact(1_200_000))
    assertEquals("Rp 5 jt", formatRupiahCompact(5_000_000))
    assertEquals("Rp 500 rb", formatRupiahCompact(500_000))
    assertEquals("Rp 900", formatRupiahCompact(900))
  }

  @Test
  fun monthLabels_useIndonesianNames() {
    assertEquals("November 2026", longMonthLabel("2026-11"))
  }
}
