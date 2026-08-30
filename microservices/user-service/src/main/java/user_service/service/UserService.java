package user_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import user_service.model.User;
import user_service.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }
    public List<User> getUsers() {
        return userRepository.findAll();
    }
}
