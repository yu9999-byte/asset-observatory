package io.observatory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CursorTest {
  private final ObjectMapper mapper=new ObjectMapper();
  @Test void roundTripsApprovedNumericCursorWithoutChangingPrecision() throws Exception {
    String raw="{\"value\":\"12345678901234567890\",\"address_hash\":\"0x0000000000000000000000000000000000000001\",\"items_count\":50}";
    String cursor=Cursor.encode(mapper.readTree(raw));
    assertThat(Cursor.query(cursor,mapper)).contains("value=12345678901234567890","items_count=50");
  }
  @Test void rejectsUrlInjectionAndOversizedCursor() throws Exception {
    assertThatThrownBy(()->Cursor.query(Cursor.encode(mapper.readTree("{\"url\":\"https://evil.test\"}")),mapper)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(()->Cursor.query("x".repeat(2049),mapper)).isInstanceOf(IllegalArgumentException.class);
  }
}
