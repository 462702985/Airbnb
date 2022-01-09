package com.laioffer.staybooking.service;

import com.laioffer.staybooking.entity.Reservation;
import com.laioffer.staybooking.entity.Stay;
import com.laioffer.staybooking.entity.User;
import com.laioffer.staybooking.exception.ReservationCollisionException;
import com.laioffer.staybooking.exception.ReservationNotFoundException;
import com.laioffer.staybooking.repository.ReservationRepository;
import com.laioffer.staybooking.repository.StayAvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    private ReservationRepository reservationRepository;
    private StayAvailabilityRepository stayAvailabilityRepository;
    @Autowired
    public ReservationService(ReservationRepository reservationRepository, StayAvailabilityRepository stayAvailabilityRepository) {
        this.reservationRepository = reservationRepository;
        this.stayAvailabilityRepository = stayAvailabilityRepository;
    }

    public List<Reservation> listByGuest(String username) {
        return reservationRepository.findByGuest(new User.Builder().setUsername(username).build());
    }

    public List<Reservation> listByStay(long stayId) {
        return reservationRepository.findByStay(new Stay.Builder().setId(stayId).build());
    }
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void add(Reservation reservation) {
        List<LocalDate> dates = stayAvailabilityRepository.countByDateBetweenAndId(
                reservation.getStay().getId(), reservation.getCheckinDate(), reservation.getCheckoutDate().minusDays(1));
        int duration = (int)Duration.between(reservation.getCheckinDate().atStartOfDay(), reservation.getCheckoutDate().atStartOfDay()).toDays();
        if (dates.size() != duration) {
            throw new ReservationCollisionException("Reservation collision");
        }
        stayAvailabilityRepository.reserveByDateBetweenAndId(reservation.getId(), reservation.getCheckinDate(), reservation.getCheckoutDate().minusDays(1));
        reservationRepository.save(reservation);
    }
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(Long reservationId) throws ReservationNotFoundException {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new ReservationNotFoundException("Reservaton is not available"));
        stayAvailabilityRepository.cancelByDateBetweenAndId(reservation.getStay().getId(), reservation.getCheckinDate(), reservation.getCheckoutDate().minusDays(1));
        reservationRepository.deleteById(reservationId);
    }
}
