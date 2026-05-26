package shaleva.run_app_proj.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import shaleva.run_app_proj.datamodels.User;
import shaleva.run_app_proj.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class LoginAndSignupController {
    private UserService userService;

    public LoginAndSignupController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        User isUserExist = userService.getUserFromDB(user);
        if (isUserExist != null)
            return ResponseEntity.ok(isUserExist);
        // user do not found
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        User isUserExist = userService.getUserFromDB(user);
        if (isUserExist == null) {
            try {
                user.setCreatedAt();
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

    @GetMapping("/check")
    public ResponseEntity<Void> checkUserExists(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);

        if (exists) {
            return ResponseEntity.ok().build(); // 200 OK
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
        }
    }
}
