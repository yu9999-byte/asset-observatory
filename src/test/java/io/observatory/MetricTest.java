package io.observatory;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MetricTest {
  @Test void preservesBeyondJavascriptPrecision() {
    assertThat(Metrics.units("123456789012345678901234567890",18)).isEqualTo("123456789012.34567890123456789");
  }
  @Test void percentDoesNotReplaceMissingOrZeroWithZero() {
    assertThat(Metrics.change("100","0")).isNull();
    assertThat(Metrics.change(null,"100")).isNull();
    assertThat(Metrics.change("110","100")).isEqualTo("10");
  }
  @Test void identityIncludesLogAndSelfDirectionIsExplicit() {
    String a="0x0000000000000000000000000000000000000001";
    assertThat(Metrics.direction(a,a,a)).isEqualTo("SELF");
    assertThat(Metrics.direction(a,a,"0x0000000000000000000000000000000000000002")).isEqualTo("OUT");
  }
  @Test void catalogRejectsArbitraryUpstreamUrlsAndInvalidAddresses() {
    assertThatThrownBy(()->Catalog.asset("https://example.org")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(()->Catalog.address("0x123")).isInstanceOf(IllegalArgumentException.class);
    assertThat(Catalog.asset("LINK").contract()).isEqualToIgnoringCase("0x514910771af9ca656af840dff83e8264ecf986ca");
  }
}
