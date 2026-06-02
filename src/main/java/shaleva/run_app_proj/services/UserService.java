package shaleva.run_app_proj.services;

import java.util.ArrayList;
import org.springframework.stereotype.Service;
import shaleva.run_app_proj.datamodels.User;
import shaleva.run_app_proj.repositories.UserRepository;
import shaleva.run_app_proj.utilities.PasswordHelper;

@Service
public class UserService {
    private UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public void insertUser(User user) throws Exception {
        if (userRepo.existsByEmailAndPassword(user.getEmail(), user.getPassword()))
            throw new Exception("User allready exists");

        // user.setPassword(PasswordHelper.encode(user.getPassword()));
        userRepo.insert(user);
    }

    public ArrayList<User> getAllUsers() {
        return (ArrayList) this.userRepo.findAll();
    }

    public void deleteUser(User user) {
        userRepo.delete(user);
    }

    public User getUserFromDB(User user) {
        User queryResult = userRepo.findByEmailAndPassword(user.getEmail(), user.getPassword());
        // if (!PasswordHelper.match(user.getPassword(), queryResult.getPassword())) return null;
        return queryResult;
    }

    public boolean existsByEmail(String email) {
        User result = userRepo.findByEmail(email);
        return result != null;
    }

    public User updateUserLastLocation(String userId, double latitude, double longitude) {
        User user = userRepo.findByEmail(userId);
        user.setLastLat(latitude);
        user.setLastLon(longitude);
        userRepo.save(user);
        return user;
    }
}
