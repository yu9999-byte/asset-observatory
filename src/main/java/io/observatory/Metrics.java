package io.observatory;
import java.math.BigDecimal;
import java.math.RoundingMode;
public final class Metrics {
 private Metrics() {}
 public static String decimal(String value) {
  if(value==null || value.isBlank()) return null;
  return new BigDecimal(value).stripTrailingZeros().toPlainString();
 }
 public static String units(String raw,int decimals) {
  if(raw==null || !raw.matches("[0-9]+") || decimals<0 || decimals>36) throw new IllegalArgumentException("无效代币数量或精度");
  return new BigDecimal(raw).movePointLeft(decimals).stripTrailingZeros().toPlainString();
 }
 public static String change(String last,String open) {
  if(last==null||open==null||new BigDecimal(open).signum()==0)return null;
  return new BigDecimal(last).divide(new BigDecimal(open),18,RoundingMode.HALF_UP).subtract(BigDecimal.ONE).movePointRight(2).stripTrailingZeros().toPlainString();
 }
 public static String multiply(String a,String b) {return a==null||b==null?null:new BigDecimal(a).multiply(new BigDecimal(b)).stripTrailingZeros().toPlainString();}
 public static String direction(String address,String from,String to) {
  if(address.equalsIgnoreCase(from)&&address.equalsIgnoreCase(to))return "SELF";
  if(address.equalsIgnoreCase(to))return "IN";
  if(address.equalsIgnoreCase(from))return "OUT";
  throw new IllegalArgumentException("来源返回了无关地址事件");
 }
}
