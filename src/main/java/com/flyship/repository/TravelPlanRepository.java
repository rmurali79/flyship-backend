package com.flyship.repository;

import com.flyship.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {
    List<TravelPlan> findByTravelerIdOrderByStartDateAsc(Long travelerId);
    List<TravelPlan> findByTravelerId(Long travelerId);
}
