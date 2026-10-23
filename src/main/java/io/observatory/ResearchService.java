package io.observatory;
import com.fasterxml.jackson.databind.*;
import io.observatory.ApiModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ResearchService {
 private final SourceHttp http;
 private final ObjectMapper mapper;
 private final boolean enabled,krakenApproved,chainApproved;
 private final SnapshotCache<List<Market>> markets=new SnapshotCache<>(1,Clock.systemUTC());
 private final SnapshotCache<List<Derivative>> derivatives=new SnapshotCache<>(1,Clock.systemUTC());
 private final SnapshotCache<List<Holder>> holders=new SnapshotCache<>(64,Clock.systemUTC());
 private final SnapshotCache<List<Transfer>> transfers=new SnapshotCache<>(128,Clock.systemUTC());
 private final SnapshotCache<List<Liquidation>> liquidations=new SnapshotCache<>(6,Clock.systemUTC());
 private final MarketQuerySnapshotStore marketQuerySnapshots;
 private static final Duration MARKET_QUERY_SNAPSHOT_TTL=Duration.ofMinutes(2);
 private static final long MAX_SAFE_INTEGER=9007199254740991L;
 private static final BigInteger MAX_UINT256=BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);
 private static final String KRAKEN="https://api.kraken.com/0/public/Ticker?pair=XBTUSD,ETHUSD,LINKUSD,UNIUSD,USDCUSD,WBTCUSD";
 private static final String FUTURES="https://futures.kraken.com/derivatives/api/v3/tickers";
 private static final String CHAIN="https://eth.blockscout.com/api/v2";
 private static final String MARKET_COVERAGE="Kraken 单交易所 USD 现货；成交额为24h VWAP×成交量估算；无逐快照源时间；不提供滚动24h开盘/涨跌";
 private static final String FUTURES_COVERAGE="Kraken 单交易所永续；OI按来源合约单位显示；不等于全市场或账户持仓";
 @Autowired
 public ResearchService(SourceHttp http,ObjectMapper mapper,@Value("${app.sources.enabled}")boolean enabled,@Value("${app.sources.kraken-approved}")boolean krakenApproved,@Value("${app.sources.blockscout-legacy-approved}")boolean chainApproved,MarketQuerySnapshotStore marketQuerySnapshots){
  this.http=http;this.mapper=mapper;this.enabled=enabled;this.krakenApproved=krakenApproved;this.chainApproved=chainApproved;this.marketQuerySnapshots=marketQuerySnapshots;
 }
 public Markets markets(){return new Markets(Catalog.ALL,market(),derivative());}
 public MarketQueryPage marketQuery(String filter,String sort,String direction,Integer limit,String cursor){
  String normalizedFilter=filter==null?"":filter.trim().toLowerCase(Locale.ROOT);
  if(normalizedFilter.length()>80)throw new IllegalArgumentException("筛选条件过长");
  String normalizedSort=sort==null||sort.isBlank()?"assetId":sort;
  if(!Set.of("assetId","instrument","lastPrice","change24hPct","volume24hBase","turnover24hQuote").contains(normalizedSort))throw new IllegalArgumentException("不支持的市场排序字段");
  boolean desc;
  if(direction==null||direction.isBlank()||direction.equalsIgnoreCase("asc"))desc=false;
  else if(direction.equalsIgnoreCase("desc"))desc=true;
  else throw new IllegalArgumentException("direction 必须为 asc 或 desc");
  int pageSize=limit==null?30:limit;
  if(pageSize<1||pageSize>100)throw new IllegalArgumentException("limit 必须在 1 到 100 之间");
  MarketQueryState state;
  int offset;
  if(cursor==null||cursor.isBlank()){
   var source=market();
   var measured=new HashMap<String,Market>();
   for(var row:source.data())measured.put(row.assetId(),row);
   var rows=new ArrayList<Market>();
   for(var asset:Catalog.ALL){
    var row=measured.get(asset.id());
    rows.add(row==null?catalogMarket(asset):row);
   }
   rows.removeIf(row->!normalizedFilter.isBlank()&&!marketMatches(row,normalizedFilter));
   rows.sort(marketComparator(normalizedSort,desc));
   state=new MarketQueryState(UUID.randomUUID().toString(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.copyOf(rows),source,normalizedFilter,normalizedSort,desc,pageSize);
   marketQuerySnapshots.save(new MarketQuerySnapshotStore.Snapshot(state.token(),state.snapshotAt(),state.rows(),state.source(),state.filter(),state.sort(),state.desc(),state.limit()));
   pruneMarketQuerySnapshots(state.snapshotAt());
   offset=0;
  }else{
   var decoded=decodeMarketCursor(cursor);
   var persisted=marketQuerySnapshots.load(decoded.token());
   state=persisted==null?null:new MarketQueryState(persisted.token(),persisted.snapshotAt(),persisted.rows(),persisted.source(),persisted.filter(),persisted.sort(),persisted.desc(),persisted.limit());
   if(state==null||Duration.between(state.snapshotAt(),Instant.now()).compareTo(MARKET_QUERY_SNAPSHOT_TTL)>0){
    marketQuerySnapshots.remove(decoded.token());
    throw new IllegalArgumentException("分页游标已过期，请重新查询");
   }
   if(!state.token().equals(decoded.token())||!state.filter().equals(normalizedFilter)||!state.sort().equals(normalizedSort)||state.desc()!=desc||state.limit()!=pageSize)
    throw new IllegalArgumentException("分页游标与查询条件不匹配，请重新查询");
   offset=decoded.offset();
   if(offset<0||offset>state.rows().size())throw new IllegalArgumentException("分页游标位置无效");
  }
  int end=Math.min(offset+pageSize,state.rows().size());
  String next=end<state.rows().size()?encodeMarketCursor(state.token(),state.filter(),state.sort(),state.desc(),state.limit(),end):null;
  var source=state.source();
  return new MarketQueryPage(state.rows().subList(offset,end),next,state.rows().size(),"partial",state.snapshotAt(),"Configured directory; Kraken measurements when enabled",KRAKEN,source.fetchedAt(),source.sourceAt(),"Partial configured directory: seven catalog entries; not reconciled to the market universe. Market measurements are independently source-gated.",source.status().equals("unavailable")?"仅返回当前配置目录，尚未与目标市场全集对账；行情测量来源当前不可用，缺失字段保留为 null":"仅返回当前配置目录，尚未与目标市场全集对账；行情测量按各自来源覆盖展示");
 }
 private record MarketQueryState(String token,Instant snapshotAt,List<Market> rows,Envelope<List<Market>> source,String filter,String sort,boolean desc,int limit) {}
 private record MarketCursor(String token,String filter,String sort,boolean desc,int limit,int offset){}
 private static Market catalogMarket(Catalog.Asset asset){return new Market(asset.id(),Objects.toString(asset.spotInstrument(),""),null,null,null,null,null,"USD",null);}
 private static boolean marketMatches(Market row,String filter){return row.assetId().toLowerCase(Locale.ROOT).contains(filter)||row.instrument().toLowerCase(Locale.ROOT).contains(filter);}
 private static Comparator<Market> marketComparator(String sort,boolean desc){
  java.util.function.Function<Market,String> getter=switch(sort){
   case "instrument"->Market::instrument;
   case "lastPrice"->Market::lastPrice;
   case "change24hPct"->Market::change24hPct;
   case "volume24hBase"->Market::volume24hBase;
   case "turnover24hQuote"->Market::turnover24hQuote;
   default->Market::assetId;
  };
  return (left,right)->{
   String leftValue=getter.apply(left),rightValue=getter.apply(right);
   if(leftValue==null||rightValue==null){if(leftValue==null&&rightValue!=null)return 1;if(rightValue==null&&leftValue!=null)return -1;}
   int result=compareValue(leftValue,rightValue,sort.equals("assetId")||sort.equals("instrument"));
   if(result!=0)return desc?-result:result;
   return left.assetId().compareTo(right.assetId());
  };
 }
 private static int compareValue(String left,String right,boolean text){
  if(left==null&&right==null)return 0;if(left==null)return 1;if(right==null)return -1;
  return text?left.compareTo(right):new BigDecimal(left).compareTo(new BigDecimal(right));
 }
 private void pruneMarketQuerySnapshots(Instant now){marketQuerySnapshots.prune(now,32);}
 private String encodeMarketCursor(String token,String filter,String sort,boolean desc,int limit,int offset){
  var node=mapper.createObjectNode().put("token",token).put("filter",filter).put("sort",sort).put("desc",desc).put("limit",limit).put("offset",offset);
  return Base64.getUrlEncoder().withoutPadding().encodeToString(node.toString().getBytes(StandardCharsets.UTF_8));
 }
 private MarketCursor decodeMarketCursor(String cursor){
  if(cursor.length()>2048)throw new IllegalArgumentException("分页游标过长");
  try{
   var node=mapper.readTree(Base64.getUrlDecoder().decode(cursor));
   if(!node.isObject()||node.size()!=6||!node.path("token").isTextual()||!node.path("filter").isTextual()||!node.path("sort").isTextual()||!node.path("desc").isBoolean()||!node.path("limit").canConvertToInt()||!node.path("offset").canConvertToInt())throw new IllegalArgumentException();
   return new MarketCursor(node.path("token").asText(),node.path("filter").asText(),node.path("sort").asText(),node.path("desc").asBoolean(),node.path("limit").asInt(),node.path("offset").asInt());
  }catch(Exception e){throw new IllegalArgumentException("无效市场查询游标");}
 }
 @Scheduled(fixedDelay=30000,initialDelay=2000) public void poll(){if(enabled&&krakenApproved){market();derivative();}}
 private Envelope<List<Market>> market(){
  if(!enabled||!krakenApproved)return missing("Kraken",KRAKEN,MARKET_COVERAGE,"来源未启用或自用许可评估尚未确认");
  return markets.get("spot",Duration.ofSeconds(30),()->{
   try{
    var root=http.get(KRAKEN);if(root.path("error").size()>0)throw new IllegalStateException("Kraken拒绝请求");
    var result=new ArrayList<Market>();
    for(var asset:Catalog.ALL){if(asset.spotInstrument()==null)continue;var row=root.path("result").path(asset.spotInstrument());if(!row.isMissingNode())result.add(krakenMarket(asset,row));}
    if(result.isEmpty())throw new IllegalStateException("源返回空行情");
    return new Envelope<>(result,"partial","Kraken",KRAKEN,Instant.now(),null,MARKET_COVERAGE,"滚动24h涨跌及源时间未提供；缺少的币对保持缺失",null);
   }catch(Exception e){return missing("Kraken",KRAKEN,MARKET_COVERAGE,failure(e));}
  },()->missing("Kraken",KRAKEN,MARKET_COVERAGE,"来源请求失败"));
 }
 static Market krakenMarket(Catalog.Asset a,JsonNode row){
  String last=required(row.path("c").path(0));String volume=required(row.path("v").path(1));String vwap=required(row.path("p").path(1));
  return new Market(a.id(),a.spotInstrument(),Metrics.decimal(last),null,null,Metrics.decimal(volume),Metrics.multiply(volume,vwap),"USD",null);
 }
 private Envelope<List<Derivative>> derivative(){
  if(!enabled||!krakenApproved)return missing("Kraken Futures",FUTURES,FUTURES_COVERAGE,"来源未启用或自用许可评估尚未确认");
  return derivatives.get("futures",Duration.ofSeconds(30),()->{
   try{
    var root=http.get(FUTURES);if(!root.path("result").asText().equals("success"))throw new IllegalStateException("Futures拒绝请求");
    var result=new ArrayList<Derivative>();
    for(var row:root.path("tickers"))for(var asset:Catalog.ALL)if(asset.swapInstrument()!=null&&asset.swapInstrument().equalsIgnoreCase(row.path("symbol").asText())){
     String oi=Metrics.decimal(required(row.path("openInterest")));
     result.add(new Derivative(asset.id(),row.path("symbol").asText(),null,null,oi,"source-native（单位待核实）",null));
    }
    if(result.isEmpty())throw new IllegalStateException("源返回空合约数据");
    return new Envelope<>(result,"partial","Kraken Futures",FUTURES,Instant.now(),null,FUTURES_COVERAGE,"OI单位尚待官方合约规格补证；lastTime是成交时间，不冒充OI更新时间；未推算USD名义价值",null);
   }catch(Exception e){return missing("Kraken Futures",FUTURES,FUTURES_COVERAGE,failure(e));}
  },()->missing("Kraken Futures",FUTURES,FUTURES_COVERAGE,"来源请求失败"));
 }
 public Envelope<List<Holder>> holders(String id,String cursor){
  var asset=Catalog.asset(id);String query=Cursor.query(cursor,mapper);
  String coverage="Ethereum mainnet / "+asset.symbol()+" / "+asset.contract()+"；仅当前索引分页；不表示全链排名或实体归属";
  if(asset.contract()==null)return unsupported("Blockscout",CHAIN,coverage,"此币种未接入Ethereum ERC-20持仓索引");
  String url=CHAIN+"/tokens/"+asset.contract()+"/holders"+(query.isEmpty()?"":"?"+query.substring(1));
  if(!enabled||!chainApproved)return missing("Blockscout legacy",url,coverage,"旧版API迁移与生产使用条件未解锁；默认禁用");
  return holders.get(id+":"+Objects.toString(cursor,""),Duration.ofSeconds(60),()->{
   try{
    var root=http.get(url);if(!root.path("items").isArray())throw new IllegalStateException("持仓结构不符合合同");
    var result=new ArrayList<Holder>();
    for(var row:root.path("items")){
     String raw=required(row.path("value"));String address=Catalog.address(required(row.path("address").path("hash")));
     result.add(new Holder(address,Metrics.units(raw,asset.decimals()),raw,asset.decimals(),row.path("address").path("is_contract").asBoolean()));
    }
    return new Envelope<>(result,"partial","Blockscout legacy",url,Instant.now(),null,coverage,"legacy来源；未提供持仓快照时间或稳定分页保证",Cursor.encode(root.get("next_page_params")));
   }catch(Exception e){return missing("Blockscout legacy",url,coverage,failure(e));}
  },()->missing("Blockscout legacy",url,coverage,"请求失败"));
 }
 public Envelope<List<Transfer>> transfers(String address,String id,String cursor){
  address=Catalog.address(address);var asset=Catalog.asset(id);String query=Cursor.query(cursor,mapper);
  String coverage="Ethereum ERC-20 / "+asset.symbol()+" / "+address+"；索引器已观察事件，不等于已最终确认或完整历史";
  if(asset.contract()==null)return unsupported("Blockscout",CHAIN,coverage,"此币种不在ERC-20转账覆盖范围");
  String url=CHAIN+"/addresses/"+address+"/token-transfers?type=ERC-20&token="+asset.contract()+query;
  if(!enabled||!chainApproved)return missing("Blockscout legacy",url,coverage,"旧版API迁移与生产使用条件未解锁；默认禁用");
  final String owner=address;
  return transfers.get(id+":"+address+":"+Objects.toString(cursor,""),Duration.ofSeconds(60),()->{
   try{
    var root=http.get(url);if(!root.path("items").isArray())throw new IllegalStateException("事件结构不符合合同");
    var result=new ArrayList<Transfer>();var seen=new HashMap<String,Transfer>();
    for(var row:root.path("items")){
     var event=transfer(asset,owner,row);var previous=seen.putIfAbsent(event.id(),event);
     if(previous==null)result.add(event);
     else if(!previous.equals(event))throw new IllegalStateException("来源返回冲突事件");
    }
    return new Envelope<>(result,"partial","Blockscout legacy",url,Instant.now(),null,coverage,"事件时间是区块时间；未承诺最终性或连续实时监听",Cursor.encode(root.get("next_page_params")));
   }catch(Exception e){return missing("Blockscout legacy",url,coverage,failure(e));}
  },()->missing("Blockscout legacy",url,coverage,"请求失败"));
 }
 static Transfer transfer(Catalog.Asset asset,String address,JsonNode row){
  if(!asset.contract().equalsIgnoreCase(required(row.path("token").path("address_hash"))))throw new IllegalArgumentException("源返回无关代币");
  int decimals=(int)unsignedInteger(row.path("total").path("decimals"),36);
  if(decimals!=asset.decimals())throw new IllegalArgumentException("代币精度与已核验合同不符");
  String hash=required(row.path("transaction_hash"));
  if(!hash.matches("0x[0-9a-fA-F]{64}"))throw new IllegalArgumentException("无效交易哈希");
  hash=hash.toLowerCase(Locale.ROOT);
  long log=unsignedInteger(row.path("log_index"),MAX_SAFE_INTEGER);
  long block=unsignedInteger(row.path("block_number"),MAX_SAFE_INTEGER);
  String from=Catalog.address(required(row.path("from").path("hash")));String to=Catalog.address(required(row.path("to").path("hash")));
  var value=row.path("total").path("value");
  if(!value.isTextual()||!value.asText().matches("[0-9]{1,78}"))throw new IllegalArgumentException("无效ERC-20数量");
  String raw=value.asText();
  if(new BigInteger(raw).compareTo(MAX_UINT256)>0)throw new IllegalArgumentException("ERC-20数量超出uint256");
  var timestamp=row.path("timestamp");Instant sourceAt=null;
  if(!timestamp.isMissingNode()&&!timestamp.isNull()){
   if(!timestamp.isTextual()||timestamp.asText().isBlank())throw new IllegalArgumentException("无效事件时间");
   try{sourceAt=Instant.parse(timestamp.asText());}catch(DateTimeException e){throw new IllegalArgumentException("无效事件时间",e);}
  }
  return new Transfer("ethereum:"+hash+":"+log,hash,log,block,from,to,Metrics.direction(address,from,to),Metrics.units(raw,decimals),raw,decimals,sourceAt);
 }
 private static long unsignedInteger(JsonNode value,long maximum){
  if(!value.isIntegralNumber()&&!value.isTextual())throw new IllegalArgumentException("缺少或无效整数身份");
  String text=value.asText();
  if(!text.matches("0|[1-9][0-9]{0,18}"))throw new IllegalArgumentException("无效整数身份");
  try{long number=Long.parseLong(text);if(number>maximum)throw new IllegalArgumentException("整数超出精确范围");return number;}
  catch(NumberFormatException e){throw new IllegalArgumentException("整数超出精确范围",e);}
 }
 public Envelope<List<Liquidation>> liquidations(String id){
  var asset=Catalog.asset(id);String coverage="Kraken Futures 最新100条成交中的 liquidation / partial liquidation 样本；历史上限7天或引擎最近重启以来的较短窗口；非24h完整爆仓量";
  if(asset.swapInstrument()==null)return unsupported("Kraken Futures",FUTURES,coverage,"无对应永续合约");
  String url="https://futures.kraken.com/derivatives/api/v3/history?symbol="+asset.swapInstrument();
  if(!enabled||!krakenApproved)return missing("Kraken Futures",url,coverage,"来源未启用或自用许可评估尚未确认");
  return liquidations.get(id,Duration.ofSeconds(60),()->{
   try{
    var root=http.get(url);if(!root.path("result").asText().equals("success")||!root.path("history").isArray())throw new IllegalStateException("历史事件结构不符合合同");
    var result=new ArrayList<Liquidation>();
    for(var row:root.path("history")){String type=row.path("type").asText();if(!type.equals("liquidation")&&!type.equals("partial liquidation"))continue;
     result.add(new Liquidation(required(row.path("uid")),required(row.path("sequence_id")),asset.swapInstrument(),required(row.path("side")),type,Metrics.decimal(required(row.path("size"))),"source-native（单位待核实）",Metrics.decimal(required(row.path("price"))),parseTime(row.path("time").asText(null))));
    }
    return new Envelope<>(result,"partial","Kraken Futures",url,Instant.now(),null,coverage,"仅过滤当前返回的有限样本；空样本不表示24h无爆仓；未验证连续性与数量单位",null);
   }catch(Exception e){return missing("Kraken Futures",url,coverage,failure(e));}
  },()->missing("Kraken Futures",url,coverage,"来源请求失败"));
 }
 public List<Map<String,Object>> sources(){return List.of(
  source("CoinLore","https://www.coinlore.com/cryptocurrency-data-api","https://www.coinlore.com/terms-of-use",false,"行情样本可获取；条款要求再分发书面同意，尚未解锁；JCT供应量、链合约映射与行情时点仍缺失"),
  source("MEXC candidate","https://mexcdevelop.github.io/apidocs/contract_v1_en/",null,false,"已按旧版官方文档纠正探测地址；Java与curl均未连通，JCT合约样本及使用条件未核验"),
  source("Routescan","https://docs.routescan.io/","https://routescan.io/terms-and-conditions",false,"历史事件及日志已实测；连续覆盖、独立对账及对外展示/存储许可未齐，不自动启用"),
  source("Kraken","https://docs.kraken.com/api/","https://www.kraken.com/legal",enabled&&krakenApproved,"仅本机自用评估；对第三方展示/再分发须另行核验许可"),
  source("Blockscout legacy","https://docs.blockscout.com/devs/apis/rest","https://eaas.blockscout.com/terms-and-conditions",enabled&&chainApproved,"旧接口生产条件未解锁；统一API需要验证Key、额度与使用许可"),
  source("DeBank Cloud","https://docs.cloud.debank.com/en/readme/api-pro-reference/user.md","https://docs.cloud.debank.com/en/terms-of-service.md",false,"history_list地址历史接口需AccessKey；免费试用最多14天，条款第4(m)限制分享账户和数据，外部展示、存储、再分发及衍生指标许可未确认"),
  source("CoinDesk Data API","https://developers.coindesk.com/documentation",null,false,"官方页面静态壳可访问，但免费计划、API专属条款、额度与字段未核实；外部展示授权未知，浏览器文档访问超时"),
  source("OKX (excluded)","https://www.okx.com/docs-v5/en/","https://www.okx.com/zh-hans/help/okx-api-agreement",false,"API协议限制聚合/第三方展示；本系统不调用"));}
 private Map<String,Object> source(String name,String docs,String terms,boolean active,String gap){
  var result=new LinkedHashMap<String,Object>(Map.of("name",name,"documentation",docs,"enabled",active,"productionApproved",false,"gap",gap));
  result.put("terms",terms);
  var review=SourceReviews.get(name);if(review!=null)result.put("verification",review);
  return Collections.unmodifiableMap(result);
 }
 private static String required(JsonNode value){if(value.isMissingNode()||value.isNull()||value.asText().isBlank())throw new IllegalArgumentException("缺少来源必需字段");return value.asText();}
 private static Instant parseTime(String text){if(text==null||text.isBlank())return null;try{return Instant.parse(text);}catch(Exception e){return null;}}
 private static String failure(Exception e){return e instanceof IllegalStateException?e.getMessage():"来源请求失败："+e.getClass().getSimpleName();}
 private static <T> Envelope<List<T>> missing(String source,String url,String coverage,String reason){return new Envelope<>(List.of(),"unavailable",source,url,null,null,coverage,reason,null);}
 private static <T> Envelope<List<T>> unsupported(String source,String url,String coverage,String reason){return new Envelope<>(List.of(),"unsupported",source,url,null,null,coverage,reason,null);}
}
