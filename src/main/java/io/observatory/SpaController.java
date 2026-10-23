package io.observatory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
class SpaController {
 @GetMapping({"/","/assets/{id:[A-Z][A-Z0-9_-]*}","/addresses/{address}","/watchlist","/sources"})String index(){return "forward:/index.html";}
}
