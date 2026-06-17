package roomescape.controller.admin;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.common.auth.Login;
import roomescape.common.auth.LoginMember;
import roomescape.controller.dto.request.ReservationRequest;
import roomescape.controller.dto.response.ReservationResponse;
import roomescape.service.ReservationService;
import roomescape.service.dto.command.ManagerReservationCommand;
import roomescape.service.dto.result.ReservationResult;

@RestController
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> read(@Login LoginMember member) {
        List<ReservationResponse> response = reservationService.findReservationsByManager(member.id()).stream()
                .map(ReservationResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ReservationRequest request,
                                                      @Login LoginMember member) {
        ReservationResult result = reservationService.changeReservationByManager(
                member.id(), id, ManagerReservationCommand.from(request));

        return ResponseEntity.ok(ReservationResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @Login LoginMember member) {
        reservationService.removeReservationByManager(member.id(), id);

        return ResponseEntity.noContent().build();
    }
}
