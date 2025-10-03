package cola.springboot.cocal.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping()
    public User signUp(@RequestBody User user) {
        return userService.signUp(user);
    }

    // 비밀번호 수정
    @PutMapping("/edit-pwd")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        userService.changePasswordById(userId, currentPassword, newPassword);

        // 메시지를 Map으로 만들어서 반환
        return ResponseEntity.ok(Collections.singletonMap("message", "비밀번호가 변경되었습니다."));
    }

    // name 수정
    @PutMapping("/edit-name")
    public ResponseEntity<Map<String, Object>> changeName(@RequestBody Map<String, String> body,
                                                          Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        String newName = body.get("name");

        User updated = userService.changeName(userId, newName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", updated.getId());
        response.put("name", updated.getName());
        response.put("updatedAt", updated.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    // 회원 탈퇴
    @DeleteMapping("/delete")
    public String deleteUser(Authentication authentication){
        Long userId = (Long) authentication.getPrincipal();
        return userService.deleteUserById(userId);
    }

    // defaultView 수정
    @PutMapping("/view")
    public ResponseEntity<Map<String, String>> updateDefaultView(
            @RequestBody Map<String, String> body,
            Authentication authentication){

        Long userId = Long.parseLong(authentication.getName());
        String viewStr = body.get("defaultView");

        User.DefaultView view;
        try {
            view = User.DefaultView.valueOf(viewStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("잘못된 뷰 입니다. (DAY, WEEK, MONTH 중 선택)");
        }

        userService.updateDefaultView(userId, view);

        return ResponseEntity.ok(
                Map.of("message", "가본 뷰가 " + view + "으로 변경되었습니다.")
        );
    }

    // 회원 조회(me)
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Map<String, Object> response = userService.getMe(userId); // Service 호출
        return ResponseEntity.ok(response);
    }
}
