package cola.springboot.cocal.user;

import cola.springboot.cocal.common.api.ApiResponse;
import cola.springboot.cocal.common.exception.BusinessException;
import cola.springboot.cocal.s3.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final S3Service s3Service;

    // 일반 회원가입
    @PostMapping()
    public ResponseEntity<ApiResponse<User>> signUp(@RequestBody User user, HttpServletRequest httpReq) {
        User data = userService.signUp(user);
        return ResponseEntity.ok(ApiResponse.ok(data,httpReq.getRequestURI()));
    }

    // 비밀번호 수정
    @PutMapping("/edit-pwd")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @RequestBody Map<String, String> body, HttpServletRequest httpReq,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        userService.changePasswordById(userId, currentPassword, newPassword);

        // 메시지를 Map으로 만들어서 반환
        Map<String, String> data = Collections.singletonMap("message", "비밀번호가 변경되었습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // name 수정
    @PutMapping("/edit-name")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeName(@RequestBody Map<String, String> body,
                                                          Authentication authentication, HttpServletRequest httpReq) {
        Long userId = Long.parseLong(authentication.getName());
        String newName = body.get("name");

        User updated = userService.changeName(userId, newName);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", updated.getId());
        data.put("name", updated.getName());
        data.put("updatedAt", updated.getUpdatedAt());

        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 유저 프로필 사진
    @PutMapping("/profile-image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadProfileImage(Authentication authentication,
                                                                               HttpServletRequest httpReq,
                                                     @RequestParam("image") MultipartFile image) throws Exception {
        Long userId = Long.parseLong(authentication.getName());

        String oldUrl = userService.getProfileImageUrl(userId);

        // 기존 이미지 삭제
        userService.deleteProfileImage(userId);

        // 새 이미지 업로드
        String fileName = "user-" + userId + "_" + UUID.randomUUID();
        String imageUrl = s3Service.uploadProfileImage(image, fileName);

        userService.updateProfileImage(userId, imageUrl); // DB에 URL 저장

        s3Service.deleteIfOurS3Url(oldUrl);

        Map<String, Object> data = Collections.singletonMap("imageUrl", imageUrl);
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 유저 프로필 사진 삭제
    @DeleteMapping("/profile-image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteProfileImage(Authentication authentication
            , HttpServletRequest httpReq) {
        Long userId = Long.parseLong(authentication.getName());

        userService.deleteProfileImage(userId);
        userService.updateProfileImage(userId, null); // DB에서 URL 제거
        Map<String, Object> data = Collections.singletonMap("message", "프로필 이미지가 삭제되었습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 회원 탈퇴
    @DeleteMapping("/delete")
    public  ResponseEntity<ApiResponse<Map<String, String>>> deleteUser(Authentication authentication,
                                                         HttpServletRequest httpReq){
        Long userId = (Long) authentication.getPrincipal();
        userService.deleteUserById(userId);
        Map<String, String> data = Map.of("message","탈퇴되었습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // defaultView 수정
    @PutMapping("/view")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateDefaultView(
            @RequestBody Map<String, String> body, HttpServletRequest httpReq,
            Authentication authentication){

        Long userId = Long.parseLong(authentication.getName());
        String viewStr = body.get("defaultView");

        User.DefaultView view;
        try {
            view = User.DefaultView.valueOf(viewStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VIEW",
                    "잘못된 뷰 입니다. (DAY, WEEK, MONTH 중 선택)"
            );
        }

        userService.updateDefaultView(userId, view);

        Map<String, String> data = Map.of("message", "기본 뷰가 " + view + "으로 변경되었습니다.");
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }

    // 회원 조회(me)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(Authentication authentication,
                                                                  HttpServletRequest httpReq) {
        Long userId = Long.parseLong(authentication.getName());
        Map<String, Object> data = userService.getMe(userId); // Service 호출
        return ResponseEntity.ok(ApiResponse.ok(data, httpReq.getRequestURI()));
    }
}
