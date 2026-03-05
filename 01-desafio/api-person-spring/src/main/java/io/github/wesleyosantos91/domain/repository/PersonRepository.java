package io.github.wesleyosantos91.domain.repository;

import io.github.wesleyosantos91.domain.entity.PersonEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonRepository extends JpaRepository<PersonEntity, UUID>, JpaSpecificationExecutor<PersonEntity> {

    Optional<PersonEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}