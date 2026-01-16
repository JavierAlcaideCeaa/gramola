package edu.uclm.esi.gramolaJavier.Dao;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;


import edu.uclm.esi.gramolaJavier.models.User;

@Repository

public interface userDao extends JpaRepository<User, String> {
    User findByEmail(String email);
   
    User findByEmailAndPassword(String email, String password);
    /*User findByCreationToken(Token token);
    User findByEmailAndCreationToken(String email, Token token);
    User findByCreationTokenAndPassword(Token token, String password);
    User findByEmailAndCreationTokenAndPassword(String email, Token token, String password); */

}
