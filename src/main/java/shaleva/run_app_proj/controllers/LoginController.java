package shaleva.run_app_proj.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import shaleva.run_app_proj.datamodels.User;
import shaleva.run_app_proj.services.LoginService;

@RestController
public class LoginController {
    private LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }
    
    @PostMapping("api/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        User isUserExist = loginService.getUserFromDB(user);
        return ResponseEntity.ok(isUserExist);

    }

}
