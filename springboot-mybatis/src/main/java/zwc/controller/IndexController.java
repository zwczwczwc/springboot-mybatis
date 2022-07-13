package zwc.controller;

/**
 * @author zwc
 * 2022-06-29
 * 14:25
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
    @GetMapping(value = {"", "/", "/index"})
    public String index() {
        return "index";
    }
}
