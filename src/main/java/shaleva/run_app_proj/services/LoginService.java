package shaleva.run_app_proj.services;

import org.springframework.stereotype.Service;

import shaleva.run_app_proj.datamodels.User;
import shaleva.run_app_proj.repositories.UserRepository;

@Service
public class LoginService {
    private UserRepository userRepo;

    public LoginService(UserRepository userRepository) {
        this.userRepo = userRepository;
    }

    public User getUserFromDB(User user) {
        User queryResult = userRepo.findByUsernameAndPassword(user.getUsername(), user.getPassword());
        return queryResult;
    }

}
