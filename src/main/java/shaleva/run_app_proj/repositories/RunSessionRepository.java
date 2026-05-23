package shaleva.run_app_proj.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import shaleva.run_app_proj.datamodels.RunSession;

import java.util.List;
import java.util.Optional;

// בתוך RunSessionRepository.java
public interface RunSessionRepository extends MongoRepository<RunSession, String> {
    
    // שליפת כל הריצות של משתמש מסוים ומיונן לפי זמן התחלה מהחדש לישן
    List<RunSession> findByUserIdOrderByStartTimeDesc(String userId);
}
