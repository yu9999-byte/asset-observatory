package io.observatory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class Workspace {
 private final JdbcTemplate db;
 public Workspace(JdbcTemplate db){this.db=db;}
 String owner() {
  var principal=SecurityContextHolder.getContext().getAuthentication();
  if(principal==null||!principal.isAuthenticated()||principal instanceof AnonymousAuthenticationToken)return "local-workspace";
  try{return "user:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(principal.getName().getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 public record Watch(String kind,String key,String createdAt) {}
 public record Note(String kind,String key,String text,boolean visible,long version,String updatedAt,String projectName,String projectInfo) {}
 public record NoteInput(@NotNull @Size(max=4000) String text,@NotNull Boolean visible,@NotNull @Min(0) Long version,
  @Size(max=120) String projectName,@Size(max=4000) String projectInfo) {}
 private static Note readNote(java.sql.ResultSet rs,int row) throws java.sql.SQLException {
  return new Note(rs.getString("kind"),rs.getString("entity_key"),rs.getString("body"),rs.getBoolean("visible"),
   rs.getLong("version"),rs.getString("updated_at"),rs.getString("project_name"),rs.getString("project_info"));
 }
 public synchronized Map<String,Object> read() {
  String owner=owner();
  return Map.of("watches",db.query("SELECT * FROM watches WHERE owner_id=? ORDER BY created_at,entity_key",(rs,n)->new Watch(rs.getString("kind"),rs.getString("entity_key"),rs.getString("created_at")),owner),
   "notes",db.query("SELECT * FROM notes WHERE owner_id=? ORDER BY updated_at,entity_key",Workspace::readNote,owner));
 }
 @Transactional public synchronized Watch watch(String kind,String key) {
  key=Catalog.key(kind,key);
  String owner=owner();
  List<Watch> old=db.query("SELECT * FROM watches WHERE owner_id=? AND kind=? AND entity_key=?",(rs,n)->new Watch(kind,rs.getString("entity_key"),rs.getString("created_at")),owner,kind,key);
  if(!old.isEmpty())return old.getFirst();
  if(db.queryForObject("SELECT COUNT(*) FROM watches WHERE owner_id=? AND kind=?",Integer.class,owner,kind)>=20)throw new ResponseStatusException(HttpStatus.CONFLICT,"每类最多关注20项");
  var result=new Watch(kind,key,Instant.now().toString());
  db.update("INSERT INTO watches(owner_id,kind,entity_key,created_at) VALUES(?,?,?,?)",owner,kind,key,result.createdAt());return result;
 }
 @Transactional public synchronized void unwatch(String kind,String key){db.update("DELETE FROM watches WHERE owner_id=? AND kind=? AND entity_key=?",owner(),kind,Catalog.key(kind,key));}
 @Transactional public synchronized Note note(String kind,String key,NoteInput input) {
  key=Catalog.key(kind,key);String time=Instant.now().toString();
  String owner=owner();
  if(input.version()==0){
   if(db.queryForObject("SELECT COUNT(*) FROM notes WHERE owner_id=?",Integer.class,owner)>=1000)throw new ResponseStatusException(HttpStatus.CONFLICT,"备注容量已满");
   try{db.update("INSERT INTO notes(owner_id,kind,entity_key,body,visible,version,updated_at,project_name,project_info) VALUES(?,?,?,?,?,1,?,?,?)",owner,kind,key,input.text(),input.visible(),time,
    input.projectName()==null?"":input.projectName(),input.projectInfo()==null?"":input.projectInfo());}
   catch(org.springframework.dao.DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"备注已更新，请重新加载");}
  }else{
   int changed=db.update("UPDATE notes SET body=?,visible=?,project_name=COALESCE(?,project_name),project_info=COALESCE(?,project_info),version=version+1,updated_at=? WHERE owner_id=? AND kind=? AND entity_key=? AND version=?",
    input.text(),input.visible(),input.projectName(),input.projectInfo(),time,owner,kind,key,input.version());
   if(changed==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"备注已更新，请重新加载");
  }
  return db.queryForObject("SELECT * FROM notes WHERE owner_id=? AND kind=? AND entity_key=?",Workspace::readNote,owner,kind,key);
 }
}
