package io.observatory;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.Set;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SourceHttp {
 private static final Logger LOG=LoggerFactory.getLogger(SourceHttp.class);
 private static final Set<String> HOSTS=Set.of("api.kraken.com","futures.kraken.com","eth.blockscout.com");
 private final HttpClient client;
 private final ObjectMapper mapper;
 private final Semaphore slots=new Semaphore(2,true);
 private final Object pacing=new Object();
 private final Duration budget,interval;
 private long nextStart=System.nanoTime();
 @Autowired
 public SourceHttp(ObjectMapper mapper,@Value("${app.proxy:}")String proxy){
  this(mapper,client(proxy),Duration.ofSeconds(10),Duration.ofSeconds(1));
 }
 SourceHttp(ObjectMapper mapper,HttpClient client,Duration budget,Duration interval){
  this.mapper=mapper;this.client=client;this.budget=budget;this.interval=interval;
 }
 static HttpClient client(String proxy){
  var builder=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER);
  if(!proxy.isBlank()){
   URI uri=URI.create(proxy);
   if(!"http".equals(uri.getScheme())||uri.getHost()==null||uri.getPort()<1||uri.getUserInfo()!=null)throw new IllegalArgumentException("APP_PROXY必须是无凭据HTTP代理URL");
   builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(),uri.getPort())));
  }
  return builder.build();
 }
 public JsonNode get(String url) throws Exception {
  URI uri=URI.create(url);
  if(!"https".equals(uri.getScheme())||!HOSTS.contains(uri.getHost())||uri.getUserInfo()!=null
   ||uri.getFragment()!=null||(uri.getPort()!=-1&&uri.getPort()!=443))throw new IllegalArgumentException("源地址不在白名单");
  long begin=System.nanoTime(),deadline=begin+budget.toNanos();
  boolean acquired;
  try{acquired=slots.tryAcquire(Math.min(budget.toNanos(),Duration.ofMillis(100).toNanos()),TimeUnit.NANOSECONDS);}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw e;}
  if(!acquired)
   throw new IllegalStateException("来源并发已满，请稍后重试");
  CompletableFuture<HttpResponse<byte[]>> pending=null;
  try {
   synchronized(pacing){
    long delay=nextStart-System.nanoTime();
    if(delay>=remaining(deadline))throw new TimeoutException();
    if(delay>0)TimeUnit.NANOSECONDS.sleep(delay);
    nextStart=System.nanoTime()+interval.toNanos();
   }
   pending=client.sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofNanos(remaining(deadline)))
    .header("Accept","application/json").header("User-Agent","AssetObservatory/0.1 personal-research")
    .GET().build(),info->new LimitedBody(4_000_000));
   var response=pending.get(remaining(deadline),TimeUnit.NANOSECONDS);
   if(response.statusCode()!=200)throw new IllegalStateException("源返回HTTP "+response.statusCode());
   var result=mapper.readTree(response.body());
   remaining(deadline);
   LOG.info("source_request host={} status={} elapsedMs={}",uri.getHost(),response.statusCode(),(System.nanoTime()-begin)/1_000_000);
   return result;
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw e;}
  catch(Exception e){LOG.warn("source_failure host={} type={}",uri.getHost(),e.getClass().getSimpleName());throw e;}
  finally{if(pending!=null&&!pending.isDone())pending.cancel(true);slots.release();}
 }
 private static long remaining(long deadline) throws TimeoutException {
  long nanos=deadline-System.nanoTime();if(nanos<=0)throw new TimeoutException("Source request deadline exceeded");return nanos;
 }
 static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
  private final HttpResponse.BodySubscriber<byte[]> delegate=HttpResponse.BodySubscribers.ofByteArray();
  private final long limit;
  private Flow.Subscription subscription;
  private long received;
  private boolean done;
  LimitedBody(long limit){this.limit=limit;}
  public CompletionStage<byte[]> getBody(){return delegate.getBody();}
  public void onSubscribe(Flow.Subscription subscription){this.subscription=subscription;delegate.onSubscribe(subscription);}
  public void onNext(List<ByteBuffer> buffers){
   if(done)return;
   for(var buffer:buffers)received+=buffer.remaining();
   if(received>limit){done=true;subscription.cancel();delegate.onError(new IllegalStateException("来源响应超过字节限制"));}
   else delegate.onNext(buffers);
  }
  public void onError(Throwable error){if(!done){done=true;delegate.onError(error);}}
  public void onComplete(){if(!done){done=true;delegate.onComplete();}}
 }
}
