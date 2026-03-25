package shaleva.run_app_proj.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import shaleva.run_app_proj.datamodels.User;

@Repository
public interface LoginRepository extends MongoRepository<User, String>{
}
