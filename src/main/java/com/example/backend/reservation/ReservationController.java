package com.example.backend.reservation;

import com.example.backend.reservation.domain.ConfirmedReservation;
import com.example.backend.reservation.domain.Reservation;
import com.example.backend.reservation.dto.ReservationDto;
import com.example.backend.reservation.dto.TimetableResponse;
import com.example.backend.user.UserService;
import com.example.backend.user.domain.User;
import com.example.backend.video.domain.Video;
import com.example.backend.reservation.dto.VideoTitleSearchResponse;
import com.example.backend.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/run/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private final VideoService videoService;
    private final UserService userService;

    @PostMapping("")
    public ResponseEntity<String> registerVideoReservation(@RequestBody ReservationDto reservationDto){
        Long loginId=1L;
        User user = userService.findUserById(loginId);
        Video video = videoService.findVideoEntityById(reservationDto.getId());
        if(video==null)
            return new ResponseEntity<>("잘못된 영상에 대한 예약 요청", HttpStatus.NOT_FOUND);
        LocalDate date=LocalDate.parse(reservationDto.getReservationDate());
        Reservation reservation = Reservation.builder()
                .user(user).video(video)
                .sTime(reservationDto.getStartTime())
                .eTime(reservationDto.getEndTime())
                .date(date)
                .build();
        boolean existence= reservationService.findReservationByDateAndVideoAndUser(date, video, user)!=null; //존재하면 true, 아니면 false

        boolean hasConfirmed=reservationService.findConfirmedReservation(date,video,reservationDto.getStartTime(),reservationDto.getEndTime())!=null; //존재하면 true, 아니면 flase
        if(hasConfirmed){
            return new ResponseEntity<>("이미 해당 시간에 예약이 확정되어 있는 영상",HttpStatus.OK);
        }

        if(existence){
            return new ResponseEntity<>("중복 예약 불가",HttpStatus.CONFLICT);
        }

        Boolean conflict = reservationService.checkReservationConflict(reservation);
        if(conflict){
            return new ResponseEntity<>("선택 불가능한 예약시간",HttpStatus.CONFLICT);
        }

        reservationService.saveReservation(reservation);
        return new ResponseEntity<>("예약 등록 성공",HttpStatus.CREATED);
    }


    @GetMapping("/title/search")
    public ResponseEntity<List<VideoTitleSearchResponse>> findVideoContainingTitle(@RequestParam String q){
        List<Video> videos = videoService.findVideoContainingTitle(q, "likeCount");
        List<VideoTitleSearchResponse> searchResponses = videos.stream()
                .map(VideoTitleSearchResponse::fromEntity)
                .limit(5)
                .collect(Collectors.toList());
        return new ResponseEntity<>(searchResponses,HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<List<TimetableResponse>> getReservationTimetable(@RequestParam String start, @RequestParam String end,
                                                                           @RequestParam String select, @RequestParam String date){
//        System.out.println("start = " + start + ", end = " + end + ", select = " + select+", date = "+date);
        //시간 순 정렬
        return new ResponseEntity<>(reservationService.test(start, end, select, date),HttpStatus.OK);
    }
}
