package user_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import user_service.entity.User;
import user_service.exception.UserNotFoundException;
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
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()->new UserNotFoundException(id));
    }
    public User updateUser(Long id, User updatedUser) {

    User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

    existingUser.setName(updatedUser.getName());
    existingUser.setEmail(updatedUser.getEmail());
    existingUser.setPhone(updatedUser.getPhone());
    existingUser.setPassword(updatedUser.getPassword());
    existingUser.setStatus(updatedUser.getStatus());

    return userRepository.save(existingUser);
    }
    public void deleteUser(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

    userRepository.delete(user);
}
}
