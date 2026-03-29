package shaleva.run_app_proj.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import shaleva.run_app_proj.datamodels.User;
import shaleva.run_app_proj.services.UserService;

@RestController
public class LoginAndSignupController {
    private UserService userService;

    public LoginAndSignupController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("api/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        User isUserExist = userService.getUserFromDB(user);
        if (isUserExist != null) return ResponseEntity.ok(isUserExist);
        // user do not found
        return ResponseEntity.status(401).build();
    }

    @PostMapping("api/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        User isUserExist = userService.getUserFromDB(user);
        if (isUserExist == null) {
            try {
                userService.insertUser(user);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                // internal server error response
                return ResponseEntity.status(500).build();
            }

            return ResponseEntity.ok("User has signed up successfuly");
        }

        return ResponseEntity.status(409).build();
    }
    

}
