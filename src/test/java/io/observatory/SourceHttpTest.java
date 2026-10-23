package io.observatory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.*;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SourceHttpTest {
 @Test void saturationRejectsThirdCallerAndSuccessfulRequestsParseJson() throws Exception {
  var client=mock(HttpClient.class);var entered=new CountDownLatch(2);
  var pending=new ConcurrentLinkedQueue<CompletableFuture<HttpResponse<byte[]>>>();
  doAnswer(invocation->{var response=new CompletableFuture<HttpResponse<byte[]>>();pending.add(response);entered.countDown();return response;})
   .when(client).sendAsync(any(),any());
  var http=new SourceHttp(new ObjectMapper(),client,Duration.ofSeconds(3),Duration.ZERO);
  HttpResponse<byte[]> response=mock(HttpResponse.class);
  when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{\"verified\":true}".getBytes(StandardCharsets.UTF_8));
  try(var workers=Executors.newFixedThreadPool(2)) {
   var first=workers.submit(()->http.get("https://api.kraken.com/0/public/Ticker"));
   var second=workers.submit(()->http.get("https://api.kraken.com/0/public/Ticker"));
   try {
    assertTrue(entered.await(1,TimeUnit.SECONDS));
    long start=System.nanoTime();
    var error=assertThrows(IllegalStateException.class,()->http.get("https://api.kraken.com/0/public/Ticker"));
    assertTrue(error.getMessage().contains("并发已满"));
    assertTrue(Duration.ofNanos(System.nanoTime()-start).toMillis()<1000);
    verify(client,times(2)).sendAsync(any(),any());
   } finally {pending.forEach(future->future.complete(response));}
   assertTrue(first.get(1,TimeUnit.SECONDS).path("verified").asBoolean());
   assertTrue(second.get(1,TimeUnit.SECONDS).path("verified").asBoolean());
  }
 }

 @Test void pacingConsumesTheTotalDeadlineBeforeSendingAgain() throws Exception {
  var client=mock(HttpClient.class);
  HttpResponse<byte[]> response=mock(HttpResponse.class);
  when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
  doReturn(CompletableFuture.completedFuture(response)).when(client).sendAsync(any(),any());
  var http=new SourceHttp(new ObjectMapper(),client,Duration.ofMillis(80),Duration.ofSeconds(1));
  http.get("https://api.kraken.com/0/public/Ticker");
  assertThrows(TimeoutException.class,()->http.get("https://api.kraken.com/0/public/Ticker"));
  verify(client,times(1)).sendAsync(any(),any());
 }
 @Test void fullBodyDeadlineCancelsRequestAndReleasesSlot() throws Exception {
  var client=mock(HttpClient.class);
  var first=new CompletableFuture<>();
  var second=new CompletableFuture<>();
  doReturn(first,second).when(client).sendAsync(any(),any());
  var http=new SourceHttp(new ObjectMapper(),client,Duration.ofMillis(80),Duration.ZERO);
  long start=System.nanoTime();
  assertThrows(Exception.class,()->http.get("https://api.kraken.com/0/public/Ticker"));
  assertTrue(first.isCancelled());
  assertThrows(Exception.class,()->http.get("https://api.kraken.com/0/public/Ticker"));
  assertTrue(second.isCancelled());
  assertTrue(Duration.ofNanos(System.nanoTime()-start).toMillis()<1500);
 }

 @Test void invalidTargetsNeverReachTransport() {
  var client=mock(HttpClient.class);
  var http=new SourceHttp(new ObjectMapper(),client,Duration.ofSeconds(1),Duration.ZERO);
  for(String url:List.of("http://api.kraken.com/x","https://api.kraken.com:8443/x",
   "https://user@api.kraken.com/x","https://api.kraken.com/x#part","https://example.com/x")) {
   assertThrows(IllegalArgumentException.class,()->http.get(url),url);
  }
  verifyNoInteractions(client);
 }

 @Test void streamingBodyLimitCancelsBeforeAccumulatingOversizeData() {
  var body=new SourceHttp.LimitedBody(8);
  var subscription=mock(Flow.Subscription.class);
  body.onSubscribe(subscription);
  body.onNext(List.of(ByteBuffer.wrap(new byte[5])));
  body.onNext(List.of(ByteBuffer.wrap(new byte[4])));
  assertThrows(Exception.class,()->body.getBody().toCompletableFuture().join());
  verify(subscription).cancel();
 }
}
