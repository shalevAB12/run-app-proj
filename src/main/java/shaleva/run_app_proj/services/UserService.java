package shaleva.run_app_proj.services;

import org.springframework.stereotype.Service;

import shaleva.run_app_proj.datamodels.User;
import shaleva.run_app_proj.repositories.UserRepository;

@Service
public class UserService {
    private UserRepository userRepo;
    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public void insertUser(User user) throws Exception {
        if (userRepo.existsById(user.getUsername()))
            throw new Exception("User allready exists");
        userRepo.insert(user);
    }
}
