package io.observatory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.validation.Valid;
import java.util.Map;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController
@RequestMapping("/api")
class ApiController {
 private final ResearchService research;private final Workspace workspace;private final AccountManagement accounts;
 ApiController(ResearchService research,Workspace workspace,AccountManagement accounts){this.research=research;this.workspace=workspace;this.accounts=accounts;}
 @GetMapping("/csrf") Object csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @PostMapping("/accounts") @ResponseStatus(HttpStatus.CREATED) Object register(@Valid @RequestBody CreateAccountRequest request){return accounts.create(request);}
 @GetMapping("/markets") Object markets(){return research.markets();}
 @GetMapping("/markets/query") Object marketQuery(@RequestParam(required=false)String q,@RequestParam(required=false)String sort,@RequestParam(required=false)String direction,@RequestParam(required=false)Integer limit,@RequestParam(required=false)String cursor){return research.marketQuery(q,sort,direction,limit,cursor);}
 @GetMapping("/assets/{id}/holders")Object holders(@PathVariable String id,@RequestParam(required=false)String cursor){return research.holders(id,cursor);}
 @GetMapping("/addresses/{address}/transfers")Object transfers(@PathVariable String address,@RequestParam String asset,@RequestParam(required=false)String cursor){return research.transfers(address,asset,cursor);}
 @GetMapping("/assets/{id}/liquidations")Object liquidations(@PathVariable String id){return research.liquidations(id);}
 @GetMapping("/sources")Object sources(){return research.sources();}
 @GetMapping("/workspace")Object workspace(){return workspace.read();}
 @PutMapping("/watches/{kind}/{key}")Object watch(@PathVariable String kind,@PathVariable String key){return workspace.watch(kind,key);}
 @DeleteMapping("/watches/{kind}/{key}")@ResponseStatus(HttpStatus.NO_CONTENT)void unwatch(@PathVariable String kind,@PathVariable String key){workspace.unwatch(kind,key);}
 @PutMapping("/notes/{kind}/{key}")Object note(@PathVariable String kind,@PathVariable String key,@Valid @RequestBody Workspace.NoteInput input){return workspace.note(kind,key,input);}
 @GetMapping("/admin/accounts") @PreAuthorize("hasRole('ADMIN')") Object accounts(){return accounts.list();}
 @PostMapping("/admin/accounts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") Object createAccount(@Valid @RequestBody CreateAccountRequest request){return accounts.create(request);}
 @PutMapping("/admin/accounts/{username}/password") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") void resetPassword(@PathVariable String username,@Valid @RequestBody PasswordResetRequest request){accounts.resetPassword(username,request);}
 @PutMapping("/admin/accounts/{username}/enabled") @PreAuthorize("hasRole('ADMIN')") Object accountEnabled(@PathVariable String username,@Valid @RequestBody AccountEnabledRequest request,Principal actor){return accounts.setEnabled(username,request,actor.getName());}
}
