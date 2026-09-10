package com.flyship.repository;

import com.flyship.entity.Dispute;
import com.flyship.entity.Dispute.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByFiledByUserIdOrRespondentUserIdOrderByCreatedAtDesc(Long filedByUserId, Long respondentUserId);
    List<Dispute> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(SubjectType subjectType, Long subjectId);
}
