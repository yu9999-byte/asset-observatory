package io.observatory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TablePreferences {
 private static final Map<String,List<String>> COLUMNS = Map.of(
  "markets", List.of("asset","price","change","marketCap","fdv","volume","turnover","oi","funding","coverage","actions"),
  "holders", List.of("address","chain","balance","proportion","value","change-1H","change-24H","change-3D","change-7D","change-1M","change-1Y","type","actions"),
  "transfers", List.of("time","direction","from","to","amount","price","value","counterparty","hash","block"),
  "liquidations", List.of("time","type","side","size","price","sequence"),
  "watches", List.of("kind","key","created","action"));

 public record Settings(List<String> hidden,List<String> order,String sortKey,boolean desc,int pageSize) {}
 public record Input(@NotNull @Size(max=16) List<@NotNull String> hidden,
                     @NotNull @Size(max=16) List<@NotNull String> order,
                     @Size(max=32) String sortKey,@NotNull Boolean desc,
                     @NotNull Integer pageSize,@NotNull @Min(0) Long version) {}
 public record Preference(String tableId,List<String> hidden,List<String> order,String sortKey,
                          boolean desc,int pageSize,long version,String updatedAt) {}

 private final JdbcTemplate db;
 private final Workspace workspace;
 private final ObjectMapper json;

 public TablePreferences(JdbcTemplate db,Workspace workspace,ObjectMapper json) {
  this.db=db; this.workspace=workspace; this.json=json;
 }

 private List<String> columns(String tableId) {
  var columns=COLUMNS.get(tableId);
  if(columns==null) throw new IllegalArgumentException("未知表格类型");
  return columns;
 }

 public Preference read(String tableId) {
  var columns=columns(tableId);
  var matches=db.query("SELECT config,version,updated_at FROM table_preferences WHERE owner_id=? AND table_id=?",
   (rs,n)->map(tableId,rs),workspace.owner(),tableId);
  return matches.isEmpty()?new Preference(tableId,List.of(),columns,null,false,15,0,null):matches.getFirst();
 }

 private Preference map(String tableId,ResultSet row) throws SQLException {
  try {
   var settings=json.readValue(row.getString("config"),Settings.class);
   var columns=columns(tableId);
   var order=new java.util.ArrayList<>(settings.order().stream().filter(columns::contains).distinct().toList());
   for(String key:columns)if(!order.contains(key))order.add(key);
   var hidden=new java.util.ArrayList<>(settings.hidden().stream().filter(columns::contains).distinct().toList());
   if(hidden.size()==columns.size())hidden.remove(order.getFirst());
   var normalized=new Settings(List.copyOf(hidden),List.copyOf(order),settings.sortKey()!=null&&columns.contains(settings.sortKey())?settings.sortKey():null,settings.desc(),settings.pageSize());
   return preference(tableId,normalized,row.getLong("version"),row.getString("updated_at"));
  } catch(JsonProcessingException e) { throw new IllegalStateException("Invalid stored table preferences",e); }
 }

 private Preference preference(String tableId,Settings settings,long version,String updatedAt) {
  return new Preference(tableId,settings.hidden(),settings.order(),settings.sortKey(),settings.desc(),settings.pageSize(),version,updatedAt);
 }

 @Transactional
 public Preference save(String tableId,Input input) {
  var columns=columns(tableId);
  if(input.order().size()!=columns.size() || !new HashSet<>(input.order()).equals(new HashSet<>(columns))
   || input.hidden().size()!=new HashSet<>(input.hidden()).size() || !columns.containsAll(input.hidden())
   || input.hidden().size()>=columns.size() || (input.sortKey()!=null&&!columns.contains(input.sortKey()))
   || !List.of(15,30,60).contains(input.pageSize()) || input.version()==Long.MAX_VALUE)
   throw new IllegalArgumentException("表格列配置、每页条数或版本无效");
  var settings=new Settings(List.copyOf(input.hidden()),List.copyOf(input.order()),input.sortKey(),input.desc(),input.pageSize());
  String payload;
  try { payload=json.writeValueAsString(settings); }
  catch(JsonProcessingException e) { throw new IllegalStateException(e); }
  String time=Instant.now().toString();
  String owner=workspace.owner();
  if(input.version()==0) {
   try { db.update("INSERT INTO table_preferences(owner_id,table_id,config,version,updated_at) VALUES(?,?,?,1,?)",owner,tableId,payload,time); }
   catch(DuplicateKeyException e) { throw conflict(); }
  } else {
   int changed=db.update("UPDATE table_preferences SET config=?,version=version+1,updated_at=? WHERE owner_id=? AND table_id=? AND version=?",
    payload,time,owner,tableId,input.version());
   if(changed==0) throw conflict();
  }
  return preference(tableId,settings,input.version()+1,time);
 }

 private ResponseStatusException conflict() {
  return new ResponseStatusException(HttpStatus.CONFLICT,"表格配置已更新，请重新读取后再修改");
 }
}

@RestController
@RequestMapping("/api/table-preferences")
class TablePreferenceController {
 private final TablePreferences preferences;
 TablePreferenceController(TablePreferences preferences) { this.preferences=preferences; }
 @GetMapping("/{tableId}") Object read(@PathVariable String tableId) { return preferences.read(tableId); }
 @PutMapping("/{tableId}") Object save(@PathVariable String tableId,@Valid @RequestBody TablePreferences.Input input) {
  return preferences.save(tableId,input);
 }
}
