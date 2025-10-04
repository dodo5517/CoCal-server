package cola.springboot.cocal.invite;

import cola.springboot.cocal.invite.dto.InviteCreateRequest;
import cola.springboot.cocal.invite.dto.InviteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {

    private final InviteService inviteService;

    // 초대 생성
    @PostMapping("/invite")
    public ResponseEntity<InviteResponse> create(@Valid @RequestBody InviteCreateRequest req,
                                                 Authentication auth) {
        Long inviterUserId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(inviteService.createInvite(inviterUserId, req));
    }
}
