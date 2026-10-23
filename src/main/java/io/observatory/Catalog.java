package io.observatory;
import java.util.List;
import java.util.Locale;
public final class Catalog {
 private Catalog() {}
 public record Asset(String id,String symbol,String name,String contract,String chain,String spotInstrument,String swapInstrument,int decimals) {}
 public static final List<Asset> ALL=List.of(
  new Asset("BTC","BTC","Bitcoin",null,null,"XXBTZUSD","PF_XBTUSD",8),
  new Asset("ETH","ETH","Ethereum",null,null,"XETHZUSD","PF_ETHUSD",18),
  new Asset("LINK","LINK","Chainlink","0x514910771af9ca656af840dff83e8264ecf986ca","ethereum","LINKUSD","PF_LINKUSD",18),
  new Asset("UNI","UNI","Uniswap","0x1f9840a85d5af5bf1d1762f925bdaddc4201f984","ethereum","UNIUSD","PF_UNIUSD",18),
  new Asset("USDC","USDC","USD Coin","0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48","ethereum","USDCUSD","PF_USDCUSD",6),
  new Asset("WBTC","WBTC","Wrapped Bitcoin","0x2260fac5e5542a773aa44fbcfedf7c193bc2c599","ethereum","WBTCUSD",null,8),
  new Asset("JCT","JCT","Janction","0xc477b6dfd26ec2460b3b92de18837fd476ea7549","ethereum",null,null,18));
 public static Asset asset(String id) {return ALL.stream().filter(a->a.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("未知币种"));}
 public static String address(String address) {
  if(address==null || !address.matches("0x[0-9a-fA-F]{40}")) throw new IllegalArgumentException("需要完整 Ethereum 0x 地址");
  return address.toLowerCase(Locale.ROOT);
 }
 public static String key(String kind,String key) {
  return switch(kind) {case "asset" -> asset(key).id();case "address" -> address(key);default -> throw new IllegalArgumentException("未知实体类型");};
 }
}
