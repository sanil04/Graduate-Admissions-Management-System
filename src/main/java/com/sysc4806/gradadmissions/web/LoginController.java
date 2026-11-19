package com.sysc4806.gradadmissions.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Shows the login form. Spring Security itself handles POST /login and POST /logout. */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
