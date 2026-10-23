package io.observatory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Set;
public final class Cursor {
 private Cursor() {}
 private static final Set<String> ALLOWED=Set.of("value","address_hash","items_count","index","token","block_number","inserted_at","hash");
 public static String encode(JsonNode node) {return node==null||node.isNull()?null:Base64.getUrlEncoder().withoutPadding().encodeToString(node.toString().getBytes(StandardCharsets.UTF_8));}
 public static String query(String cursor,ObjectMapper mapper) {
  if(cursor==null||cursor.isBlank())return "";
  if(cursor.length()>2048)throw new IllegalArgumentException("分页游标过长");
  try {
   JsonNode node=mapper.readTree(Base64.getUrlDecoder().decode(cursor));
   if(!node.isObject()||node.size()>8)throw new IllegalArgumentException("无效分页游标");
   var result=new StringBuilder();
   var fields=node.fields();
   while(fields.hasNext()) {
    var field=fields.next();String k=field.getKey();String v=field.getValue().asText();
    if(!ALLOWED.contains(k)||!field.getValue().isValueNode()||v.length()>160||!v.matches("[a-zA-Z0-9xX.:-]+"))throw new IllegalArgumentException("不允许的分页参数");
    result.append('&').append(k).append('=').append(URLEncoder.encode(v,StandardCharsets.UTF_8));
   }
   return result.toString();
  }catch(Exception e){throw new IllegalArgumentException("无效分页游标");}
 }
}
