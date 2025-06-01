package tw.yukina.thinkorbit.service.event.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    Page<EventEntity> findByIdGreaterThanEqualAndOccurredAtGreaterThanEqual(Long fromEventId, Instant fromTime, Pageable pageable);
    Page<EventEntity> findByIdGreaterThanEqual(Long fromEventId, Pageable pageable);
    Page<EventEntity> findByOccurredAtGreaterThanEqual(Instant fromTime, Pageable pageable);
    Page<EventEntity> findAll(Pageable pageable);
} 