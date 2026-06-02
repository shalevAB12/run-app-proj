package shaleva.run_app_proj.utilities;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHelper {

    public static String encode(String password) {
        String encoded = null;
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        passwordEncoder.encode(password);
        return encoded;
    }

    public static boolean match(String passwordLogin, String passwordDB) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(passwordLogin, passwordDB);
    }
}
